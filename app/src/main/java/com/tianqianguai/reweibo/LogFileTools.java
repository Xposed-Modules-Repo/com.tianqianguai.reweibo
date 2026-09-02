package com.tianqianguai.reweibo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Streaming log inspection, filtering, preview, and export shared by UI and ADB CLI. */
public final class LogFileTools {
    public static final int UI_PREVIEW_MAX_CHARS = 256 * 1024;
    public static final int CLI_PREVIEW_MAX_CHARS = 48 * 1024;
    private static final long NO_TIMESTAMP = Long.MIN_VALUE;
    private static final long LEGACY_TIMESTAMP = Long.MIN_VALUE + 1L;
    private static final String[] DATE_TIME_PATTERNS = new String[] {
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd_HH-mm-ss.SSS",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd_HH-mm-ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd_HH-mm",
        "yyyy-MM-dd"
    };

    private LogFileTools() {}

    public static final class Range {
        public final long startMs;
        public final long endMs;
        public final boolean hasStart;
        public final boolean hasEnd;

        private Range(long startMs, long endMs, boolean hasStart, boolean hasEnd) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.hasStart = hasStart;
            this.hasEnd = hasEnd;
        }

        public boolean isAll() {
            return !hasStart && !hasEnd;
        }

        public String describe() {
            if (isAll()) return "all";
            String start = hasStart ? formatDisplayTimestamp(startMs) : "-infinity";
            String end = hasEnd ? formatDisplayTimestamp(endMs) : "+infinity";
            return start + " .. " + end;
        }
    }

    /** Immutable byte boundary captured between complete synchronized log appends. */
    public static final class Snapshot {
        public final File file;
        public final long length;

        private Snapshot(File file, long length) {
            this.file = file;
            this.length = length;
        }
    }

    public static final class Result {
        public final String text;
        public final long fileBytes;
        public final long totalLines;
        public final long matchedLines;
        public final long legacyLines;
        public final long skippedLegacyLines;
        public final long outputBytes;
        public final long firstTimestampMs;
        public final long lastTimestampMs;
        public final boolean truncated;

        private Result(
                String text,
                long fileBytes,
                long totalLines,
                long matchedLines,
                long legacyLines,
                long skippedLegacyLines,
                long outputBytes,
                long firstTimestampMs,
                long lastTimestampMs,
                boolean truncated
        ) {
            this.text = text;
            this.fileBytes = fileBytes;
            this.totalLines = totalLines;
            this.matchedLines = matchedLines;
            this.legacyLines = legacyLines;
            this.skippedLegacyLines = skippedLegacyLines;
            this.outputBytes = outputBytes;
            this.firstTimestampMs = firstTimestampMs;
            this.lastTimestampMs = lastTimestampMs;
            this.truncated = truncated;
        }
    }

    public static Range parseRange(String startText, String endText) {
        String start = trimToEmpty(startText);
        String end = trimToEmpty(endText);
        boolean hasStart = !start.isEmpty();
        boolean hasEnd = !end.isEmpty();
        long startMs = hasStart ? parseBound(start, false) : Long.MIN_VALUE;
        long endMs = hasEnd ? parseBound(end, true) : Long.MAX_VALUE;
        if (hasStart && startMs < 0L) {
            throw new IllegalArgumentException(
                "invalid start; use yyyy-MM-dd or yyyy-MM-dd_HH-mm[-ss]"
            );
        }
        if (hasEnd && endMs < 0L) {
            throw new IllegalArgumentException(
                "invalid end; use yyyy-MM-dd or yyyy-MM-dd_HH-mm[-ss]"
            );
        }
        if (startMs > endMs) {
            throw new IllegalArgumentException("start must not be later than end");
        }
        return new Range(startMs, endMs, hasStart, hasEnd);
    }

    public static String formatLogTimestamp(long timeMs) {
        return formatter("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(timeMs));
    }

    public static String formatDisplayTimestamp(long timeMs) {
        return formatter("yyyy-MM-dd HH:mm:ss").format(new Date(timeMs));
    }

    public static Result inspect(File file) throws IOException {
        return inspect(snapshot(file));
    }

    public static Result inspect(Snapshot snapshot) throws IOException {
        return scan(snapshot, parseRange(null, null), 0, null);
    }

    public static Result readPreview(File file, Range range, int maxChars) throws IOException {
        return readPreview(snapshot(file), range, maxChars);
    }

    public static Result readPreview(Snapshot snapshot, Range range, int maxChars)
            throws IOException {
        if (maxChars <= 0) throw new IllegalArgumentException("maxChars must be positive");
        return scan(requireSnapshot(snapshot), requireRange(range), maxChars, null);
    }

    public static Result writeFiltered(File file, Range range, OutputStream output)
            throws IOException {
        return writeFiltered(snapshot(file), range, output);
    }

    public static Result writeFiltered(
            Snapshot snapshot,
            Range range,
            OutputStream output
    ) throws IOException {
        if (output == null) throw new NullPointerException("output");
        return scan(requireSnapshot(snapshot), requireRange(range), 0, output);
    }

    public static Snapshot snapshot(File file) throws IOException {
        File source = requireReadableFile(file);
        return new Snapshot(source, source.length());
    }

    private static Result scan(Snapshot snapshot, Range range, int maxChars, OutputStream output)
            throws IOException {
        File source = requireReadableFile(snapshot.file);
        long snapshotLength = Math.min(snapshot.length, source.length());
        ArrayDeque<String> preview = maxChars > 0 ? new ArrayDeque<>() : null;
        int previewChars = 0;
        boolean truncated = false;
        long totalLines = 0L;
        long matchedLines = 0L;
        long legacyLines = 0L;
        long skippedLegacyLines = 0L;
        long outputBytes = 0L;
        long firstTimestampMs = 0L;
        long lastTimestampMs = 0L;
        boolean includeContinuation = false;

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new BoundedInputStream(new FileInputStream(source), snapshotLength),
                    StandardCharsets.UTF_8
                ),
                64 * 1024
            );
            BufferedWriter writer = output == null ? null : new BufferedWriter(
                new OutputStreamWriter(output, StandardCharsets.UTF_8),
                64 * 1024
            )
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                long timestamp = parseLineTimestamp(line);
                boolean include;
                if (timestamp == LEGACY_TIMESTAMP) {
                    legacyLines++;
                    include = range.isAll();
                    if (!include) skippedLegacyLines++;
                } else if (timestamp == NO_TIMESTAMP) {
                    include = range.isAll() || includeContinuation;
                } else {
                    if (firstTimestampMs == 0L || timestamp < firstTimestampMs) {
                        firstTimestampMs = timestamp;
                    }
                    if (timestamp > lastTimestampMs) lastTimestampMs = timestamp;
                    include = timestamp >= range.startMs && timestamp <= range.endMs;
                }
                includeContinuation = include;
                if (!include) continue;

                matchedLines++;
                String rendered = line + '\n';
                outputBytes += rendered.getBytes(StandardCharsets.UTF_8).length;
                if (writer != null) writer.write(rendered);
                if (preview == null) continue;

                if (rendered.length() > maxChars) {
                    preview.clear();
                    rendered = rendered.substring(rendered.length() - maxChars);
                    previewChars = 0;
                    truncated = true;
                }
                preview.addLast(rendered);
                previewChars += rendered.length();
                while (previewChars > maxChars && preview.size() > 1) {
                    previewChars -= preview.removeFirst().length();
                    truncated = true;
                }
            }
            if (writer != null) writer.flush();
        }

        String text = "";
        if (preview != null && !preview.isEmpty()) {
            StringBuilder value = new StringBuilder(Math.min(previewChars, maxChars));
            for (String line : preview) value.append(line);
            text = value.toString();
        }
        return new Result(
            text,
            snapshotLength,
            totalLines,
            matchedLines,
            legacyLines,
            skippedLegacyLines,
            outputBytes,
            firstTimestampMs,
            lastTimestampMs,
            truncated
        );
    }

    private static long parseLineTimestamp(String line) {
        if (line == null) return NO_TIMESTAMP;
        if (line.length() >= 23
                && line.charAt(4) == '-'
                && line.charAt(7) == '-'
                && line.charAt(10) == ' '
                && line.charAt(13) == ':'
                && line.charAt(16) == ':'
                && line.charAt(19) == '.') {
            long parsed = parseExact(line.substring(0, 23), "yyyy-MM-dd HH:mm:ss.SSS");
            if (parsed >= 0L) return parsed;
        }
        if (line.length() >= 19
                && line.charAt(4) == '-'
                && line.charAt(7) == '-'
                && line.charAt(10) == ' '
                && line.charAt(13) == ':'
                && line.charAt(16) == ':') {
            long parsed = parseExact(line.substring(0, 19), "yyyy-MM-dd HH:mm:ss");
            if (parsed >= 0L) return parsed;
        }
        if (line.length() >= 8
                && line.charAt(2) == ':'
                && line.charAt(5) == ':'
                && parseExact(line.substring(0, 8), "HH:mm:ss") >= 0L) {
            return LEGACY_TIMESTAMP;
        }
        return NO_TIMESTAMP;
    }

    private static long parseBound(String value, boolean end) {
        for (String pattern : DATE_TIME_PATTERNS) {
            long parsed = parseExact(value, pattern);
            if (parsed < 0L) continue;
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(parsed);
            if ("yyyy-MM-dd".equals(pattern)) {
                calendar.set(Calendar.HOUR_OF_DAY, end ? 23 : 0);
                calendar.set(Calendar.MINUTE, end ? 59 : 0);
                calendar.set(Calendar.SECOND, end ? 59 : 0);
                calendar.set(Calendar.MILLISECOND, end ? 999 : 0);
            } else if (pattern.endsWith("HH:mm") || pattern.endsWith("HH-mm")) {
                calendar.set(Calendar.SECOND, end ? 59 : 0);
                calendar.set(Calendar.MILLISECOND, end ? 999 : 0);
            } else if (pattern.endsWith("HH:mm:ss") || pattern.endsWith("HH-mm-ss")) {
                calendar.set(Calendar.MILLISECOND, end ? 999 : 0);
            }
            return calendar.getTimeInMillis();
        }
        return -1L;
    }

    private static long parseExact(String value, String pattern) {
        SimpleDateFormat format = formatter(pattern);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        Date parsed = format.parse(value, position);
        return parsed != null && position.getIndex() == value.length()
            ? parsed.getTime()
            : -1L;
    }

    private static SimpleDateFormat formatter(String pattern) {
        return new SimpleDateFormat(pattern, Locale.US);
    }

    private static Range requireRange(Range range) {
        if (range == null) throw new NullPointerException("range");
        return range;
    }

    private static Snapshot requireSnapshot(Snapshot snapshot) {
        if (snapshot == null) throw new NullPointerException("snapshot");
        return snapshot;
    }

    private static File requireReadableFile(File file) throws IOException {
        if (file == null) throw new IOException("log file is unavailable");
        if (!file.isFile()) throw new IOException("log file does not exist: " + file.getAbsolutePath());
        if (!file.canRead()) throw new IOException("log file is not readable: " + file.getAbsolutePath());
        return file;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class BoundedInputStream extends InputStream {
        private final InputStream delegate;
        private long remaining;

        BoundedInputStream(InputStream delegate, long remaining) {
            this.delegate = delegate;
            this.remaining = Math.max(0L, remaining);
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0L) return -1;
            int value = delegate.read();
            if (value >= 0) remaining--;
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0L) return -1;
            int allowed = (int) Math.min((long) length, remaining);
            int read = delegate.read(buffer, offset, allowed);
            if (read > 0) remaining -= read;
            return read;
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
