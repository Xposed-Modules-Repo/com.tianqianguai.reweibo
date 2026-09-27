package com.tianqianguai.reweibo;

import android.net.Uri;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleSettings {
    public static final String PREFS_NAME = "reweibo_settings";
    public static final String PROVIDER_AUTHORITY = "com.tianqianguai.reweibo.settings";
    public static final String SETTINGS_PATH = "settings";

    public static final String KEY_WEICO_PROFILE_ENTRY = "weico_profile_entry";
    public static final String KEY_WEICO_SPLASH_AD_REMOVAL = "weico_splash_ad_removal";
    public static final String KEY_WEICO_TIMELINE_AD_REMOVAL = "weico_timeline_ad_removal";
    public static final String KEY_WEICO_CONTENTLESS_FILTER = "weico_contentless_filter";
    public static final String KEY_WEICO_FEED_REVERSE = "weico_feed_reverse";
    public static final String KEY_WEICO_TIMELINE_CACHE = "weico_timeline_cache";
    public static final String KEY_WEICO_TIMELINE_PRELOAD = "weico_timeline_preload";
    public static final String KEY_WEICO_TIMELINE_GAP_FILL = "weico_timeline_gap_fill";
    public static final String KEY_WEICO_TIMELINE_LAST_READ = "weico_timeline_last_read";
    public static final String KEY_WEICO_TIMELINE_JUMP = "weico_timeline_jump";
    public static final String KEY_WEICO_STATUS_HYDRATION = "weico_status_hydration";
    public static final String KEY_WEICO_VIDEO_REFRESH = "weico_video_refresh";
    public static final String KEY_WEICO_DISABLE_PULL_REFRESH = "weico_disable_pull_refresh";
    public static final String KEY_WEICO_PERSISTENT_LOGS = "weico_persistent_logs";
    public static final String KEY_WEICO_TIMELINE_JUMP_BUTTON = "weico_timeline_jump_button";
    public static final String KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON = "weico_timeline_cache_clear_button";
    public static final String KEY_WEICO_TIMELINE_CACHE_DAYS = "weico_timeline_cache_days";

    public static final int DEFAULT_WEICO_TIMELINE_CACHE_DAYS = 3;
    public static final int MIN_WEICO_TIMELINE_CACHE_DAYS = 1;
    public static final int MAX_WEICO_TIMELINE_CACHE_DAYS = 30;

    private static final String[] BOOLEAN_KEYS = new String[] {
        KEY_WEICO_PROFILE_ENTRY,
        KEY_WEICO_SPLASH_AD_REMOVAL,
        KEY_WEICO_TIMELINE_AD_REMOVAL,
        KEY_WEICO_CONTENTLESS_FILTER,
        KEY_WEICO_FEED_REVERSE,
        KEY_WEICO_TIMELINE_CACHE,
        KEY_WEICO_TIMELINE_PRELOAD,
        KEY_WEICO_TIMELINE_GAP_FILL,
        KEY_WEICO_TIMELINE_LAST_READ,
        KEY_WEICO_TIMELINE_JUMP,
        KEY_WEICO_STATUS_HYDRATION,
        KEY_WEICO_VIDEO_REFRESH,
        KEY_WEICO_DISABLE_PULL_REFRESH,
        KEY_WEICO_PERSISTENT_LOGS,
        KEY_WEICO_TIMELINE_JUMP_BUTTON,
        KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON
    };
    private static final String[] ALL_KEYS = new String[] {
        KEY_WEICO_PROFILE_ENTRY,
        KEY_WEICO_SPLASH_AD_REMOVAL,
        KEY_WEICO_TIMELINE_AD_REMOVAL,
        KEY_WEICO_CONTENTLESS_FILTER,
        KEY_WEICO_FEED_REVERSE,
        KEY_WEICO_TIMELINE_CACHE,
        KEY_WEICO_TIMELINE_PRELOAD,
        KEY_WEICO_TIMELINE_GAP_FILL,
        KEY_WEICO_TIMELINE_LAST_READ,
        KEY_WEICO_TIMELINE_JUMP,
        KEY_WEICO_STATUS_HYDRATION,
        KEY_WEICO_VIDEO_REFRESH,
        KEY_WEICO_DISABLE_PULL_REFRESH,
        KEY_WEICO_PERSISTENT_LOGS,
        KEY_WEICO_TIMELINE_JUMP_BUTTON,
        KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON,
        KEY_WEICO_TIMELINE_CACHE_DAYS
    };

    public static String titleFor(String key) {
        if (KEY_WEICO_PROFILE_ENTRY.equals(key)) return "我的页 ReWeibo 入口";
        if (KEY_WEICO_SPLASH_AD_REMOVAL.equals(key)) return "移除启动广告";
        if (KEY_WEICO_TIMELINE_AD_REMOVAL.equals(key)) return "移除信息流广告";
        if (KEY_WEICO_CONTENTLESS_FILTER.equals(key)) return "过滤无正文内容";
        if (KEY_WEICO_FEED_REVERSE.equals(key)) return "倒序浏览";
        if (KEY_WEICO_TIMELINE_CACHE.equals(key)) return "扩展缓存";
        if (KEY_WEICO_TIMELINE_PRELOAD.equals(key)) return "自动预加载";
        if (KEY_WEICO_TIMELINE_GAP_FILL.equals(key)) return "补齐信息流断层";
        if (KEY_WEICO_TIMELINE_LAST_READ.equals(key)) return "阅读位置记忆";
        if (KEY_WEICO_TIMELINE_JUMP.equals(key)) return "双击与时间跳转";
        if (KEY_WEICO_STATUS_HYDRATION.equals(key)) return "补齐正文与媒体";
        if (KEY_WEICO_VIDEO_REFRESH.equals(key)) return "刷新过期视频链接";
        if (KEY_WEICO_DISABLE_PULL_REFRESH.equals(key)) return "禁用下拉刷新";
        if (KEY_WEICO_TIMELINE_JUMP_BUTTON.equals(key)) return "显示“跳转”按钮";
        if (KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON.equals(key)) return "显示“删除”按钮";
        if (KEY_WEICO_PERSISTENT_LOGS.equals(key)) return "持久诊断日志";
        return key;
    }

    public static String descriptionFor(String key) {
        if (KEY_WEICO_PROFILE_ENTRY.equals(key)) return "在微博轻享版“我的”页显示模块配置入口";
        if (KEY_WEICO_SPLASH_AD_REMOVAL.equals(key)) return "跳过启动页广告";
        if (KEY_WEICO_TIMELINE_AD_REMOVAL.equals(key)) return "过滤首页信息流广告；与启动广告开关相互独立";
        if (KEY_WEICO_CONTENTLESS_FILTER.equals(key)) return "过滤非广告且没有可显示正文或媒体的空条目";
        if (KEY_WEICO_FEED_REVERSE.equals(key)) return "同步调整宿主排序设置、列表布局和数据顺序";
        if (KEY_WEICO_TIMELINE_CACHE.equals(key)) return "持久保存并恢复本地信息流缓存；关闭后不会删除已有缓存文件";
        if (KEY_WEICO_TIMELINE_PRELOAD.equals(key)) return "自动向前加载信息流内容；需要开启扩展缓存";
        if (KEY_WEICO_TIMELINE_GAP_FILL.equals(key)) return "检测到缓存断层时继续加载；需要开启扩展缓存";
        if (KEY_WEICO_TIMELINE_LAST_READ.equals(key)) return "保存上次阅读位置、显示标记并在回到首页时恢复锚点；需要开启扩展缓存";
        if (KEY_WEICO_TIMELINE_JUMP.equals(key)) return "允许顶部双击跳到列表两端，并支持按时间跳转";
        if (KEY_WEICO_STATUS_HYDRATION.equals(key)) return "修复微博正文格式和图片媒体字段";
        if (KEY_WEICO_VIDEO_REFRESH.equals(key)) return "打开链接过期的视频时重新请求可用地址";
        if (KEY_WEICO_DISABLE_PULL_REFRESH.equals(key)) return "阻止微博轻享版首页的下拉刷新行为";
        if (KEY_WEICO_TIMELINE_JUMP_BUTTON.equals(key)) return "在首页显示按时间跳转快捷入口";
        if (KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON.equals(key)) return "在首页显示指定日期缓存清理入口";
        if (KEY_WEICO_PERSISTENT_LOGS.equals(key)) return "将模块详细运行记录保存到微博轻享版的私有文件";
        return key;
    }

    private ModuleSettings() {}

    public static boolean defaultFor(String key) {
        return isBooleanKey(key);
    }

    public static int defaultIntFor(String key) {
        return KEY_WEICO_TIMELINE_CACHE_DAYS.equals(key)
            ? DEFAULT_WEICO_TIMELINE_CACHE_DAYS
            : 0;
    }

    public static int clampTimelineCacheDays(int days) {
        if (days < MIN_WEICO_TIMELINE_CACHE_DAYS) return MIN_WEICO_TIMELINE_CACHE_DAYS;
        if (days > MAX_WEICO_TIMELINE_CACHE_DAYS) return MAX_WEICO_TIMELINE_CACHE_DAYS;
        return days;
    }

    public static String[] allKeys() {
        return ALL_KEYS.clone();
    }

    public static String[] booleanKeys() {
        return BOOLEAN_KEYS.clone();
    }

    public static boolean isKnownKey(String key) {
        if (key == null) return false;
        for (String candidate : ALL_KEYS) {
            if (candidate.equals(key)) return true;
        }
        return false;
    }

    public static boolean isBooleanKey(String key) {
        if (key == null) return false;
        for (String candidate : BOOLEAN_KEYS) {
            if (candidate.equals(key)) return true;
        }
        return false;
    }

    public static boolean isIntegerKey(String key) {
        return KEY_WEICO_TIMELINE_CACHE_DAYS.equals(key);
    }

    public static Uri settingsUriFor(String key) {
        return Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + SETTINGS_PATH + "/" + key);
    }

    public static Uri allSettingsUri() {
        return Uri.parse("content://" + PROVIDER_AUTHORITY + "/" + SETTINGS_PATH + "/all");
    }

    public static Map<String, Boolean> onlyAdsPreset() {
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        for (String key : BOOLEAN_KEYS) {
            boolean enabled = KEY_WEICO_SPLASH_AD_REMOVAL.equals(key)
                || KEY_WEICO_TIMELINE_AD_REMOVAL.equals(key);
            result.put(key, Boolean.valueOf(enabled));
        }
        return Collections.unmodifiableMap(result);
    }

    public static FeatureSnapshot featureSnapshot(Map<String, ?> settings) {
        boolean[] values = new boolean[BOOLEAN_KEYS.length];
        for (int i = 0; i < BOOLEAN_KEYS.length; i++) {
            String key = BOOLEAN_KEYS[i];
            Object raw = settings == null ? null : settings.get(key);
            values[i] = raw == null ? defaultFor(key) : asBoolean(raw, defaultFor(key));
        }
        return new FeatureSnapshot(values);
    }

    public static FeatureSnapshot featureSnapshot(boolean[] values) {
        if (values == null || values.length != BOOLEAN_KEYS.length) {
            return featureSnapshot(Collections.<String, Object>emptyMap());
        }
        return new FeatureSnapshot(values);
    }

    public static final class FeatureSnapshot {
        private final boolean[] values;

        private FeatureSnapshot(boolean[] values) {
            this.values = values.clone();
        }

        public boolean isEnabled(String key) {
            int index = indexOfBooleanKey(key);
            return index >= 0 ? values[index] : defaultFor(key);
        }

        public boolean isEffective(String key) {
            if (!isEnabled(key)) return false;
            if (KEY_WEICO_TIMELINE_PRELOAD.equals(key)
                || KEY_WEICO_TIMELINE_GAP_FILL.equals(key)
                || KEY_WEICO_TIMELINE_LAST_READ.equals(key)) {
                return isEnabled(KEY_WEICO_TIMELINE_CACHE);
            }
            return true;
        }

        public boolean[] toArray() {
            return values.clone();
        }

        private static int indexOfBooleanKey(String key) {
            if (key == null) return -1;
            for (int i = 0; i < BOOLEAN_KEYS.length; i++) {
                if (BOOLEAN_KEYS[i].equals(key)) return i;
            }
            return -1;
        }
    }

    private static boolean asBoolean(Object value, boolean fallback) {
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if ("true".equals(value) || "1".equals(value)) return true;
        if ("false".equals(value) || "0".equals(value)) return false;
        return fallback;
    }
}
