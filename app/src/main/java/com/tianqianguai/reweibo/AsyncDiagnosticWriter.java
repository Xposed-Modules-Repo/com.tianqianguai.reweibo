package com.tianqianguai.reweibo;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

/** Producers never touch storage or wait for the file lock. Only this worker appends/rotates. */
final class AsyncDiagnosticWriter implements Closeable {
    static final int QUEUE_CAPACITY = 1024;
    static final int MAX_LINE_CHARS = 4096;
    static final long SEGMENT_BYTES = 4L * 1024 * 1024;
    static final int ARCHIVES = 3;
    private final File file;
    private final Object fileLock;
    private final long segmentBytes;
    private final int archives;
    private final ArrayBlockingQueue<Entry> queue;
    private final ScheduledExecutorService worker;
    private BufferedOutputStream output;
    private long bytes;
    private volatile boolean closed;
    private volatile boolean writing;
    private final Object admissionLock = new Object();
    private boolean accepting = true;
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong unconfirmed = new AtomicLong();
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong writeNanos = new AtomicLong();
    private long reportedDropped;
    private static final Pattern URL_QUERY = Pattern.compile("(https?://[^\\s?]+)\\?[^\\s]*");
    private static final Pattern SECRET = Pattern.compile("(?i)((?:authorization|access_token|refresh_token|password|cookie|token)[=:]\\s*)(?:Bearer\\s+)?[^\\s,;]+");

    AsyncDiagnosticWriter(File file, Object lock) {
        this(file, lock, QUEUE_CAPACITY, SEGMENT_BYTES, ARCHIVES);
    }

    AsyncDiagnosticWriter(File file, Object lock, int capacity, long segmentBytes, int archives) {
        this.file = file;
        fileLock = lock;
        this.segmentBytes = segmentBytes;
        this.archives = archives;
        queue = new ArrayBlockingQueue<>(capacity);
        worker = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "ReWeibo-diagnostic-writer");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
        worker.scheduleWithFixedDelay(this::drainSafely, 0, 250, TimeUnit.MILLISECONDS);
    }

    boolean offer(String line) {
        return offer(0L, line);
    }

    boolean offer(long wallTime, String line) {
        String bounded = line.length() > MAX_LINE_CHARS
            ? line.substring(0, MAX_LINE_CHARS) + " [truncated]" : line;
        synchronized (admissionLock) {
            if (!accepting) return false;
            if (queue.offer(new Entry(wallTime, bounded))) return true;
            dropped.incrementAndGet();
            return false;
        }
    }

    int pending() { return queue.size(); }
    long dropped() { return dropped.get(); }
    long failures() { return failures.get(); }
    long unconfirmed() { return unconfirmed.get(); }
    long written() { return written.get(); }
    long writeNanos() { return writeNanos.get(); }
    boolean busy() { return writing || !queue.isEmpty(); }

    /** Called from log-read workers only; never from an input/layout callback. */
    void flush() {
        // Flush the bounded backlog observed by this reader, not an endlessly growing producer stream.
        int batches = Math.max(1, (queue.size() + 127) / 128);
        for (int i = 0; i < batches; i++) drainSafely();
    }

    void flushAfterCrash() {
        if (closed) return;
        try {
            Future<?> pending = worker.submit(this::drainSafely);
            pending.get(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
        catch (Exception ignored) { /* The original crash handler must always run. */ }
    }

    private void drainSafely() {
        long started = System.nanoTime();
        synchronized (fileLock) {
            if (closed) return;
            ArrayList<Entry> batch = new ArrayList<>(128);
            queue.drainTo(batch, 128);
            long lost = dropped.get();
            if (batch.isEmpty() && lost == reportedDropped) return;
            writing = true;
            try {
                open();
                if (lost != reportedDropped) {
                    append(LogFileTools.formatLogTimestamp(System.currentTimeMillis())
                        + " module=logger event=overflow dropped_total=" + lost);
                }
                for (Entry entry : batch) append(entry.wallTime == 0L ? entry.line
                    : LogFileTools.formatLogTimestamp(entry.wallTime) + " " + entry.line);
                output.flush();
                written.addAndGet(batch.size() + (lost != reportedDropped ? 1 : 0));
                reportedDropped = lost;
            } catch (IOException error) {
                failures.incrementAndGet();
                unconfirmed.addAndGet(batch.size());
                try { if (output != null) output.close(); } catch (IOException ignored) {}
                output = null;
            } finally {
                writeNanos.addAndGet(System.nanoTime() - started);
                writing = false;
            }
        }
    }

    private void open() throws IOException {
        if (output != null) return;
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("cannot create log directory");
        // A sidecar marks the new rotation policy. Existing unbounded logs are never silently removed.
        File marker = new File(file.getPath() + ".rotation-v1");
        if (!marker.exists()) {
            if (file.isFile() && file.length() > 0) {
                File legacy = new File(file.getPath() + ".legacy-" + System.currentTimeMillis());
                if (!file.renameTo(legacy)) throw new IOException("cannot preserve legacy log");
            }
            if (!marker.createNewFile() && !marker.exists()) throw new IOException("cannot mark log policy");
        }
        bytes = file.length();
        output = new BufferedOutputStream(new FileOutputStream(file, true), 32768);
    }

    private void append(String line) throws IOException {
        line = SECRET.matcher(URL_QUERY.matcher(line).replaceAll("$1?[redacted]")).replaceAll("$1[redacted]");
        byte[] encoded = (line + "\n").getBytes(StandardCharsets.UTF_8);
        if (bytes > 0 && bytes + encoded.length > segmentBytes) rotate();
        output.write(encoded);
        bytes += encoded.length;
    }

    private void rotate() throws IOException {
        output.close();
        output = null;
        File oldest = new File(file.getPath() + "." + archives);
        if (oldest.exists() && !oldest.delete()) throw new IOException("cannot expire old segment");
        for (int i = archives - 1; i >= 1; i--) {
            File from = new File(file.getPath() + "." + i);
            if (from.exists() && !from.renameTo(new File(file.getPath() + "." + (i + 1)))) {
                throw new IOException("cannot rotate segment");
            }
        }
        if (file.exists() && !file.renameTo(new File(file.getPath() + ".1"))) {
            throw new IOException("cannot rotate current log");
        }
        bytes = 0;
        output = new BufferedOutputStream(new FileOutputStream(file, true), 32768);
    }

    @Override public void close() {
        synchronized (admissionLock) { accepting = false; }
        worker.shutdown();
        // Called by the background reload coordinator, not by UI cleanup.
        while (!queue.isEmpty()) drainSafely();
        synchronized (fileLock) {
            closed = true;
            try { if (output != null) output.close(); } catch (IOException error) { failures.incrementAndGet(); }
            output = null;
        }
    }

    private static final class Entry {
        final long wallTime;
        final String line;
        Entry(long wallTime, String line) { this.wallTime = wallTime; this.line = line; }
    }
}
