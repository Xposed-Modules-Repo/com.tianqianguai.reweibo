package com.tianqianguai.reweibo;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeiboLiteHook {

    private static final String REVERSE_ORDER_KEY = "key_order_browser";
    private static final int PRELOAD_MAX_PAGES = 100;
    private static final int PRELOAD_MAX_ITEMS = 2200;
    private static final long PRELOAD_DELAY_MS = 700L;
    private static final long PRELOAD_WATCHDOG_MS = 4500L;
    private static final long TOP_ANCHOR_WINDOW_MS = 16000L;
    private static final long TOP_BAR_DOUBLE_TAP_MS = 500L;
    private static final long TOP_BAR_JUMP_RETRY_MS = 120L;
    private static final long VIDEO_URL_EXPIRY_MARGIN_SEC = 300L;
    private static final long VIDEO_OPEN_REFRESH_BYPASS_MS = 30000L;
    private static final int TOP_ANCHOR_MAX_ATTEMPTS = 80;
    private static final int TOP_BAR_JUMP_MAX_ATTEMPTS = 8;
    private static final int PRELOAD_DONE_MIN_ITEMS = 1300;
    private static final int PRELOAD_RESTORED_TRUST_MIN_ITEMS = 1800;
    private static final int PRELOAD_STABLE_DONE_ROUNDS = 8;
    private static final int TIMELINE_CACHE_MIN_ITEMS = 80;
    private static final int TOP_BAR_TAP_MAX_DP = 100;
    private static final int TOP_BAR_TAP_SLOP_DP = 64;
    private static final int TOP_BAR_RIGHT_EXCLUDE_DP = 72;
    private static final String PRELOAD_DONE_FILE = "reweibo_weico_preload_done";
    private static final String LAST_READ_FILE = "reweibo_weico_last_read";
    private static final String TIMELINE_FULL_CACHE_PREFIX = "reweibo_fullTimeline_";
    private static final String TIMELINE_SHADOW_CACHE_FILE = "reweibo_weico_full_cache_shadow.txt";
    private static final String TIMELINE_SHADOW_CACHE_META_FILE = "reweibo_weico_full_cache_shadow.meta";
    private static final Map<Object, PreloadState> sPreloadStates = new WeakHashMap<>();
    private static final Map<Object, TopAnchorState> sTopAnchorStates = new WeakHashMap<>();
    private static final Map<Object, Boolean> sTimelineRecyclerViews = new WeakHashMap<>();
    private static final Map<Object, Long> sTimelineUserMovedAt = new WeakHashMap<>();
    private static final Map<Object, Integer> sPersistedTimelineCacheCounts = new WeakHashMap<>();
    private static final Map<Object, Boolean> sRestoringTimelineCaches = new WeakHashMap<>();
    private static final Map sTimelineCumulativeStatusesById = new LinkedHashMap();
    private static File sLogFile = null;
    private static Boolean sTimelinePreloadDone = null;
    private static long sSuppressTimelineLoadMoreUntilMs = 0L;
    private static boolean sTimelineOldestFirstMode = false;
    private static boolean sTimelineRestoredCacheMode = false;
    private static Object sLastTimelinePresenter = null;
    private static Long sLastReadStatusId = null;
    private static boolean sLastReadMarkerShown = false;
    private static long sLastReadPersistAtMs = 0L;
    private static long sTopBarLastTapAtMs = 0L;
    private static float sTopBarLastTapX = 0f;
    private static float sTopBarLastTapY = 0f;
    private static long sVideoOpenRefreshBypassStatusId = 0L;
    private static long sVideoOpenRefreshBypassUntilMs = 0L;
    private static int sTimelineShadowCacheCount = 0;
    private static boolean sTimelineCursorActionDumped = false;

    private static final class PreloadState {
        int requestedPages = 0;
        int requestToken = 0;
        int stableRounds = 0;
        int lastCount = 0;
        boolean scheduled = false;
        boolean inFlight = false;
        boolean stopped = false;
    }

    private static final class TopAnchorState {
        int generation = 0;
        int attempts = 0;
        int lastTarget = -1;
        long untilElapsedMs = 0L;
        boolean userTouched = false;
        boolean finishing = false;
    }

    private static void log(String msg) {
        XposedBridge.log("ReWeibo: " + msg);
        if (sLogFile != null) {
            try {
                FileWriter fw = new FileWriter(sLogFile, true);
                String ts = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                fw.write(ts + " " + msg + "\n");
                fw.close();
            } catch (Throwable ignored) {}
        }
    }

    private static void initLogFile() {
        try {
            File dir = new File("/data/data/com.weico.international/files");
            if (!dir.exists()) dir.mkdirs();
            sLogFile = new File(dir, "reweibo_weico.log");
            log("=== Log started ===");
        } catch (Throwable ignored) {}
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            log("WeicoHook start");
            initLogFile();
            forceNativeReverseOrder(lpparam.classLoader);
            forceTimelineLayoutDirection(lpparam.classLoader);
            forceTimelineDataOrder(lpparam.classLoader);
            hookTimelineNoMoreContentMarker(lpparam.classLoader);
            hookTopBarDoubleTap(lpparam.classLoader);
            hookStaleVideoOpenRefresh(lpparam.classLoader);
            disableWeicoPullRefresh(lpparam.classLoader);
            removeSplashAd(lpparam.classLoader);
            removeTimelineAd(lpparam.classLoader);

            log("WeicoHook done");
        } catch (Throwable t) {
            log("WeicoHook failed: " + t.getMessage());
        }
    }

    private static void forceTimelineDataOrder(ClassLoader cl) {
        hookTimelineMergeOrder(cl);
        hookV2TimelineDataOrder(cl);
        hookV3TimelineDataOrder(cl);
    }

    private static void hookTimelineNoMoreContentMarker(ClassLoader cl) {
        try {
            Class<?> textViewClass = XposedHelpers.findClass("android.widget.TextView", cl);
            XC_MethodHook noMoreTextHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object value = param.args == null || param.args.length == 0 ? null : param.args[0];
                    if (isTimelineNoMoreContentText(value)) {
                        markTimelineNoMoreContent("text:" + value);
                    }
                }
            };
            XposedHelpers.findAndHookMethod(textViewClass, "setText", CharSequence.class, noMoreTextHook);
            Class<?> bufferTypeClass = Class.forName("android.widget.TextView$BufferType", false, cl);
            XposedHelpers.findAndHookMethod(textViewClass, "setText", CharSequence.class, bufferTypeClass, noMoreTextHook);
            log("Timeline no-more marker hook installed");
        } catch (Throwable t) {
            log("Timeline no-more marker hook error: " + t.getMessage());
        }
    }

    private static boolean isTimelineNoMoreContentText(Object value) {
        if (value == null) return false;
        String text = String.valueOf(value)
            .replace(" ", "")
            .replace("\n", "")
            .replace("\r", "");
        return text.contains("没有更多")
            || text.contains("暂无更多")
            || text.contains("已加载全部")
            || text.contains("已经到底")
            || text.contains("到底了");
    }

    private static void hookTopBarDoubleTap(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.Activity",
                cl,
                "dispatchTouchEvent",
                MotionEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object event = param.args[0];
                        if (event instanceof MotionEvent) {
                            handleTopBarDoubleTap((MotionEvent) event);
                        }
                    }
                }
            );
            log("Timeline top-bar double-tap hook installed");
        } catch (Throwable t) {
            log("Timeline top-bar double-tap hook error: " + t.getMessage());
        }
    }

    private static void handleTopBarDoubleTap(MotionEvent event) {
        try {
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return;
            if (!isTimelineTopBarTap(event)) return;

            long now = SystemClock.elapsedRealtime();
            float x = event.getRawX();
            float y = event.getRawY();
            int slop = dpToPx(getAnyTimelineRecyclerView(), TOP_BAR_TAP_SLOP_DP);
            boolean isDoubleTap = now - sTopBarLastTapAtMs <= TOP_BAR_DOUBLE_TAP_MS
                && Math.abs(x - sTopBarLastTapX) <= slop
                && Math.abs(y - sTopBarLastTapY) <= slop;

            sTopBarLastTapAtMs = now;
            sTopBarLastTapX = x;
            sTopBarLastTapY = y;

            if (isDoubleTap) {
                sTopBarLastTapAtMs = 0L;
                jumpTimelineToAbsoluteTopForKnownRecyclerViews("top-bar-double-tap");
            }
        } catch (Throwable t) {
            log("Timeline top-bar double-tap error: " + t.getMessage());
        }
    }

    private static boolean isTimelineTopBarTap(MotionEvent event) {
        Object recyclerView = getAnyTimelineRecyclerView();
        int topLimit = dpToPx(recyclerView, TOP_BAR_TAP_MAX_DP);
        if (event.getRawY() > topLimit) return false;
        try {
            int width = recyclerView == null ? 0 : callIntMethodSafe(recyclerView, "getWidth", 0);
            if (width > 0 && event.getRawX() >= width - dpToPx(recyclerView, TOP_BAR_RIGHT_EXCLUDE_DP)) {
                return false;
            }
        } catch (Throwable ignored) {}
        return true;
    }

    private static void hookStaleVideoOpenRefresh(final ClassLoader cl) {
        try {
            final Class<?> activityClass = Class.forName(
                "com.weico.international.ui.smallvideo.SmallVideoActivity",
                false,
                cl
            );
            final Class<?> companionClass = Class.forName(
                "com.weico.international.ui.smallvideo.SmallVideoActivity$Companion",
                false,
                cl
            );
            final Class<?> videoInfoClass = Class.forName("com.weico.international.data.VideoInfo", false, cl);
            final Class<?> statusClass = Class.forName("com.weico.international.model.sina.Status", false, cl);

            XposedHelpers.findAndHookMethod(
                activityClass,
                "openVideo",
                Context.class,
                videoInfoClass,
                statusClass,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object companion = getStaticFieldSafe(activityClass, "INSTANCE");
                        if (redirectStaleVideoOpen(companion, param.args, "SmallVideoActivity.openVideo")) {
                            param.setResult(null);
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(
                companionClass,
                "openVideo",
                Context.class,
                videoInfoClass,
                statusClass,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (redirectStaleVideoOpen(param.thisObject, param.args, "SmallVideoActivity.Companion.openVideo")) {
                            param.setResult(null);
                        }
                    }
                }
            );

            log("Timeline stale video-open hook installed");
        } catch (Throwable t) {
            log("Timeline stale video-open hook error: " + t.getMessage());
        }
    }

    private static boolean redirectStaleVideoOpen(Object opener, Object[] args, String source) {
        try {
            if (opener == null || args == null || args.length < 3) return false;
            if (!(args[0] instanceof Context)) return false;

            Context context = (Context) args[0];
            Object videoInfo = args[1];
            Object status = args[2];
            long statusId = getStatusId(status);
            if (videoInfo == null || statusId <= 0) return false;
            if (isVideoOpenRefreshBypassed(statusId)) return false;

            String url = getStringMethodOrField(videoInfo, "getUrl", "url");
            if (isUsableVideoUrl(url)) return false;

            String playUrl = getStringMethodOrField(videoInfo, "getPlayUrl", null);
            if (isUsableVideoUrl(playUrl)) {
                if (replaceVideoInfoUrl(videoInfo, playUrl, source, statusId)) {
                    return false;
                }
            }

            if (!shouldRefreshVideoUrl(url)) return false;

            markVideoOpenRefreshBypass(statusId);
            XposedHelpers.callMethod(opener, "openWeiboVideo", context, Long.valueOf(statusId));
            log("Timeline stale video url refreshed source=" + source
                + " id=" + statusId
                + " url=" + describeVideoUrlState(url)
                + " playUrl=" + describeVideoUrlState(playUrl));
            return true;
        } catch (Throwable t) {
            log("Timeline stale video url refresh error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static boolean replaceVideoInfoUrl(Object videoInfo, String playUrl, String source, long statusId) {
        try {
            XposedHelpers.callMethod(videoInfo, "setUrl", playUrl);
            log("Timeline video url replaced source=" + source + " id=" + statusId
                + " playUrl=" + describeVideoUrlState(playUrl));
            return true;
        } catch (Throwable ignored) {}
        try {
            setFieldValue(videoInfo, "url", playUrl);
            log("Timeline video url field replaced source=" + source + " id=" + statusId
                + " playUrl=" + describeVideoUrlState(playUrl));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isUsableVideoUrl(String url) {
        return hasMeaningfulString(url) && !isVideoUrlExpiredOrNear(url);
    }

    private static boolean shouldRefreshVideoUrl(String url) {
        return !hasMeaningfulString(url) || isVideoUrlExpiredOrNear(url);
    }

    private static boolean isVideoUrlExpiredOrNear(String url) {
        long expiresAt = extractVideoUrlExpiresAtSec(url);
        if (expiresAt <= 0L) return false;
        long now = System.currentTimeMillis() / 1000L;
        return expiresAt <= now + VIDEO_URL_EXPIRY_MARGIN_SEC;
    }

    private static long extractVideoUrlExpiresAtSec(String url) {
        if (!hasMeaningfulString(url)) return 0L;
        String[] keys = {"Expires=", "expires=", "Expires%3D", "expires%3D"};
        for (int i = 0; i < keys.length; i++) {
            int start = url.indexOf(keys[i]);
            if (start < 0) continue;
            long value = parseLeadingLong(url, start + keys[i].length());
            if (value > 0L) return value;
        }
        return 0L;
    }

    private static long parseLeadingLong(String value, int start) {
        if (value == null || start < 0 || start >= value.length()) return 0L;
        long result = 0L;
        boolean hasDigit = false;
        for (int i = start; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < '0' || c > '9') break;
            hasDigit = true;
            long next = result * 10L + (c - '0');
            if (next < result) return 0L;
            result = next;
        }
        return hasDigit ? result : 0L;
    }

    private static String describeVideoUrlState(String url) {
        if (!hasMeaningfulString(url)) return "empty";
        long expiresAt = extractVideoUrlExpiresAtSec(url);
        if (expiresAt <= 0L) return "no-expiry";
        return "expires=" + expiresAt + " now=" + (System.currentTimeMillis() / 1000L);
    }

    private static boolean isVideoOpenRefreshBypassed(long statusId) {
        return statusId > 0L
            && statusId == sVideoOpenRefreshBypassStatusId
            && SystemClock.elapsedRealtime() < sVideoOpenRefreshBypassUntilMs;
    }

    private static void markVideoOpenRefreshBypass(long statusId) {
        sVideoOpenRefreshBypassStatusId = statusId;
        sVideoOpenRefreshBypassUntilMs = SystemClock.elapsedRealtime() + VIDEO_OPEN_REFRESH_BYPASS_MS;
    }

    private static Object getStaticFieldSafe(Class<?> clazz, String fieldName) {
        try {
            java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void disableWeicoPullRefresh(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                "androidx.swiperefreshlayout.widget.SwipeRefreshLayout",
                cl,
                "canChildScrollUp",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(true);
                    }
                }
            );
            XposedHelpers.findAndHookMethod(
                "androidx.swiperefreshlayout.widget.SwipeRefreshLayout",
                cl,
                "setRefreshing",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.args[0] = false;
                    }
                }
            );
            XposedHelpers.findAndHookMethod(
                "androidx.swiperefreshlayout.widget.SwipeRefreshLayout",
                cl,
                "isRefreshing",
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) {
                        return false;
                    }
                }
            );
            log("SwipeRefreshLayout pull-refresh disabled");
        } catch (Throwable t) {
            log("SwipeRefreshLayout pull-refresh hook error: " + t.getMessage());
        }
    }

    private static void forceTimelineLayoutDirection(ClassLoader cl) {
        try {
            Class<?> recyclerViewClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.RecyclerView",
                cl
            );
            Class<?> layoutManagerClass = Class.forName(
                "androidx.recyclerview.widget.RecyclerView$LayoutManager",
                false,
                cl
            );
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "setLayoutManager",
                layoutManagerClass,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) {
                        final Object recyclerView = param.thisObject;
                        final Object layoutManager = param.args[0];
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (fixTimelineLayoutDirection(recyclerView, layoutManager)) {
                                    beginTimelineTopAnchor(recyclerView, "setLayoutManager", true);
                                }
                            }
                        }, 300L);
                    }
                }
            );
            Class<?> adapterClass = Class.forName(
                "androidx.recyclerview.widget.RecyclerView$Adapter",
                false,
                cl
            );
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "setAdapter",
                adapterClass,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) {
                        final Object recyclerView = param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Object layoutManager = XposedHelpers.callMethod(recyclerView, "getLayoutManager");
                                    if (fixTimelineLayoutDirection(recyclerView, layoutManager)) {
                                        beginTimelineTopAnchor(recyclerView, "setAdapter", true);
                                    }
                                } catch (Throwable t) {
                                    log("Timeline layout adapter fix error: " + t.getMessage());
                                }
                            }
                        }, 300L);
                    }
                }
            );
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "onLayout",
                boolean.class,
                int.class,
                int.class,
                int.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object recyclerView = param.thisObject;
                        if (isTimelineRecyclerView(recyclerView)) {
                            scheduleTimelineTopAnchor(recyclerView, "onLayout", 0L);
                        }
                    }
                }
            );
            XC_MethodHook redirectInitialScroll = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    Object recyclerView = param.thisObject;
                    if (!shouldAnchorTimelineTop(recyclerView)) return;
                    int target = getTimelineTopAdapterPosition(recyclerView);
                    if (target < 0) return;
                    Object oldTarget = param.args[0];
                    if (oldTarget instanceof Integer && ((Integer) oldTarget).intValue() == target) return;
                    param.args[0] = Integer.valueOf(target);
                    log("Timeline initial scroll redirected from=" + oldTarget + " to=" + target);
                }
            };
            XposedHelpers.findAndHookMethod(recyclerViewClass, "scrollToPosition", int.class, redirectInitialScroll);
            XposedHelpers.findAndHookMethod(recyclerViewClass, "smoothScrollToPosition", int.class, redirectInitialScroll);
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "onTouchEvent",
                MotionEvent.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Object event = param.args[0];
                        if (!(event instanceof MotionEvent)) return;
                        int action = ((MotionEvent) event).getActionMasked();
                        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                            markTimelineTopAnchorTouched(param.thisObject);
                            if (action == MotionEvent.ACTION_MOVE) {
                                markTimelineLastReadTouched(param.thisObject);
                            }
                        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            schedulePersistTimelineLastRead(param.thisObject, "touch-up", 800L);
                        }
                    }
                }
            );
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "onScrollStateChanged",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object recyclerView = param.thisObject;
                        int state = ((Integer) param.args[0]).intValue();
                        if (state == 0 && isTimelineRecyclerView(recyclerView)) {
                            schedulePersistTimelineLastRead(recyclerView, "scroll-idle", 300L);
                        }
                    }
                }
            );
            log("Timeline layout hook installed");
        } catch (Throwable t) {
            log("Timeline layout hook error: " + t.getMessage());
        }
    }

    private static boolean fixTimelineLayoutDirection(Object recyclerView, Object layoutManager) {
        if (recyclerView == null || layoutManager == null) return false;
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            if (adapter == null) return false;
            String adapterName = adapter.getClass().getName();
            if (!adapterName.contains("TimelineAdapter")) return false;

            XposedHelpers.callMethod(layoutManager, "setReverseLayout", true);
            XposedHelpers.callMethod(layoutManager, "setStackFromEnd", false);
            rememberTimelineRecyclerView(recyclerView);
            log("Timeline layout fixed reverse adapter=" + adapterName);
            return true;
        } catch (Throwable t) {
            log("Timeline layout fix error: " + t.getMessage());
            return false;
        }
    }

    private static void hookTimelineMergeOrder(ClassLoader cl) {
        try {
            Class<?> presenterClass = Class.forName("com.weico.international.ui.indexv2.IndexV2Presenter", false, cl);
            XposedHelpers.findAndHookMethod(presenterClass, "addData", List.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    if (shouldFreezeTimelineNetworkMutation(param.thisObject)) {
                        int incoming = param.args[0] instanceof List ? ((List) param.args[0]).size() : -1;
                        log("Timeline addData suppressed stable-cache incoming=" + incoming);
                        param.setResult(null);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    if (shouldFreezeTimelineNetworkMutation(param.thisObject)) {
                        finishTimelineTopAnchorForKnownRecyclerViews("presenter-addData-suppressed");
                        return;
                    }
                    List incomingData = null;
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        incomingData = (List) param.args[0];
                        mergeTimelineCumulativeStatuses(incomingData, param.thisObject, "presenter-addData-incoming");
                    }
                    normalizePresenterTimeline(param.thisObject, "presenter-addData");
                    persistTimelineNativeCache(param.thisObject, "presenter-addData");
                    markTimelineNoMoreIfEmptyPage(param.thisObject, incomingData, "presenter-addData");
                    scheduleTimelinePreload(param.thisObject, "presenter-addData");
                    if (isTimelinePreloadStopped(param.thisObject)) {
                        finishTimelineTopAnchorForKnownRecyclerViews("presenter-addData-stop");
                    } else {
                        beginTimelineTopAnchorForKnownRecyclerViews("presenter-addData", false);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(presenterClass, "setData", List.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    if (isTimelineCacheRestoring(param.thisObject)) {
                        normalizePresenterTimeline(param.thisObject, "presenter-setData-restored");
                        persistTimelineNativeCache(param.thisObject, "presenter-setData-restored");
                        return;
                    }
                    resetPreloadState(param.thisObject, "presenter-setData");
                    if (isTimelinePreloadDone()
                        && restoreTimelineNativeCache(param.thisObject, "presenter-setData-warmed")) {
                        beginTimelineTopAnchorForKnownRecyclerViews("presenter-cache-restore", true);
                        scheduleTimelinePreload(param.thisObject, "presenter-cache-restore");
                        return;
                    }
                    normalizePresenterTimeline(param.thisObject, "presenter-setData");
                    if (!isTimelinePreloadDone()) {
                        persistTimelineNativeCache(param.thisObject, "presenter-setData");
                    }
                    if (restoreTimelineNativeCache(param.thisObject, "presenter-setData")) {
                        beginTimelineTopAnchorForKnownRecyclerViews("presenter-cache-restore", true);
                        scheduleTimelinePreload(param.thisObject, "presenter-cache-restore");
                        return;
                    }
                    beginTimelineTopAnchorForKnownRecyclerViews("presenter-setData", true);
                    scheduleTimelinePreload(param.thisObject, "presenter-setData");
                }
            });
            XposedHelpers.findAndHookMethod(presenterClass, "distinct", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    if (shouldFreezeTimelineNetworkMutation(param.thisObject)) {
                        finishTimelineTopAnchorForKnownRecyclerViews("presenter-distinct-suppressed");
                        return;
                    }
                    normalizePresenterTimeline(param.thisObject, "presenter-distinct");
                    persistTimelineNativeCache(param.thisObject, "presenter-distinct");
                    if (!isTimelinePreloadStopped(param.thisObject)) {
                        beginTimelineTopAnchorForKnownRecyclerViews("presenter-distinct", false);
                    }
                }
            });
            XposedHelpers.findAndHookMethod(presenterClass, "loadMore", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    if (shouldSuppressTimelineLoadMore(param.thisObject)) {
                        log("Timeline loadMore suppressed warmed");
                        param.setResult(null);
                    }
                }
            });

            Class<?> fragmentClass = Class.forName("com.weico.international.ui.indexv2.IndexV2Fragment", false, cl);
            Class<?> commonLoadEventClass = Class.forName("com.weico.international.flux.Events$CommonLoadEvent", false, cl);
            XposedHelpers.findAndHookMethod(fragmentClass, "showData", commonLoadEventClass,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        normalizeShowDataEvent(param.thisObject, param.args[0]);
                    }
                }
            );

            log("Timeline merge-order hooks installed");
        } catch (Throwable t) {
            log("Timeline merge-order hook error: " + t.getMessage());
        }
    }

    private static void hookV2TimelineDataOrder(ClassLoader cl) {
        try {
            Class<?> actionClass = Class.forName("com.weico.international.ui.indexv2.IndexV2Action", false, cl);
            Class<?> batchClass = Class.forName("com.weico.international.ui.indexv2.IndexV2Action$doLoadData$4", false, cl);

            XposedHelpers.findAndHookMethod(batchClass, "invoke", java.util.ArrayList.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object action = getOuterAction(param.thisObject);
                        boolean loadNew = getBooleanFieldSafe(param.thisObject, "$isLoadNew", false);
                        if (param.args[0] instanceof List) {
                            List list = (List) param.args[0];
                            hydrateTimelineStatusText(list, "v2-network");
                            List filtered = filterTimelineAds(list, action, "v2-network");
                            filtered = filterTimelineContentless(filtered, action, "v2-network");
                            List ordered = ensureNewestFirst(filtered, action, "v2-network", loadNew);
                            if (ordered != filtered) filtered = ordered;
                            if (filtered != list) {
                                replaceListContents(list, filtered, "v2-network");
                                param.args[0] = filtered;
                            }
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(actionClass, "doLoadCache",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            hydrateTimelineStatusText((List) result, "v2-cache");
                            List filtered = filterTimelineAds((List) result, param.thisObject, "v2-cache");
                            filtered = filterTimelineContentless(filtered, param.thisObject, "v2-cache");
                            List ordered = ensureNewestFirst(filtered, param.thisObject, "v2-cache", false);
                            param.setResult(ordered);
                            log("Timeline native cache restored source=v2-cache count=" + countTimelineStatuses(ordered));
                        }
                    }
                }
            );

            log("V2 timeline data-order hooks installed");
        } catch (Throwable t) {
            log("V2 timeline data-order hook error: " + t.getMessage());
        }
    }

    private static void hookV3TimelineDataOrder(ClassLoader cl) {
        try {
            Class<?> actionClass = Class.forName("com.weico.international.ui.indexv2.IndexV3Action", false, cl);
            Class<?> batchClass = Class.forName("com.weico.international.ui.indexv2.IndexV3Action$doLoadData$1", false, cl);
            Class<?> feedResultClass = Class.forName("com.weico.international.ui.indexv2.FeedResult", false, cl);

            XposedHelpers.findAndHookMethod(batchClass, "invoke", feedResultClass,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        Object action = getOuterAction(param.thisObject);
                        boolean loadNew = getBooleanFieldSafe(param.thisObject, "$isLoadNew", false);
                        if (result instanceof List) {
                            hydrateTimelineStatusText((List) result, "v3-network");
                            List filtered = filterTimelineAds((List) result, action, "v3-network");
                            filtered = filterTimelineContentless(filtered, action, "v3-network");
                            param.setResult(ensureNewestFirst(filtered, action, "v3-network", loadNew));
                        }
                    }
                }
            );

            XposedHelpers.findAndHookMethod(actionClass, "doLoadCache",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        if (result instanceof List) {
                            hydrateTimelineStatusText((List) result, "v3-cache");
                            List filtered = filterTimelineAds((List) result, param.thisObject, "v3-cache");
                            filtered = filterTimelineContentless(filtered, param.thisObject, "v3-cache");
                            param.setResult(ensureNewestFirst(filtered, param.thisObject, "v3-cache", false));
                        }
                    }
                }
            );

            log("V3 timeline data-order hooks installed");
        } catch (Throwable t) {
            log("V3 timeline data-order hook error: " + t.getMessage());
        }
    }

    private static List ensureNewestFirst(List list, Object action, String source, boolean loadNew) {
        if (list == null || list.size() < 2) return list;

        String groupId = getTimelineGroupId(action);
        if (!"-1".equals(groupId)) {
            log("Timeline order skipped source=" + source + " group=" + groupId + " size=" + list.size());
            return list;
        }

        Object first = findComparableStatus(list, true);
        Object last = findComparableStatus(list, false);
        long firstId = getStatusId(first);
        long lastId = getStatusId(last);
        if (firstId <= 0 || lastId <= 0 || firstId == lastId) {
            log("Timeline order unknown source=" + source + " group=" + groupId + " size=" + list.size()
                + " first=" + firstId + " last=" + lastId + " loadNew=" + loadNew);
            return list;
        }

        if (firstId > lastId) {
            log("Timeline order kept source=" + source + " group=" + groupId + " size=" + list.size()
                + " first=" + firstId + " last=" + lastId + " loadNew=" + loadNew);
            return list;
        }

        try {
            Collections.reverse(list);
            log("Timeline order reversed source=" + source + " group=" + groupId + " size=" + list.size()
                + " first=" + firstId + " last=" + lastId + " loadNew=" + loadNew);
            return list;
        } catch (Throwable t) {
            java.util.ArrayList copy = new java.util.ArrayList(list);
            Collections.reverse(copy);
            log("Timeline order copied+reversed source=" + source + " group=" + groupId + " size=" + list.size()
                + " first=" + firstId + " last=" + lastId + " loadNew=" + loadNew);
            return copy;
        }
    }

    private static void normalizePresenterTimeline(Object presenter, String source) {
        try {
            Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
            if (statusList instanceof List) {
                hydrateTimelineStatusText((List) statusList, source);
                List filtered = filterTimelineAds((List) statusList, presenter, source);
                filtered = filterTimelineContentless(filtered, presenter, source);
                List sorted = sTimelineOldestFirstMode
                    ? sortTimelineOldestFirst(filtered, presenter, source)
                    : sortTimelineNewestFirst(filtered, presenter, source, false);
                if (filtered != statusList || sorted != filtered) {
                    Object delegate = getFieldValue(presenter, "statusPresenterDelegate");
                    XposedHelpers.callMethod(delegate, "setData", sorted);
                    log("Presenter timeline replaced sorted copy source=" + source + " size=" + sorted.size());
                }
            }
        } catch (Throwable t) {
            log("Presenter timeline normalize error source=" + source + ": " + t.getMessage());
        }
    }

    private static void normalizeShowDataEvent(Object fragment, Object event) {
        try {
            Object presenter = XposedHelpers.callMethod(fragment, "getPresenter");
            Object loadEvent = getFieldValue(event, "loadEvent");
            Object data = getFieldValue(loadEvent, "data");
            Object type = getFieldValue(loadEvent, "type");
            if (data instanceof List) {
                String source = "showData-" + type;
                hydrateTimelineStatusText((List) data, source);
                List filtered = filterTimelineAds((List) data, presenter, source);
                filtered = filterTimelineContentless(filtered, presenter, source);
                List sorted = sTimelineOldestFirstMode
                    ? sortTimelineOldestFirst(filtered, presenter, source)
                    : sortTimelineNewestFirst(filtered, presenter, source, false);
                if (filtered != data || sorted != filtered) {
                    setFieldValue(loadEvent, "data", sorted);
                }
            }
        } catch (Throwable t) {
            log("ShowData timeline normalize error: " + t.getMessage());
        }
    }

    private static void persistTimelineNativeCache(Object presenter, String source) {
        try {
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            rememberTimelinePresenter(presenter);

            Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
            if (!(statusList instanceof List)) return;

            ArrayList statuses = mergeTimelineCumulativeStatuses((List) statusList, presenter, source);
            List filtered = filterTimelineContentless(statuses, presenter, source);
            if (filtered != statuses) {
                statuses = new ArrayList(filtered);
            }
            int count = statuses.size();
            if (count < TIMELINE_CACHE_MIN_ITEMS) return;

            synchronized (sPersistedTimelineCacheCounts) {
                Integer lastCount = sPersistedTimelineCacheCounts.get(presenter);
                if (lastCount != null && lastCount.intValue() >= count) return;
            }

            Object action = getTimelineAction(presenter);
            if (action == null) {
                log("Timeline native cache persist skipped source=" + source + " count=" + count + " no action");
                return;
            }

            String maxId = getTimelineCacheMaxId(action, statuses);
            syncTimelineActionMaxId(action, maxId, source);
            Object result = newTimelineStatusResult(action, statuses, maxId);
            Object builder = getTimelineCacheBuilder(action);
            Class<?> diskCacheClass = Class.forName("com.weico.diskcache.DiskCache", false, action.getClass().getClassLoader());
            Object cache = callStaticNoArg(diskCacheClass, "getInstance");
            XposedHelpers.callMethod(cache, "cache", result, builder, true);
            persistTimelineShadowCache(source, count, maxId, false);

            synchronized (sPersistedTimelineCacheCounts) {
                sPersistedTimelineCacheCounts.put(presenter, Integer.valueOf(count));
            }
            log("Timeline full cache persisted source=" + source + " count=" + count + " maxId=" + maxId);
        } catch (Throwable t) {
            log("Timeline full cache persist error source=" + source + ": " + t.getMessage());
        }
    }

    private static void persistTimelineShadowCache(String source, int count, String maxId, boolean force) {
        try {
            if (!force) {
                if (count < PRELOAD_DONE_MIN_ITEMS) return;
                if (sTimelineShadowCacheCount > 0 && count < sTimelineShadowCacheCount + 100) return;
            }

            File nativeFile = findTimelineNativeCacheFile();
            if (nativeFile == null || !nativeFile.exists()) {
                log("Timeline shadow cache persist skipped source=" + source + " count=" + count + " no native file");
                return;
            }

            File shadow = getTimelineShadowCacheFile();
            File parent = shadow.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            copyFile(nativeFile, shadow);
            writeTimelineShadowCacheMeta(nativeFile.getName(), source, count, maxId, shadow.length());
            sTimelineShadowCacheCount = Math.max(sTimelineShadowCacheCount, count);
            log("Timeline shadow cache persisted source=" + source + " count=" + count
                + " file=" + nativeFile.getName() + " size=" + shadow.length());
        } catch (Throwable t) {
            log("Timeline shadow cache persist error source=" + source + ": " + t.getMessage());
        }
    }

    private static Object restoreTimelineShadowCache(Object cache, Object builder, String source) {
        try {
            File shadow = getTimelineShadowCacheFile();
            if (shadow == null || !shadow.exists() || shadow.length() < 1024L) return null;

            String fileName = readTimelineShadowCacheFileName();
            if (!hasMeaningfulString(fileName)) return null;

            File dir = getTimelineDataCacheDir();
            if (!dir.exists()) dir.mkdirs();
            File nativeFile = new File(dir, fileName);
            copyFile(shadow, nativeFile);

            Object cached = XposedHelpers.callMethod(cache, "get", builder);
            if (cached != null) {
                log("Timeline shadow cache restored source=" + source + " file=" + fileName
                    + " size=" + shadow.length());
                return cached;
            }
            log("Timeline shadow cache restored file but DiskCache missed source=" + source + " file=" + fileName);
        } catch (Throwable t) {
            log("Timeline shadow cache restore error source=" + source + ": " + t.getMessage());
        }
        return null;
    }

    private static File findTimelineNativeCacheFile() {
        File dir = getTimelineDataCacheDir();
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return null;

        File best = null;
        long bestSize = 0L;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file == null || !file.isFile()) continue;
            String name = file.getName();
            long length = file.length();
            if (name == null || !name.endsWith(".txt") || length < 100000L) continue;
            if (!hasTimelineNativeCacheSignature(file)) continue;
            if (best == null || length > bestSize || (length == bestSize && file.lastModified() > best.lastModified())) {
                best = file;
                bestSize = length;
            }
        }
        return best;
    }

    private static boolean hasTimelineNativeCacheSignature(File file) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            byte[] buffer = new byte[512];
            int read = input.read(buffer);
            if (read <= 0) return false;
            String head = new String(buffer, 0, read);
            return head.contains("\"statuses\"") && head.contains("\"max_id\"");
        } catch (Throwable ignored) {
            return false;
        } finally {
            closeQuietly(input);
        }
    }

    private static void writeTimelineShadowCacheMeta(String fileName, String source, int count, String maxId, long size) {
        FileWriter fw = null;
        try {
            File file = getTimelineShadowCacheMetaFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            fw = new FileWriter(file, false);
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            fw.write("saved_at=" + ts + "\n");
            fw.write("source=" + source + "\n");
            fw.write("file_name=" + fileName + "\n");
            fw.write("count=" + count + "\n");
            fw.write("size=" + size + "\n");
            if (maxId != null) fw.write("max_id=" + maxId + "\n");
        } catch (Throwable t) {
            log("Timeline shadow cache meta write error source=" + source + ": " + t.getMessage());
        } finally {
            closeQuietly(fw);
        }
    }

    private static String readTimelineShadowCacheFileName() {
        BufferedReader reader = null;
        try {
            File file = getTimelineShadowCacheMetaFile();
            if (file == null || !file.exists()) return null;
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("file_name=")) {
                    return line.substring("file_name=".length()).trim();
                }
            }
        } catch (Throwable ignored) {
        } finally {
            closeQuietly(reader);
        }
        return null;
    }

    private static void copyFile(File from, File to) throws Exception {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(from);
            output = new FileOutputStream(to, false);
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Throwable ignored) {}
    }

    private static File getTimelineDataCacheDir() {
        return new File("/data/data/com.weico.international/files/dataCache");
    }

    private static File getTimelineShadowCacheFile() {
        return new File("/data/data/com.weico.international/files", TIMELINE_SHADOW_CACHE_FILE);
    }

    private static File getTimelineShadowCacheMetaFile() {
        return new File("/data/data/com.weico.international/files", TIMELINE_SHADOW_CACHE_META_FILE);
    }

    private static void forgetTimelinePreloadDone(String source) {
        try {
            File marker = getPreloadDoneFile();
            if (marker != null && marker.exists()) marker.delete();
            sTimelinePreloadDone = Boolean.FALSE;
            sTimelineRestoredCacheMode = false;
            log("Timeline preload marker cleared source=" + source);
        } catch (Throwable t) {
            sTimelinePreloadDone = Boolean.FALSE;
            log("Timeline preload marker clear error source=" + source + ": " + t.getMessage());
        }
    }

    private static void forgetTimelineLastRead(String source) {
        try {
            File file = getLastReadFile();
            if (file != null && file.exists()) file.delete();
            sLastReadStatusId = Long.valueOf(0L);
            sLastReadMarkerShown = false;
            log("Timeline last-read cleared source=" + source);
        } catch (Throwable t) {
            sLastReadStatusId = Long.valueOf(0L);
            log("Timeline last-read clear error source=" + source + ": " + t.getMessage());
        }
    }

    private static boolean restoreTimelineNativeCache(Object presenter, String source) {
        try {
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return false;
            if (getTimelineStatusCount(presenter) >= PRELOAD_DONE_MIN_ITEMS) return false;

            Object action = getTimelineAction(presenter);
            if (action == null) {
                log("Timeline full cache restore skipped source=" + source + " no action");
                return false;
            }

            Object builder = getTimelineCacheBuilder(action);
            Class<?> diskCacheClass = Class.forName("com.weico.diskcache.DiskCache", false, action.getClass().getClassLoader());
            Object cache = callStaticNoArg(diskCacheClass, "getInstance");
            Object cached = XposedHelpers.callMethod(cache, "get", builder);
            if (cached == null) {
                cached = restoreTimelineShadowCache(cache, builder, source);
                if (cached == null) {
                    log("Timeline full cache restore miss source=" + source);
                    forgetTimelinePreloadDone("restore-miss");
                    return false;
                }
            }

            Object cachedStatuses = XposedHelpers.callMethod(cached, "getStatuses");
            if (!(cachedStatuses instanceof List)) {
                log("Timeline full cache restore invalid source=" + source);
                forgetTimelinePreloadDone("restore-invalid");
                return false;
            }

            ArrayList statuses = collectTimelineStatuses((List) cachedStatuses);
            hydrateTimelineStatusText(statuses, "reweibo-cache");
            List filtered = filterTimelineAds(statuses, presenter, "reweibo-cache");
            filtered = filterTimelineContentless(filtered, presenter, "reweibo-cache");
            List sorted = sortTimelineNewestFirst(filtered, presenter, "reweibo-cache", false);
            int count = countTimelineStatuses(sorted);
            if (count < TIMELINE_CACHE_MIN_ITEMS) {
                log("Timeline full cache restore ignored source=" + source + " count=" + count);
                forgetTimelinePreloadDone("restore-too-small");
                return false;
            }
            replaceTimelineCumulativeStatuses(sorted, presenter, "reweibo-cache");
            syncTimelineActionMaxId(action, sorted, "reweibo-cache");
            if (!isTimelinePreloadDone() && count >= PRELOAD_RESTORED_TRUST_MIN_ITEMS) {
                markTimelinePreloadDone("restore-large-cache", 0, count);
            }

            setTimelineCacheRestoring(presenter, true);
            try {
                setPresenterTimelineData(presenter, sorted, "reweibo-cache");
            } finally {
                setTimelineCacheRestoring(presenter, false);
            }

            synchronized (sPersistedTimelineCacheCounts) {
                sPersistedTimelineCacheCounts.put(presenter, Integer.valueOf(count));
            }
            sTimelineOldestFirstMode = false;
            sTimelineRestoredCacheMode = true;
            log("Timeline full cache restored source=" + source + " count=" + count);
            return true;
        } catch (Throwable t) {
            log("Timeline full cache restore error source=" + source + ": " + t.getMessage());
            forgetTimelinePreloadDone("restore-error");
            return false;
        }
    }

    private static void setPresenterTimelineData(Object presenter, List data, String source) throws Exception {
        hydrateTimelineStatusText(data, source);
        Throwable delegateError = null;
        try {
            Object delegate = getFieldValue(presenter, "statusPresenterDelegate");
            XposedHelpers.callMethod(delegate, "setData", data);
        } catch (Throwable t) {
            delegateError = t;
        }

        int expected = countTimelineStatuses(data);
        if (getTimelineStatusCount(presenter) < Math.min(PRELOAD_DONE_MIN_ITEMS, expected)) {
            try {
                XposedHelpers.callMethod(presenter, "setData", data);
            } catch (Throwable t) {
                if (delegateError != null) throw new Exception(delegateError);
                throw new Exception(t);
            }
        }
        log("Presenter timeline restored from cache source=" + source + " size=" + data.size());
    }

    private static boolean isTimelineCacheRestoring(Object presenter) {
        synchronized (sRestoringTimelineCaches) {
            return sRestoringTimelineCaches.containsKey(presenter);
        }
    }

    private static void setTimelineCacheRestoring(Object presenter, boolean restoring) {
        synchronized (sRestoringTimelineCaches) {
            if (restoring) {
                sRestoringTimelineCaches.put(presenter, Boolean.TRUE);
            } else {
                sRestoringTimelineCaches.remove(presenter);
            }
        }
    }

    private static Object getTimelineAction(Object presenter) {
        try {
            return XposedHelpers.callMethod(presenter, "getAction");
        } catch (Throwable ignored) {}
        try {
            return getFieldValue(presenter, "mAction");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object newTimelineStatusResult(Object action, ArrayList statuses, String maxId) throws Exception {
        ClassLoader cl = action.getClass().getClassLoader();
        Class<?> statusResultClass = Class.forName("com.weico.international.model.sina.StatusResult", false, cl);
        String sinceId = getStringFieldOrFallback(action, "sinId", "0");
        return statusResultClass
            .getConstructor(ArrayList.class, String.class, String.class)
            .newInstance(statuses, sinceId, maxId);
    }

    private static Object getTimelineCacheBuilder(Object action) throws Exception {
        return buildTimelineCacheBuilder(action);
    }

    private static Object buildTimelineCacheBuilder(Object action) throws Exception {
        ClassLoader cl = action.getClass().getClassLoader();
        Class<?> statusResultClass = Class.forName("com.weico.international.model.sina.StatusResult", false, cl);
        Class<?> diskCacheClass = Class.forName("com.weico.diskcache.DiskCache", false, cl);
        String groupId = getTimelineGroupId(action);
        if (groupId == null) groupId = "-1";
        Class<?> accountsStoreClass = Class.forName("com.weico.international.manager.accounts.AccountsStore", false, cl);
        Object userId = callStaticNoArg(accountsStoreClass, "getCurUserId");
        Object builder = callStatic(
            diskCacheClass,
            "fastGetBuilder",
            new Class<?>[] {java.lang.reflect.Type.class, String.class, String.class},
            new Object[] {
            statusResultClass,
            TIMELINE_FULL_CACHE_PREFIX + groupId,
            String.valueOf(userId)
            }
        );

        Class<?> expireKeyClass = Class.forName("com.weico.diskcache.impl.ExpireKey", false, cl);
        Object expireKey = expireKeyClass.getConstructor(long.class).newInstance(Long.valueOf(0xf731400L));
        builder = XposedHelpers.callMethod(builder, "with", expireKey);

        Class<?> dataCacheClass = Class.forName("com.weico.international.manager.DataCache.DataCache", false, cl);
        Object dataCachePath = getStaticFieldValue(dataCacheClass, "DATA_CACHE_PATH");
        Class<?> customPathClass = Class.forName("com.weico.diskcache.impl.CustomCachePath", false, cl);
        Object customPath = customPathClass.getConstructor(String.class).newInstance(String.valueOf(dataCachePath));
        return XposedHelpers.callMethod(builder, "with", customPath);
    }

    private static Object callStaticNoArg(Class<?> clazz, String methodName) throws Exception {
        return callStatic(clazz, methodName, new Class<?>[0], new Object[0]);
    }

    private static Object callStatic(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        java.lang.reflect.Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object getStaticFieldValue(Class<?> clazz, String fieldName) throws Exception {
        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    private static String getTimelineCacheMaxId(Object action, ArrayList statuses) {
        try {
            Object value = XposedHelpers.callMethod(action, "quickGetMaxId", statuses);
            if (value != null) return String.valueOf(value);
        } catch (Throwable ignored) {}
        if (!statuses.isEmpty()) {
            long id = getStatusId(statuses.get(statuses.size() - 1));
            if (id > 0) return String.valueOf(id);
        }
        return getStringFieldOrFallback(action, "maxId", "0");
    }

    private static void syncTimelineActionMaxId(Object action, List statuses, String source) {
        if (statuses == null) return;
        syncTimelineActionMaxId(action, getTimelineCacheMaxId(action, new ArrayList(statuses)), source);
    }

    private static void syncTimelineActionMaxId(Object action, String maxId, String source) {
        if (action == null || !hasMeaningfulString(maxId) || "0".equals(maxId)) return;
        boolean changed = false;
        try {
            XposedHelpers.callMethod(action, "setMaxId", maxId);
            changed = true;
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.callMethod(action, "setMaxId", Long.valueOf(maxId));
            changed = true;
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.callMethod(action, "setMax_id", maxId);
            changed = true;
        } catch (Throwable ignored) {}
        try {
            XposedHelpers.callMethod(action, "setMax_id", Long.valueOf(maxId));
            changed = true;
        } catch (Throwable ignored) {}

        String[] fields = new String[] {
            "lastMaxId", "maxId", "mMaxId", "max_id", "maxid", "maxID", "maxIdStr", "max_id_str"
        };
        for (int i = 0; i < fields.length; i++) {
            changed = setTimelineCursorField(action, fields[i], maxId) || changed;
        }

        if (changed) {
            log("Timeline cursor synced source=" + source + " maxId=" + maxId);
        } else {
            log("Timeline cursor sync skipped source=" + source + " maxId=" + maxId);
            dumpTimelineCursorFieldsOnce(action, source);
        }
    }

    private static boolean setTimelineCursorField(Object target, String fieldName, String value) {
        if (target == null || fieldName == null || value == null) return false;
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                Class<?> type = field.getType();
                if (type == String.class || type.isAssignableFrom(String.class)) {
                    field.set(target, value);
                } else if (type == long.class || type == Long.class) {
                    field.set(target, Long.valueOf(value));
                } else if (type == int.class || type == Integer.class) {
                    field.set(target, Integer.valueOf(value));
                } else {
                    return false;
                }
                return true;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (Throwable ignored) {
                return false;
            }
        }
        return false;
    }

    private static void dumpTimelineCursorFieldsOnce(Object target, String source) {
        if (target == null) return;
        if (sTimelineCursorActionDumped) return;
        sTimelineCursorActionDumped = true;

        try {
            log("Timeline cursor field dump source=" + source + " class=" + target.getClass().getName());
            Class<?> clazz = target.getClass();
            int emitted = 0;
            while (clazz != null && emitted < 80) {
                java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
                for (int i = 0; i < fields.length && emitted < 80; i++) {
                    java.lang.reflect.Field field = fields[i];
                    field.setAccessible(true);
                    Class<?> type = field.getType();
                    String name = field.getName();
                    Object value = null;
                    boolean simple = type.isPrimitive()
                        || type == String.class
                        || Number.class.isAssignableFrom(type)
                        || type == Boolean.class
                        || name.toLowerCase(Locale.US).contains("id")
                        || name.toLowerCase(Locale.US).contains("max")
                        || name.toLowerCase(Locale.US).contains("since")
                        || name.toLowerCase(Locale.US).contains("page")
                        || name.toLowerCase(Locale.US).contains("cursor");
                    if (!simple) continue;
                    try {
                        value = field.get(target);
                    } catch (Throwable ignored) {}
                    log("Timeline cursor field source=" + source + " field=" + clazz.getSimpleName()
                        + "." + name + " type=" + type.getName() + " value=" + value);
                    emitted++;
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Throwable t) {
            log("Timeline cursor field dump error source=" + source + ": " + t.getMessage());
        }
    }

    private static String getStringFieldOrFallback(Object target, String fieldName, String fallback) {
        try {
            Object value = getFieldValue(target, fieldName);
            return value == null ? fallback : String.valueOf(value);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int countTimelineStatuses(List list) {
        return collectTimelineStatuses(list).size();
    }

    private static ArrayList mergeTimelineCumulativeStatuses(List list, Object owner, String source) {
        ArrayList incoming = collectTimelineStatuses(list);
        if (!"-1".equals(getTimelineGroupId(owner))) return incoming;
        synchronized (sTimelineCumulativeStatusesById) {
            int before = sTimelineCumulativeStatusesById.size();
            for (int i = 0; i < incoming.size(); i++) {
                Object status = unwrapStatus(incoming.get(i));
                long id = getStatusId(status);
                if (status == null || id <= 0L) continue;
                sTimelineCumulativeStatusesById.put(Long.valueOf(id), status);
            }
            ArrayList snapshot = new ArrayList(sTimelineCumulativeStatusesById.values());
            List sorted = sortTimelineNewestFirst(snapshot, owner, source + "-cumulative", false);
            replaceTimelineCumulativeStatusesLocked(sorted);
            int after = sTimelineCumulativeStatusesById.size();
            if (after != before && (source.contains("addData") || after >= PRELOAD_DONE_MIN_ITEMS)) {
                log("Timeline cumulative cache merged source=" + source + " incoming=" + incoming.size()
                    + " count=" + after + " previous=" + before);
            }
            return new ArrayList(sTimelineCumulativeStatusesById.values());
        }
    }

    private static void replaceTimelineCumulativeStatuses(List list, Object owner, String source) {
        if (!"-1".equals(getTimelineGroupId(owner))) return;
        synchronized (sTimelineCumulativeStatusesById) {
            replaceTimelineCumulativeStatusesLocked(list);
            log("Timeline cumulative cache replaced source=" + source
                + " count=" + sTimelineCumulativeStatusesById.size());
        }
    }

    private static void replaceTimelineCumulativeStatusesLocked(List list) {
        sTimelineCumulativeStatusesById.clear();
        if (list == null) return;
        int limit = Math.min(list.size(), PRELOAD_MAX_ITEMS);
        for (int i = 0; i < limit; i++) {
            Object status = unwrapStatus(list.get(i));
            long id = getStatusId(status);
            if (status == null || id <= 0L || isLoadMoreStatus(status) || isTimelineAdStatus(status)
                || isTimelineContentlessStatus(status)) {
                continue;
            }
            sTimelineCumulativeStatusesById.put(Long.valueOf(id), status);
        }
    }

    private static int getTimelineCumulativeStatusCount() {
        synchronized (sTimelineCumulativeStatusesById) {
            return sTimelineCumulativeStatusesById.size();
        }
    }

    private static ArrayList collectTimelineStatuses(List list) {
        ArrayList statuses = new ArrayList();
        ArrayList ids = new ArrayList();
        if (list == null) return statuses;

        for (int i = 0; i < list.size(); i++) {
            Object status = unwrapStatus(list.get(i));
            long id = getStatusId(status);
            if (status == null || id <= 0 || isLoadMoreStatus(status)) {
                continue;
            }
            if (isTimelineAdStatus(status) || isTimelineContentlessStatus(status)) {
                continue;
            }
            Long key = Long.valueOf(id);
            if (ids.contains(key)) continue;
            ids.add(key);
            statuses.add(status);
        }
        return statuses;
    }

    private static List filterTimelineAds(List list, Object owner, String source) {
        if (list == null || list.isEmpty()) return list;

        String groupId = getTimelineGroupId(owner);
        if (!"-1".equals(groupId)) return list;

        int removed = 0;
        for (int i = 0; i < list.size(); i++) {
            Object status = unwrapStatus(list.get(i));
            if (status != null && isTimelineAdStatus(status)) {
                removed++;
            }
        }
        if (removed == 0) return list;

        ArrayList copy = new ArrayList(list.size() - removed);
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object status = unwrapStatus(item);
            if (status == null || !isTimelineAdStatus(status)) {
                copy.add(item);
            }
        }

        log("Timeline ads filtered source=" + source + " group=" + groupId + " removed=" + removed
            + " size=" + list.size() + " kept=" + copy.size());
        return copy;
    }

    private static List filterTimelineContentless(List list, Object owner, String source) {
        if (list == null || list.isEmpty()) return list;

        String groupId = getTimelineGroupId(owner);
        if (!"-1".equals(groupId)) return list;

        int removed = 0;
        String sampleIds = "";
        for (int i = 0; i < list.size(); i++) {
            Object status = unwrapStatus(list.get(i));
            if (status != null && isTimelineContentlessStatus(status)) {
                if (removed < 3) {
                    if (sampleIds.length() > 0) sampleIds += ",";
                    sampleIds += String.valueOf(getStatusId(status));
                }
                removed++;
            }
        }
        if (removed == 0) return list;

        ArrayList copy = new ArrayList(list.size() - removed);
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object status = unwrapStatus(item);
            if (status == null || !isTimelineContentlessStatus(status)) {
                copy.add(item);
            }
        }

        log("Timeline empty statuses filtered source=" + source + " group=" + groupId
            + " removed=" + removed + " size=" + list.size() + " kept=" + copy.size()
            + " sample=" + sampleIds);
        return copy;
    }

    private static int hydrateTimelineStatusText(List list, String source) {
        if (list == null || list.isEmpty()) return 0;
        int changed = 0;
        for (int i = 0; i < list.size(); i++) {
            changed += hydrateStatusText(unwrapStatus(list.get(i)), 0);
        }
        if (changed > 0) {
            log("Timeline text hydrated source=" + source + " count=" + changed);
        }
        return changed + hydrateTimelineStatusMedia(list, source);
    }

    private static int hydrateStatusText(Object status, int depth) {
        if (status == null || depth > 2) return 0;
        int changed = 0;
        try {
            Object decorated = getObjectMethodOrField(status, null, "decTextSapnned");
            String text = getStringMethodOrField(status, "getText", "text");
            if (!hasNonEmptyObject(decorated) && hasMeaningfulString(text)) {
                setFieldValue(status, "decTextSapnned", android.text.Html.fromHtml(text));
                changed++;
            }
        } catch (Throwable ignored) {}
        try {
            Object decorated = getObjectMethodOrField(status, null, "decTrTextSapnned");
            String text = getStringMethodOrField(status, "getTanslateText", "translateText");
            if (!hasNonEmptyObject(decorated) && hasMeaningfulString(text)) {
                setFieldValue(status, "decTrTextSapnned", android.text.Html.fromHtml(text));
                changed++;
            }
        } catch (Throwable ignored) {}

        changed += hydrateStatusText(getObjectMethodOrField(status, "getRetweeted_status", "retweeted_status"), depth + 1);
        changed += hydrateStatusText(getObjectMethodOrField(status, null, "reprinted_status"), depth + 1);
        return changed;
    }

    private static int hydrateTimelineStatusMedia(List list, String source) {
        if (list == null || list.isEmpty()) return 0;
        int changed = 0;
        for (int i = 0; i < list.size(); i++) {
            changed += hydrateStatusMedia(unwrapStatus(list.get(i)), 0);
        }
        if (changed > 0) {
            log("Timeline media hydrated source=" + source + " count=" + changed);
        }
        return changed;
    }

    private static int hydrateStatusMedia(Object status, int depth) {
        if (status == null || depth > 2) return 0;
        if (!hasTimelinePicSource(status, depth)) return 0;

        int before = countTimelineDisplayImages(status, depth);
        try {
            Class<?> statusClass = Class.forName(
                "com.weico.international.model.sina.Status",
                false,
                status.getClass().getClassLoader()
            );
            java.lang.reflect.Method method = statusClass.getDeclaredMethod("transformPicUrls", statusClass);
            method.setAccessible(true);
            method.invoke(null, status);
            resetTimelineStatusViewType(status, depth);
            int after = countTimelineDisplayImages(status, depth);
            return after > before ? 1 : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static boolean hasTimelinePicSource(Object status, int depth) {
        if (status == null || depth > 2) return false;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "pic_ids"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, "getPic_detail_infos", "pic_detail_infos"))) return true;
        if (hasMeaningfulString(getStringMethodOrField(status, "getThumbnail_pic", "thumbnail_pic"))) return true;
        if (hasMeaningfulString(getStringMethodOrField(status, "getBmiddle_pic", "bmiddle_pic"))) return true;
        if (hasMeaningfulString(getStringMethodOrField(status, "getOriginal_pic", "original_pic"))) return true;
        if (hasTimelinePicSource(getObjectMethodOrField(status, "getRetweeted_status", "retweeted_status"), depth + 1)) {
            return true;
        }
        return hasTimelinePicSource(getObjectMethodOrField(status, null, "reprinted_status"), depth + 1);
    }

    private static int countTimelineDisplayImages(Object status, int depth) {
        if (status == null || depth > 2) return 0;
        int count = objectItemCount(getObjectMethodOrField(status, null, "picPathUrls"));
        count += objectItemCount(getObjectMethodOrField(status, null, "thumbPicPathUrls"));
        count += objectItemCount(getObjectMethodOrField(status, null, "largePicPathUrls"));
        if (hasMeaningfulString(getStringMethodOrField(status, "getThumbnail_pic", "thumbnail_pic"))) count++;
        count += countTimelineDisplayImages(getObjectMethodOrField(status, "getRetweeted_status", "retweeted_status"), depth + 1);
        count += countTimelineDisplayImages(getObjectMethodOrField(status, null, "reprinted_status"), depth + 1);
        return count;
    }

    private static int objectItemCount(Object value) {
        if (value == null) return 0;
        if (value instanceof java.util.Collection) return ((java.util.Collection) value).size();
        if (value instanceof Map) return ((Map) value).size();
        Class<?> clazz = value.getClass();
        if (clazz.isArray()) return java.lang.reflect.Array.getLength(value);
        return hasNonEmptyObject(value) ? 1 : 0;
    }

    private static void resetTimelineStatusViewType(Object status, int depth) {
        if (status == null || depth > 2) return;
        try {
            setFieldValue(status, "viewType", Integer.valueOf(0));
        } catch (Throwable ignored) {}
        resetTimelineStatusViewType(getObjectMethodOrField(status, "getRetweeted_status", "retweeted_status"), depth + 1);
        resetTimelineStatusViewType(getObjectMethodOrField(status, null, "reprinted_status"), depth + 1);
    }

    private static boolean replaceListContents(List target, List replacement, String source) {
        try {
            target.clear();
            target.addAll(replacement);
            return true;
        } catch (Throwable t) {
            log("Timeline ads filtered copy could not replace source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static void rememberTimelineRecyclerView(Object recyclerView) {
        synchronized (sTopAnchorStates) {
            sTimelineRecyclerViews.put(recyclerView, Boolean.TRUE);
        }
    }

    private static Object getAnyTimelineRecyclerView() {
        synchronized (sTopAnchorStates) {
            ArrayList recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
            for (int i = 0; i < recyclerViews.size(); i++) {
                Object recyclerView = recyclerViews.get(i);
                if (isTimelineRecyclerView(recyclerView)) return recyclerView;
            }
        }
        return null;
    }

    private static TopAnchorState getTopAnchorStateLocked(Object recyclerView) {
        TopAnchorState state = sTopAnchorStates.get(recyclerView);
        if (state == null) {
            state = new TopAnchorState();
            sTopAnchorStates.put(recyclerView, state);
        }
        return state;
    }

    private static boolean isTimelineRecyclerView(Object recyclerView) {
        if (recyclerView == null) return false;
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            return adapter != null && adapter.getClass().getName().contains("TimelineAdapter");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void beginTimelineTopAnchorForKnownRecyclerViews(String source, boolean resetUserTouch) {
        ArrayList recyclerViews;
        synchronized (sTopAnchorStates) {
            recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
        }
        for (int i = 0; i < recyclerViews.size(); i++) {
            beginTimelineTopAnchor(recyclerViews.get(i), source, resetUserTouch);
        }
    }

    private static void jumpTimelineToAbsoluteTopForKnownRecyclerViews(String source) {
        ArrayList recyclerViews;
        synchronized (sTopAnchorStates) {
            recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
        }
        boolean jumped = false;
        for (int i = 0; i < recyclerViews.size(); i++) {
            jumped |= jumpTimelineToAbsoluteTop(recyclerViews.get(i), source, 0);
        }
        if (!jumped) {
            log("Timeline absolute top jump skipped source=" + source + " no recycler");
        }
    }

    private static boolean jumpTimelineToAbsoluteTop(final Object recyclerView, final String source, final int attempt) {
        try {
            if (!isTimelineRecyclerView(recyclerView)) return false;

            int target = getTimelineAbsoluteTopAdapterPosition(recyclerView);
            if (target < 0) {
                log("Timeline absolute top jump skipped source=" + source + " no target");
                return false;
            }

            Object layoutManager = XposedHelpers.callMethod(recyclerView, "getLayoutManager");
            int offset = getTimelineTopOffset(recyclerView);
            boolean usedOffset = false;
            try {
                XposedHelpers.callMethod(layoutManager, "scrollToPositionWithOffset", target, offset);
                usedOffset = true;
            } catch (Throwable ignored) {
                XposedHelpers.callMethod(recyclerView, "scrollToPosition", target);
            }
            if (!isTimelineTargetVisible(layoutManager, target)) {
                try {
                    XposedHelpers.callMethod(recyclerView, "scrollToPosition", target);
                } catch (Throwable ignored) {}
                scrollTimelineTowardTarget(recyclerView, layoutManager, target);
            }

            synchronized (sTopAnchorStates) {
                TopAnchorState state = getTopAnchorStateLocked(recyclerView);
                state.userTouched = true;
                state.untilElapsedMs = 0L;
                state.attempts = TOP_ANCHOR_MAX_ATTEMPTS;
            }
            log("Timeline absolute top jumped source=" + source + " target=" + target
                + " attempt=" + attempt + " offset=" + offset + " usedOffset=" + usedOffset + " "
                + describeTimelineViewport(recyclerView, layoutManager));
            if (!isTimelineTargetVisible(layoutManager, target) && attempt < TOP_BAR_JUMP_MAX_ATTEMPTS) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        jumpTimelineToAbsoluteTop(recyclerView, source, attempt + 1);
                    }
                }, TOP_BAR_JUMP_RETRY_MS);
            }
            return true;
        } catch (Throwable t) {
            log("Timeline absolute top jump error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static int getTimelineAbsoluteTopAdapterPosition(Object recyclerView) {
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            if (adapter == null) return -1;

            int dataCount = callIntMethodSafe(adapter, "getCount", -1);
            int headerCount = callIntMethodSafe(adapter, "getHeaderCount", 0);
            if (dataCount >= TIMELINE_CACHE_MIN_ITEMS) {
                if (sTimelineOldestFirstMode) return Math.max(0, headerCount);
                return Math.max(0, headerCount) + dataCount - 1;
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static void beginTimelineTopAnchor(Object recyclerView, String source, boolean resetUserTouch) {
        if (!isTimelineRecyclerView(recyclerView)) return;
        synchronized (sTopAnchorStates) {
            TopAnchorState state = getTopAnchorStateLocked(recyclerView);
            if (state.userTouched && !resetUserTouch) return;
            state.generation++;
            state.attempts = 0;
            state.lastTarget = -1;
            state.untilElapsedMs = SystemClock.elapsedRealtime() + TOP_ANCHOR_WINDOW_MS;
            if (resetUserTouch) state.userTouched = false;
            state.finishing = false;
        }
        beginTimelineLoadMoreSuppression(source);
        log("Timeline top anchor begin source=" + source);
        scheduleTimelineTopAnchor(recyclerView, source, 50L);
        scheduleTimelineTopAnchor(recyclerView, source, 350L);
        scheduleTimelineTopAnchor(recyclerView, source, 900L);
    }

    private static void finishTimelineTopAnchorForKnownRecyclerViews(String source) {
        ArrayList recyclerViews;
        synchronized (sTopAnchorStates) {
            recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
        }
        for (int i = 0; i < recyclerViews.size(); i++) {
            finishTimelineTopAnchor(recyclerViews.get(i), source);
        }
    }

    private static void finishTimelineTopAnchor(Object recyclerView, String source) {
        if (!isTimelineRecyclerView(recyclerView)) return;
        synchronized (sTopAnchorStates) {
            TopAnchorState state = getTopAnchorStateLocked(recyclerView);
            if (state.userTouched) return;
            state.generation++;
            state.attempts = 0;
            state.lastTarget = -1;
            state.untilElapsedMs = SystemClock.elapsedRealtime() + 1500L;
            state.finishing = true;
        }
        beginTimelineLoadMoreSuppression(source);
        log("Timeline top anchor finish source=" + source);
        scheduleTimelineTopAnchor(recyclerView, source, 120L);
    }

    private static void scheduleTimelineTopAnchor(final Object recyclerView, final String source, long delayMs) {
        final int generation;
        synchronized (sTopAnchorStates) {
            TopAnchorState state = sTopAnchorStates.get(recyclerView);
            if (state == null || state.userTouched) return;
            if (SystemClock.elapsedRealtime() > state.untilElapsedMs) return;
            if (state.attempts >= TOP_ANCHOR_MAX_ATTEMPTS) return;
            generation = state.generation;
            state.attempts++;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                anchorTimelineTop(recyclerView, source, generation);
            }
        }, delayMs);
    }

    private static void anchorTimelineTop(Object recyclerView, String source, int generation) {
        try {
            synchronized (sTopAnchorStates) {
                TopAnchorState state = sTopAnchorStates.get(recyclerView);
                if (state == null || state.generation != generation || state.userTouched) return;
                if (SystemClock.elapsedRealtime() > state.untilElapsedMs) return;
            }
            if (!isTimelineRecyclerView(recyclerView)) return;

            int target = getTimelineTopAdapterPosition(recyclerView);
            if (target < 0) {
                scheduleTimelineTopAnchor(recyclerView, source, 250L);
                return;
            }

            Object layoutManager = XposedHelpers.callMethod(recyclerView, "getLayoutManager");
            int offset = getTimelineTopOffset(recyclerView);
            boolean usedOffset = false;
            boolean restoredCache = sTimelineRestoredCacheMode;
            try {
                int layoutOffset = restoredCache
                    ? getTimelineLayoutManagerOffset(recyclerView, layoutManager, offset)
                    : offset;
                XposedHelpers.callMethod(layoutManager, "scrollToPositionWithOffset", target, layoutOffset);
                usedOffset = true;
            } catch (Throwable ignored) {
                XposedHelpers.callMethod(recyclerView, "scrollToPosition", target);
            }
            if (!isTimelineTargetVisible(layoutManager, target)) {
                if (restoredCache && usedOffset) {
                    int attempts;
                    synchronized (sTopAnchorStates) {
                        TopAnchorState state = sTopAnchorStates.get(recyclerView);
                        attempts = state == null ? 0 : state.attempts;
                    }
                    if (attempts < 8) {
                        scheduleTimelineTopAnchor(recyclerView, source, 80L);
                        return;
                    }
                }
                if (!sTimelineRestoredCacheMode) {
                    try {
                        XposedHelpers.callMethod(layoutManager, "scrollToPosition", target);
                    } catch (Throwable ignored) {}
                    try {
                        XposedHelpers.callMethod(recyclerView, "scrollToPosition", target);
                    } catch (Throwable ignored) {}
                    try {
                        XposedHelpers.callMethod(recyclerView, "smoothScrollToPosition", target);
                    } catch (Throwable ignored) {}
                }
                scrollTimelineTowardTarget(recyclerView, layoutManager, target);
            }
            boolean targetVisible = isTimelineTargetVisible(layoutManager, target);

            boolean logTarget = false;
            synchronized (sTopAnchorStates) {
                TopAnchorState state = sTopAnchorStates.get(recyclerView);
                if (state != null && state.generation == generation && state.lastTarget != target) {
                    state.lastTarget = target;
                    logTarget = true;
                }
            }
            if (sTimelineRestoredCacheMode && source.contains("cache-restore") && !isTimelinePreloadDone()) {
                logTarget = true;
            }
            boolean shouldRepeat = false;
            synchronized (sTopAnchorStates) {
                TopAnchorState state = sTopAnchorStates.get(recyclerView);
                if (state != null && state.generation == generation && !state.userTouched) {
                    if ((state.finishing || restoredCache) && targetVisible) {
                        state.userTouched = true;
                    }
                    shouldRepeat = !state.userTouched;
                }
            }
            if (targetVisible && isTimelineLastReadTarget(recyclerView, target)) {
                showTimelineLastReadMarker(recyclerView);
            }
            if (logTarget) {
                log("Timeline top anchored source=" + source + " target=" + target + " offset=" + offset
                    + " usedOffset=" + usedOffset
                    + " " + describeTimelineViewport(recyclerView, layoutManager));
            }
            if (shouldRepeat) {
                scheduleTimelineTopAnchor(recyclerView, source, 250L);
            }
        } catch (Throwable t) {
            log("Timeline top anchor error source=" + source + ": " + t.getMessage());
        }
    }

    private static boolean shouldAnchorTimelineTop(Object recyclerView) {
        if (!isTimelineRecyclerView(recyclerView)) return false;
        synchronized (sTopAnchorStates) {
            TopAnchorState state = sTopAnchorStates.get(recyclerView);
            return state != null && !state.userTouched
                && SystemClock.elapsedRealtime() <= state.untilElapsedMs
                && state.attempts < TOP_ANCHOR_MAX_ATTEMPTS;
        }
    }

    private static int getTimelineTopAdapterPosition(Object recyclerView) {
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            if (adapter == null) return -1;

            int dataCount = callIntMethodSafe(adapter, "getCount", -1);
            if (sTimelineRestoredCacheMode && dataCount >= 0 && dataCount < TIMELINE_CACHE_MIN_ITEMS) {
                return -1;
            }
            if (dataCount >= 10) {
                int headerCount = callIntMethodSafe(adapter, "getHeaderCount", 0);
                int lastReadPosition = getTimelineLastReadAdapterPosition(adapter, headerCount);
                if (lastReadPosition >= 0) return lastReadPosition;
                if (sTimelineOldestFirstMode) return Math.max(0, headerCount);
                return headerCount + dataCount - 1;
            }

            int itemCount = callIntMethodSafe(adapter, "getItemCount", -1);
            if (sTimelineOldestFirstMode) return 0;
            return itemCount >= 10 ? itemCount - 1 : -1;
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int getTimelineLastReadAdapterPosition(Object adapter, int headerCount) {
        try {
            if (!sTimelineRestoredCacheMode || !isTimelinePreloadDone()) return -1;
            long statusId = getLastReadStatusId();
            if (statusId <= 0 || adapter == null) return -1;

            int dataCount = callIntMethodSafe(adapter, "getCount", -1);
            if (dataCount < 1) return -1;
            for (int i = 0; i < dataCount; i++) {
                long itemId = getTimelineAdapterDataStatusId(adapter, i);
                if (itemId == statusId) {
                    return Math.max(0, headerCount) + i;
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    private static String describeTimelineViewport(Object recyclerView, Object layoutManager) {
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            int dataCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getCount", -1);
            int itemCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getItemCount", -1);
            int headerCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getHeaderCount", -1);
            int first = callIntMethodSafe(layoutManager, "findFirstVisibleItemPosition", -1);
            int last = callIntMethodSafe(layoutManager, "findLastVisibleItemPosition", -1);
            return "viewport first=" + first + " last=" + last + " data=" + dataCount
                + " item=" + itemCount + " headers=" + headerCount
                + " firstItem=" + describeTimelineAdapterItem(adapter, first, headerCount)
                + " lastItem=" + describeTimelineAdapterItem(adapter, last, headerCount);
        } catch (Throwable ignored) {
            return "viewport unknown";
        }
    }

    private static String describeTimelineAdapterItem(Object adapter, int adapterPosition, int headerCount) {
        if (adapter == null || adapterPosition < 0) return "-";
        try {
            int dataPosition = adapterPosition - Math.max(0, headerCount);
            if (dataPosition < 0) return "header";
            long id = getTimelineAdapterDataStatusId(adapter, dataPosition);
            String user = "-";
            try {
                Object item = XposedHelpers.callMethod(adapter, "getItem", dataPosition);
                Object status = unwrapStatus(item);
                Object userObj = XposedHelpers.callMethod(status, "getUser");
                Object name = XposedHelpers.callMethod(userObj, "getScreenName");
                if (name != null) user = String.valueOf(name);
            } catch (Throwable ignored) {}
            return dataPosition + ":" + id + ":" + user;
        } catch (Throwable ignored) {
            return "?";
        }
    }

    private static long getTimelineAdapterDataStatusId(Object adapter, int dataPosition) {
        if (adapter == null || dataPosition < 0) return 0L;
        try {
            Object item = XposedHelpers.callMethod(adapter, "getItem", dataPosition);
            return getStatusId(unwrapStatus(item));
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static void markTimelineLastReadTouched(Object recyclerView) {
        if (!isTimelineRecyclerView(recyclerView)) return;
        synchronized (sTimelineUserMovedAt) {
            sTimelineUserMovedAt.put(recyclerView, Long.valueOf(SystemClock.elapsedRealtime()));
        }
    }

    private static boolean hasTimelineLastReadTouch(Object recyclerView) {
        synchronized (sTimelineUserMovedAt) {
            Long movedAt = sTimelineUserMovedAt.get(recyclerView);
            return movedAt != null && SystemClock.elapsedRealtime() - movedAt.longValue() <= 6000L;
        }
    }

    private static void schedulePersistTimelineLastRead(final Object recyclerView, final String source, long delayMs) {
        if (!isTimelineRecyclerView(recyclerView) || !hasTimelineLastReadTouch(recyclerView)) return;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                persistTimelineLastRead(recyclerView, source);
            }
        }, delayMs);
    }

    private static void persistTimelineLastRead(Object recyclerView, String source) {
        try {
            if (!isTimelineRecyclerView(recyclerView) || !hasTimelineLastReadTouch(recyclerView)) return;
            if (!sTimelineRestoredCacheMode || !isTimelinePreloadDone()) return;
            long now = SystemClock.elapsedRealtime();
            if (now - sLastReadPersistAtMs < 1000L) return;

            Object layoutManager = XposedHelpers.callMethod(recyclerView, "getLayoutManager");
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            int headerCount = adapter == null ? 0 : callIntMethodSafe(adapter, "getHeaderCount", 0);
            int dataCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getCount", -1);
            int first = callIntMethodSafe(layoutManager, "findFirstVisibleItemPosition", -1);
            int last = callIntMethodSafe(layoutManager, "findLastVisibleItemPosition", -1);
            int position = findVisibleTimelineStatusPosition(adapter, headerCount, first, last);
            if (position < 0) return;

            int dataPosition = position - Math.max(0, headerCount);
            long statusId = getTimelineAdapterDataStatusId(adapter, dataPosition);
            if (statusId <= 0L) return;

            File file = getLastReadFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileWriter fw = new FileWriter(file, false);
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            fw.write("saved_at=" + ts + "\n");
            fw.write("source=" + source + "\n");
            fw.write("preload_done=true\n");
            fw.write("cache_count=" + dataCount + "\n");
            fw.write("status_id=" + statusId + "\n");
            fw.write("adapter_position=" + position + "\n");
            fw.write("data_position=" + dataPosition + "\n");
            fw.close();

            sLastReadPersistAtMs = now;
            sLastReadStatusId = Long.valueOf(statusId);
            log("Timeline last-read saved source=" + source + " id=" + statusId
                + " position=" + position + " visible=" + first + ".." + last);
        } catch (Throwable t) {
            log("Timeline last-read save error source=" + source + ": " + t.getMessage());
        }
    }

    private static int findVisibleTimelineStatusPosition(Object adapter, int headerCount, int first, int last) {
        if (adapter == null || first < 0 || last < 0) return -1;
        int min = Math.min(first, last);
        int max = Math.max(first, last);
        for (int pos = min; pos <= max; pos++) {
            int dataPosition = pos - Math.max(0, headerCount);
            if (dataPosition >= 0 && getTimelineAdapterDataStatusId(adapter, dataPosition) > 0L) {
                return pos;
            }
        }
        return -1;
    }

    private static long getLastReadStatusId() {
        if (sLastReadStatusId != null) return sLastReadStatusId.longValue();
        File file = getLastReadFile();
        if (file == null || !file.exists()) {
            sLastReadStatusId = Long.valueOf(0L);
            return 0L;
        }
        BufferedReader reader = null;
        long statusId = 0L;
        boolean trusted = false;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("status_id=")) {
                    statusId = Long.parseLong(line.substring("status_id=".length()).trim());
                } else if ("preload_done=true".equals(line)) {
                    trusted = true;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {}
            }
        }
        if (!trusted) {
            statusId = 0L;
        }
        sLastReadStatusId = Long.valueOf(statusId);
        return statusId;
    }

    private static boolean isTimelineLastReadTarget(Object recyclerView, int target) {
        try {
            if (!sTimelineRestoredCacheMode || !isTimelinePreloadDone()) return false;
            long statusId = getLastReadStatusId();
            if (statusId <= 0L) return false;
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            int headerCount = adapter == null ? 0 : callIntMethodSafe(adapter, "getHeaderCount", 0);
            int dataPosition = target - Math.max(0, headerCount);
            return dataPosition >= 0 && getTimelineAdapterDataStatusId(adapter, dataPosition) == statusId;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static File getLastReadFile() {
        return new File("/data/data/com.weico.international/files", LAST_READ_FILE);
    }

    private static void showTimelineLastReadMarker(Object recyclerView) {
        if (sLastReadMarkerShown) return;
        try {
            if (!(recyclerView instanceof View)) return;
            Object contextObject = XposedHelpers.callMethod(recyclerView, "getContext");
            if (!(contextObject instanceof Context)) return;

            final FrameLayout parent = findTimelineMarkerParent((View) recyclerView);
            if (parent == null) return;

            final TextView marker = new TextView((Context) contextObject);
            marker.setText("上次读到这里");
            marker.setTextColor(Color.WHITE);
            marker.setTextSize(14f);
            marker.setGravity(Gravity.CENTER);
            marker.setBackgroundColor(Color.rgb(46, 125, 246));
            marker.setAlpha(0.92f);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(recyclerView, 34)
            );
            lp.gravity = Gravity.TOP;
            lp.topMargin = getTimelineTopOffset(recyclerView);
            parent.addView(marker, lp);
            sLastReadMarkerShown = true;
            log("Timeline last-read marker shown");

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        parent.removeView(marker);
                    } catch (Throwable ignored) {}
                }
            }, 6000L);
        } catch (Throwable t) {
            log("Timeline last-read marker error: " + t.getMessage());
        }
    }

    private static FrameLayout findTimelineMarkerParent(View view) {
        try {
            View current = view;
            while (current != null) {
                Object parent = current.getParent();
                if (parent instanceof FrameLayout) return (FrameLayout) parent;
                if (parent instanceof View) {
                    current = (View) parent;
                } else if (parent instanceof ViewGroup) {
                    break;
                } else {
                    break;
                }
            }
            View root = view.getRootView();
            return root instanceof FrameLayout ? (FrameLayout) root : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean isTimelineTargetVisible(Object layoutManager, int target) {
        try {
            int first = callIntMethodSafe(layoutManager, "findFirstVisibleItemPosition", -1);
            int last = callIntMethodSafe(layoutManager, "findLastVisibleItemPosition", -1);
            if (first < 0 || last < 0) return false;
            int min = Math.min(first, last);
            int max = Math.max(first, last);
            return target >= min && target <= max;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void scrollTimelineTowardTarget(Object recyclerView, Object layoutManager, int target) {
        try {
            int first = callIntMethodSafe(layoutManager, "findFirstVisibleItemPosition", -1);
            int last = callIntMethodSafe(layoutManager, "findLastVisibleItemPosition", -1);
            if (first < 0 || last < 0) return;
            int min = Math.min(first, last);
            int max = Math.max(first, last);
            int step = callIntMethodSafe(recyclerView, "getHeight", 1600);
            if (step < 400) step = 1600;
            if (sTimelineRestoredCacheMode) step *= 4;
            boolean reverseLayout = callBooleanMethodSafe(layoutManager, "getReverseLayout");
            int dy;
            if (target > max) {
                dy = reverseLayout ? -step : step;
            } else if (target < min) {
                dy = reverseLayout ? step : -step;
            } else {
                return;
            }
            XposedHelpers.callMethod(recyclerView, "scrollBy", 0, dy);
            if (sTimelineRestoredCacheMode) {
                log("Timeline restored scroll step target=" + target + " visible=" + min + ".." + max
                    + " reverse=" + reverseLayout + " dy=" + dy);
            }
        } catch (Throwable ignored) {}
    }

    private static int getTimelineTopOffset(Object recyclerView) {
        try {
            int paddingTop = callIntMethodSafe(recyclerView, "getPaddingTop", 0);
            if (sTimelineRestoredCacheMode) {
                return Math.max(paddingTop, dpToPx(recyclerView, 72));
            }
            return Math.max(0, paddingTop);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int getTimelineLayoutManagerOffset(Object recyclerView, Object layoutManager, int topOffset) {
        try {
            if (!callBooleanMethodSafe(layoutManager, "getReverseLayout")) return topOffset;
            int height = callIntMethodSafe(recyclerView, "getHeight", 0);
            int paddingBottom = callIntMethodSafe(recyclerView, "getPaddingBottom", 0);
            int offsetFromEnd = height - paddingBottom - Math.max(0, topOffset);
            return Math.max(0, offsetFromEnd);
        } catch (Throwable ignored) {
            return topOffset;
        }
    }

    private static int dpToPx(Object view, int dp) {
        try {
            Object resources = XposedHelpers.callMethod(view, "getResources");
            DisplayMetrics metrics = (DisplayMetrics) XposedHelpers.getObjectField(resources, "mMetrics");
            if (metrics != null && metrics.density > 0f) return Math.round(dp * metrics.density);
        } catch (Throwable ignored) {}
        try {
            Object context = XposedHelpers.callMethod(view, "getContext");
            Object resources = XposedHelpers.callMethod(context, "getResources");
            Object metrics = XposedHelpers.callMethod(resources, "getDisplayMetrics");
            Object density = XposedHelpers.getObjectField(metrics, "density");
            if (density instanceof Float && ((Float) density).floatValue() > 0f) {
                return Math.round(dp * ((Float) density).floatValue());
            }
        } catch (Throwable ignored) {}
        return dp * 3;
    }

    private static void markTimelineTopAnchorTouched(Object recyclerView) {
        if (!isTimelineRecyclerView(recyclerView)) return;
        synchronized (sTopAnchorStates) {
            TopAnchorState state = sTopAnchorStates.get(recyclerView);
            if (state == null || state.userTouched) return;
            state.userTouched = true;
        }
        sSuppressTimelineLoadMoreUntilMs = 0L;
        log("Timeline top anchor cancelled by touch");
    }

    private static void rememberTimelinePresenter(Object presenter) {
        if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
        sLastTimelinePresenter = presenter;
    }

    private static void markTimelineNoMoreContent(String source) {
        try {
            Object presenter = sLastTimelinePresenter;
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            int count = getTimelineStatusCount(presenter);
            if (count < TIMELINE_CACHE_MIN_ITEMS) {
                log("Timeline no-more ignored source=" + source + " count=" + count);
                return;
            }

            persistTimelineNativeCache(presenter, "no-more-content");
            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                state.stopped = true;
                state.scheduled = false;
                state.inFlight = false;
                state.stableRounds = PRELOAD_STABLE_DONE_ROUNDS;
                state.lastCount = count;
            }
            markTimelinePreloadDone("no-more-content", 0, count);
            log("Timeline no-more marker detected source=" + source + " count=" + count);
        } catch (Throwable t) {
            log("Timeline no-more marker error source=" + source + ": " + t.getMessage());
        }
    }

    private static void markTimelineNoMoreIfEmptyPage(Object presenter, List incomingData, String source) {
        try {
            if (presenter == null || incomingData == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            int incoming = countTimelineStatuses(incomingData);
            int count = getTimelineStatusCount(presenter);
            if (incoming <= 1 && count >= PRELOAD_DONE_MIN_ITEMS) {
                rememberTimelinePresenter(presenter);
                log("Timeline no-more empty page source=" + source + " incoming=" + incoming + " count=" + count);
                markTimelineNoMoreContent(source + "-empty");
            }
        } catch (Throwable t) {
            log("Timeline no-more empty-page error source=" + source + ": " + t.getMessage());
        }
    }

    private static boolean isTimelinePreloadStopped(Object presenter) {
        if (isTimelinePreloadReady(getTimelineStatusCount(presenter))) return true;
        synchronized (sPreloadStates) {
            PreloadState state = sPreloadStates.get(presenter);
            return state != null && state.stopped;
        }
    }

    private static void resetPreloadState(Object presenter, String source) {
        if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
        synchronized (sPreloadStates) {
            sPreloadStates.put(presenter, new PreloadState());
        }
        log("Timeline preload reset source=" + source);
    }

    private static void scheduleTimelinePreload(final Object presenter, final String source) {
        if (presenter == null) return;
        try {
            if (!"-1".equals(getTimelineGroupId(presenter))) return;
            rememberTimelinePresenter(presenter);
            int count = getTimelineStatusCount(presenter);
            if (count < 2) return;
            if (isTimelinePreloadReady(count)) {
                log("Timeline preload skipped warmed source=" + source + " count=" + count);
                return;
            }

            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                if (count > state.lastCount) {
                    state.lastCount = count;
                    state.stableRounds = 0;
                    state.inFlight = false;
                }
                if (state.stopped || state.inFlight || state.scheduled) return;
                if (state.requestedPages >= PRELOAD_MAX_PAGES || count >= PRELOAD_MAX_ITEMS) {
                    state.stopped = true;
                    log("Timeline preload safety stop source=" + source + " pages=" + state.requestedPages
                        + " count=" + count);
                    return;
                }
                state.scheduled = true;
            }

            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestTimelinePreload(presenter, source);
                }
            }, PRELOAD_DELAY_MS);
        } catch (Throwable t) {
            log("Timeline preload schedule error source=" + source + ": " + t.getMessage());
        }
    }

    private static void requestTimelinePreload(final Object presenter, String source) {
        final int token;
        int count;
        int page;
        try {
            if (!"-1".equals(getTimelineGroupId(presenter))) return;
            count = getTimelineStatusCount(presenter);
            if (isTimelinePreloadReady(count)) return;
            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                state.scheduled = false;
                if (state.stopped || state.inFlight) return;
                if (state.requestedPages >= PRELOAD_MAX_PAGES || count >= PRELOAD_MAX_ITEMS) {
                    state.stopped = true;
                    log("Timeline preload safety stop source=" + source + " pages=" + state.requestedPages
                        + " count=" + count);
                    return;
                }
                state.inFlight = true;
                state.requestedPages++;
                state.lastCount = count;
                token = ++state.requestToken;
                page = state.requestedPages;
            }

            log("Timeline preload loadMore page=" + page + " count=" + count + " source=" + source);
            XposedHelpers.callMethod(presenter, "loadMore");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishTimelinePreloadWatchdog(presenter, token);
                }
            }, PRELOAD_WATCHDOG_MS);
        } catch (Throwable t) {
            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                state.scheduled = false;
                state.inFlight = false;
                state.stableRounds++;
            }
            log("Timeline preload loadMore error source=" + source + ": " + t.getMessage());
        }
    }

    private static void finishTimelinePreloadWatchdog(Object presenter, int token) {
        try {
            if (!"-1".equals(getTimelineGroupId(presenter))) return;
            int count = getTimelineStatusCount(presenter);
            boolean shouldContinue;
            boolean shouldMarkDone = false;
            int pages = 0;
            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                if (state.requestToken != token || !state.inFlight) return;
                state.inFlight = false;
                if (count > state.lastCount) {
                    state.lastCount = count;
                    state.stableRounds = 0;
                } else {
                    state.stableRounds++;
                }
                if (state.stableRounds >= PRELOAD_STABLE_DONE_ROUNDS
                    || state.requestedPages >= PRELOAD_MAX_PAGES
                    || count >= PRELOAD_MAX_ITEMS) {
                    state.stopped = true;
                    pages = state.requestedPages;
                    shouldMarkDone = state.stableRounds >= PRELOAD_STABLE_DONE_ROUNDS
                        && shouldRememberPreloadDone(pages, count);
                    log("Timeline preload watchdog stop pages=" + state.requestedPages
                        + " stable=" + state.stableRounds + " count=" + count);
                    shouldContinue = false;
                } else {
                    shouldContinue = true;
                }
            }
            if (shouldContinue) {
                scheduleTimelinePreload(presenter, "preload-watchdog");
            } else if (shouldMarkDone) {
                markTimelinePreloadDone("preload-watchdog", pages, count);
            }
        } catch (Throwable t) {
            log("Timeline preload watchdog error: " + t.getMessage());
        }
    }

    private static PreloadState getPreloadStateLocked(Object presenter) {
        PreloadState state = sPreloadStates.get(presenter);
        if (state == null) {
            state = new PreloadState();
            sPreloadStates.put(presenter, state);
        }
        return state;
    }

    private static void beginTimelineLoadMoreSuppression(String source) {
        sSuppressTimelineLoadMoreUntilMs = 0L;
    }

    private static boolean shouldFreezeTimelineNetworkMutation(Object presenter) {
        try {
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return false;
            return isTimelinePreloadReady(getTimelineStatusCount(presenter));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean shouldSuppressTimelineLoadMore(Object presenter) {
        try {
            return presenter != null && "-1".equals(getTimelineGroupId(presenter))
                && isTimelinePreloadReady(getTimelineStatusCount(presenter));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean shouldRememberPreloadDone(int pages, int count) {
        return count >= PRELOAD_DONE_MIN_ITEMS;
    }

    private static boolean isTimelinePreloadReady(int count) {
        if (!isTimelinePreloadDone()) return false;
        return count >= PRELOAD_DONE_MIN_ITEMS;
    }

    private static boolean isTimelinePreloadDone() {
        if (sTimelinePreloadDone != null) return sTimelinePreloadDone.booleanValue();
        try {
            File marker = getPreloadDoneFile();
            int count = readPreloadDoneCount(marker);
            boolean done = count >= PRELOAD_DONE_MIN_ITEMS;
            sTimelinePreloadDone = Boolean.valueOf(done);
            if (!done && marker != null && marker.exists()) {
                marker.delete();
                log("Timeline preload marker ignored count=" + count);
            }
        } catch (Throwable ignored) {
            sTimelinePreloadDone = Boolean.FALSE;
        }
        return sTimelinePreloadDone.booleanValue();
    }

    private static int readPreloadDoneCount(File marker) {
        if (marker == null || !marker.exists()) return 0;
        BufferedReader reader = null;
        int count = 0;
        boolean stableDone = false;
        try {
            reader = new BufferedReader(new FileReader(marker));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("count=")) {
                    count = Integer.parseInt(line.substring("count=".length()).trim());
                } else if ("source=preload-watchdog".equals(line)
                    || "done_reason=stable".equals(line)) {
                    stableDone = true;
                }
            }
        } catch (Throwable ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Throwable ignored) {}
            }
        }
        return stableDone ? count : 0;
    }

    private static void markTimelinePreloadDone(String source, int pages, int count) {
        if (!shouldRememberPreloadDone(pages, count)) return;
        if (isTimelinePreloadDone()) {
            persistTimelineShadowCache(source, count, null, true);
            return;
        }
        try {
            File marker = getPreloadDoneFile();
            File parent = marker.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            FileWriter fw = new FileWriter(marker, false);
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            fw.write("done_at=" + ts + "\n");
            fw.write("source=" + source + "\n");
            fw.write("done_reason=stable\n");
            fw.write("pages=" + pages + "\n");
            fw.write("count=" + count + "\n");
            fw.close();
            sTimelinePreloadDone = Boolean.TRUE;
            persistTimelineShadowCache(source, count, null, true);
            log("Timeline preload remembered source=" + source + " pages=" + pages + " count=" + count);
        } catch (Throwable t) {
            log("Timeline preload remember error source=" + source + ": " + t.getMessage());
        }
    }

    private static File getPreloadDoneFile() {
        return new File("/data/data/com.weico.international/files", PRELOAD_DONE_FILE);
    }

    private static int getTimelineStatusCount(Object presenter) {
        try {
            Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
            if (!(statusList instanceof List)) return 0;
            List list = (List) statusList;
            int count = 0;
            for (int i = 0; i < list.size(); i++) {
                Object status = unwrapStatus(list.get(i));
                if (status != null && getStatusId(status) > 0 && !isLoadMoreStatus(status)
                    && !isTimelineAdStatus(status) && !isTimelineContentlessStatus(status)) {
                    count++;
                }
            }
            if ("-1".equals(getTimelineGroupId(presenter))) {
                count = Math.max(count, getTimelineCumulativeStatusCount());
            }
            return count;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static List sortTimelineNewestFirst(List list, Object owner, String source, boolean loadNew) {
        if (list == null || list.size() < 2) return list;

        String groupId = getTimelineGroupId(owner);
        if (!"-1".equals(groupId)) return list;

        ArrayList positions = new ArrayList();
        ArrayList items = new ArrayList();
        long previousId = Long.MIN_VALUE;
        boolean needsSort = false;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object status = unwrapStatus(item);
            long id = getStatusId(status);
            if (status != null && id > 0 && !isLoadMoreStatus(status) && !isTimelineAdStatus(status)) {
                positions.add(Integer.valueOf(i));
                items.add(item);
                if (previousId != Long.MIN_VALUE && id > previousId) {
                    needsSort = true;
                }
                previousId = id;
            }
        }

        if (!needsSort || items.size() < 2) return list;

        final long beforeFirst = getStatusId(unwrapStatus(items.get(0)));
        final long beforeLast = getStatusId(unwrapStatus(items.get(items.size() - 1)));
        Collections.sort(items, new Comparator() {
            @Override
            public int compare(Object left, Object right) {
                long leftId = getStatusId(unwrapStatus(left));
                long rightId = getStatusId(unwrapStatus(right));
                if (leftId == rightId) return 0;
                return leftId > rightId ? -1 : 1;
            }
        });

        try {
            for (int i = 0; i < positions.size(); i++) {
                int index = ((Integer) positions.get(i)).intValue();
                list.set(index, items.get(i));
            }
            log("Timeline order sorted newest-first source=" + source + " group=" + groupId + " size=" + list.size()
                + " statuses=" + items.size() + " first=" + beforeFirst + " last=" + beforeLast
                + " loadNew=" + loadNew);
            return list;
        } catch (Throwable t) {
            ArrayList copy = new ArrayList(list);
            for (int i = 0; i < positions.size(); i++) {
                int index = ((Integer) positions.get(i)).intValue();
                copy.set(index, items.get(i));
            }
            log("Timeline order copied+sorted newest-first source=" + source + " group=" + groupId + " size=" + list.size()
                + " statuses=" + items.size() + " first=" + beforeFirst + " last=" + beforeLast
                + " loadNew=" + loadNew);
            return copy;
        }
    }

    private static List sortTimelineOldestFirst(List list, Object owner, String source) {
        if (list == null || list.size() < 2) return list;

        String groupId = getTimelineGroupId(owner);
        if (!"-1".equals(groupId)) return list;

        final long beforeFirst = getStatusId(unwrapStatus(list.get(0)));
        final long beforeLast = getStatusId(unwrapStatus(list.get(list.size() - 1)));
        try {
            Collections.sort(list, new Comparator() {
                @Override
                public int compare(Object left, Object right) {
                    long leftId = getStatusId(unwrapStatus(left));
                    long rightId = getStatusId(unwrapStatus(right));
                    if (leftId == rightId) return 0;
                    return leftId < rightId ? -1 : 1;
                }
            });
            log("Timeline order sorted oldest-first source=" + source + " group=" + groupId + " size=" + list.size()
                + " first=" + beforeFirst + " last=" + beforeLast);
            return list;
        } catch (Throwable t) {
            ArrayList copy = new ArrayList(list);
            Collections.sort(copy, new Comparator() {
                @Override
                public int compare(Object left, Object right) {
                    long leftId = getStatusId(unwrapStatus(left));
                    long rightId = getStatusId(unwrapStatus(right));
                    if (leftId == rightId) return 0;
                    return leftId < rightId ? -1 : 1;
                }
            });
            log("Timeline order copied+sorted oldest-first source=" + source + " group=" + groupId + " size=" + list.size()
                + " first=" + beforeFirst + " last=" + beforeLast);
            return copy;
        }
    }

    private static Object findComparableStatus(List list, boolean fromStart) {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            int index = fromStart ? i : size - 1 - i;
            Object status = unwrapStatus(list.get(index));
            if (status != null && !isLoadMoreStatus(status) && !isTimelineAdStatus(status)
                && getStatusId(status) > 0) {
                return status;
            }
        }
        return null;
    }

    private static boolean isTimelineAdStatus(Object status) {
        if (status == null || isLoadMoreStatus(status)) return false;
        try {
            if (getBooleanFieldSafe(status, "isUVEAd", false)) return true;
            if (getBooleanFieldSafe(status, "isad", false)) return true;
            if (callBooleanMethodSafe(status, "isAdWeibo")) return true;
            if (callBooleanMethodSafe(status, "isIsad")) return true;

            Object promotion = callMethodSafe(status, "getPromotion");
            if (promotion != null) return true;
            try {
                if (getFieldValue(status, "promotion") != null) return true;
            } catch (Throwable ignored) {}

            String mblogTypeName = getStringMethodOrField(status, "getMblogtypename", "mblogtypename");
            if (mblogTypeName != null && mblogTypeName.contains("广告")) return true;

            int viewType = callIntMethodSafe(status, "getViewType", -1);
            return isTimelineAdViewType(viewType);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isTimelineAdViewType(int viewType) {
        return viewType == 0x17
            || viewType == 0x1b
            || viewType == 0x1e
            || viewType == 0x6c
            || viewType == 0x70;
    }

    private static boolean isTimelineContentlessStatus(Object status) {
        if (status == null || isLoadMoreStatus(status) || isTimelineAdStatus(status)) return false;
        if (getStatusId(status) <= 0) return false;
        return !hasTimelineRenderableContent(status);
    }

    private static boolean hasTimelineRenderableContent(Object status) {
        if (hasMeaningfulString(getStringMethodOrField(status, "getText", "text"))) return true;
        if (hasMeaningfulString(getStringMethodOrField(status, null, "translateText"))) return true;
        if (hasMeaningfulString(getStringMethodOrField(status, null, "decTextSapnned"))) return true;

        if (hasNonEmptyObject(getObjectMethodOrField(status, "getRetweeted_status", "retweeted_status"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "reprinted_status"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, "getPage_info", "page_info"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, "getPicInfos", "pic_detail_infos"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, "getUrl_struct_list", "url_struct"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, "getMixMediaInfo", "mixMediaInfo"))) return true;

        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "pic_ids"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "picPathUrls"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "thumbPicPathUrls"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "url_objects"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "url_objects_true"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "videoInfo"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "uveVideoInfo"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "blog_audio"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "commonStruct"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "multiEntryWrapper"))) return true;
        if (hasNonEmptyObject(getObjectMethodOrField(status, null, "topCard"))) return true;

        if (hasMeaningfulString(getStringMethodOrField(status, "getThumbnail_pic", "thumbnail_pic"))) return true;
        if (hasMeaningfulString(getStringMethodOrField(status, "getBmiddle_pic", "bmiddle_pic"))) return true;
        if (hasMeaningfulString(getStringMethodOrField(status, "getOriginal_pic", "original_pic"))) return true;
        if (getIntMethodOrField(status, null, "picNum", 0) > 0) return true;

        int viewType = getStatusViewType(status);
        if (isTimelineContentViewType(viewType)) return true;
        return callBooleanMethodSafe(status, "isMulPicShow")
            || callBooleanMethodSafe(status, "isSinglePicShow")
            || callBooleanMethodSafe(status, "isLiveCard");
    }

    private static boolean isTimelineContentViewType(int viewType) {
        switch (viewType) {
            case 0x2:
            case 0x3:
            case 0x4:
            case 0x5:
            case 0x7:
            case 0x8:
            case 0x9:
            case 0xa:
            case 0xb:
            case 0xc:
            case 0xd:
            case 0xe:
            case 0x10:
            case 0x12:
            case 0x13:
            case 0x18:
            case 0x19:
            case 0x1a:
            case 0x1f:
            case 0x20:
            case 0x22:
            case 0x23:
                return true;
            default:
                return false;
        }
    }

    private static int getStatusViewType(Object status) {
        int viewType = callIntMethodSafe(status, "getViewType", Integer.MIN_VALUE);
        if (viewType != Integer.MIN_VALUE) return viewType;
        return getIntMethodOrField(status, null, "viewType", -1);
    }

    private static boolean isLoadMoreStatus(Object status) {
        try {
            Object value = getFieldValue(status, "isLoadMoreButton");
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {}
        try {
            Object value = XposedHelpers.callMethod(status, "isLoadMoreButton");
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {}
        return false;
    }

    private static long getStatusId(Object status) {
        if (status == null) return 0;
        try {
            Object value = XposedHelpers.callMethod(status, "getId");
            if (value instanceof Number) return ((Number) value).longValue();
        } catch (Throwable ignored) {}
        try {
            Object value = XposedHelpers.callMethod(status, "getIdstr");
            if (value != null) return Long.parseLong(String.valueOf(value));
        } catch (Throwable ignored) {}
        return 0;
    }

    private static Object unwrapStatus(Object item) {
        if (item == null) return null;
        try {
            Object value = XposedHelpers.callMethod(item, "getStatus");
            if (value != null) return value;
        } catch (Throwable ignored) {}
        return item;
    }

    private static Object getOuterAction(Object lambda) {
        try {
            return XposedHelpers.getObjectField(lambda, "this$0");
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean getBooleanFieldSafe(Object target, String field, boolean fallback) {
        try {
            Object value = getFieldValue(target, field);
            return value instanceof Boolean ? (Boolean) value : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean callBooleanMethodSafe(Object target, String method) {
        try {
            Object value = XposedHelpers.callMethod(target, method);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int callIntMethodSafe(Object target, String method, int fallback) {
        try {
            Object value = XposedHelpers.callMethod(target, method);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Object callMethodSafe(Object target, String method) {
        if (target == null || method == null) return null;
        try {
            return XposedHelpers.callMethod(target, method);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getStringMethodOrField(Object target, String method, String field) {
        Object value = callMethodSafe(target, method);
        if (value != null) return String.valueOf(value);
        try {
            value = getFieldValue(target, field);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object getObjectMethodOrField(Object target, String method, String field) {
        Object value = callMethodSafe(target, method);
        if (value != null) return value;
        if (target == null || field == null) return null;
        try {
            return getFieldValue(target, field);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int getIntMethodOrField(Object target, String method, String field, int fallback) {
        Object value = callMethodSafe(target, method);
        if (value instanceof Number) return ((Number) value).intValue();
        if (target == null || field == null) return fallback;
        try {
            value = getFieldValue(target, field);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean hasMeaningfulString(String value) {
        if (value == null) return false;
        String text = value.trim();
        return text.length() > 0
            && !"null".equalsIgnoreCase(text)
            && !"[]".equals(text)
            && !"{}".equals(text);
    }

    private static boolean hasNonEmptyObject(Object value) {
        if (value == null) return false;
        if (value instanceof CharSequence) return hasMeaningfulString(String.valueOf(value));
        if (value instanceof java.util.Collection) return !((java.util.Collection) value).isEmpty();
        if (value instanceof Map) return !((Map) value).isEmpty();
        Class<?> clazz = value.getClass();
        if (clazz.isArray()) return java.lang.reflect.Array.getLength(value) > 0;
        return true;
    }

    private static Object getFieldValue(Object target, String fieldName) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static String getTimelineGroupId(Object owner) {
        try {
            Object groupId = XposedHelpers.callMethod(owner, "getGroupId");
            return groupId == null ? null : String.valueOf(groupId);
        } catch (Throwable ignored) {}
        try {
            Object presenter = XposedHelpers.callMethod(owner, "getPresenter");
            Object groupId = XposedHelpers.callMethod(presenter, "getGroupId");
            return groupId == null ? null : String.valueOf(groupId);
        } catch (Throwable ignored) {}
        return null;
    }

    private static void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    private static void forceNativeReverseOrder(ClassLoader cl) {
        try {
            Class<?> settingNativeClass = XposedHelpers.findClass(
                "com.weico.international.activity.v4.SettingNative",
                cl
            );
            XC_MethodHook forceLoad = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (REVERSE_ORDER_KEY.equals(param.args[0])) {
                        param.setResult(true);
                    }
                }
            };
            XposedHelpers.findAndHookMethod(settingNativeClass, "loadBoolean", String.class, forceLoad);
            XposedHelpers.findAndHookMethod(settingNativeClass, "loadBoolean", String.class, boolean.class, forceLoad);
            XposedHelpers.findAndHookMethod(settingNativeClass, "loadBoolean", String.class, boolean.class, boolean.class, forceLoad);

            XC_MethodHook forceSave = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (REVERSE_ORDER_KEY.equals(param.args[0])) {
                        param.args[1] = true;
                    }
                }
            };
            XposedHelpers.findAndHookMethod(settingNativeClass, "saveBoolean", String.class, boolean.class, forceSave);
            XposedHelpers.findAndHookMethod(settingNativeClass, "saveBoolean", String.class, boolean.class, boolean.class, forceSave);

            Class<?> appClass = XposedHelpers.findClass("com.weico.international.WApplication", cl);
            XC_MethodHook setReverseOrder = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    setNativeReverseOrder(cl);
                }
            };
            XposedHelpers.findAndHookMethod(appClass, "basicInit", setReverseOrder);
            XposedHelpers.findAndHookMethod(appClass, "onCreate", setReverseOrder);

            XposedHelpers.findAndHookMethod(
                "com.weico.international.ui.indexv2.IndexV2Presenter",
                cl,
                "isReverseOrder",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Object groupId = XposedHelpers.callMethod(param.thisObject, "getGroupId");
                            if ("-1".equals(groupId)) {
                                param.setResult(true);
                            }
                        } catch (Throwable ignored) {}
                    }
                }
            );
            log("Native reverse-order forced");
        } catch (Throwable t) {
            log("forceNativeReverseOrder error: " + t.getMessage());
        }
    }

    private static void setNativeReverseOrder(ClassLoader cl) {
        try {
            Class<?> appClass = XposedHelpers.findClass("com.weico.international.WApplication", cl);
            java.lang.reflect.Field field = appClass.getDeclaredField("mReverseOrder");
            field.setAccessible(true);
            field.setBoolean(null, true);
            log("WApplication.mReverseOrder=true");
        } catch (Throwable t) {
            log("setNativeReverseOrder error: " + t.getMessage());
        }
    }

    private static void removeSplashAd(ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod("com.weico.international.activity.LogoActivity", cl, "doWhatNext",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult("main");
                    }
                });
            XposedHelpers.findAndHookMethod("com.weico.international.activity.LogoActivity", cl, "triggerPermission", boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) { param.args[0] = true; }
                });
            XposedHelpers.findAndHookMethod("com.weico.international.manager.ProcessMonitor", cl, "attach", Application.class,
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) { return null; }
                });
        } catch (Throwable ignored) {}
    }

    private static void removeTimelineAd(ClassLoader cl) {
        String[] lambdas = {"queryUveAdRequest$lambda$151", "queryUveAdRequest$lambda$152", "queryUveAdRequest$lambda$153"};
        try {
            for (int i = 0; i < lambdas.length; i++) {
                try {
                    if (i == 0) {
                        XposedHelpers.findAndHookMethod("com.weico.international.api.RxApiKt", cl, lambdas[i], java.util.Map.class,
                            new XC_MethodHook() { @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(""); } });
                    } else {
                        Class<?> func1 = XposedHelpers.findClass("kotlin.jvm.functions.Function1", cl);
                        XposedHelpers.findAndHookMethod("com.weico.international.api.RxApiKt", cl, lambdas[i], func1, Object.class,
                            new XC_MethodHook() { @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(""); } });
                    }
                } catch (Throwable ignored) {}
            }
            try {
                Class<?> statusClass = XposedHelpers.findClass("com.weico.international.model.sina.Status", cl);
                XposedHelpers.findAndHookMethod("com.weico.international.utility.KotlinExtendKt", cl, "isWeiboUVEAd", statusClass,
                    new XC_MethodHook() { @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(false); } });
            } catch (Throwable ignored) {}
            try {
                Class<?> pageInfoClass = XposedHelpers.findClass("com.weico.international.model.sina.PageInfo", cl);
                XposedHelpers.findAndHookMethod("com.weico.international.utility.KotlinUtilKt", cl, "findUVEAd", pageInfoClass,
                    new XC_MethodHook() { @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(null); } });
            } catch (Throwable ignored) {}
            XC_MethodHook adBoolHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    String key = (String) p.args[0];
                    if ("BOOL_UVE_FEED_AD".equals(key)) p.setResult(false);
                    else if (key != null && key.startsWith("BOOL_AD_ACTIVITY_BLOCK_")) p.setResult(true);
                }
            };
            try {
                XposedHelpers.findAndHookMethod("com.weico.international.activity.v4.Setting", cl, "loadBoolean", String.class, boolean.class, adBoolHook);
            } catch (Throwable ignored) {}
            XC_MethodHook adIntHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    String key = (String) p.args[0];
                    if ("ad_interval".equals(key)) p.setResult(Integer.MAX_VALUE);
                    else if ("display_ad".equals(key)) p.setResult(0);
                }
            };
            try {
                XposedHelpers.findAndHookMethod("com.weico.international.activity.v4.Setting", cl, "loadInt", String.class, adIntHook);
                XposedHelpers.findAndHookMethod("com.weico.international.activity.v4.Setting", cl, "loadInt", String.class, int.class, adIntHook);
            } catch (Throwable ignored) {}
            XC_MethodHook adStrHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) {
                    if ("video_ad".equals(p.args[0])) p.setResult("");
                }
            };
            try {
                XposedHelpers.findAndHookMethod("com.weico.international.activity.v4.Setting", cl, "loadString", String.class, adStrHook);
                XposedHelpers.findAndHookMethod("com.weico.international.activity.v4.Setting", cl, "loadString", String.class, String.class, adStrHook);
            } catch (Throwable ignored) {}
        } catch (Throwable ignored) {}
    }
}
