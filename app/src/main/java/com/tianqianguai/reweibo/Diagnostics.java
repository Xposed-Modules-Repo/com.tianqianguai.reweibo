package com.tianqianguai.reweibo;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;

/** Small constant-time producer API. No UI objects, status lists or filesystem queries in events. */
final class Diagnostics {
    static final long DETAIL_MAX_MS = 10 * 60 * 1000L;
    private static volatile AsyncDiagnosticWriter writer;
    private static volatile boolean enabled;
    private static volatile long detailUntilNanos;
    private static volatile String session = "none";
    private static volatile String process = "unknown";
    private static volatile String page = "none";
    private static volatile long uiOperation;
    private static volatile long uiOperationAt;
    private static final AtomicLong sequence = new AtomicLong();
    private static final AtomicLong sampledOut = new AtomicLong();
    private static final ThreadLocal<Long> operation = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, Long> lastEvents = new ConcurrentHashMap<>();
    private static Thread.UncaughtExceptionHandler previousCrashHandler;
    private static Thread.UncaughtExceptionHandler crashHandler;

    private Diagnostics() {}

    static synchronized void start(File file, Object lock, String processName) {
        if (writer != null) return;
        process = clean(processName);
        session = Long.toHexString(System.currentTimeMillis()) + "-" + Long.toHexString(System.nanoTime());
        writer = new AsyncDiagnosticWriter(file, lock);
        enabled = true;
        event("session", "start", "retention=4x4MiB legacy=preserved", true);
        previousCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (previousCrashHandler != null) {
            final Thread.UncaughtExceptionHandler delegate = previousCrashHandler;
            crashHandler = (thread, error) -> {
                try {
                    StringBuilder stack = new StringBuilder(error.getClass().getName());
                    StackTraceElement[] trace = error.getStackTrace();
                    for (int i = 0; i < Math.min(24, trace.length); i++) stack.append(" <- ").append(trace[i]);
                    event("process", "uncaught_exception", stack.toString(), true);
                    AsyncDiagnosticWriter current = writer;
                    if (current != null) current.flushAfterCrash();
                } finally { delegate.uncaughtException(thread, error); }
            };
            Thread.setDefaultUncaughtExceptionHandler(crashHandler);
        }
    }

    static synchronized void stop() {
        enabled = false;
        detailUntilNanos = 0;
        AsyncDiagnosticWriter previous = writer;
        writer = null;
        if (previous != null) previous.close();
        if (crashHandler != null && Thread.getDefaultUncaughtExceptionHandler() == crashHandler) {
            Thread.setDefaultUncaughtExceptionHandler(previousCrashHandler);
        }
        crashHandler = null;
        operation.remove();
        lastEvents.clear();
    }

    static boolean enabled() { return enabled; }
    static boolean busy() { AsyncDiagnosticWriter current = writer; return current != null && current.busy(); }
    static void pause() { enabled = false; }
    static void resume() { enabled = writer != null; }
    static boolean detailed() { return enabled && System.nanoTime() < detailUntilNanos; }

    static boolean startDetailed() {
        if (!enabled) return false;
        detailUntilNanos = System.nanoTime() + DETAIL_MAX_MS * 1_000_000L;
        event("diagnostics", "detail_start", "duration_ms=" + DETAIL_MAX_MS, true);
        return true;
    }

    static void stopDetailed() {
        detailUntilNanos = 0;
        event("diagnostics", "detail_stop", "", true);
    }

    static String status() {
        AsyncDiagnosticWriter current = writer;
        return "enabled=" + enabled + " detailed=" + detailed()
            + " detail_remaining_ms=" + Math.max(0, (detailUntilNanos - System.nanoTime()) / 1_000_000L)
            + " session=" + session + " pending=" + (current == null ? 0 : current.pending())
            + " dropped=" + (current == null ? 0 : current.dropped())
            + " write_failures=" + (current == null ? 0 : current.failures())
            + " unconfirmed=" + (current == null ? 0 : current.unconfirmed())
            + " written=" + (current == null ? 0 : current.written())
            + " writer_ms=" + (current == null ? 0 : current.writeNanos() / 1_000_000L)
            + " sampled_out=" + sampledOut.get();
    }

    static void flushOnWorker() { AsyncDiagnosticWriter current = writer; if (current != null) current.flush(); }
    static String currentPage() { return page; }

    static void network(long parent, String source, int probe, String stage, long started, Throwable error) {
        if (!enabled) return;
        long elapsed = (System.nanoTime() - started) / 1_000_000L;
        if (error == null && elapsed < 1000 && !detailed()) return;
        Long previous = enter(parent);
        try {
            event("network", stage, "source=" + source + " probe=" + probe
                + " observable_age_ms=" + elapsed + " error=" + (error == null ? "none" : error.getClass().getName()), true);
        } finally { leave(previous); }
    }

    /** Run this BEFORE evaluating legacy log arguments, especially reflection/list summaries. */
    static boolean shouldLog(String eventName) {
        if (!enabled) return false;
        boolean important = eventName.contains("error") || eventName.contains("failed")
            || eventName.contains("completed") || eventName.contains("stopped")
            || eventName.contains("restored") || eventName.contains("settings")
            || eventName.contains("hook installed");
        if (!important && !detailed()) return false;
        return allow(eventName, important ? 1000 : 250);
    }

    static boolean allow(String key, long intervalMs) {
        if (!enabled) return false;
        long now = System.nanoTime();
        Long previous = lastEvents.putIfAbsent(key, now);
        if (previous == null) return true;
        if (now - previous < intervalMs * 1_000_000L) { sampledOut.incrementAndGet(); return false; }
        return lastEvents.replace(key, previous, now);
    }

    static void legacy(String message) {
        String module = message.startsWith("Timeline") ? "timeline"
            : message.startsWith("Video") ? "video" : "hook";
        event(module, "detail", message, true);
    }

    static void event(String module, String event, String fields, boolean important) {
        AsyncDiagnosticWriter current = writer;
        if (!enabled || current == null || (!important && !detailed())) return;
        current.offer(System.currentTimeMillis(), "session=" + session + " process=" + process
            + " thread=" + clean(Thread.currentThread().getName()) + " page=" + page
            + " op=" + context() + " correlation=" + (operation.get() != null ? "explicit" : context() == 0 ? "none" : "recent_ui")
            + " module=" + clean(module) + " event=" + clean(event)
            + " " + clean(fields));
    }

    static long context() {
        Long current = operation.get();
        if (current != null) return current;
        return System.nanoTime() - uiOperationAt < 10_000_000_000L ? uiOperation : 0;
    }

    static Long enter(long id) { Long previous = operation.get(); operation.set(id); return previous; }
    static void leave(Long previous) { if (previous == null) operation.remove(); else operation.set(previous); }

    static void page(String name, String transition) {
        if (!enabled) return;
        page = clean(name);
        uiOperation = sequence.incrementAndGet();
        uiOperationAt = System.nanoTime();
        event("ui", transition, "", true);
    }

    static void interaction(String name) {
        if (!enabled) return;
        uiOperation = sequence.incrementAndGet();
        uiOperationAt = System.nanoTime();
        event("ui", name, "", false);
    }

    static Span begin(String module, String name) {
        if (!enabled) return null;
        Span span = new Span(module, name);
        event(module, name + "_begin", "parent=" + span.parent, false);
        return span;
    }

    static void end(Span span, Throwable error) {
        if (span == null) return;
        long elapsed = System.nanoTime() - span.start;
        try {
            if ((error != null || elapsed >= 32_000_000L || detailed())
                    && allow("span:" + span.module + ":" + span.name, detailed() ? 0 : 1000)) {
                event(span.module, span.name + "_end", "parent=" + span.parent
                    + " page_at_start=" + span.pageAtStart
                    + " elapsed_ms=" + elapsed / 1_000_000L
                    + " error=" + (error == null ? "none" : error.getClass().getName()), true);
            }
        } finally { leave(span.previous); }
    }

    static void endHook(Span span, Throwable error) {
        if (span == null) return;
        long now = System.nanoTime();
        try {
            if (span.name.equals("showData") && allow("content-applied", 1000)) {
                event("ui", "content_applied", "parent=" + span.parent
                    + " total_ms=" + (now - span.start) / 1_000_000L, true);
            }
            if ((error != null || now - span.start >= 32_000_000L || detailed())
                    && allow("hook:" + span.name, detailed() ? 0 : 1000)) {
                event("hook", span.name, "parent=" + span.parent
                    + " page_at_start=" + span.pageAtStart
                    + " before_ms=" + Math.max(0, span.beforeEnd - span.start) / 1_000_000L
                    + " host_ms=" + Math.max(0, span.afterStart - span.beforeEnd) / 1_000_000L
                    + " after_ms=" + Math.max(0, now - span.afterStart) / 1_000_000L
                    + " total_ms=" + (now - span.start) / 1_000_000L
                    + " error=" + (error == null ? "none" : error.getClass().getName()), true);
            }
        } finally { leave(span.previous); }
    }

    static void task(String name, long id, long queueNanos, long runNanos, Throwable error) {
        if (!enabled || (error == null && queueNanos < 100_000_000L && runNanos < 32_000_000L && !detailed())) return;
        if (!allow("task:" + name, 1000)) return;
        event("scheduler", "task", "task=" + clean(name) + " parent=" + id
            + " queue_ms=" + queueNanos / 1_000_000L + " run_ms=" + runNanos / 1_000_000L
            + " error=" + (error == null ? "none" : error.getClass().getName()), true);
    }

    static Runnable wrap(Runnable task, long delayMs) {
        if (!enabled) return task;
        final long parent = context();
        final long due = System.nanoTime() + Math.max(0, delayMs) * 1_000_000L;
        final String name = task.getClass().getName();
        return () -> {
            long started = System.nanoTime();
            Long previous = enter(parent);
            Throwable failure = null;
            try { task.run(); }
            catch (RuntimeException | Error error) { failure = error; throw error; }
            finally {
                task(name, parent, Math.max(0, started - due), System.nanoTime() - started, failure);
                leave(previous);
            }
        };
    }

    static final class Span {
        final String module, name;
        final String pageAtStart = page;
        final long start = System.nanoTime();
        final long parent = context();
        final long id = sequence.incrementAndGet();
        final Long previous;
        long beforeEnd;
        long afterStart;
        Throwable failure;
        Span(String module, String name) { this.module = module; this.name = name; previous = enter(id); }
    }

    static String clean(String value) {
        if (value == null) return "none";
        // Values are names/counts, never Intent extras, URLs, request headers or full content.
        String bounded = value.length() > 3500 ? value.substring(0, 3500) : value;
        return bounded.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }
}
