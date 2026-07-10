package com.tianqianguai.reweibo;

import android.net.Uri;

public final class ModuleSettings {
    public static final String PREFS_NAME = "reweibo_settings";
    public static final String PROVIDER_AUTHORITY = "com.tianqianguai.reweibo.settings";
    public static final String SETTINGS_PATH = "settings";
    public static final String KEY_WEICO_PROFILE_ENTRY = "weico_profile_entry";
    public static final String KEY_WEICO_TIMELINE_CACHE_DAYS = "weico_timeline_cache_days";
    public static final int DEFAULT_WEICO_TIMELINE_CACHE_DAYS = 3;
    public static final int MIN_WEICO_TIMELINE_CACHE_DAYS = 1;
    public static final int MAX_WEICO_TIMELINE_CACHE_DAYS = 3;

    private ModuleSettings() {}

    public static boolean defaultFor(String key) {
        if (KEY_WEICO_PROFILE_ENTRY.equals(key)) {
            return true;
        }
        return false;
    }

    public static int defaultIntFor(String key) {
        if (KEY_WEICO_TIMELINE_CACHE_DAYS.equals(key)) {
            return DEFAULT_WEICO_TIMELINE_CACHE_DAYS;
        }
        return 0;
    }

    public static int clampTimelineCacheDays(int days) {
        if (days < MIN_WEICO_TIMELINE_CACHE_DAYS) return MIN_WEICO_TIMELINE_CACHE_DAYS;
        if (days > MAX_WEICO_TIMELINE_CACHE_DAYS) return MAX_WEICO_TIMELINE_CACHE_DAYS;
        return days;
    }

    public static Uri settingsUriFor(String key) {
        return Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + SETTINGS_PATH + "/" + key);
    }
}
