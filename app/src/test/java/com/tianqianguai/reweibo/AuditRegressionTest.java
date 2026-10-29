package com.tianqianguai.reweibo;

import android.content.SharedPreferences;
import com.tianqianguai.reweibo.compat.HookTestDriver;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.io.File;
import java.util.*;
import static org.junit.Assert.*;

public class AuditRegressionTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Object oldFeatures;
    private Field features;
    @Before public void before() throws Exception {
        features = WeiboLiteHook.class.getDeclaredField("sFeatures");
        features.setAccessible(true);
        oldFeatures = features.get(null);
    }
    @After public void after() throws Exception {
        features.set(null, oldFeatures);
        Diagnostics.stop();
    }

    @Test public void onlyTheShellModuleAndScopedHostCanAccessCommands() {
        for (int uid : new int[]{0, 2000, 10123, 10456}) assertTrue(IpcAccess.isTrustedUid(uid, 10123, 10456));
        for (int uid : new int[]{1000, 10555, 110123, -1}) assertFalse(IpcAccess.isTrustedUid(uid, 10123, 10456));
        assertFalse(IpcAccess.isTrustedUid(10456, 10123, -1));
    }

    @Test public void resetPersistsAnExplicitDefaultInsteadOfTriggeringLegacyMigration() {
        Map<String, Object> stored = new HashMap<>();
        stored.put(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS, 30);
        stored.put(ModuleSettings.KEY_WEICO_FEED_REVERSE, false);
        SharedPreferences.Editor editor = (SharedPreferences.Editor) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{SharedPreferences.Editor.class}, (proxy, method, args) -> {
                if (method.getName().startsWith("put")) { stored.put((String) args[0], args[1]); return proxy; }
                if (method.getName().equals("commit")) return true;
                throw new AssertionError(method.getName());
            });
        SharedPreferences prefs = (SharedPreferences) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{SharedPreferences.class}, (proxy, method, args) -> {
                if (method.getName().equals("edit")) return editor;
                throw new AssertionError(method.getName());
            });
        SettingsProvider.resetPreference(prefs, ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS);
        SettingsProvider.resetPreference(prefs, ModuleSettings.KEY_WEICO_FEED_REVERSE);
        assertEquals(3, stored.get(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS));
        assertEquals(true, stored.get(ModuleSettings.KEY_WEICO_FEED_REVERSE));
        assertTrue(stored.containsKey(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS));
    }

    public Object isRefreshing() { return true; }
    public Object addData() { return null; }

    @Test public void installedRefreshHookHonorsAdsOnlyAndEnabledModes() throws Throwable {
        Method method = getClass().getMethod("isRefreshing");
        features.set(null, ModuleSettings.featureSnapshot(ModuleSettings.onlyAdsPreset()));
        assertEquals(true, HookTestDriver.invoke(method, WeiboLiteHook.pullRefreshStateHook(), true));
        features.set(null, ModuleSettings.featureSnapshot(Collections.emptyMap()));
        assertEquals(false, HookTestDriver.invoke(method, WeiboLiteHook.pullRefreshStateHook(), true));
    }

    @Test public void beforeFailureFallsBackToHostAndRestoresDiagnosticParent() throws Throwable {
        features.set(null, ModuleSettings.featureSnapshot(Collections.emptyMap()));
        Diagnostics.start(new File(temporary.getRoot(), "events.log"), new Object(), "test");
        Long previous = Diagnostics.enter(77L);
        try {
            WeiboLiteHook.FeatureHook failing = new WeiboLiteHook.FeatureHook(ModuleSettings.KEY_WEICO_TIMELINE_CACHE) {
                @Override protected void beforeEnabled(MethodHookParam param) { throw new IllegalStateException("fixture"); }
                @Override protected void afterEnabled(MethodHookParam param) { fail("after must be skipped by the bridge"); }
            };
            assertEquals("host", HookTestDriver.invoke(getClass().getMethod("addData"), failing, "host"));
            assertEquals(77L, Diagnostics.context());
            Diagnostics.Span next = Diagnostics.begin("hook", "next");
            assertEquals(77L, next.parent);
            Diagnostics.end(next, null);
            assertEquals(77L, Diagnostics.context());
        } finally { Diagnostics.leave(previous); }
    }

    @Test public void lateSnapshotWinsEvenAfterPresenterWasReplaced() throws Exception {
        Status old = new Status(1), fresh = new Status(2), updated = new Status(1);
        Presenter presenter = new Presenter(Collections.singletonList(old));
        Method snapshot = WeiboLiteHook.class.getDeclaredMethod("snapshotTimelineStatuses", Object.class, List.class);
        snapshot.setAccessible(true);
        assertEquals(Arrays.asList(fresh, updated), snapshot.invoke(null, presenter, Arrays.asList(fresh, updated)));
        assertEquals(Collections.singletonList(old), snapshot.invoke(null, presenter, null));
    }

    @Test public void oldTimeJumpTokenStopsBeforeItTouchesTheHost() throws Exception {
        features.set(null, ModuleSettings.featureSnapshot(Collections.emptyMap()));
        Field generation = WeiboLiteHook.class.getDeclaredField("sTimelineEdgeJumpGeneration");
        generation.setAccessible(true);
        int original = generation.getInt(null);
        try {
            generation.setInt(null, 21);
            Method jump = WeiboLiteHook.class.getDeclaredMethod("jumpTimelineToTime", Object.class,
                long.class, long.class, long.class, String.class, int.class, int.class);
            jump.setAccessible(true);
            assertEquals(false, jump.invoke(null, new Object(), 1L, 0L, 0L, "old", 1, 20));
        } finally { generation.setInt(null, original); }
    }

    @Test public void keyboardAndDpadUseTheHostFocusWindowWhileTouchKeepsPanels() {
        assertTrue(WeiboLiteHook.useTimelineShortcutPanel(
            android.content.res.Configuration.KEYBOARD_NOKEYS,
            android.content.res.Configuration.NAVIGATION_NONAV));
        assertFalse(WeiboLiteHook.useTimelineShortcutPanel(
            android.content.res.Configuration.KEYBOARD_QWERTY,
            android.content.res.Configuration.NAVIGATION_NONAV));
        assertFalse(WeiboLiteHook.useTimelineShortcutPanel(
            android.content.res.Configuration.KEYBOARD_NOKEYS,
            android.content.res.Configuration.NAVIGATION_DPAD));
    }

    public static final class Status {
        private final long id;
        Status(long id) { this.id = id; }
        public long getId() { return id; }
    }
    public static final class Presenter {
        private final List<Status> statuses;
        Presenter(List<Status> statuses) { this.statuses = statuses; }
        public List<Status> getStatusList() { return statuses; }
    }
}
