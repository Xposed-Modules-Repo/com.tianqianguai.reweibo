package com.tianqianguai.reweibo;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class TimelineAccountTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @After public void clear() { WeiboLiteHook.activateTimelineAccount(null); }
    private TimelineAccountFiles account(String id) {
        return new TimelineAccountFiles(id, new File(temporary.getRoot(), "native-" + id + ".txt"), temporary.getRoot());
    }

    @Test public void distinctAccountsCannotReadEachOthersShadowOrBookmarks() throws Exception {
        TimelineAccountFiles a = account("123"), b = account("456");
        a.directory.mkdirs();
        Files.write(a.file("shadow.txt").toPath(), "private-to-a".getBytes(StandardCharsets.UTF_8));
        assertFalse(b.file("shadow.txt").exists());
        assertNotEquals(a.file("last-read"), b.file("last-read"));
        File meta = new File(temporary.getRoot(), "legacy.meta");
        Files.write(meta.toPath(), "file_name=native-123.txt\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(a.ownsLegacyShadow("legacy.meta"));
        assertFalse(b.ownsLegacyShadow("legacy.meta"));
        assertFalse(a.ownsLegacyShadow("missing.meta"));
    }

    @Test public void switchingClearsMemoryAndReturningToSameUidRejectsOldTasks() throws Exception {
        TimelineAccountFiles a = account("123"), b = account("456"), aAgain = account("123");
        WeiboLiteHook.activateTimelineAccount(a);
        Field data = WeiboLiteHook.class.getDeclaredField("sTimelineCumulativeStatusesById");
        data.setAccessible(true);
        Map cache = (Map) data.get(null);
        cache.put(1L, "account-a");
        WeiboLiteHook.activateTimelineAccount(b);
        assertTrue(cache.isEmpty());
        AtomicInteger calls = new AtomicInteger();
        WeiboLiteHook.runTimelineTask(a, calls::incrementAndGet);
        assertEquals(0, calls.get());
        WeiboLiteHook.activateTimelineAccount(aAgain);
        WeiboLiteHook.runTimelineTask(a, calls::incrementAndGet);
        assertEquals(0, calls.get());
        WeiboLiteHook.runTimelineTask(aAgain, calls::incrementAndGet);
        assertEquals(1, calls.get());
    }

    @Test public void runningIoRetainsItsPathsWhileNewQueueWorkUsesNewAccount() throws Exception {
        TimelineAccountFiles a = account("123"), b = account("456");
        Method path = WeiboLiteHook.class.getDeclaredMethod("timelineAccountFile", String.class);
        path.setAccessible(true);
        WeiboLiteHook.activateTimelineAccount(a);
        WeiboLiteHook.runTimelineTask(a, () -> {
            WeiboLiteHook.activateTimelineAccount(b);
            try {
                assertEquals(a.file("shadow"), path.invoke(null, "shadow"));
                WeiboLiteHook.runTimelineTask(b, () -> {
                    try { assertEquals(b.file("shadow"), path.invoke(null, "shadow")); }
                    catch (Exception error) { throw new AssertionError(error); }
                });
                assertEquals(a.file("shadow"), path.invoke(null, "shadow"));
            } catch (Exception error) { throw new AssertionError(error); }
        });
        assertEquals(b.file("shadow"), path.invoke(null, "shadow"));
    }

    @Test public void firstAccountDoesNotCancelCurrentViewInitialization() {
        TimelineAccountFiles a = account("123"), b = account("456");
        assertTrue(WeiboLiteHook.shouldApplyTimelineViewInitialization(null, a, true));
        assertFalse(WeiboLiteHook.shouldApplyTimelineViewInitialization(null, a, false));
        assertFalse(WeiboLiteHook.shouldApplyTimelineViewInitialization(a, b, true));
        assertTrue(WeiboLiteHook.shouldApplyTimelineViewInitialization(a, a, true));
    }

    @Test public void migrationNeverPairsAnExistingShadowWithLegacyMetadata() throws Exception {
        TimelineAccountFiles a = account("123");
        Files.write(new File(temporary.getRoot(), "legacy.meta").toPath(),
            "file_name=native-123.txt\ncount=999\n".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(temporary.getRoot(), "shadow").toPath(), new byte[]{1});
        assertTrue(a.canMigrateLegacyShadow("shadow", "legacy.meta"));
        a.directory.mkdirs();
        Files.write(a.file("shadow").toPath(), new byte[]{2});
        assertFalse(a.canMigrateLegacyShadow("shadow", "legacy.meta"));
        assertFalse(a.file("legacy.meta").exists());
    }

    @Test public void accountSwitchTerminatesOldCliOperationAndRejectsLateSuccess() throws Exception {
        TimelineAccountFiles a = account("123"), b = account("456");
        WeiboLiteHook.activateTimelineAccount(a);
        Method begin = privateMethod("beginCliOperation", String.class, String.class);
        Method finish = privateMethod("finishCliOperation", String.class, String.class, String.class);
        String oldId = (String) begin.invoke(null, "cache.stats", "scheduled");
        WeiboLiteHook.activateTimelineAccount(b);
        assertEquals("error", field("sCliLastOperationState").get(null));
        finish.invoke(null, oldId, "completed", "late account-a result");
        assertEquals("error", field("sCliLastOperationState").get(null));
        String newId = (String) begin.invoke(null, "cache.clear", "scheduled");
        finish.invoke(null, oldId, "error", "cancelled");
        assertEquals(newId, field("sCliLastOperationId").get(null));
        assertEquals("accepted", field("sCliLastOperationState").get(null));
    }

    @Test public void oldNativeEnqueueCannotOverwriteNewAccountPendingWrite() throws Exception {
        verifyQueueRace(true);
    }

    @Test public void oldShadowEnqueueCannotOverwriteNewAccountPendingWrite() throws Exception {
        verifyQueueRace(false);
    }

    private void verifyQueueRace(boolean nativeWrite) throws Exception {
        TimelineAccountFiles a = account("123"), b = account("456");
        Field features = field("sFeatures");
        Object oldFeatures = features.get(null);
        features.set(null, ModuleSettings.featureSnapshot(java.util.Collections.emptyMap()));
        Object queueLock = field("sTimelinePersistQueueLock").get(null);
        Field scheduled = field("sTimelinePersistWorkerScheduled");
        Field pending = field(nativeWrite ? "sPendingTimelineNativePersist" : "sPendingTimelineShadowPersist");
        Class<?> requestType = Class.forName(WeiboLiteHook.class.getName()
            + (nativeWrite ? "$TimelineNativePersistRequest" : "$TimelineShadowPersistRequest"));
        java.lang.reflect.Constructor<?> constructor = requestType.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Method enqueue = privateMethod(nativeWrite ? "enqueueTimelineNativeCachePersist"
            : "enqueueTimelineShadowCachePersist", requestType);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        WeiboLiteHook.activateTimelineAccount(a);
        Object oldRequest = newRequest(constructor, a, nativeWrite);
        Thread stale = new Thread(() -> {
            try { enqueue.invoke(null, oldRequest); }
            catch (Throwable error) { failure.set(error); }
        });
        try {
            Object newRequest;
            synchronized (queueLock) {
                scheduled.setBoolean(null, true); // Keep this deterministic; don't launch the drainer.
                stale.start();
                long deadline = System.nanoTime() + 2_000_000_000L;
                while (stale.getState() != Thread.State.BLOCKED && stale.isAlive()
                    && System.nanoTime() < deadline) Thread.sleep(1);
                assertEquals(Thread.State.BLOCKED, stale.getState());
                WeiboLiteHook.activateTimelineAccount(b);
                newRequest = newRequest(constructor, b, nativeWrite);
                enqueue.invoke(null, newRequest);
            }
            stale.join(2000);
            assertFalse(stale.isAlive());
            assertNull(failure.get());
            assertSame(newRequest, pending.get(null));
        } finally {
            stale.join(2000);
            synchronized (queueLock) { pending.set(null, null); scheduled.setBoolean(null, false); }
            features.set(null, oldFeatures);
        }
    }

    private Object newRequest(java.lang.reflect.Constructor<?> constructor,
        TimelineAccountFiles account, boolean nativeWrite) throws Exception {
        return nativeWrite
            ? constructor.newInstance(new Object(), new Object(), account.nativeFile, null, 5, 1L,
                "1", "test", false, null)
            : constructor.newInstance("test", 5, "1", false, null);
    }

    private static Field field(String name) throws Exception {
        Field field = WeiboLiteHook.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Method privateMethod(String name, Class<?>... types) throws Exception {
        Method method = WeiboLiteHook.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }
}
