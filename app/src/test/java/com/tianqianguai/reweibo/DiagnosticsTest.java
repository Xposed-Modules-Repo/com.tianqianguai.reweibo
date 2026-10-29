package com.tianqianguai.reweibo;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class DiagnosticsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @After public void stop() { Diagnostics.stop(); }

    private File file() { return new File(temporary.getRoot(), "reweibo.log"); }
    private String read() throws Exception {
        Diagnostics.flushOnWorker();
        return LogFileTools.readPreview(file(), LogFileTools.parseRange(null, null), 100_000).text;
    }

    @Test public void disabledAndNormalLoggingDoNotEvaluateDetailedDescriptions() {
        AtomicInteger descriptions = new AtomicInteger();
        for (int i = 0; i < 1000; i++) {
            if (Diagnostics.shouldLog("Timeline detailed status")) descriptions.incrementAndGet();
        }
        assertEquals(0, descriptions.get());
        Diagnostics.start(file(), new Object(), "test");
        assertFalse(Diagnostics.shouldLog("Timeline detailed status"));
        assertTrue(Diagnostics.startDetailed());
        assertTrue(Diagnostics.shouldLog("Timeline detailed status"));
        assertFalse(Diagnostics.shouldLog("Timeline detailed status")); // rate limited
        Diagnostics.stopDetailed();
        assertFalse(Diagnostics.shouldLog("Timeline another detailed status"));
        assertTrue(Diagnostics.shouldLog("Timeline error"));
    }

    @Test public void detailExpiresWithoutChangingPersistentLogSwitch() throws Exception {
        Diagnostics.start(file(), new Object(), "test");
        Diagnostics.startDetailed();
        Field deadline = Diagnostics.class.getDeclaredField("detailUntilNanos");
        deadline.setAccessible(true);
        deadline.setLong(null, System.nanoTime() - 1);
        assertFalse(Diagnostics.detailed());
        assertTrue(Diagnostics.enabled());
        assertEquals(600_000, Diagnostics.DETAIL_MAX_MS);
    }

    @Test public void hookTimingIncludesPostProcessingAndRestoresParent() throws Exception {
        Diagnostics.start(file(), new Object(), "test");
        Diagnostics.startDetailed();
        Long original = Diagnostics.enter(42);
        Diagnostics.Span span = Diagnostics.begin("hook", "addData");
        span.beforeEnd = System.nanoTime();
        span.afterStart = System.nanoTime();
        Thread.sleep(40); // Simulated expensive after-hook, previously absent from addData exit timing.
        Diagnostics.endHook(span, null);
        assertEquals(42, Diagnostics.context());
        Diagnostics.leave(original);
        String text = read();
        assertTrue(text.contains("event=addData parent=42"));
        java.util.regex.Matcher duration = java.util.regex.Pattern.compile("after_ms=(\\d+)").matcher(text);
        assertTrue(duration.find());
        assertTrue(Long.parseLong(duration.group(1)) >= 30);
    }

    @Test public void asyncTasksKeepParentAndRethrowOriginalException() throws Exception {
        Diagnostics.start(file(), new Object(), "test");
        Diagnostics.startDetailed();
        Long original = Diagnostics.enter(123);
        RuntimeException expected = new IllegalStateException("test");
        Runnable task = Diagnostics.wrap(() -> { assertEquals(123, Diagnostics.context()); throw expected; }, 0);
        Diagnostics.leave(original);
        AtomicReference<Throwable> actual = new AtomicReference<>();
        Thread thread = new Thread(() -> { try { task.run(); } catch (Throwable error) { actual.set(error); } });
        thread.start(); thread.join(1000);
        assertSame(expected, actual.get());
        assertTrue(read().contains("parent=123"));
    }

    @Test public void writerDoesNotBlockProducerOnFileLockAndBoundsQueue() throws Exception {
        Object lock = new Object();
        AsyncDiagnosticWriter writer = new AsyncDiagnosticWriter(file(), lock, 8, 4096, 3);
        try {
            AtomicInteger accepted = new AtomicInteger();
            synchronized (lock) {
                Thread producer = new Thread(() -> {
                    for (int i = 0; i < 100; i++) if (writer.offer("record " + i)) accepted.incrementAndGet();
                });
                producer.start(); producer.join(1000);
                assertFalse("Producer waited for the writer's file lock", producer.isAlive());
                assertEquals(8, accepted.get());
                assertEquals(8, writer.pending());
                assertEquals(92, writer.dropped());
            }
            writer.flush();
            assertEquals(0, writer.pending());
        } finally { writer.close(); }
        assertTrue(Files.readString(file().toPath()).contains("dropped_total=92"));
    }

    @Test public void rotationPreservesLegacyAndLimitsNewSegments() throws Exception {
        Files.writeString(file().toPath(), "12:00:00 old history\n");
        AsyncDiagnosticWriter writer = new AsyncDiagnosticWriter(file(), new Object(), 32, 160, 3);
        try {
            for (int i = 0; i < 30; i++) {
                writer.offer("2026-10-18 12:00:00.000 entry=" + i + " padding-padding-padding");
                writer.flush();
            }
        } finally { writer.close(); }
        assertEquals(1, temporary.getRoot().listFiles((dir, name) -> name.contains(".legacy-")).length);
        for (int i = 0; i <= 3; i++) {
            File segment = new File(file().getPath() + (i == 0 ? "" : "." + i));
            assertTrue(segment.exists()); assertTrue(segment.length() <= 160);
        }
        assertFalse(new File(file().getPath() + ".4").exists());
        String all = LogFileTools.readPreview(file(), LogFileTools.parseRange(null, null), 4096).text;
        assertTrue(all.startsWith("12:00:00 old history"));
        assertTrue(all.contains("entry=29"));
        assertFalse(all.contains("entry=0 "));
        LogFileTools.Result ranged = LogFileTools.readPreview(file(), LogFileTools.parseRange("2026-10-18", "2026-10-18"), 4096);
        assertFalse(ranged.text.contains("old history"));
        assertEquals(1, ranged.skippedLegacyLines);
    }

    @Test public void snapshotRetainsOpenedSegmentsAcrossRenameAndAppend() throws Exception {
        File first = new File(file().getPath() + ".1");
        Files.writeString(first.toPath(), "2026-10-18 12:00:00.000 first\n");
        Files.writeString(file().toPath(), "2026-10-18 12:01:00.000 second\n");
        try (LogFileTools.Snapshot snapshot = LogFileTools.snapshot(file())) {
            Files.move(first.toPath(), new File(temporary.getRoot(), "moved").toPath());
            Files.writeString(file().toPath(), "2026-10-18 12:02:00.000 late\n", StandardOpenOption.APPEND);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            LogFileTools.Result result = LogFileTools.writeFiltered(snapshot, LogFileTools.parseRange(null, null), bytes);
            assertEquals(2, result.totalLines);
            assertTrue(bytes.toString("UTF-8").contains("first"));
            assertTrue(bytes.toString("UTF-8").contains("second"));
            assertFalse(bytes.toString("UTF-8").contains("late"));
        }
    }

    @Test public void errorsAreCountedAndCrashHandlerIsDelegated() throws Exception {
        File nonDirectory = temporary.newFile("not-a-dir");
        AsyncDiagnosticWriter broken = new AsyncDiagnosticWriter(new File(nonDirectory, "log"), new Object(), 8, 1000, 3);
        try {
            broken.offer("entry"); broken.flush(); assertTrue(broken.failures() > 0);
            assertEquals(0, broken.written()); assertEquals(1, broken.unconfirmed());
        }
        finally { broken.close(); }
        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        AtomicInteger delegated = new AtomicInteger();
        Thread.UncaughtExceptionHandler previous = (thread, error) -> delegated.incrementAndGet();
        Thread.setDefaultUncaughtExceptionHandler(previous);
        try {
            Diagnostics.start(file(), new Object(), "test");
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new IllegalStateException());
            assertEquals(1, delegated.get());
            Diagnostics.stop();
            assertSame(previous, Thread.getDefaultUncaughtExceptionHandler());
        } finally { Thread.setDefaultUncaughtExceptionHandler(original); }
    }

    @Test public void sensitiveQueryAndCredentialValuesAreRedacted() throws Exception {
        AsyncDiagnosticWriter writer = new AsyncDiagnosticWriter(file(), new Object());
        try { writer.offer("url=https://example.test/path?access_token=private password=hidden token=secret"); writer.flush(); }
        finally { writer.close(); }
        String saved = Files.readString(file().toPath());
        assertFalse(saved.contains("private")); assertFalse(saved.contains("hidden")); assertFalse(saved.contains("secret"));
        assertTrue(saved.contains("[redacted]"));
    }

    @Test public void performanceSamplingIsThresholdedAndRefreshRateAware() {
        assertFalse(PerformanceMonitor.shouldSampleStall(999, 0, 2000));
        assertTrue(PerformanceMonitor.shouldSampleStall(1000, 0, 2000));
        assertFalse(PerformanceMonitor.shouldSampleStall(5000, 3, 2000));
        assertFalse(PerformanceMonitor.shouldSampleStall(5000, 1, 1999));
        assertTrue(PerformanceMonitor.isSlowFrame(20_000_000L, 8_333_333L));
        assertFalse(PerformanceMonitor.isSlowFrame(20_000_000L, 16_666_667L));
    }

    @Test public void closeStopsAdmissionAndFlushesAcceptedRecords() throws Exception {
        Object lock = new Object();
        AsyncDiagnosticWriter writer = new AsyncDiagnosticWriter(file(), lock, 32, 4096, 3);
        Thread closing;
        synchronized (lock) {
            assertTrue(writer.offer("accepted before close"));
            closing = new Thread(writer::close);
            closing.start();
            long deadline = System.nanoTime() + 1_000_000_000L;
            boolean stopped = false;
            while (System.nanoTime() < deadline) {
                if (!writer.offer("closing")) { stopped = true; break; }
                Thread.yield();
            }
            assertTrue(stopped);
        }
        closing.join(2000);
        assertFalse(closing.isAlive());
        assertFalse(writer.offer("must reject"));
        assertEquals(0, writer.pending());
        assertTrue(Files.readString(file().toPath()).contains("accepted before close"));
    }

    @Test public void reportProducerCostWithDisabledNormalAndDetailedModes() throws Exception {
        final int count = 20000;
        long disabled = benchmark(count);
        Object fileLock = new Object();
        Diagnostics.start(file(), fileLock, "benchmark");
        long normal = benchmark(count);
        long stageStart = System.nanoTime();
        for (int i = 0; i < count; i++) Diagnostics.end(Diagnostics.begin("benchmark", "stage"), null);
        long stages = System.nanoTime() - stageStart;
        Diagnostics.startDetailed();
        long detailed = benchmark(count);
        Diagnostics.flushOnWorker();
        long events;
        synchronized (fileLock) {
            long started = System.nanoTime();
            for (int i = 0; i < 2000; i++) Diagnostics.event("benchmark", "record", "size=16000", true);
            events = System.nanoTime() - started;
        }
        System.out.println("diagnostic_benchmark iterations=" + count + " disabled_ns_per_call=" + disabled / count
            + " normal_ns_per_call=" + normal / count + " detailed_ns_per_call=" + detailed / count
            + " normal_span_ns=" + stages / count + " event_enqueue_ns=" + events / 2000 + " " + Diagnostics.status());
        // This is a measurement, not a device FPS claim; deterministic non-blocking behavior is tested above.
    }

    private long benchmark(int count) {
        long started = System.nanoTime();
        for (int i = 0; i < count; i++) {
            if (Diagnostics.shouldLog("Timeline benchmark")) Diagnostics.legacy("Timeline benchmark size=16000");
        }
        return System.nanoTime() - started;
    }
}
