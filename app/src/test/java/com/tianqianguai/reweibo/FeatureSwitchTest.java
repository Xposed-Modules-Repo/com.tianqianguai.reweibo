package com.tianqianguai.reweibo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

public class FeatureSwitchTest {
    private Field featureField;
    private Object previous;

    @Before public void saveFeatures() throws Exception {
        featureField = WeiboLiteHook.class.getDeclaredField("sFeatures");
        featureField.setAccessible(true);
        previous = featureField.get(null);
        featureField.set(null, ModuleSettings.featureSnapshot(ModuleSettings.onlyAdsPreset()));
    }

    @After public void restoreFeatures() throws Exception {
        featureField.set(null, previous);
    }

    @Test public void existingInstallKeepsEveryDefaultFeatureAndCacheWindow() {
        ModuleSettings.FeatureSnapshot defaults = ModuleSettings.featureSnapshot(Collections.emptyMap());
        for (String key : ModuleSettings.booleanKeys()) {
            assertTrue(key, defaults.isEnabled(key));
            assertTrue(key, defaults.isEffective(key));
            assertNotEquals(key, ModuleSettings.titleFor(key));
        }
        assertEquals(3, ModuleSettings.defaultIntFor(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS));
    }

    @Test public void adsPresetDisablesEveryNonAdFeatureIncludingEntrances() {
        Map<String, Boolean> preset = ModuleSettings.onlyAdsPreset();
        assertEquals(ModuleSettings.booleanKeys().length, preset.size());
        ModuleSettings.FeatureSnapshot snapshot = ModuleSettings.featureSnapshot(preset);
        for (String key : ModuleSettings.booleanKeys()) {
            boolean ad = key.equals(ModuleSettings.KEY_WEICO_SPLASH_AD_REMOVAL)
                || key.equals(ModuleSettings.KEY_WEICO_TIMELINE_AD_REMOVAL);
            assertEquals(key, ad, snapshot.isEffective(key));
        }
        assertFalse(preset.containsKey(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS));
    }

    @Test public void cacheDependencyDoesNotErasePreferencesOrDisableManualNavigation() {
        Map<String, Boolean> preferences = new LinkedHashMap<>();
        preferences.put(ModuleSettings.KEY_WEICO_TIMELINE_CACHE, false);
        ModuleSettings.FeatureSnapshot snapshot = ModuleSettings.featureSnapshot(preferences);
        for (String key : new String[] {ModuleSettings.KEY_WEICO_TIMELINE_PRELOAD,
                ModuleSettings.KEY_WEICO_TIMELINE_GAP_FILL, ModuleSettings.KEY_WEICO_TIMELINE_LAST_READ}) {
            assertTrue(snapshot.isEnabled(key));
            assertFalse(snapshot.isEffective(key));
        }
        assertTrue(snapshot.isEffective(ModuleSettings.KEY_WEICO_TIMELINE_JUMP));
        preferences.put(ModuleSettings.KEY_WEICO_TIMELINE_CACHE, true);
        assertTrue(ModuleSettings.featureSnapshot(preferences).isEffective(ModuleSettings.KEY_WEICO_TIMELINE_GAP_FILL));
        assertFalse(snapshot.isEffective(ModuleSettings.KEY_WEICO_TIMELINE_CACHE));
    }

    @Test public void hotReloadKeepsActiveSnapshotAndAcceptsLegacyState() {
        ModuleSettings.FeatureSnapshot active = ModuleSettings.featureSnapshot(ModuleSettings.onlyAdsPreset());
        Object legacy = HotReloadState.compose(null, null, null, null, null, 5L);
        assertTrue(HotReloadState.isValid(legacy, getClass().getClassLoader()));
        Object state = HotReloadState.withFeatures(legacy, active.toArray());
        assertTrue(HotReloadState.isValid(state, getClass().getClassLoader()));
        boolean[] restored = HotReloadState.features(state);
        assertArrayEquals(active.toArray(), restored);
        restored[0] = !restored[0];
        assertArrayEquals(active.toArray(), HotReloadState.features(state));
        assertEquals(5L, HotReloadState.previousGeneration(state));
    }

    @Test public void adsOnlyFiltersAdsWithoutReorderingOrRepairingOtherStatuses() throws Exception {
        Status first = new Status(1, false), ad = new Status(3, true), last = new Status(2, false);
        List<Status> feed = new ArrayList<>(Arrays.asList(first, ad, last));
        Object filtered = call("filterTimelineAds", new Class<?>[] {List.class, Object.class, String.class}, feed, new Owner(), "test");
        assertEquals(Arrays.asList(first, last), filtered);
        assertSame(filtered, call("ensureNewestFirst", new Class<?>[] {List.class, Object.class, String.class, boolean.class}, filtered, new Owner(), "test", false));
        assertSame(filtered, call("filterTimelineContentless", new Class<?>[] {List.class, Object.class, String.class}, filtered, new Owner(), "test"));
        assertEquals(0, call("hydrateTimelineStatusText", new Class<?>[] {List.class, String.class}, filtered, "test"));
        assertEquals(0, first.textReads + last.textReads);
    }

    @Test public void turningOffAdRemovalPassesOriginalListAndAdPredicateThrough() throws Exception {
        Map<String, Boolean> config = new LinkedHashMap<>(ModuleSettings.onlyAdsPreset());
        config.put(ModuleSettings.KEY_WEICO_TIMELINE_AD_REMOVAL, false);
        featureField.set(null, ModuleSettings.featureSnapshot(config));
        Status ad = new Status(9, true);
        List<Status> feed = Collections.singletonList(ad);
        assertSame(feed, call("filterTimelineAds", new Class<?>[] {List.class, Object.class, String.class}, feed, new Owner(), "test"));
        assertEquals(false, call("isTimelineAdStatus", new Class<?>[] {Object.class}, ad));
    }

    @Test public void disabledNavigationCacheAndReadingReturnBeforeTouchingRuntime() throws Exception {
        Object sentinel = new Object();
        assertEquals(false, call("scheduleTimelineCacheRestore",
            new Class<?>[] {Object.class, List.class, String.class, boolean.class}, sentinel, Collections.emptyList(), "test", false));
        assertEquals(false, call("jumpTimelineToAbsoluteEdge",
            new Class<?>[] {Object.class, String.class, int.class, boolean.class}, sentinel, "test", 0, true));
        assertEquals(0L, call("getLastReadStatusId", new Class<?>[0]));
        call("scheduleTimelinePreload", new Class<?>[] {Object.class, String.class}, sentinel, "test");
        call("scheduleTimelineGapFill", new Class<?>[] {Object.class, String.class}, sentinel, "test");
    }

    @Test public void contentlessFilteringDoesNotOverrideDisabledAdRemoval() throws Exception {
        Map<String, Boolean> config = new LinkedHashMap<>(ModuleSettings.onlyAdsPreset());
        config.put(ModuleSettings.KEY_WEICO_TIMELINE_AD_REMOVAL, false);
        config.put(ModuleSettings.KEY_WEICO_CONTENTLESS_FILTER, true);
        featureField.set(null, ModuleSettings.featureSnapshot(config));
        Status ad = new Status(9, true), empty = new Status(10, false);
        assertEquals(false, call("isTimelineContentlessStatus", new Class<?>[] {Object.class}, ad));
        assertEquals(true, call("isTimelineContentlessStatus", new Class<?>[] {Object.class}, empty));
    }

    private static Object call(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = WeiboLiteHook.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    public static class Owner { public String getGroupId() { return "-1"; } }
    public static class Status {
        public final boolean isad;
        private final long id;
        public int textReads;
        Status(long id, boolean ad) { this.id = id; isad = ad; }
        public long getId() { return id; }
        public String getText() { textReads++; return ""; }
    }
}
