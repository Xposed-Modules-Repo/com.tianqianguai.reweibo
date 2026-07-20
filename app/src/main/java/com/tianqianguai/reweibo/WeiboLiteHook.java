package com.tianqianguai.reweibo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.system.Os;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.AtomicFile;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeiboLiteHook {

    private static final String REVERSE_ORDER_KEY = "key_order_browser";
    private static final String REWEIBO_DRAWER_TYPE = "reweibo_settings";
    private static final String REWEIBO_DRAWER_SCHEMA = "reweibo://settings";
    private static final String REWEIBO_DRAWER_ICON = "exo_ic_settings";
    private static final int PRELOAD_BASE_MAX_PAGES = 240;
    private static final int PRELOAD_MAX_PAGES_PER_CACHE_DAY = 40;
    private static final int PRELOAD_BASE_MAX_ITEMS = 10000;
    private static final int PRELOAD_MAX_ITEMS_PER_CACHE_DAY = 1000;
    private static final long PRELOAD_DELAY_MS = 700L;
    private static final long PRELOAD_WATCHDOG_MS = 4500L;
    private static final long PRELOAD_STABLE_RETRY_BASE_MS = 15000L;
    private static final long PRELOAD_STABLE_RETRY_MAX_MS = 300000L;
    private static final long TOP_ANCHOR_WINDOW_MS = 16000L;
    private static final long TOP_BAR_DOUBLE_TAP_MS = 500L;
    private static final long TOP_BAR_JUMP_RETRY_MS = 120L;
    private static final long VIDEO_URL_EXPIRY_MARGIN_SEC = 300L;
    private static final long VIDEO_OPEN_REFRESH_BYPASS_MS = 30000L;
    private static final long REFRESH_ANCHOR_WINDOW_MS = 30000L;
    private static final long TIMELINE_GAP_ID_THRESHOLD = 80000000000L;
    private static final long TIMELINE_GAP_FILL_DELAY_MS = 900L;
    private static final long TIMELINE_GAP_FILL_WATCHDOG_MS = 7000L;
    private static final long TIMELINE_GAP_FILL_WINDOW_MS = 300000L;
    private static final long TIMELINE_GAP_FILL_PROGRESS_HIDE_MS = 2200L;
    private static final long TIMELINE_GAP_FILL_SHOWDATA_SUPPRESS_MS = 6000L;
    private static final long TIMELINE_GAP_FILL_FALLBACK_DELAY_MS = 2500L;
    private static final long TIMELINE_GAP_FILL_SMALL_BACKOFF_ID = 5000000000L;
    private static final long TIMELINE_CACHE_DAY_MS = 86400000L;
    private static final long TIMELINE_CACHE_DAY_TOLERANCE_MS = 3600000L;
    private static final long TIMELINE_CACHE_HEAD_MAX_AGE_MS = TIMELINE_CACHE_DAY_MS;
    private static final long TIMELINE_CACHE_RECENCY_TOLERANCE_MS = 3600000L;
    private static final long TIMELINE_NO_MORE_DEDUP_MS = 30000L;
    private static final long TIME_JUMP_RETRY_MS = 140L;
    private static final long TIME_JUMP_FUTURE_GRACE_MS = 300000L;
    private static final long TIME_JUMP_MAX_ACCEPT_DIFF_MS = 36L * 3600000L;
    private static final long HOME_TAB_DOUBLE_TAP_MS = 500L;
    private static final int TOP_ANCHOR_MAX_ATTEMPTS = 80;
    private static final int TOP_BAR_JUMP_MAX_ATTEMPTS = 8;
    private static final int REFRESH_ANCHOR_MAX_ATTEMPTS = 12;
    private static final int TIME_JUMP_MAX_ATTEMPTS = 8;
    private static final int TIMELINE_GAP_FILL_MAX_FALLBACKS = 8;
    private static final int TIMELINE_GAP_FILL_EMPTY_PROOF_MIN = 4;
    private static final int TIMELINE_GAP_FILL_CHECKPOINT_PAGES = 5;
    private static final int TIMELINE_GAP_FILL_CHECKPOINT_ITEMS = 100;
    private static final int TIMELINE_PRELOAD_CHECKPOINT_ITEMS = 500;
    private static final int PRELOAD_DONE_MIN_ITEMS = 1300;
    private static final int PRELOAD_STABLE_DONE_ROUNDS = 8;
    private static final int TIMELINE_CACHE_MIN_ITEMS = 5;
    private static final int TIMELINE_CACHE_COUNT_TOLERANCE = 2;
    private static final int TOP_BAR_TAP_MAX_DP = 100;
    private static final int TOP_BAR_TAP_SLOP_DP = 64;
    private static final int TOP_BAR_RIGHT_EXCLUDE_DP = 72;
    private static final int HOME_TAB_TAP_BOTTOM_DP = 128;
    private static final float HOME_TAB_MAX_X_RATIO = 0.30f;
    private static final String PRELOAD_DONE_FILE = "reweibo_weico_preload_done";
    private static final String LAST_READ_FILE = "reweibo_weico_last_read";
    private static final String LAST_READ_HISTORY_FILE = "reweibo_weico_last_read_history";
    private static final int LAST_READ_HISTORY_LIMIT = 64;
    private static final String TIMELINE_FULL_CACHE_PREFIX = "reweibo_fullTimeline_";
    private static final String TIMELINE_SHADOW_CACHE_FILE = "reweibo_weico_full_cache_shadow.txt";
    private static final String TIMELINE_SHADOW_CACHE_META_FILE = "reweibo_weico_full_cache_shadow.meta";
    private static final String TIMELINE_NATIVE_CACHE_BACKUP_SUFFIX = ".reweibo-native-backup";
    private static final String TIMELINE_CLEAR_TIME_PATTERN = "yyyy-MM-dd HH:mm";
    private static final Map<Object, PreloadState> sPreloadStates = new WeakHashMap<>();
    private static final Map<Object, TopAnchorState> sTopAnchorStates = new WeakHashMap<>();
    private static final Map<Object, Boolean> sTimelineRecyclerViews = new WeakHashMap<>();
    private static final Map<Object, Long> sTimelineUserMovedAt = new WeakHashMap<>();
    private static final Map<Object, Integer> sPersistedTimelineCacheCounts = new WeakHashMap<>();
    private static final Map<Object, Long> sPersistedTimelineCacheNewestIds = new WeakHashMap<>();
    private static final Map<Object, Boolean> sRestoringTimelineCaches = new WeakHashMap<>();
    private static final Map<Object, TimelineRestoreState> sTimelineRestoreStates = new WeakHashMap<>();
    private static final Map<View, Boolean> sReWeiboProfileRows = new WeakHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, FieldLookup>> sFieldLookups = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, ConcurrentHashMap<String, MethodLookup>> sNoArgMethodLookups = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> sModuleIntSettings = new ConcurrentHashMap<>();
    private static final Object sTimelineFileLock = new Object();
    private static final Object sTimelinePersistQueueLock = new Object();
    private static final Object sTimelineRestoreLock = new Object();
    private static final ExecutorService sTimelinePersistExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ReWeibo-timeline-cache");
                thread.setPriority(Thread.MIN_PRIORITY);
                return thread;
            }
        }
    );
    private static final ExecutorService sTimelineRestoreExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ReWeibo-timeline-restore");
                thread.setPriority(Math.max(Thread.MIN_PRIORITY, Thread.NORM_PRIORITY - 1));
                return thread;
            }
        }
    );
    private static final Map sTimelineCumulativeStatusesById = new LinkedHashMap();
    private static final GapFillState sTimelineGapFillState = new GapFillState();
    private static File sLogFile = null;
    private static Context sWeicoContext = null;
    private static volatile boolean sTimelineCacheDaysSettingConfirmed = false;
    private static volatile boolean sTimelineCacheClearInFlight = false;
    private static TimelineNativePersistRequest sPendingTimelineNativePersist = null;
    private static TimelineShadowPersistRequest sPendingTimelineShadowPersist = null;
    private static boolean sTimelinePersistWorkerScheduled = false;
    private static Boolean sTimelinePreloadDone = null;
    private static int sTimelinePreloadDoneCacheDays = -1;
    private static long sSuppressTimelineLoadMoreUntilMs = 0L;
    private static long sTimelineNoMoreHandledAtMs = 0L;
    private static boolean sTimelineOldestFirstMode = false;
    private static boolean sTimelineRestoredCacheMode = false;
    private static Object sLastTimelinePresenter = null;
    private static View sTimelineGapFillProgressView = null;
    private static TextView sTimelineGapFillProgressText = null;
    private static ProgressBar sTimelineGapFillProgressBar = null;
    private static View sTimelineTimeJumpButton = null;
    private static WindowManager sTimelineTimeJumpWindowManager = null;
    private static Activity sTimelineTimeJumpActivity = null;
    private static Object sTimelineTimeJumpRecyclerView = null;
    private static Long sLastReadStatusId = null;
    private static boolean sLastReadMarkerShown = false;
    private static long sLastReadPersistAtMs = 0L;
    private static long sTopBarLastTapAtMs = 0L;
    private static float sTopBarLastTapX = 0f;
    private static float sTopBarLastTapY = 0f;
    private static int sTopBarLastTapSide = -1;
    private static long sHomeTabLastTapAtMs = 0L;
    private static float sHomeTabLastTapX = 0f;
    private static float sHomeTabLastTapY = 0f;
    private static long sHomeTabPendingAnchorStatusId = 0L;
    private static long sHomeTabPendingAnchorUntilMs = 0L;
    private static int sHomeTabPendingAnchorPosition = -1;
    private static int sHomeTabPendingAnchorFirst = -1;
    private static int sHomeTabPendingAnchorLast = -1;
    private static long sVideoOpenRefreshBypassStatusId = 0L;
    private static long sVideoOpenRefreshBypassUntilMs = 0L;
    private static long sTimelineGapFillSuppressShowDataUntilMs = 0L;
    private static long sTimelineRefreshAnchorStatusId = 0L;
    private static long sTimelineRefreshAnchorUntilMs = 0L;
    private static int sTimelineRefreshAnchorPosition = -1;
    private static int sTimelineRefreshAnchorGeneration = 0;
    private static int sTimelineGapFillProgressGeneration = 0;
    private static int sTimelineShadowCacheCount = 0;
    private static int sTimelineNetworkProbeSeq = 0;
    private static boolean sTimelineCursorActionDumped = false;
    private static String sPendingTimelineNoMoreSource = null;
    private static int sPendingTimelineNoMoreGeneration = 0;

    private static final class PreloadState {
        int requestedPages = 0;
        int requestToken = 0;
        int stableRounds = 0;
        int lastCount = 0;
        boolean scheduled = false;
        boolean inFlight = false;
        boolean stopped = false;
        boolean retryScheduled = false;
        int retryToken = 0;
        boolean topAnchorsSuspended = false;
    }

    private static final class TimelineGap {
        long cursorId;
        long targetId;
        long distance;
        int index;
    }

    private static final class GapFillState {
        boolean active = false;
        boolean scheduled = false;
        boolean inFlight = false;
        int requestedPages = 0;
        int requestToken = 0;
        int lastCount = 0;
        int lastCheckpointPage = 0;
        int lastCheckpointCount = 0;
        int fallbackAttempts = 0;
        int emptyResponses = 0;
        int errorResponses = 0;
        long gapCursorId = 0L;
        long cursorId = 0L;
        long targetId = 0L;
        long distance = 0L;
        long untilElapsedMs = 0L;
        long lastEmptyCursorId = 0L;
        long lastErrorCursorId = 0L;
        long minEmptyCursorId = 0L;
        long maxEmptyCursorId = 0L;
        boolean newSideEmptyProbed = false;
        boolean boundaryEmptyProbed = false;
    }

    private static final class TopAnchorState {
        int generation = 0;
        int attempts = 0;
        int lastTarget = -1;
        long untilElapsedMs = 0L;
        boolean userTouched = false;
        boolean finishing = false;
    }

    private static final class TimelineCacheStats {
        int count = 0;
        int datedCount = 0;
        long newestMs = 0L;
        long oldestMs = 0L;

        long spanMs() {
            if (newestMs <= 0L || oldestMs <= 0L || newestMs < oldestMs) return 0L;
            return newestMs - oldestMs;
        }
    }

    private static final class TimelineTimeJumpTarget {
        long statusId = 0L;
        long createdMs = 0L;
        long diffMs = Long.MAX_VALUE;
        int adapterPosition = -1;
        int dataPosition = -1;
        int searchedCount = 0;
        Object recyclerView = null;
    }

    private static final class TimelineRestoreState {
        boolean inFlight = false;
        boolean diskRestoreResolved = false;
        int generation = 0;
        ArrayList pendingLiveStatuses = null;
        String pendingSource = null;
    }

    private static final class TimelineRestoreRequest {
        final Object presenter;
        final ArrayList liveStatuses;
        final ArrayList cumulativeStatuses;
        final String source;
        final int cacheDays;
        final boolean settingConfirmed;
        final boolean cumulative;
        final int generation;

        TimelineRestoreRequest(
            Object presenter,
            ArrayList liveStatuses,
            ArrayList cumulativeStatuses,
            String source,
            int cacheDays,
            boolean settingConfirmed,
            boolean cumulative,
            int generation
        ) {
            this.presenter = presenter;
            this.liveStatuses = liveStatuses;
            this.cumulativeStatuses = cumulativeStatuses;
            this.source = source;
            this.cacheDays = cacheDays;
            this.settingConfirmed = settingConfirmed;
            this.cumulative = cumulative;
            this.generation = generation;
        }
    }

    private static final class TimelineRestoreResult {
        final Object action;
        final Object builder;
        final ArrayList statuses;
        final LinkedHashMap<Long, Object> statusesById;
        final TimelineCacheStats stats;
        final TimelineGapScan gapScan;
        final long newestId;
        final String maxId;
        final String cacheSource;

        TimelineRestoreResult(
            Object action,
            Object builder,
            ArrayList statuses,
            LinkedHashMap<Long, Object> statusesById,
            TimelineCacheStats stats,
            TimelineGapScan gapScan,
            long newestId,
            String maxId,
            String cacheSource
        ) {
            this.action = action;
            this.builder = builder;
            this.statuses = statuses;
            this.statusesById = statusesById;
            this.stats = stats;
            this.gapScan = gapScan;
            this.newestId = newestId;
            this.maxId = maxId;
            this.cacheSource = cacheSource;
        }
    }

    private static final class TimelineCacheLoad {
        final List statuses;
        final String source;

        TimelineCacheLoad(List statuses, String source) {
            this.statuses = statuses;
            this.source = source;
        }
    }

    private static final class TimelinePreparedStatus {
        final Object status;
        final long id;
        final long createdMs;

        TimelinePreparedStatus(Object status, long id, long createdMs) {
            this.status = status;
            this.id = id;
            this.createdMs = createdMs;
        }
    }

    private static final class TimelineGapScan {
        TimelineGap gap = null;
        int count = 0;
    }

    private static final class TimelineNativePersistRequest {
        final Object result;
        final Object gson;
        final File nativeFile;
        final Object presenter;
        final int count;
        final long newestId;
        final String maxId;
        final String source;
        final boolean force;
        final TimelineCacheStats stats;

        TimelineNativePersistRequest(
            Object result,
            Object gson,
            File nativeFile,
            Object presenter,
            int count,
            long newestId,
            String maxId,
            String source,
            boolean force,
            TimelineCacheStats stats
        ) {
            this.result = result;
            this.gson = gson;
            this.nativeFile = nativeFile;
            this.presenter = presenter;
            this.count = count;
            this.newestId = newestId;
            this.maxId = maxId;
            this.source = source;
            this.force = force;
            this.stats = stats;
        }
    }

    private static final class TimelineShadowPersistRequest {
        final String source;
        final int count;
        final String maxId;
        final boolean force;
        final TimelineCacheStats stats;

        TimelineShadowPersistRequest(
            String source,
            int count,
            String maxId,
            boolean force,
            TimelineCacheStats stats
        ) {
            this.source = source;
            this.count = count;
            this.maxId = maxId;
            this.force = force;
            this.stats = stats;
        }
    }

    private static final class TimelineCacheRangeFilterResult {
        final ArrayList retainedStatuses;
        final int sourceCount;
        final int removedCount;
        final boolean removedLastRead;

        TimelineCacheRangeFilterResult(
            ArrayList retainedStatuses,
            int sourceCount,
            int removedCount,
            boolean removedLastRead
        ) {
            this.retainedStatuses = retainedStatuses;
            this.sourceCount = sourceCount;
            this.removedCount = removedCount;
            this.removedLastRead = removedLastRead;
        }
    }

    private static final class TimelineCacheRangeClearResult {
        final ArrayList retainedStatuses;
        final int sourceCount;
        final int removedCount;
        final boolean removedLastRead;
        final String maxId;

        TimelineCacheRangeClearResult(
            ArrayList retainedStatuses,
            int sourceCount,
            int removedCount,
            boolean removedLastRead,
            String maxId
        ) {
            this.retainedStatuses = retainedStatuses;
            this.sourceCount = sourceCount;
            this.removedCount = removedCount;
            this.removedLastRead = removedLastRead;
            this.maxId = maxId;
        }
    }

    private interface TimelineDateTimeCallback {
        void onSelected(long timeMs);
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
            hookMainProfileSettingsEntry(lpparam.classLoader);
            forceNativeReverseOrder(lpparam.classLoader);
            forceTimelineLayoutDirection(lpparam.classLoader);
            forceTimelineDataOrder(lpparam.classLoader);
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

    private static void hookMainProfileSettingsEntry(final ClassLoader cl) {
        try {
            XposedHelpers.findAndHookMethod(
                Application.class,
                "attach",
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.thisObject instanceof Context) {
                            rememberWeicoContext((Context) param.thisObject, "Application.attach");
                        }
                    }
                }
            );
        } catch (Throwable t) {
            log("MainProfile application attach hook error: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.thisObject instanceof Context) {
                        rememberWeicoContext((Context) param.thisObject, "Application.onCreate");
                    }
                }
            });
        } catch (Throwable t) {
            log("MainProfile context hook error: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(
                "com.weico.international.ui.mainprofile.MainProfileViewModel",
                cl,
                "postValue2DrawerInfo",
                List.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (!isModuleOptionEnabled(ModuleSettings.KEY_WEICO_PROFILE_ENTRY, true)) {
                            return;
                        }
                        if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                            param.args[0] = appendReWeiboDrawerItem((List) param.args[0], cl);
                        }
                    }
                }
            );
            log("MainProfile drawer entry hook installed");
        } catch (Throwable t) {
            log("MainProfile drawer entry hook error: " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(View.class, "performClick", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!(param.thisObject instanceof View)) return;
                    View row = (View) param.thisObject;
                    boolean isReWeibo;
                    synchronized (sReWeiboProfileRows) {
                        isReWeibo = sReWeiboProfileRows.containsKey(row);
                    }
                    if (!isReWeibo
                        && "com.weico.international.view.node.ProfileItemNode"
                            .equals(row.getClass().getName())) {
                        try {
                            Object textView = getFieldValue(row, "mTextView");
                            isReWeibo = textView instanceof TextView
                                && "ReWeibo".contentEquals(((TextView) textView).getText());
                        } catch (Throwable ignored) {
                        }
                    }
                    if (!isReWeibo) return;
                    log("ReWeibo profile row click intercepted");
                    openReWeiboSettings(row.getContext());
                    param.setResult(Boolean.TRUE);
                }
            });
            log("MainProfile stable click hook installed");
        } catch (Throwable t) {
            log("MainProfile stable click hook error: " + t.getMessage());
        }

        try {
            Class<?> drawerInfoClass = XposedHelpers.findClass(
                "com.weico.international.ui.drawer.DrawerInfo",
                cl
            );
            XposedHelpers.findAndHookMethod(
                "com.weico.international.view.node.ProfileItemNode",
                cl,
                "bindModel",
                drawerInfoClass,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!(param.thisObject instanceof View)) return;
                        View row = (View) param.thisObject;
                        boolean isReWeibo = param.args != null
                            && param.args.length > 0
                            && isReWeiboDrawerInfo(param.args[0]);
                        if (isReWeibo) {
                            configureReWeiboProfileRow(row);
                        } else {
                            forgetReWeiboProfileRow(row);
                        }
                    }
                }
            );
            log("MainProfile drawer click hook installed");
        } catch (Throwable t) {
            log("MainProfile drawer click hook error: " + t.getMessage());
        }

        try {
            Class<?> onUpdateClass = Class.forName(
                "com.weico.international.view.node.ProfileItemNode$onUpdate$2",
                false,
                cl
            );
            XposedHelpers.findAndHookMethod(
                onUpdateClass,
                "invokeSuspend",
                Object.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object item = XposedHelpers.getObjectField(param.thisObject, "L$0");
                        if (!isReWeiboDrawerInfo(item)) {
                            return;
                        }
                        Object row = XposedHelpers.getObjectField(param.thisObject, "this$0");
                        if (row instanceof View) {
                            configureReWeiboProfileRow((View) row);
                        }
                    }
                }
            );
            log("MainProfile drawer update hook installed");
        } catch (Throwable t) {
            log("MainProfile drawer update hook error: " + t.getMessage());
        }
    }

    private static List appendReWeiboDrawerItem(List original, ClassLoader cl) {
        if (original == null) return original;
        try {
            for (Object item : original) {
                if (isReWeiboDrawerInfo(item)) {
                    return original;
                }
            }

            Object entry = createReWeiboDrawerInfo(cl);
            if (entry == null) return original;

            ArrayList copy = new ArrayList(original);
            int insertAt = copy.size();
            for (int i = 0; i < copy.size(); i++) {
                String type = getStringMethodOrField(copy.get(i), "getType", "type");
                if ("personal_setting".equals(type)) {
                    insertAt = i;
                    break;
                }
            }
            copy.add(insertAt, entry);
            return copy;
        } catch (Throwable t) {
            log("append ReWeibo drawer item error: " + t.getMessage());
            return original;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object createReWeiboDrawerInfo(ClassLoader cl) {
        try {
            Class<?> drawerInfoClass = XposedHelpers.findClass(
                "com.weico.international.ui.drawer.DrawerInfo",
                cl
            );
            Class<?> navItemTypeClass = XposedHelpers.findClass(
                "com.weico.international.ui.drawer.NavItemType",
                cl
            );
            Object navNormal = Enum.valueOf((Class<Enum>) navItemTypeClass.asSubclass(Enum.class), "NavNormal");
            return drawerInfoClass.getConstructor(
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                String.class,
                navItemTypeClass
            ).newInstance(
                "",
                "",
                REWEIBO_DRAWER_TYPE,
                "",
                "",
                "",
                REWEIBO_DRAWER_SCHEMA,
                "模块设置",
                "ReWeibo",
                REWEIBO_DRAWER_TYPE,
                navNormal
            );
        } catch (Throwable t) {
            log("create ReWeibo drawer item error: " + t.getMessage());
            return null;
        }
    }

    private static void configureReWeiboProfileRow(final View row) {
        if (row == null) return;
        synchronized (sReWeiboProfileRows) {
            sReWeiboProfileRows.put(row, Boolean.TRUE);
        }
        row.setClickable(true);
        applyReWeiboProfileIcon(row);
    }

    private static void forgetReWeiboProfileRow(View row) {
        if (row == null) return;
        synchronized (sReWeiboProfileRows) {
            sReWeiboProfileRows.remove(row);
        }
    }

    private static void applyReWeiboProfileIcon(View row) {
        try {
            Object iconObject = XposedHelpers.getObjectField(row, "mIconView");
            if (!(iconObject instanceof ImageView)) return;

            ImageView iconView = (ImageView) iconObject;
            Context context = iconView.getContext();
            int resId = 0;
            if (context != null) {
                resId = context.getResources().getIdentifier(
                    REWEIBO_DRAWER_ICON,
                    "drawable",
                    context.getPackageName()
                );
            }
            if (resId != 0) {
                iconView.setImageResource(resId);
            } else {
                iconView.setImageResource(android.R.drawable.ic_menu_manage);
            }
            iconView.setColorFilter(Color.rgb(185, 192, 202));
            iconView.setAlpha(1f);
            iconView.setVisibility(View.VISIBLE);
        } catch (Throwable t) {
            log("apply ReWeibo profile icon error: " + t.getMessage());
        }
    }

    private static boolean isReWeiboDrawerInfo(Object item) {
        if (item == null) return false;
        String type = getStringMethodOrField(item, "getType", "type");
        if (REWEIBO_DRAWER_TYPE.equals(type)) return true;
        String schema = getStringMethodOrField(item, "getSchema", "schema");
        if (REWEIBO_DRAWER_SCHEMA.equals(schema)) return true;
        String title = getStringMethodOrField(item, "getTitle", "title");
        return "ReWeibo".equals(title);
    }

    private static void openReWeiboSettings(Context context) {
        if (context == null) return;
        Activity activity = findActivity(context);
        if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
            showReWeiboSettingsDialog(activity);
            return;
        }
        try {
            Context launchContext = activity != null ? activity : context;
            Intent intent = new Intent();
            intent.setClassName("com.tianqianguai.reweibo", "com.tianqianguai.reweibo.SettingsActivity");
            if (activity == null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            launchContext.startActivity(intent);
            log("Opened ReWeibo settings activity");
            return;
        } catch (Throwable t) {
            log("open ReWeibo settings error: " + t.getMessage());
            Toast.makeText(context, "无法打开 ReWeibo 设置", Toast.LENGTH_SHORT).show();
        }
    }

    private static void showReWeiboSettingsDialog(final Activity activity) {
        try {
            final SharedPreferences prefs = getWeicoSettingsPrefs();
            if (prefs == null) return;

            LinearLayout panel = new LinearLayout(activity);
            panel.setOrientation(LinearLayout.VERTICAL);
            int padding = dpToPx(activity.getWindow().getDecorView(), 20);
            panel.setPadding(padding, padding / 2, padding, padding / 2);

            TextView label = new TextView(activity);
            label.setText("首页缓存天数（1-30）");
            label.setTextColor(Color.WHITE);
            label.setTextSize(15f);
            panel.addView(label);

            final EditText input = new EditText(activity);
            input.setSingleLine(true);
            input.setSelectAllOnFocus(true);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(Color.rgb(145, 155, 170));
            input.setBackground(makeDarkDialogInputBackground(activity.getWindow().getDecorView()));
            int current = getTimelineCacheDaysSetting();
            if (!prefs.contains(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS)) {
                boolean migrated = prefs.edit()
                    .putInt(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS, current)
                    .commit();
                log("Timeline cache-days target migration value=" + current
                    + " saved=" + migrated);
            }
            input.setText(String.valueOf(current));
            panel.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(activity.getWindow().getDecorView(), 48)
            ));

            View divider = new View(activity);
            divider.setBackgroundColor(0xFF343946);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, dpToPx(activity.getWindow().getDecorView(), 1))
            );
            dividerParams.setMargins(0, padding, 0, padding / 2);
            panel.addView(divider, dividerParams);

            TextView clearLabel = new TextView(activity);
            clearLabel.setText("缓存微博清理");
            clearLabel.setTextColor(Color.WHITE);
            clearLabel.setTextSize(15f);
            panel.addView(clearLabel);

            TimelineCacheStats cacheStats = buildBestTimelineCacheStats(sLastTimelinePresenter);
            TextView clearDescription = new TextView(activity);
            clearDescription.setText(
                formatTimelineCacheRangeSummary(cacheStats)
                    + "\n可按微博发布时间选择起止范围；清理后再次浏览仍可能重新缓存。"
            );
            clearDescription.setTextColor(Color.rgb(160, 169, 184));
            clearDescription.setTextSize(12f);
            clearDescription.setPadding(0, dpToPx(activity.getWindow().getDecorView(), 6), 0, 0);
            panel.addView(clearDescription);

            TextView clearButton = new TextView(activity);
            clearButton.setText("选择时间范围并清除");
            clearButton.setTextColor(0xFFFF6B73);
            clearButton.setTextSize(14f);
            clearButton.setGravity(Gravity.CENTER);
            clearButton.setClickable(true);
            clearButton.setFocusable(true);
            clearButton.setBackground(makeDarkDialogInputBackground(activity.getWindow().getDecorView()));
            LinearLayout.LayoutParams clearButtonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(activity.getWindow().getDecorView(), 44)
            );
            clearButtonParams.setMargins(0, dpToPx(activity.getWindow().getDecorView(), 12), 0, 0);
            panel.addView(clearButton, clearButtonParams);
            clearButton.setOnClickListener(v -> showTimelineCacheClearRangeDialog(activity));

            final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("ReWeibo 设置")
                .setView(panel)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null)
                .create();
            dialog.setOnShowListener(ignored -> {
                styleDarkDialog(dialog, activity.getWindow().getDecorView());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String raw = input.getText() == null ? "" : input.getText().toString().trim();
                    int days;
                    try {
                        days = Integer.parseInt(raw);
                    } catch (Throwable ignoredParse) {
                        input.setError("请输入 1-30");
                        return;
                    }
                    if (days < ModuleSettings.MIN_WEICO_TIMELINE_CACHE_DAYS
                        || days > ModuleSettings.MAX_WEICO_TIMELINE_CACHE_DAYS) {
                        input.setError("请输入 1-30");
                        return;
                    }
                    boolean saved = prefs.edit()
                        .putInt(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS, days)
                        .commit();
                    if (!saved) {
                        input.setError("保存失败，请重试");
                        return;
                    }
                    rememberModuleIntSetting(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS, days);
                    if (sLastTimelinePresenter != null) {
                        resetPreloadState(sLastTimelinePresenter, "settings-saved");
                        scheduleTimelinePreload(sLastTimelinePresenter, "settings-saved");
                    }
                    Toast.makeText(activity, "已保存：" + days + " 天", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            });
            dialog.show();
            input.requestFocus();
        } catch (Throwable t) {
            log("show ReWeibo settings dialog error: " + t.getMessage());
        }
    }

    private static String formatTimelineCacheRangeSummary(TimelineCacheStats stats) {
        if (stats == null || stats.count <= 0) return "当前未检测到缓存微博";
        if (stats.oldestMs <= 0L || stats.newestMs <= 0L) {
            return "当前检测到 " + stats.count + " 条缓存微博，部分发布时间未知";
        }
        return "当前约 " + stats.count + " 条："
            + formatTimelineClearTime(stats.oldestMs)
            + " 至 "
            + formatTimelineClearTime(stats.newestMs);
    }

    private static String formatTimelineClearTime(long timeMs) {
        if (timeMs <= 0L) return "未知";
        return new SimpleDateFormat(TIMELINE_CLEAR_TIME_PATTERN, Locale.getDefault())
            .format(new Date(timeMs));
    }

    private static EditText newTimelineClearTimeInput(Activity activity, long timeMs) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setText(formatTimelineClearTime(timeMs));
        input.setHint("yyyy-MM-dd HH:mm");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(145, 155, 170));
        input.setTextSize(14f);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setPadding(dpToPx(activity.getWindow().getDecorView(), 12), 0, 0, 0);
        input.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_NORMAL);
        input.setKeyListener(DigitsKeyListener.getInstance("0123456789-/: .年月日号"));
        input.setBackground(makeDarkDialogInputBackground(activity.getWindow().getDecorView()));
        return input;
    }

    private static EditText newTimelineClearSingleDayInput(Activity activity) {
        EditText input = newTimelineClearTimeInput(activity, 0L);
        input.setText("");
        input.setHint("如 7号、7-7、2026-07-07");
        input.setContentDescription("缓存清理单日快捷输入");
        return input;
    }

    private static TextView newTimelineClearCalendarButton(Activity activity, String contentDescription) {
        TextView button = new TextView(activity);
        button.setText("日历");
        button.setContentDescription(contentDescription);
        button.setTextColor(0xFFFF6B73);
        button.setTextSize(14f);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(makeDarkDialogInputBackground(activity.getWindow().getDecorView()));
        return button;
    }

    private static void showTimelineCacheClearRangeDialog(final Activity activity) {
        try {
            TimelineCacheStats stats = buildBestTimelineCacheStats(sLastTimelinePresenter);
            long nowMs = System.currentTimeMillis();
            long initialStart = stats != null && stats.oldestMs > 0L
                ? stats.oldestMs
                : nowMs - TIMELINE_CACHE_DAY_MS;
            long initialEnd = stats != null && stats.newestMs > 0L
                ? stats.newestMs
                : nowMs;
            final long[] selectedRange = new long[] {
                normalizeTimelineClearMinute(initialStart, false),
                normalizeTimelineClearMinute(initialEnd, true)
            };

            LinearLayout panel = new LinearLayout(activity);
            panel.setOrientation(LinearLayout.VERTICAL);
            int padding = dpToPx(activity.getWindow().getDecorView(), 20);
            panel.setPadding(padding, padding / 2, padding, padding / 2);

            TextView hint = new TextView(activity);
            hint.setText(
                "可直接输入日期或时间，也可使用右侧日历。日期支持 7号、7-7、"
                    + "2026-07-07 等写法；清理范围包含起止边界。"
            );
            hint.setTextColor(Color.rgb(160, 169, 184));
            hint.setTextSize(13f);
            hint.setPadding(0, 0, 0, dpToPx(activity.getWindow().getDecorView(), 12));
            panel.addView(hint);

            TextView singleDayLabel = new TextView(activity);
            singleDayLabel.setText("只清除某一天（可选，填写后优先）");
            singleDayLabel.setTextColor(Color.WHITE);
            singleDayLabel.setTextSize(14f);
            panel.addView(singleDayLabel);

            final EditText singleDayInput = newTimelineClearSingleDayInput(activity);
            LinearLayout.LayoutParams singleDayParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(activity.getWindow().getDecorView(), 46)
            );
            singleDayParams.setMargins(0, dpToPx(activity.getWindow().getDecorView(), 6), 0,
                dpToPx(activity.getWindow().getDecorView(), 14));
            panel.addView(singleDayInput, singleDayParams);

            TextView startLabel = new TextView(activity);
            startLabel.setText("开始时间");
            startLabel.setTextColor(Color.WHITE);
            startLabel.setTextSize(14f);
            panel.addView(startLabel);

            final EditText startInput = newTimelineClearTimeInput(activity, selectedRange[0]);
            startInput.setContentDescription("缓存清理开始时间");
            final TextView startCalendar = newTimelineClearCalendarButton(
                activity,
                "为开始时间打开日历"
            );
            LinearLayout startRow = new LinearLayout(activity);
            startRow.setOrientation(LinearLayout.HORIZONTAL);
            startRow.setGravity(Gravity.CENTER_VERTICAL);
            startRow.addView(startInput, new LinearLayout.LayoutParams(
                0,
                dpToPx(activity.getWindow().getDecorView(), 46),
                1f
            ));
            LinearLayout.LayoutParams startCalendarParams = new LinearLayout.LayoutParams(
                dpToPx(activity.getWindow().getDecorView(), 68),
                dpToPx(activity.getWindow().getDecorView(), 46)
            );
            startCalendarParams.setMargins(dpToPx(activity.getWindow().getDecorView(), 8), 0, 0, 0);
            startRow.addView(startCalendar, startCalendarParams);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(activity.getWindow().getDecorView(), 46)
            );
            timeParams.setMargins(0, dpToPx(activity.getWindow().getDecorView(), 6), 0,
                dpToPx(activity.getWindow().getDecorView(), 14));
            panel.addView(startRow, timeParams);

            TextView endLabel = new TextView(activity);
            endLabel.setText("结束时间");
            endLabel.setTextColor(Color.WHITE);
            endLabel.setTextSize(14f);
            panel.addView(endLabel);

            final EditText endInput = newTimelineClearTimeInput(activity, selectedRange[1]);
            endInput.setContentDescription("缓存清理结束时间");
            final TextView endCalendar = newTimelineClearCalendarButton(
                activity,
                "为结束时间打开日历"
            );
            LinearLayout endRow = new LinearLayout(activity);
            endRow.setOrientation(LinearLayout.HORIZONTAL);
            endRow.setGravity(Gravity.CENTER_VERTICAL);
            endRow.addView(endInput, new LinearLayout.LayoutParams(
                0,
                dpToPx(activity.getWindow().getDecorView(), 46),
                1f
            ));
            LinearLayout.LayoutParams endCalendarParams = new LinearLayout.LayoutParams(
                dpToPx(activity.getWindow().getDecorView(), 68),
                dpToPx(activity.getWindow().getDecorView(), 46)
            );
            endCalendarParams.setMargins(dpToPx(activity.getWindow().getDecorView(), 8), 0, 0, 0);
            endRow.addView(endCalendar, endCalendarParams);
            LinearLayout.LayoutParams endParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(activity.getWindow().getDecorView(), 46)
            );
            endParams.setMargins(0, dpToPx(activity.getWindow().getDecorView(), 6), 0, 0);
            panel.addView(endRow, endParams);

            startInput.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) singleDayInput.setText("");
            });
            endInput.setOnFocusChangeListener((view, hasFocus) -> {
                if (hasFocus) singleDayInput.setText("");
            });

            startCalendar.setOnClickListener(v -> {
                singleDayInput.setText("");
                long typed = parseTimelineClearTimeInput(
                    startInput.getText() == null ? null : startInput.getText().toString(),
                    false
                );
                showTimelineClearDateTimePicker(
                    activity,
                    typed > 0L ? typed : selectedRange[0],
                    false,
                    timeMs -> {
                        selectedRange[0] = timeMs;
                        startInput.setError(null);
                        startInput.setText(formatTimelineClearTime(timeMs));
                    }
                );
            });
            endCalendar.setOnClickListener(v -> {
                singleDayInput.setText("");
                long typed = parseTimelineClearTimeInput(
                    endInput.getText() == null ? null : endInput.getText().toString(),
                    true
                );
                showTimelineClearDateTimePicker(
                    activity,
                    typed > 0L ? typed : selectedRange[1],
                    true,
                    timeMs -> {
                        selectedRange[1] = timeMs;
                        endInput.setError(null);
                        endInput.setText(formatTimelineClearTime(timeMs));
                    }
                );
            });

            final AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("清除缓存微博")
                .setView(panel)
                .setNegativeButton("取消", null)
                .setPositiveButton("下一步", null)
                .create();
            dialog.setOnShowListener(ignored -> {
                styleDarkDialog(dialog, activity.getWindow().getDecorView());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String singleDayRaw = singleDayInput.getText() == null
                        ? ""
                        : singleDayInput.getText().toString().trim();
                    if (!singleDayRaw.isEmpty()) {
                        long dayStart = parseTimelineClearDateOnlyInput(singleDayRaw, false);
                        long dayEnd = parseTimelineClearDateOnlyInput(singleDayRaw, true);
                        if (dayStart <= 0L || dayEnd <= 0L) {
                            singleDayInput.setError("请输入有效日期，如 7号、7-7 或 2026-07-07");
                            singleDayInput.requestFocus();
                            return;
                        }
                        selectedRange[0] = dayStart;
                        selectedRange[1] = dayEnd;
                        startInput.setText(formatTimelineClearTime(dayStart));
                        endInput.setText(formatTimelineClearTime(dayEnd));
                        singleDayInput.clearFocus();
                        startInput.clearFocus();
                        endInput.clearFocus();
                        showTimelineCacheClearConfirmation(
                            activity,
                            dialog,
                            dayStart,
                            dayEnd
                        );
                        return;
                    }
                    long parsedStart = parseTimelineClearTimeInput(
                        startInput.getText() == null ? null : startInput.getText().toString(),
                        false
                    );
                    if (parsedStart <= 0L) {
                        startInput.setError("请输入有效日期或时间，如 7号、7-7、2026-07-07 00:00");
                        startInput.requestFocus();
                        return;
                    }
                    long parsedEnd = parseTimelineClearTimeInput(
                        endInput.getText() == null ? null : endInput.getText().toString(),
                        true
                    );
                    if (parsedEnd <= 0L) {
                        endInput.setError("请输入有效日期或时间，如 7号、7-7、2026-07-07 23:59");
                        endInput.requestFocus();
                        return;
                    }
                    if (parsedStart > parsedEnd) {
                        startInput.setError("开始时间不能晚于结束时间");
                        startInput.requestFocus();
                        return;
                    }
                    selectedRange[0] = parsedStart;
                    selectedRange[1] = parsedEnd;
                    startInput.setText(formatTimelineClearTime(parsedStart));
                    endInput.setText(formatTimelineClearTime(parsedEnd));
                    startInput.clearFocus();
                    endInput.clearFocus();
                    showTimelineCacheClearConfirmation(
                        activity,
                        dialog,
                        selectedRange[0],
                        selectedRange[1]
                    );
                });
            });
            dialog.show();
        } catch (Throwable t) {
            log("show timeline cache clear range error: " + t.getMessage());
            Toast.makeText(activity, "无法打开缓存清理，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private static void showTimelineClearDateTimePicker(
        final Activity activity,
        long initialTimeMs,
        final boolean endOfMinute,
        final TimelineDateTimeCallback callback
    ) {
        final Calendar selected = Calendar.getInstance();
        selected.setTimeInMillis(initialTimeMs > 0L ? initialTimeMs : System.currentTimeMillis());
        DatePickerDialog dateDialog = new DatePickerDialog(
            activity,
            (datePicker, year, month, dayOfMonth) -> {
                selected.set(Calendar.YEAR, year);
                selected.set(Calendar.MONTH, month);
                selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                TimePickerDialog timeDialog = new TimePickerDialog(
                    activity,
                    (timePicker, hourOfDay, minute) -> {
                        selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selected.set(Calendar.MINUTE, minute);
                        selected.set(Calendar.SECOND, endOfMinute ? 59 : 0);
                        selected.set(Calendar.MILLISECOND, endOfMinute ? 999 : 0);
                        callback.onSelected(selected.getTimeInMillis());
                    },
                    selected.get(Calendar.HOUR_OF_DAY),
                    selected.get(Calendar.MINUTE),
                    true
                );
                timeDialog.show();
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH)
        );
        dateDialog.show();
    }

    private static long normalizeTimelineClearMinute(long timeMs, boolean endOfMinute) {
        Calendar value = Calendar.getInstance();
        value.setTimeInMillis(timeMs > 0L ? timeMs : System.currentTimeMillis());
        value.set(Calendar.SECOND, endOfMinute ? 59 : 0);
        value.set(Calendar.MILLISECOND, endOfMinute ? 999 : 0);
        return value.getTimeInMillis();
    }

    static long parseTimelineClearTimeInput(String raw, boolean endOfMinute) {
        if (raw == null) return -1L;
        String text = raw.trim();
        if (text.isEmpty()) return -1L;
        String normalized = normalizeTimelineClearInput(text);
        String[] patterns = new String[] {
            "yyyy-M-d H:m",
            "yyyy/M/d H:m",
            "yyyy.M.d H:m",
            "yyyyMMddHHmm"
        };
        for (int i = 0; i < patterns.length; i++) {
            SimpleDateFormat parser = new SimpleDateFormat(patterns[i], Locale.getDefault());
            parser.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date parsed = parser.parse(normalized, position);
            if (parsed == null || position.getIndex() != normalized.length()) continue;
            return normalizeTimelineClearMinute(parsed.getTime(), endOfMinute);
        }
        return parseTimelineClearDateOnlyInput(text, endOfMinute);
    }

    static long parseTimelineClearDateOnlyInput(String raw, boolean endOfDay) {
        if (raw == null) return -1L;
        String text = normalizeTimelineClearInput(raw.trim());
        if (text.isEmpty() || text.indexOf(':') >= 0) return -1L;

        int year;
        int month;
        int day;
        Calendar reference = Calendar.getInstance();
        String[] parts;
        if (text.matches("\\d{8}")) {
            year = parsePositiveInt(text.substring(0, 4));
            month = parsePositiveInt(text.substring(4, 6));
            day = parsePositiveInt(text.substring(6, 8));
        } else if (text.matches("\\d{4}[-/.]\\d{1,2}[-/.]\\d{1,2}")) {
            parts = text.split("[-/.]");
            year = parsePositiveInt(parts[0]);
            month = parsePositiveInt(parts[1]);
            day = parsePositiveInt(parts[2]);
        } else if (text.matches("\\d{1,2}[-/.]\\d{1,2}")) {
            parts = text.split("[-/.]");
            year = reference.get(Calendar.YEAR);
            month = parsePositiveInt(parts[0]);
            day = parsePositiveInt(parts[1]);
        } else if (text.matches("\\d{1,2}")) {
            year = reference.get(Calendar.YEAR);
            month = reference.get(Calendar.MONTH) + 1;
            day = parsePositiveInt(text);
        } else {
            return -1L;
        }

        if (year < 1970 || month < 1 || month > 12 || day < 1 || day > 31) return -1L;
        Calendar value = Calendar.getInstance();
        value.clear();
        value.setLenient(false);
        value.set(
            year,
            month - 1,
            day,
            endOfDay ? 23 : 0,
            endOfDay ? 59 : 0,
            endOfDay ? 59 : 0
        );
        value.set(Calendar.MILLISECOND, endOfDay ? 999 : 0);
        try {
            return value.getTimeInMillis();
        } catch (Throwable ignored) {
            return -1L;
        }
    }

    private static String normalizeTimelineClearInput(String raw) {
        if (raw == null) return "";
        return raw.trim()
            .replace('年', '-')
            .replace('月', '-')
            .replace("日", "")
            .replace("号", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private static int parsePositiveInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static void showTimelineCacheClearConfirmation(
        final Activity activity,
        final AlertDialog rangeDialog,
        final long startMs,
        final long endMs
    ) {
        AlertDialog confirmation = new AlertDialog.Builder(activity)
            .setTitle("确认清除？")
            .setMessage(
                "将清除 " + formatTimelineClearTime(startMs)
                    + " 至 " + formatTimelineClearTime(endMs)
                    + " 发布的缓存微博。此操作无法撤销。"
            )
            .setNegativeButton("返回", null)
            .setPositiveButton("确认清除", (dialog, which) -> {
                rangeDialog.dismiss();
                startTimelineCacheRangeClear(activity, startMs, endMs);
            })
            .create();
        confirmation.setOnShowListener(ignored ->
            styleDarkDialog(confirmation, activity.getWindow().getDecorView())
        );
        confirmation.show();
    }

    private static void startTimelineCacheRangeClear(
        final Activity activity,
        final long startMs,
        final long endMs
    ) {
        if (startMs <= 0L || endMs < startMs) {
            Toast.makeText(activity, "清理时间范围无效", Toast.LENGTH_SHORT).show();
            return;
        }
        final Object presenter = sLastTimelinePresenter;
        if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))
            || getTimelineAction(presenter) == null) {
            Toast.makeText(activity, "请先打开首页时间线，待微博显示后再清理", Toast.LENGTH_LONG).show();
            return;
        }
        synchronized (WeiboLiteHook.class) {
            if (sTimelineCacheClearInFlight) {
                Toast.makeText(activity, "缓存正在清理，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }
            sTimelineCacheClearInFlight = true;
        }

        final ArrayList liveStatuses = snapshotTimelineStatuses(presenter, null);
        final ArrayList cumulativeStatuses;
        synchronized (sTimelineCumulativeStatusesById) {
            cumulativeStatuses = new ArrayList(sTimelineCumulativeStatusesById.values());
        }
        prepareTimelineStateForCacheClear(presenter);

        ProgressBar progressBar = new ProgressBar(activity);
        int progressPadding = dpToPx(activity.getWindow().getDecorView(), 24);
        progressBar.setPadding(progressPadding, progressPadding, progressPadding, progressPadding);
        final AlertDialog progressDialog = new AlertDialog.Builder(activity)
            .setTitle("正在清除缓存")
            .setMessage("正在安全改写缓存文件，请勿关闭微博轻享版。")
            .setView(progressBar)
            .setCancelable(false)
            .create();
        progressDialog.setOnShowListener(ignored ->
            styleDarkDialog(progressDialog, activity.getWindow().getDecorView())
        );
        progressDialog.show();

        sTimelinePersistExecutor.execute(new Runnable() {
            @Override
            public void run() {
                TimelineCacheRangeClearResult result = null;
                Throwable error = null;
                try {
                    result = clearTimelineCacheRangeNow(
                        presenter,
                        liveStatuses,
                        cumulativeStatuses,
                        startMs,
                        endMs
                    );
                } catch (Throwable t) {
                    error = t;
                }

                final TimelineCacheRangeClearResult completed = result;
                final Throwable failure = error;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (progressDialog.isShowing()) progressDialog.dismiss();
                            if (failure != null || completed == null) {
                                resetPreloadState(presenter, "manual-cache-clear-failed");
                                String detail = failure == null
                                    ? "unknown error"
                                    : failure.getClass().getSimpleName() + ": " + failure.getMessage();
                                log("Timeline cache range clear failed: " + detail);
                                Toast.makeText(activity, "缓存清理失败，请重试", Toast.LENGTH_LONG).show();
                                return;
                            }
                            applyTimelineCacheRangeClear(presenter, completed);
                            if (completed.removedCount <= 0) {
                                resetPreloadState(presenter, "manual-cache-clear-empty");
                                Toast.makeText(
                                    activity,
                                    "所选时间范围内没有缓存微博",
                                    Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                Toast.makeText(
                                    activity,
                                    "已清除 " + completed.removedCount
                                        + " 条，保留 " + completed.retainedStatuses.size() + " 条",
                                    Toast.LENGTH_LONG
                                ).show();
                            }
                        } finally {
                            sTimelineCacheClearInFlight = false;
                        }
                    }
                });
            }
        });
    }

    private static void prepareTimelineStateForCacheClear(Object presenter) {
        synchronized (sTimelineRestoreLock) {
            TimelineRestoreState state = sTimelineRestoreStates.get(presenter);
            if (state == null) {
                state = new TimelineRestoreState();
                sTimelineRestoreStates.put(presenter, state);
            }
            state.generation++;
            state.inFlight = false;
            state.diskRestoreResolved = true;
            state.pendingLiveStatuses = null;
            state.pendingSource = null;
        }
        synchronized (sTimelinePersistQueueLock) {
            sPendingTimelineNativePersist = null;
            sPendingTimelineShadowPersist = null;
        }
        synchronized (sPreloadStates) {
            PreloadState state = getPreloadStateLocked(presenter);
            state.stopped = true;
            state.scheduled = false;
            state.inFlight = false;
            state.retryScheduled = false;
            state.requestToken++;
            state.retryToken++;
        }
        synchronized (WeiboLiteHook.class) {
            sPendingTimelineNoMoreSource = null;
            sPendingTimelineNoMoreGeneration++;
        }
        stopTimelineGapFill("manual-cache-clear");
        log("Timeline cache range clear prepared live=" + snapshotTimelineStatuses(presenter, null).size());
    }

    private static TimelineCacheRangeClearResult clearTimelineCacheRangeNow(
        Object presenter,
        List liveStatuses,
        List cumulativeStatuses,
        long startMs,
        long endMs
    ) throws Exception {
        Object action = getTimelineAction(presenter);
        if (action == null) throw new IllegalStateException("timeline action is missing");
        Object builder = getTimelineCacheBuilder(action);
        TimelineCacheLoad load = loadTimelineCacheSingleSource(action, builder, "manual-range-clear");
        List diskStatuses = load == null ? null : load.statuses;
        ArrayList merged = mergeTimelineStatusLists(diskStatuses, cumulativeStatuses, liveStatuses);
        TimelineCacheRangeFilterResult filtered = filterTimelineCacheRange(
            merged,
            startMs,
            endMs
        );
        ArrayList retained = new ArrayList(
            sortTimelineNewestFirst(
                filtered.retainedStatuses,
                presenter,
                "manual-range-clear",
                false
            )
        );
        String maxId = retained.isEmpty() ? "0" : getTimelineCacheMaxId(action, retained);
        if (filtered.removedCount <= 0) {
            return new TimelineCacheRangeClearResult(
                retained,
                filtered.sourceCount,
                0,
                false,
                maxId
            );
        }

        Object result = newTimelineStatusResult(action, retained, maxId);
        File nativeFile = getTimelineNativeCacheFile(builder);
        if (nativeFile == null) throw new IllegalStateException("timeline native cache file is missing");
        writeTimelineNativeCacheStreaming(result, getTimelineCacheGson(builder), nativeFile);

        File shadowFile = getTimelineShadowCacheFile();
        File parent = shadowFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("cannot create timeline shadow cache directory");
        }
        copyFile(nativeFile, shadowFile);
        TimelineCacheStats stats = buildTimelineCacheStats(retained);
        writeTimelineShadowCacheMeta(
            nativeFile.getName(),
            "manual-range-clear",
            retained.size(),
            maxId,
            shadowFile.length(),
            stats
        );
        sTimelineShadowCacheCount = retained.size();
        log("Timeline cache range cleared start=" + startMs
            + " end=" + endMs
            + " source=" + (load == null ? "memory" : load.source)
            + " before=" + filtered.sourceCount
            + " removed=" + filtered.removedCount
            + " retained=" + retained.size());
        return new TimelineCacheRangeClearResult(
            retained,
            filtered.sourceCount,
            filtered.removedCount,
            filtered.removedLastRead,
            maxId
        );
    }

    private static TimelineCacheRangeFilterResult filterTimelineCacheRange(
        List source,
        long startMs,
        long endMs
    ) {
        ArrayList statuses = collectTimelineStatuses(source);
        ArrayList retained = new ArrayList(statuses.size());
        int removed = 0;
        boolean removedLastRead = false;
        long lastReadId = sLastReadStatusId == null ? 0L : sLastReadStatusId.longValue();
        for (int i = 0; i < statuses.size(); i++) {
            Object status = unwrapStatus(statuses.get(i));
            long createdMs = getStatusCreatedAtMillis(status);
            if (shouldClearTimelineStatusCreatedAt(createdMs, startMs, endMs)) {
                removed++;
                if (lastReadId > 0L && getStatusId(status) == lastReadId) {
                    removedLastRead = true;
                }
            } else {
                retained.add(status);
            }
        }
        return new TimelineCacheRangeFilterResult(
            retained,
            statuses.size(),
            removed,
            removedLastRead
        );
    }

    static boolean shouldClearTimelineStatusCreatedAt(long createdMs, long startMs, long endMs) {
        return createdMs > 0L && startMs > 0L && endMs >= startMs
            && createdMs >= startMs && createdMs <= endMs;
    }

    private static void applyTimelineCacheRangeClear(
        Object presenter,
        TimelineCacheRangeClearResult result
    ) {
        if (result.removedCount <= 0) return;
        try {
            replaceTimelineCumulativeStatuses(
                result.retainedStatuses,
                presenter,
                "manual-range-clear"
            );
            setTimelineCacheRestoring(presenter, true);
            try {
                setPresenterTimelineDataPrepared(
                    presenter,
                    result.retainedStatuses,
                    result.retainedStatuses.size(),
                    "manual-range-clear"
                );
            } finally {
                setTimelineCacheRestoring(presenter, false);
            }
            Object action = getTimelineAction(presenter);
            syncTimelineActionAfterCacheClear(action, result.maxId);
            synchronized (sPersistedTimelineCacheCounts) {
                sPersistedTimelineCacheCounts.put(
                    presenter,
                    Integer.valueOf(result.retainedStatuses.size())
                );
                sPersistedTimelineCacheNewestIds.put(
                    presenter,
                    Long.valueOf(getNewestTimelineStatusId(result.retainedStatuses))
                );
            }
            forgetTimelinePreloadDone("manual-range-clear");
            if (result.removedLastRead) forgetTimelineLastRead("manual-range-clear");
            sTimelineOldestFirstMode = false;
            sTimelineRestoredCacheMode = false;
            log("Timeline cache range clear applied before=" + result.sourceCount
                + " removed=" + result.removedCount
                + " retained=" + result.retainedStatuses.size());
        } catch (Throwable t) {
            setTimelineCacheRestoring(presenter, false);
            log("Timeline cache range clear apply error: " + t.getMessage());
        }
    }

    private static void syncTimelineActionAfterCacheClear(Object action, String maxId) {
        if (action == null || !hasMeaningfulString(maxId)) return;
        if (!"0".equals(maxId)) {
            syncTimelineActionMaxId(action, maxId, "manual-range-clear");
            return;
        }
        try { XposedHelpers.callMethod(action, "setMaxId", "0"); } catch (Throwable ignored) {}
        try { XposedHelpers.callMethod(action, "setMaxId", Long.valueOf(0L)); } catch (Throwable ignored) {}
        String[] fields = new String[] {
            "lastMaxId", "maxId", "mMaxId", "max_id", "maxid", "maxID", "maxIdStr", "max_id_str"
        };
        for (int i = 0; i < fields.length; i++) {
            setTimelineCursorField(action, fields[i], "0");
        }
        log("Timeline cursor reset source=manual-range-clear");
    }

    private static GradientDrawable makeDarkDialogInputBackground(View anchor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF242936);
        background.setCornerRadius(dpToPx(anchor, 8));
        background.setStroke(Math.max(1, dpToPx(anchor, 1)), 0xFF536A9F);
        return background;
    }

    private static void styleDarkDialog(AlertDialog dialog, View anchor) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(0xFF17181C);
        background.setCornerRadius(dpToPx(anchor, 12));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(background);
        }
        Context context = anchor.getContext();
        int titleId = context.getResources().getIdentifier("alertTitle", "id", "android");
        View title = titleId == 0 ? null : dialog.findViewById(titleId);
        if (title instanceof TextView) {
            ((TextView) title).setTextColor(Color.WHITE);
        }
        if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFFF5A63);
        }
        if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFFF5A63);
        }
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            current = ((ContextWrapper) current).getBaseContext();
        }
        return null;
    }

    private static SharedPreferences getWeicoSettingsPrefs() {
        Context context = getWeicoContext();
        if (context == null) return null;
        try {
            return context.getSharedPreferences(ModuleSettings.PREFS_NAME, Context.MODE_PRIVATE);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Context getWeicoContext() {
        return sWeicoContext;
    }

    private static void rememberWeicoContext(Context context, String source) {
        if (context == null) return;
        Context applicationContext = context.getApplicationContext();
        if (applicationContext != null) context = applicationContext;
        boolean recovered = sWeicoContext == null;
        sWeicoContext = context;
        if (recovered) {
            log("Weico application context captured source=" + source);
        }
        getTimelineCacheDaysSetting();
    }

    private static boolean isModuleOptionEnabled(String key, boolean fallback) {
        SharedPreferences prefs = getWeicoSettingsPrefs();
        if (prefs != null && prefs.contains(key)) {
            try {
                return prefs.getBoolean(key, fallback);
            } catch (Throwable ignored) {
            }
        }
        Context context = sWeicoContext;
        if (context != null) {
            Cursor cursor = null;
            try {
                Uri uri = ModuleSettings.settingsUriFor(key);
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int enabledColumn = cursor.getColumnIndex("enabled");
                    if (enabledColumn >= 0) {
                        return cursor.getInt(enabledColumn) != 0;
                    }
                }
            } catch (Throwable t) {
                log("read module setting error: " + key + " " + t.getMessage());
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable ignored) {}
                }
            }
        }

        prefs = getWeicoSettingsPrefs();
        if (prefs != null && prefs.contains(key)) {
            try {
                return prefs.getBoolean(key, fallback);
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    private static int readModuleIntSetting(String key, int fallback) {
        SharedPreferences targetPrefs = getWeicoSettingsPrefs();
        if (targetPrefs != null && targetPrefs.contains(key)) {
            try {
                int value = targetPrefs.getInt(key, fallback);
                rememberModuleIntSetting(key, value);
                return value;
            } catch (Throwable ignored) {
            }
        }

        Context context = getWeicoContext();
        if (context != null) {
            Cursor cursor = null;
            try {
                Uri uri = ModuleSettings.settingsUriFor(key);
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int valueColumn = cursor.getColumnIndex("value");
                    if (valueColumn >= 0) {
                        int value = cursor.getInt(valueColumn);
                        rememberModuleIntSetting(key, value);
                        return value;
                    }
                    int enabledColumn = cursor.getColumnIndex("enabled");
                    if (enabledColumn >= 0) {
                        int value = cursor.getInt(enabledColumn);
                        rememberModuleIntSetting(key, value);
                        return value;
                    }
                }
            } catch (Throwable t) {
                log("read module int setting error: " + key + " " + t.getMessage());
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Throwable ignored) {}
                }
            }
        }

        Integer cached = sModuleIntSettings.get(key);
        if (cached != null) return cached.intValue();

        return fallback;
    }

    private static boolean rememberModuleIntSetting(String key, int value) {
        Integer previous = sModuleIntSettings.put(key, Integer.valueOf(value));
        if (!ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS.equals(key)) return false;
        boolean changed = previous == null || previous.intValue() != value;
        boolean firstConfirmation = !sTimelineCacheDaysSettingConfirmed;
        sTimelineCacheDaysSettingConfirmed = true;
        if (changed || firstConfirmation) {
            sTimelinePreloadDone = null;
            sTimelinePreloadDoneCacheDays = -1;
            log("Timeline cache-days setting confirmed value=" + value);
        }
        return changed || firstConfirmation;
    }

    private static int getTimelineCacheDaysSetting() {
        int days = readModuleIntSetting(
            ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS,
            ModuleSettings.DEFAULT_WEICO_TIMELINE_CACHE_DAYS
        );
        return ModuleSettings.clampTimelineCacheDays(days);
    }

    static int timelinePreloadMaxPagesForCacheDays(int days) {
        int cacheDays = ModuleSettings.clampTimelineCacheDays(days);
        return Math.max(PRELOAD_BASE_MAX_PAGES, cacheDays * PRELOAD_MAX_PAGES_PER_CACHE_DAY);
    }

    static int timelinePreloadMaxItemsForCacheDays(int days) {
        int cacheDays = ModuleSettings.clampTimelineCacheDays(days);
        return Math.max(PRELOAD_BASE_MAX_ITEMS, cacheDays * PRELOAD_MAX_ITEMS_PER_CACHE_DAY);
    }

    private static int getTimelinePreloadMaxPages() {
        return timelinePreloadMaxPagesForCacheDays(getTimelineCacheDaysSetting());
    }

    private static int getTimelinePreloadMaxItems() {
        return timelinePreloadMaxItemsForCacheDays(getTimelineCacheDaysSetting());
    }

    private static void forceTimelineDataOrder(ClassLoader cl) {
        hookTimelineMergeOrder(cl);
        hookV2TimelineDataOrder(cl);
        hookV3TimelineDataOrder(cl);
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
                            handleHomeTabDoubleTap((MotionEvent) event);
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
            int side = getTimelineTopBarTapSide(event);
            if (side < 0) return;

            long now = SystemClock.elapsedRealtime();
            float x = event.getRawX();
            float y = event.getRawY();
            int slop = dpToPx(getAnyTimelineRecyclerView(), TOP_BAR_TAP_SLOP_DP);
            boolean isDoubleTap = now - sTopBarLastTapAtMs <= TOP_BAR_DOUBLE_TAP_MS
                && side == sTopBarLastTapSide
                && Math.abs(x - sTopBarLastTapX) <= slop
                && Math.abs(y - sTopBarLastTapY) <= slop;

            sTopBarLastTapAtMs = now;
            sTopBarLastTapX = x;
            sTopBarLastTapY = y;
            sTopBarLastTapSide = side;

            if (isDoubleTap) {
                sTopBarLastTapAtMs = 0L;
                sTopBarLastTapSide = -1;
                if (side == 0) {
                    jumpTimelineToAbsoluteTopForKnownRecyclerViews("top-bar-left-double-tap");
                } else {
                    jumpTimelineToAbsoluteBottomForKnownRecyclerViews("top-bar-right-double-tap");
                }
            }
        } catch (Throwable t) {
            log("Timeline top-bar double-tap error: " + t.getMessage());
        }
    }

    private static void handleHomeTabDoubleTap(MotionEvent event) {
        try {
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_UP) return;
            if (!isTimelineHomeTabTap(event)) return;

            long now = SystemClock.elapsedRealtime();
            if (action == MotionEvent.ACTION_DOWN) {
                captureHomeTabPendingRefreshAnchor(now);
                return;
            }

            float x = event.getRawX();
            float y = event.getRawY();
            captureHomeTabPendingRefreshAnchor(now);
            int slop = dpToPx(getAnyTimelineRecyclerView(), TOP_BAR_TAP_SLOP_DP);
            boolean isDoubleTap = now - sHomeTabLastTapAtMs <= HOME_TAB_DOUBLE_TAP_MS
                && Math.abs(x - sHomeTabLastTapX) <= slop
                && Math.abs(y - sHomeTabLastTapY) <= slop;

            sHomeTabLastTapAtMs = now;
            sHomeTabLastTapX = x;
            sHomeTabLastTapY = y;

            if (isDoubleTap) {
                sHomeTabLastTapAtMs = 0L;
                if (!captureTimelineRefreshAnchorFromPendingHomeTab("home-tab-double-tap")
                    && !captureTimelineRefreshAnchorForKnownRecyclerViews(sLastTimelinePresenter, "home-tab-double-tap")) {
                    log("Timeline refresh anchor capture skipped source=home-tab-double-tap");
                }
            }
        } catch (Throwable t) {
            log("Timeline home-tab double-tap error: " + t.getMessage());
        }
    }

    private static boolean isTimelineHomeTabTap(MotionEvent event) {
        Object recyclerView = getAnyTimelineRecyclerView();
        if (recyclerView == null) return false;
        try {
            int height = callIntMethodSafe(recyclerView, "getHeight", 0);
            int width = callIntMethodSafe(recyclerView, "getWidth", 0);
            if (height <= 0 || width <= 0) return false;
            if (event.getRawY() < height - dpToPx(recyclerView, HOME_TAB_TAP_BOTTOM_DP)) return false;
            return event.getRawX() >= 0f && event.getRawX() <= width * HOME_TAB_MAX_X_RATIO;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isTimelineTopBarTap(MotionEvent event) {
        return getTimelineTopBarTapSide(event) >= 0;
    }

    private static int getTimelineTopBarTapSide(MotionEvent event) {
        Object recyclerView = getAnyTimelineRecyclerView();
        int topLimit = dpToPx(recyclerView, TOP_BAR_TAP_MAX_DP);
        if (event.getRawY() > topLimit) return -1;
        try {
            int width = recyclerView == null ? 0 : callIntMethodSafe(recyclerView, "getWidth", 0);
            if (width > 0 && event.getRawX() >= width - dpToPx(recyclerView, TOP_BAR_RIGHT_EXCLUDE_DP)) {
                return -1;
            }
            if (width > 0) {
                return event.getRawX() < width / 2f ? 0 : 1;
            }
        } catch (Throwable ignored) {}
        return 0;
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
                            ensureTimelineTimeJumpButton(recyclerView, "onLayout");
                        }
                    }
                }
            );
            XposedHelpers.findAndHookMethod(
                recyclerViewClass,
                "onDetachedFromWindow",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.thisObject == sTimelineTimeJumpRecyclerView) {
                            removeTimelineTimeJumpButton("timeline-detached");
                        }
                    }
                }
            );
            XposedHelpers.findAndHookMethod(
                Activity.class,
                "onDestroy",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if (param.thisObject == sTimelineTimeJumpActivity) {
                            removeTimelineTimeJumpButton("activity-destroy");
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
            ensureTimelineTimeJumpButton(recyclerView, "layout-fix");
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
                        if (!scheduleTimelineRefreshAnchorForKnownRecyclerViews("presenter-addData-suppressed")) {
                            finishTimelineTopAnchorForKnownRecyclerViews("presenter-addData-suppressed");
                        }
                        return;
                    }
                    if (isTimelineCacheRestoreInFlight(param.thisObject)) {
                        scheduleTimelineCacheRestore(
                            param.thisObject,
                            null,
                            "presenter-addData-in-flight",
                            false
                        );
                        log("Timeline addData deferred during cache restore");
                        return;
                    }
                    List incomingData = null;
                    ArrayList mergedData = null;
                    boolean gapFillActive = hasActiveTimelineGapFill();
                    if (param.args != null && param.args.length > 0 && param.args[0] instanceof List) {
                        incomingData = (List) param.args[0];
                        if (gapFillActive) {
                            mergedData = mergeTimelineCumulativeStatuses(
                                incomingData,
                                param.thisObject,
                                "presenter-addData-incoming-gap"
                            );
                        } else {
                            mergeTimelineCumulativeStatusesIncremental(
                                incomingData,
                                param.thisObject,
                                "presenter-addData-incoming"
                            );
                        }
                    }
                    if (gapFillActive) {
                        if (pauseTimelineGapFillOnEmptyPage(param.thisObject, incomingData, mergedData, "presenter-addData")) {
                            return;
                        }
                        markTimelineNoMoreIfEmptyPage(param.thisObject, incomingData, "presenter-addData");
                        continueTimelineGapFill(param.thisObject, mergedData, "presenter-addData");
                        checkpointTimelineGapFillCache(param.thisObject, mergedData, "presenter-addData");
                        consumePendingTimelineNoMoreContent("presenter-addData-gap");
                        return;
                    }
                    normalizePresenterTimeline(param.thisObject, "presenter-addData");
                    persistTimelineNativeCache(param.thisObject, "presenter-addData");
                    consumePendingTimelineNoMoreContent("presenter-addData");
                    markTimelineNoMoreIfEmptyPage(param.thisObject, incomingData, "presenter-addData");
                    continueTimelineGapFill(param.thisObject, mergedData, "presenter-addData");
                    scheduleTimelinePreload(param.thisObject, "presenter-addData");
                    boolean anchoredRefresh = scheduleTimelineRefreshAnchorForKnownRecyclerViews("presenter-addData");
                    if (isTimelinePreloadStoppedState(param.thisObject)) {
                        if (!anchoredRefresh) {
                            finishTimelineTopAnchorForKnownRecyclerViews("presenter-addData-stop");
                        }
                    }
                }
            });
            XposedHelpers.findAndHookMethod(presenterClass, "setData", List.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    captureTimelineRefreshAnchorIfNeeded(param.thisObject, "presenter-setData-before");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    if (isTimelineCacheRestoring(param.thisObject)) {
                        log("Timeline setData observed prepared cache apply");
                        return;
                    }
                    resetPreloadState(param.thisObject, "presenter-setData");
                    List incomingData = param.args != null && param.args.length > 0 && param.args[0] instanceof List
                        ? (List) param.args[0]
                        : null;
                    if (getActiveTimelineRefreshAnchorStatusId() > 0L
                        && restoreTimelineCumulativeCache(param.thisObject, incomingData, "presenter-setData-refresh")) {
                        return;
                    }
                    if (restoreTimelineNativeCache(param.thisObject, incomingData, "presenter-setData")) {
                        return;
                    }
                    normalizePresenterTimeline(param.thisObject, "presenter-setData");
                    if (!isTimelinePreloadDone()) {
                        persistTimelineNativeCache(param.thisObject, "presenter-setData");
                    }
                    if (!scheduleTimelineRefreshAnchorForKnownRecyclerViews("presenter-setData")) {
                        beginTimelineTopAnchorForKnownRecyclerViews("presenter-setData", true);
                    }
                    scheduleTimelinePreload(param.thisObject, "presenter-setData");
                }
            });
            XposedHelpers.findAndHookMethod(presenterClass, "distinct", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    rememberTimelinePresenter(param.thisObject);
                    if (hasActiveTimelineGapFill()) {
                        return;
                    }
                    if (isTimelineCacheRestoreInFlight(param.thisObject)) {
                        scheduleTimelineCacheRestore(
                            param.thisObject,
                            null,
                            "presenter-distinct-in-flight",
                            false
                        );
                        log("Timeline distinct deferred during cache restore");
                        return;
                    }
                    if (shouldFreezeTimelineNetworkMutation(param.thisObject)) {
                        if (!scheduleTimelineRefreshAnchorForKnownRecyclerViews("presenter-distinct-suppressed")) {
                            finishTimelineTopAnchorForKnownRecyclerViews("presenter-distinct-suppressed");
                        }
                        return;
                    }
                    normalizePresenterTimeline(param.thisObject, "presenter-distinct");
                    persistTimelineNativeCache(param.thisObject, "presenter-distinct");
                    scheduleTimelineRefreshAnchorForKnownRecyclerViews("presenter-distinct");
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
                        if (loadNew) {
                            captureTimelineRefreshAnchorForKnownRecyclerViews(action, "v2-network-loadNew");
                        }
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
                            filtered = trimTimelineStatusesToCacheDays(filtered, param.thisObject, "v2-cache");
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

            try {
                XposedHelpers.findAndHookMethod(actionClass, "loadHomeTimeline",
                    long.class, long.class, long.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!shouldLogTimelineNetworkProbe(param.thisObject)) return;
                            boolean loadNew = param.args[3] instanceof Boolean && (Boolean) param.args[3];
                            log("Timeline v3 loadHomeTimeline request"
                                + " loadNew=" + loadNew
                                + " sinId=" + param.args[0]
                                + " maxId=" + param.args[1]
                                + " itemTime=" + param.args[2]
                                + describeTimelineActionState(param.thisObject));
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!shouldLogTimelineNetworkProbe(param.thisObject)) return;
                            boolean loadNew = param.args[3] instanceof Boolean && (Boolean) param.args[3];
                            Throwable throwable = getHookThrowableSafe(param);
                            if (throwable != null) {
                                log("Timeline v3 loadHomeTimeline threw"
                                    + " loadNew=" + loadNew
                                    + describeTimelineActionState(param.thisObject)
                                    + " error=" + describeThrowableForLog(throwable));
                                return;
                            }
                            Object result = param.getResult();
                            Object wrapped = wrapTimelineObservableProbe(result, cl, param.thisObject,
                                loadNew, "v3-loadHomeTimeline", true);
                            if (wrapped != result) {
                                param.setResult(wrapped);
                            }
                        }
                    }
                );
            } catch (Throwable t) {
                log("V3 timeline loadHomeTimeline probe hook error: " + t.getMessage());
            }

            try {
                XposedHelpers.findAndHookMethod(actionClass, "doLoadData", boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (!shouldLogTimelineNetworkProbe(param.thisObject)) return;
                            boolean loadNew = param.args[0] instanceof Boolean && (Boolean) param.args[0];
                            log("Timeline v3 doLoadData request"
                                + " loadNew=" + loadNew
                                + describeTimelineActionState(param.thisObject));
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (!shouldLogTimelineNetworkProbe(param.thisObject)) return;
                            boolean loadNew = param.args[0] instanceof Boolean && (Boolean) param.args[0];
                            Throwable throwable = getHookThrowableSafe(param);
                            if (throwable != null) {
                                log("Timeline v3 doLoadData threw"
                                    + " loadNew=" + loadNew
                                    + describeTimelineActionState(param.thisObject)
                                    + " error=" + describeThrowableForLog(throwable));
                                return;
                            }
                            Object result = param.getResult();
                            Object wrapped = wrapTimelineObservableProbe(result, cl, param.thisObject,
                                loadNew, "v3-doLoadData", false);
                            if (wrapped != result) {
                                param.setResult(wrapped);
                            }
                        }
                    }
                );
            } catch (Throwable t) {
                log("V3 timeline doLoadData probe hook error: " + t.getMessage());
            }

            XposedHelpers.findAndHookMethod(batchClass, "invoke", feedResultClass,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object result = param.getResult();
                        Object action = getOuterAction(param.thisObject);
                        boolean loadNew = getBooleanFieldSafe(param.thisObject, "$isLoadNew", false);
                        if (loadNew) {
                            captureTimelineRefreshAnchorForKnownRecyclerViews(action, "v3-network-loadNew");
                        }
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
                            filtered = trimTimelineStatusesToCacheDays(filtered, param.thisObject, "v3-cache");
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

    private static Object wrapTimelineObservableProbe(
        Object observable,
        ClassLoader cl,
        final Object action,
        final boolean loadNew,
        final String source,
        final boolean feedResultValues
    ) {
        if (observable == null || !shouldLogTimelineNetworkProbe(action)) return observable;
        try {
            final int probeId = nextTimelineNetworkProbeSeq();
            final boolean[] seenNext = new boolean[]{false};
            Class<?> consumerClass = Class.forName("io.reactivex.functions.Consumer", false, cl);
            Object wrapped = observable;

            Object onSubscribe = java.lang.reflect.Proxy.newProxyInstance(
                consumerClass.getClassLoader(),
                new Class[]{consumerClass},
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if (isObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnSubscribe")) {
                            return handleObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnSubscribe");
                        }
                        if ("accept".equals(method.getName())) {
                            log("Timeline v3 observable subscribe"
                                + " probe=" + probeId
                                + " source=" + source
                                + " loadNew=" + loadNew
                                + describeTimelineActionState(action));
                        }
                        return null;
                    }
                }
            );
            wrapped = XposedHelpers.callMethod(wrapped, "doOnSubscribe", onSubscribe);

            Object onNext = java.lang.reflect.Proxy.newProxyInstance(
                consumerClass.getClassLoader(),
                new Class[]{consumerClass},
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if (isObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnNext")) {
                            return handleObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnNext");
                        }
                        if ("accept".equals(method.getName())) {
                            seenNext[0] = true;
                            Object value = args != null && args.length > 0 ? args[0] : null;
                            if (!loadNew && isTimelineEmptyNetworkValue(value, feedResultValues)) {
                                recordTimelineGapFillEmptyResponse(source);
                            }
                            if (!loadNew && isTimelineServerTerminalFeedResult(value, feedResultValues)) {
                                deferTimelineNoMoreContent(source + "-server-no-more-feedresult");
                            }
                            log("Timeline v3 observable next"
                                + " probe=" + probeId
                                + " source=" + source
                                + " loadNew=" + loadNew
                                + " value=" + describeTimelineObservableValue(value, feedResultValues)
                                + describeTimelineActionState(action));
                        }
                        return null;
                    }
                }
            );
            wrapped = XposedHelpers.callMethod(wrapped, "doOnNext", onNext);

            Object onError = java.lang.reflect.Proxy.newProxyInstance(
                consumerClass.getClassLoader(),
                new Class[]{consumerClass},
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if (isObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnError")) {
                            return handleObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnError");
                        }
                        if ("accept".equals(method.getName())) {
                            Throwable throwable = args != null && args.length > 0 && args[0] instanceof Throwable
                                ? (Throwable) args[0]
                                : null;
                            if (!loadNew) {
                                recordTimelineGapFillErrorResponse(source, throwable);
                            }
                            log("Timeline v3 observable error"
                                + " probe=" + probeId
                                + " source=" + source
                                + " loadNew=" + loadNew
                                + " seenNext=" + seenNext[0]
                                + " error=" + describeThrowableForLog(throwable)
                                + describeTimelineActionState(action));
                        }
                        return null;
                    }
                }
            );
            wrapped = XposedHelpers.callMethod(wrapped, "doOnError", onError);

            Class<?> actionClass = Class.forName("io.reactivex.functions.Action", false, cl);
            Object onComplete = java.lang.reflect.Proxy.newProxyInstance(
                actionClass.getClassLoader(),
                new Class[]{actionClass},
                new java.lang.reflect.InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                        if (isObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnComplete")) {
                            return handleObjectProxyMethod(proxy, method, args, "ReWeiboTimelineOnComplete");
                        }
                        if ("run".equals(method.getName())) {
                            if (!loadNew && !seenNext[0] && isTimelineActionNoMoreCursor(action)) {
                                markTimelineNoMoreContent(source + "-server-no-more-cursor");
                            }
                            log("Timeline v3 observable complete"
                                + " probe=" + probeId
                                + " source=" + source
                                + " loadNew=" + loadNew
                                + " seenNext=" + seenNext[0]
                                + describeTimelineActionState(action));
                        }
                        return null;
                    }
                }
            );
            wrapped = XposedHelpers.callMethod(wrapped, "doOnComplete", onComplete);
            return wrapped;
        } catch (Throwable t) {
            log("Timeline v3 observable probe wrap error"
                + " source=" + source
                + " observable=" + shortClassName(observable)
                + " error=" + describeThrowableForLog(t));
            return observable;
        }
    }

    private static boolean shouldLogTimelineNetworkProbe(Object owner) {
        try {
            String groupId = getTimelineGroupId(owner);
            return "-1".equals(groupId) || hasActiveTimelineGapFill();
        } catch (Throwable ignored) {
            return hasActiveTimelineGapFill();
        }
    }

    private static int nextTimelineNetworkProbeSeq() {
        synchronized (WeiboLiteHook.class) {
            return ++sTimelineNetworkProbeSeq;
        }
    }

    private static boolean isObjectProxyMethod(
        Object proxy,
        java.lang.reflect.Method method,
        Object[] args,
        String name
    ) {
        if (method == null) return false;
        String methodName = method.getName();
        return "toString".equals(methodName)
            || "hashCode".equals(methodName)
            || "equals".equals(methodName);
    }

    private static Object handleObjectProxyMethod(
        Object proxy,
        java.lang.reflect.Method method,
        Object[] args,
        String name
    ) {
        String methodName = method.getName();
        if ("toString".equals(methodName)) return name;
        if ("hashCode".equals(methodName)) return Integer.valueOf(System.identityHashCode(proxy));
        if ("equals".equals(methodName)) return Boolean.valueOf(args != null && args.length > 0 && proxy == args[0]);
        return null;
    }

    private static Throwable getHookThrowableSafe(XC_MethodHook.MethodHookParam param) {
        try {
            return param.getThrowable();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void logTimelineShowDataEvent(Object presenter, Object type, Object data, String source) {
        try {
            String typeText = String.valueOf(type);
            if (typeText.contains("load_more_empty")) {
                recordTimelineGapFillEmptyResponse(source + "-" + typeText);
                if (!hasActiveTimelineGapFill()) {
                    rememberTimelinePresenter(presenter);
                    markTimelineNoMoreContent(source + "-" + typeText);
                }
            } else if (typeText.contains("load_more_error")) {
                recordTimelineGapFillErrorResponse(source + "-" + typeText, null);
            }
            log("Timeline " + source + " event"
                + " type=" + compactLogValue(type)
                + " data=" + describeTimelineObservableValue(data, false)
                + describeTimelineActionState(presenter));
        } catch (Throwable t) {
            log("Timeline showData event probe error: " + t.getMessage());
        }
    }

    private static String describeTimelineObservableValue(Object value, boolean feedResultValue) {
        if (value == null) return "null";
        if (feedResultValue || shortClassName(value).contains("FeedResult")) {
            return describeTimelineFeedResult(value);
        }
        if (value instanceof List) {
            return describeTimelineStatusList((List) value);
        }
        if (value instanceof Throwable) {
            return describeThrowableForLog((Throwable) value);
        }
        return shortClassName(value) + "(" + compactLogValue(value) + ")";
    }

    private static String describeTimelineFeedResult(Object feedResult) {
        if (feedResult == null) return "FeedResult(null)";
        Object items = callMethodSafe(feedResult, "getItems");
        Object allStatus = callMethodSafe(feedResult, "getAllStatus");
        int allStatusCount = objectItemCount(allStatus);
        int validStatusCount = allStatus instanceof List ? countTimelineStatuses((List) allStatus) : allStatusCount;
        Object moreInfo = callMethodSafe(feedResult, "getMoreInfo");
        Object refreshInfo = callMethodSafe(feedResult, "getRefreshInfo");
        Object loadedInfo = callMethodSafe(feedResult, "getLoadedInfo");
        Object pageData = callMethodSafe(feedResult, "getPageData");
        return "FeedResult(items=" + objectItemCount(items)
            + ",allStatus=" + allStatusCount
            + ",validStatus=" + validStatusCount
            + ",more=" + describeTimelineFeedMoreInfo(moreInfo)
            + ",refresh=" + describeTimelineFeedParamsHolder(refreshInfo)
            + ",loaded=" + shortClassName(loadedInfo)
            + ",pageData=" + shortClassName(pageData)
            + ")";
    }

    private static String describeTimelineFeedMoreInfo(Object moreInfo) {
        if (moreInfo == null) return "null";
        Object params = callMethodSafe(moreInfo, "getParams");
        return "{error=" + compactLogValue(getStringMethodOrField(moreInfo, "getError", "error"))
            + ",loading=" + compactLogValue(getStringMethodOrField(moreInfo, "getLoading", "loading"))
            + ",moreType=" + compactLogValue(getStringMethodOrField(moreInfo, "getMoreType", "moreType"))
            + ",pagingType=" + compactLogValue(getStringMethodOrField(moreInfo, "getPagingType", "pagingType"))
            + ",params=" + describeTimelineFeedParams(params)
            + "}";
    }

    private static String describeTimelineFeedParamsHolder(Object holder) {
        if (holder == null) return "null";
        return describeTimelineFeedParams(callMethodSafe(holder, "getParams"));
    }

    private static String describeTimelineFeedParams(Object params) {
        if (params == null) return "null";
        return "{count=" + getIntMethodOrField(params, "getCount", "count", -1)
            + ",maxId=" + getLongMethodOrField(params, "getMaxId", "maxId", -1L)
            + ",lastItemTime=" + getLongMethodOrField(params, "getLastItemTime", "lastItemTime", -1L)
            + ",lastSinceId=" + getLongMethodOrField(params, "getLastSinceId", "lastSinceId", -1L)
            + ",sinceId=" + compactLogValue(getStringMethodOrField(params, "getSinceId", "sinceId"))
            + "}";
    }

    private static String describeTimelineStatusList(List list) {
        if (list == null) return "List(null)";
        Object first = findComparableStatus(list, true);
        Object last = findComparableStatus(list, false);
        return "List(size=" + list.size()
            + ",validStatus=" + countTimelineStatuses(list)
            + ",first=" + getStatusId(first)
            + ",last=" + getStatusId(last)
            + ")";
    }

    private static String describeTimelineActionState(Object action) {
        return " group=" + getTimelineGroupId(action)
            + " lastMaxId=" + getLongMethodOrField(action, null, "lastMaxId", -1L)
            + " lastSinceId=" + getLongMethodOrField(action, null, "lastSinceId", -1L)
            + " lastItemTime=" + getLongMethodOrField(action, null, "lastItemTime", -1L)
            + describeTimelineGapFillProbeState();
    }

    private static String describeTimelineGapFillProbeState() {
        synchronized (sTimelineGapFillState) {
            GapFillState state = sTimelineGapFillState;
            return " gapActive=" + state.active
                + " gapInFlight=" + state.inFlight
                + " gapPages=" + state.requestedPages
                + " gapCursor=" + state.cursorId
                + " gapTarget=" + state.targetId
                + " gapFallback=" + state.fallbackAttempts;
        }
    }

    private static boolean isTimelineEmptyNetworkValue(Object value, boolean feedResultValue) {
        if (value == null) return false;
        if (value instanceof List) return countTimelineStatuses((List) value) <= 0;
        if (feedResultValue || shortClassName(value).contains("FeedResult")) {
            Object items = callMethodSafe(value, "getItems");
            Object allStatus = callMethodSafe(value, "getAllStatus");
            return objectItemCount(items) <= 0 && objectItemCount(allStatus) <= 0;
        }
        return false;
    }

    private static boolean isTimelineServerTerminalFeedResult(Object value, boolean feedResultValue) {
        if (value == null) return false;
        if (!(feedResultValue || shortClassName(value).contains("FeedResult"))) return false;
        Object moreInfo = callMethodSafe(value, "getMoreInfo");
        if (moreInfo != null) return false;
        Object items = callMethodSafe(value, "getItems");
        Object allStatus = callMethodSafe(value, "getAllStatus");
        return objectItemCount(items) > 0 || objectItemCount(allStatus) > 0;
    }

    private static boolean isTimelineActionNoMoreCursor(Object action) {
        if (!"-1".equals(getTimelineGroupId(action))) return false;
        return getLongMethodOrField(action, null, "lastMaxId", -1L) == 0L;
    }

    private static void recordTimelineGapFillEmptyResponse(String source) {
        synchronized (sTimelineGapFillState) {
            GapFillState state = sTimelineGapFillState;
            if (!state.active || !state.inFlight) return;
            long cursor = state.cursorId;
            if (cursor <= 0L || cursor == state.lastEmptyCursorId) return;

            state.emptyResponses++;
            state.lastEmptyCursorId = cursor;
            if (state.minEmptyCursorId <= 0L || cursor < state.minEmptyCursorId) state.minEmptyCursorId = cursor;
            if (state.maxEmptyCursorId <= 0L || cursor > state.maxEmptyCursorId) state.maxEmptyCursorId = cursor;
            if (state.gapCursorId > 0L && cursor >= state.gapCursorId - 1L) {
                state.newSideEmptyProbed = true;
            }
            if (state.targetId > 0L && cursor <= state.targetId + 1L) {
                state.boundaryEmptyProbed = true;
            }
            log("Timeline gap-fill empty proof sample source=" + source
                + " cursor=" + cursor
                + " " + describeTimelineGapFillEmptyProofLocked(state));
        }
    }

    private static void recordTimelineGapFillErrorResponse(String source, Throwable throwable) {
        synchronized (sTimelineGapFillState) {
            GapFillState state = sTimelineGapFillState;
            if (!state.active || !state.inFlight) return;
            long cursor = state.cursorId;
            if (cursor > 0L && cursor == state.lastErrorCursorId) return;
            state.errorResponses++;
            state.lastErrorCursorId = cursor;
            log("Timeline gap-fill server proof rejected source=" + source
                + " cursor=" + cursor
                + " error=" + describeThrowableForLog(throwable)
                + " " + describeTimelineGapFillEmptyProofLocked(state));
        }
    }

    private static boolean hasTimelineGapFillServerEmptyProofLocked(GapFillState state) {
        return state != null
            && state.emptyResponses >= TIMELINE_GAP_FILL_EMPTY_PROOF_MIN
            && state.errorResponses == 0
            && state.newSideEmptyProbed
            && state.boundaryEmptyProbed;
    }

    private static String describeTimelineGapFillEmptyProofLocked(GapFillState state) {
        if (state == null) return "proof=null";
        return "proof{empty=" + state.emptyResponses
            + ",errors=" + state.errorResponses
            + ",newSide=" + state.newSideEmptyProbed
            + ",boundary=" + state.boundaryEmptyProbed
            + ",minCursor=" + state.minEmptyCursorId
            + ",maxCursor=" + state.maxEmptyCursorId
            + ",target=" + state.targetId
            + ",gapCursor=" + state.gapCursorId
            + "}";
    }

    private static void resetTimelineGapFillEmptyProofLocked(GapFillState state) {
        state.emptyResponses = 0;
        state.errorResponses = 0;
        state.lastEmptyCursorId = 0L;
        state.lastErrorCursorId = 0L;
        state.minEmptyCursorId = 0L;
        state.maxEmptyCursorId = 0L;
        state.newSideEmptyProbed = false;
        state.boundaryEmptyProbed = false;
    }

    private static String describeThrowableForLog(Throwable throwable) {
        if (throwable == null) return "null";
        StringBuilder builder = new StringBuilder();
        builder.append(shortClassName(throwable));
        Object code = callMethodSafe(throwable, "code");
        if (code == null) {
            try {
                code = getFieldValue(throwable, "errorCode");
            } catch (Throwable ignored) {}
        }
        if (code != null) {
            builder.append("{code=").append(compactLogValue(code)).append("}");
        }
        builder.append(":").append(compactLogValue(throwable.getMessage()));
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            builder.append(" cause=").append(shortClassName(cause))
                .append(":").append(compactLogValue(cause.getMessage()));
        }
        return builder.toString();
    }

    private static String shortClassName(Object value) {
        if (value == null) return "null";
        String name = value instanceof Class ? ((Class<?>) value).getName() : value.getClass().getName();
        int index = name.lastIndexOf('.');
        return index >= 0 ? name.substring(index + 1) : name;
    }

    private static String compactLogValue(Object value) {
        if (value == null) return "null";
        String text = String.valueOf(value).replace('\n', ' ').replace('\r', ' ').replace('\t', ' ').trim();
        if (text.length() > 160) {
            return text.substring(0, 157) + "...";
        }
        return text;
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
                if ("-1".equals(getTimelineGroupId(presenter)) && !source.contains("presenter-addData")) {
                    mergeTimelineCumulativeStatusesIncremental(sorted, presenter, source + "-normalize");
                }
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
            if (shouldLogTimelineNetworkProbe(presenter)) {
                logTimelineShowDataEvent(presenter, type, data, "showData");
            }
            if (shouldSkipShowDataNormalizeForGapFill()) return;
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

    private static boolean shouldSkipShowDataNormalizeForGapFill() {
        return hasActiveTimelineGapFill()
            || SystemClock.elapsedRealtime() < sTimelineGapFillSuppressShowDataUntilMs;
    }

    private static void persistTimelineNativeCache(Object presenter, String source) {
        try {
            if (sTimelineCacheClearInFlight) {
                log("Timeline full cache persist skipped during manual clear source=" + source);
                return;
            }
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            if (isTimelineCacheRestoreInFlight(presenter)) {
                log("Timeline full cache persist deferred source=" + source + " restore-in-flight");
                return;
            }
            rememberTimelinePresenter(presenter);

            Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
            if (!(statusList instanceof List)) return;

            mergeTimelineCumulativeStatusesIncremental((List) statusList, presenter, source + "-incremental");
            ArrayList statuses;
            synchronized (sTimelineCumulativeStatusesById) {
                statuses = new ArrayList(sTimelineCumulativeStatusesById.values());
            }
            if ((source.contains("presenter-addData") || source.contains("presenter-distinct"))
                && !shouldCheckpointTimelineCache(presenter, statuses)) {
                return;
            }
            statuses = new ArrayList(sortTimelineNewestFirst(statuses, presenter, source + "-checkpoint", false));
            persistTimelineNativeCacheList(presenter, statuses, source, false);
        } catch (Throwable t) {
            log("Timeline full cache persist error source=" + source + ": " + t.getMessage());
        }
    }

    private static boolean persistTimelineNativeCacheList(Object presenter, List list, String source, boolean force) {
        try {
            if (sTimelineCacheClearInFlight) {
                log("Timeline full cache list persist skipped during manual clear source=" + source);
                return false;
            }
            if (presenter == null || list == null || !"-1".equals(getTimelineGroupId(presenter))) return false;
            if (!force && isTimelineCacheRestoreInFlight(presenter)) {
                log("Timeline full cache persist deferred source=" + source + " restore-in-flight");
                return false;
            }
            rememberTimelinePresenter(presenter);

            ArrayList statuses = collectTimelineStatuses(list);
            List filtered = filterTimelineContentless(statuses, presenter, source);
            if (filtered != statuses) {
                statuses = new ArrayList(filtered);
            }
            List trimmed = trimTimelineStatusesToCacheDays(statuses, presenter, source);
            if (trimmed != statuses) {
                statuses = new ArrayList(trimmed);
            }
            int count = statuses.size();
            if (count < TIMELINE_CACHE_MIN_ITEMS) return false;
            long newestId = getNewestTimelineStatusId(statuses);

            if (!force) {
                synchronized (sPersistedTimelineCacheCounts) {
                    Integer lastCount = sPersistedTimelineCacheCounts.get(presenter);
                    Long lastNewestId = sPersistedTimelineCacheNewestIds.get(presenter);
                    if (lastCount != null && lastCount.intValue() >= count
                        && lastNewestId != null && lastNewestId.longValue() >= newestId) {
                        return false;
                    }
                }
            }

            Object action = getTimelineAction(presenter);
            if (action == null) {
                log("Timeline native cache persist skipped source=" + source + " count=" + count + " no action");
                return false;
            }

            String maxId = getTimelineCacheMaxId(action, statuses);
            syncTimelineActionMaxId(action, maxId, source);
            Object result = newTimelineStatusResult(action, statuses, maxId);
            Object builder = getTimelineCacheBuilder(action);
            enqueueTimelineNativeCachePersist(new TimelineNativePersistRequest(
                result,
                getTimelineCacheGson(builder),
                getTimelineNativeCacheFile(builder),
                presenter,
                count,
                newestId,
                maxId,
                source,
                force,
                buildTimelineCacheStats(statuses)
            ));
            log("Timeline full cache queued source=" + source + " count=" + count
                + " maxId=" + maxId + " force=" + force);
            return true;
        } catch (Throwable t) {
            log("Timeline full cache persist error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static void persistTimelineShadowCache(String source, int count, String maxId, boolean force) {
        persistTimelineShadowCache(
            source,
            count,
            maxId,
            force,
            buildBestTimelineCacheStats(sLastTimelinePresenter)
        );
    }

    private static void persistTimelineShadowCache(
        String source,
        int count,
        String maxId,
        boolean force,
        TimelineCacheStats candidateStats
    ) {
        if (sTimelineCacheClearInFlight) {
            log("Timeline shadow cache persist skipped during manual clear source=" + source);
            return;
        }
        enqueueTimelineShadowCachePersist(new TimelineShadowPersistRequest(
            source,
            count,
            maxId,
            force,
            candidateStats
        ));
    }

    private static void persistTimelineShadowCacheNow(
        String source,
        int count,
        String maxId,
        boolean force,
        TimelineCacheStats candidateStats,
        File expectedNativeFile
    ) {
        try {
            if (!force) {
                if (!shouldRememberPreloadDone(source, 0, count)) return;
            }

            int previousCount = Math.max(sTimelineShadowCacheCount, readTimelineShadowCacheCount());
            int cacheDays = getTimelineCacheDaysSetting();
            if (previousCount >= TIMELINE_CACHE_MIN_ITEMS && !sTimelineCacheDaysSettingConfirmed) {
                log("Timeline shadow cache persist skipped until setting is confirmed source=" + source
                    + " count=" + count + " previous=" + previousCount);
                return;
            }
            long previousOldestMs = readTimelineShadowCacheOldestMs();
            long previousNewestMs = readTimelineShadowCacheNewestMs();
            long candidateOldestMs = candidateStats == null ? 0L : candidateStats.oldestMs;
            long candidateNewestMs = candidateStats == null ? 0L : candidateStats.newestMs;
            if (!shouldReplaceTimelineShadowCache(
                previousCount,
                previousOldestMs,
                previousNewestMs,
                count,
                candidateOldestMs,
                candidateNewestMs,
                timelineCacheCutoffMs(System.currentTimeMillis(), cacheDays)
            )) {
                log("Timeline shadow cache persist skipped older source=" + source
                    + " count=" + count + " previous=" + previousCount
                    + " oldestMs=" + candidateOldestMs
                    + " previousOldestMs=" + previousOldestMs
                    + " newestMs=" + candidateNewestMs
                    + " previousNewestMs=" + previousNewestMs
                    + " force=" + force);
                return;
            }

            File nativeFile = expectedNativeFile;
            if (nativeFile == null || !nativeFile.exists()) {
                nativeFile = findTimelineNativeCacheFile();
            }
            if (nativeFile == null || !nativeFile.exists()) {
                log("Timeline shadow cache persist skipped source=" + source + " count=" + count + " no native file");
                return;
            }

            File shadow = getTimelineShadowCacheFile();
            long previousSize = readTimelineShadowCacheMetaLong("size");
            if (previousSize <= 0L && shadow.exists()) previousSize = shadow.length();
            if (!isTimelineShadowPayloadPlausible(
                previousCount,
                previousSize,
                count,
                nativeFile.length()
            )) {
                log("Timeline shadow cache persist skipped incomplete source=" + source
                    + " count=" + count
                    + " nativeSize=" + nativeFile.length()
                    + " previousCount=" + previousCount
                    + " previousSize=" + previousSize);
                return;
            }
            File parent = shadow.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            copyFile(nativeFile, shadow);
            writeTimelineShadowCacheMeta(
                nativeFile.getName(),
                source,
                count,
                maxId,
                shadow.length(),
                candidateStats
            );
            sTimelineShadowCacheCount = count;
            log("Timeline shadow cache persisted source=" + source + " count=" + count
                + " newestMs=" + candidateNewestMs
                + " file=" + nativeFile.getName() + " size=" + shadow.length());
        } catch (Throwable t) {
            log("Timeline shadow cache persist error source=" + source + ": " + t.getMessage());
        }
    }

    private static void enqueueTimelineNativeCachePersist(TimelineNativePersistRequest request) {
        if (sTimelineCacheClearInFlight) {
            log("Timeline native cache enqueue skipped during manual clear source=" + request.source);
            return;
        }
        synchronized (sTimelinePersistQueueLock) {
            if (sPendingTimelineNativePersist != null) {
                log("Timeline native cache write coalesced old=" + sPendingTimelineNativePersist.source
                    + " new=" + request.source
                    + " oldCount=" + sPendingTimelineNativePersist.count
                    + " newCount=" + request.count);
            }
            sPendingTimelineNativePersist = request;
            sPendingTimelineShadowPersist = null;
            scheduleTimelinePersistWorkerLocked();
        }
    }

    private static void enqueueTimelineShadowCachePersist(TimelineShadowPersistRequest request) {
        if (sTimelineCacheClearInFlight) {
            log("Timeline shadow cache enqueue skipped during manual clear source=" + request.source);
            return;
        }
        synchronized (sTimelinePersistQueueLock) {
            sPendingTimelineShadowPersist = request;
            scheduleTimelinePersistWorkerLocked();
        }
    }

    private static void scheduleTimelinePersistWorkerLocked() {
        if (sTimelinePersistWorkerScheduled) return;
        sTimelinePersistWorkerScheduled = true;
        sTimelinePersistExecutor.execute(new Runnable() {
            @Override
            public void run() {
                drainTimelinePersistQueue();
            }
        });
    }

    private static void drainTimelinePersistQueue() {
        while (true) {
            TimelineNativePersistRequest nativeRequest;
            TimelineShadowPersistRequest shadowRequest;
            synchronized (sTimelinePersistQueueLock) {
                nativeRequest = sPendingTimelineNativePersist;
                if (nativeRequest != null) {
                    sPendingTimelineNativePersist = null;
                    shadowRequest = null;
                } else {
                    shadowRequest = sPendingTimelineShadowPersist;
                    sPendingTimelineShadowPersist = null;
                }
                if (nativeRequest == null && shadowRequest == null) {
                    sTimelinePersistWorkerScheduled = false;
                    return;
                }
            }

            if (nativeRequest != null) {
                persistTimelineNativeCacheNow(nativeRequest);
            } else {
                persistTimelineShadowCacheNow(
                    shadowRequest.source,
                    shadowRequest.count,
                    shadowRequest.maxId,
                    shadowRequest.force,
                    shadowRequest.stats,
                    null
                );
            }
        }
    }

    private static void persistTimelineNativeCacheNow(TimelineNativePersistRequest request) {
        try {
            if (request.presenter != null
                && shouldDeferTimelinePersist(
                    request.force,
                    isTimelineCacheRestoreInFlight(request.presenter)
                )) {
                log("Timeline queued cache persist dropped source=" + request.source
                    + " restore-in-flight");
                return;
            }
            writeTimelineNativeCacheStreaming(request.result, request.gson, request.nativeFile);
            boolean newerWritePending;
            synchronized (sTimelinePersistQueueLock) {
                newerWritePending = sPendingTimelineNativePersist != null;
            }
            if (newerWritePending) {
                log("Timeline shadow cache write coalesced source=" + request.source
                    + " count=" + request.count);
            } else {
                persistTimelineShadowCacheNow(
                    request.source,
                    request.count,
                    request.maxId,
                    request.force,
                    request.stats,
                    request.nativeFile
                );
            }
            if (request.presenter != null) {
                synchronized (sPersistedTimelineCacheCounts) {
                    sPersistedTimelineCacheCounts.put(request.presenter, Integer.valueOf(request.count));
                    sPersistedTimelineCacheNewestIds.put(request.presenter, Long.valueOf(request.newestId));
                }
            }
            log("Timeline full cache persisted source=" + request.source
                + " count=" + request.count
                + " maxId=" + request.maxId
                + " force=" + request.force
                + " streaming=true");
        } catch (Throwable t) {
            log("Timeline full cache persist error source=" + request.source + ": " + t.getMessage());
        }
    }

    private static int readTimelineShadowCacheCount() {
        return (int) readTimelineShadowCacheMetaLong("count");
    }

    private static long readTimelineShadowCacheOldestMs() {
        return readTimelineShadowCacheMetaLong("oldest_ms");
    }

    private static long readTimelineShadowCacheNewestMs() {
        return readTimelineShadowCacheMetaLong("newest_ms");
    }

    private static long readTimelineShadowCacheMetaLong(String key) {
        BufferedReader reader = null;
        try {
            File file = getTimelineShadowCacheMetaFile();
            if (file == null || !file.exists()) return 0;
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                String prefix = key + "=";
                if (line.startsWith(prefix)) {
                    return Long.parseLong(line.substring(prefix.length()).trim());
                }
            }
        } catch (Throwable ignored) {
        } finally {
            closeQuietly(reader);
        }
        return 0;
    }

    static boolean shouldReplaceTimelineShadowCache(
        int existingCount,
        long existingOldestMs,
        long existingNewestMs,
        int candidateCount,
        long candidateOldestMs,
        long candidateNewestMs,
        long cutoffMs
    ) {
        if (candidateCount < TIMELINE_CACHE_MIN_ITEMS) return false;
        if (existingCount < TIMELINE_CACHE_MIN_ITEMS) return true;
        if (candidateNewestMs <= 0L && existingNewestMs > 0L) return false;
        if (existingNewestMs > candidateNewestMs + TIMELINE_CACHE_RECENCY_TOLERANCE_MS) return false;

        boolean existingTailExpired = existingOldestMs > 0L && existingOldestMs < cutoffMs;
        if (existingTailExpired) {
            if (candidateOldestMs <= 0L
                || candidateOldestMs > cutoffMs + TIMELINE_CACHE_DAY_TOLERANCE_MS) {
                return false;
            }
        } else if (existingOldestMs > 0L) {
            if (candidateOldestMs <= 0L) return false;
            if (candidateOldestMs > existingOldestMs + TIMELINE_CACHE_RECENCY_TOLERANCE_MS) return false;
            if (candidateCount + TIMELINE_CACHE_COUNT_TOLERANCE < existingCount) return false;
        } else if (existingOldestMs <= 0L
            && candidateCount + TIMELINE_CACHE_COUNT_TOLERANCE < existingCount) {
            return false;
        }
        return true;
    }

    static boolean isTimelineShadowPayloadPlausible(
        int existingCount,
        long existingSize,
        int candidateCount,
        long candidateSize
    ) {
        if (candidateSize <= 0L || candidateCount < TIMELINE_CACHE_MIN_ITEMS) return false;
        long candidateBytesPerItem = candidateSize / Math.max(1, candidateCount);
        if (candidateCount >= 100 && candidateBytesPerItem < 256L) return false;
        if (existingCount < TIMELINE_CACHE_MIN_ITEMS || existingSize <= 0L) return true;
        long existingBytesPerItem = existingSize / Math.max(1, existingCount);
        return candidateBytesPerItem * 4L >= existingBytesPerItem;
    }

    static boolean shouldUseTimelineLastRead(boolean restoredCacheMode, boolean preloadDone) {
        return true;
    }

    private static Object restoreTimelineShadowCache(Object cache, Object builder, String source) {
        File backup = null;
        try {
            File shadow = getTimelineShadowCacheFile();
            if (shadow == null || !shadow.exists() || shadow.length() < 1024L) return null;

            String fileName = readTimelineShadowCacheFileName();
            if (!hasMeaningfulString(fileName)) return null;

            File dir = getTimelineDataCacheDir();
            if (!dir.exists()) dir.mkdirs();
            File nativeFile = new File(dir, fileName);
            if (nativeFile.exists()) {
                backup = new File(dir, fileName + TIMELINE_NATIVE_CACHE_BACKUP_SUFFIX);
                copyFile(nativeFile, backup);
            }
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
        } finally {
            if (backup != null && backup.exists()) {
                String name = backup.getName();
                String nativeName = name.substring(0, name.length() - TIMELINE_NATIVE_CACHE_BACKUP_SUFFIX.length());
                boolean restored = false;
                try {
                    copyFile(backup, new File(backup.getParentFile(), nativeName));
                    restored = true;
                } catch (Throwable restoreError) {
                    log("Timeline native cache backup restore error source=" + source
                        + ": " + restoreError.getMessage());
                }
                if (restored && !backup.delete()) {
                    log("Timeline native cache backup cleanup deferred source=" + source);
                }
            }
        }
        return null;
    }

    private static void restoreInterruptedTimelineNativeCacheBackups() {
        File dir = getTimelineDataCacheDir();
        File[] files = dir.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            File backup = files[i];
            if (backup == null || !backup.isFile()
                || !backup.getName().endsWith(TIMELINE_NATIVE_CACHE_BACKUP_SUFFIX)) {
                continue;
            }
            String name = backup.getName();
            String nativeName = name.substring(0, name.length() - TIMELINE_NATIVE_CACHE_BACKUP_SUFFIX.length());
            try {
                copyFile(backup, new File(dir, nativeName));
                if (!backup.delete()) {
                    log("Timeline interrupted native backup cleanup deferred file=" + name);
                }
                log("Timeline interrupted native cache backup restored file=" + nativeName);
            } catch (Throwable t) {
                log("Timeline interrupted native cache backup restore error file=" + name
                    + ": " + t.getMessage());
            }
        }
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
            if (name.endsWith(TIMELINE_NATIVE_CACHE_BACKUP_SUFFIX)
                || name.endsWith(".bak") || name.endsWith(".new")) continue;
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

    private static void writeTimelineShadowCacheMeta(
        String fileName,
        String source,
        int count,
        String maxId,
        long size,
        TimelineCacheStats stats
    ) {
        try {
            File file = getTimelineShadowCacheMetaFile();
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            StringBuilder content = new StringBuilder();
            content.append("saved_at=").append(ts).append('\n');
            content.append("source=").append(source).append('\n');
            content.append("file_name=").append(fileName).append('\n');
            content.append("count=").append(count).append('\n');
            content.append("size=").append(size).append('\n');
            if (stats != null) {
                content.append("oldest_ms=").append(stats.oldestMs).append('\n');
                content.append("newest_ms=").append(stats.newestMs).append('\n');
            }
            if (maxId != null) content.append("max_id=").append(maxId).append('\n');
            writeTextFileAtomically(file, content.toString());
        } catch (Throwable t) {
            log("Timeline shadow cache meta write error source=" + source + ": " + t.getMessage());
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
        synchronized (sTimelineFileLock) {
            FileInputStream input = null;
            FileOutputStream output = null;
            AtomicFile target = new AtomicFile(to);
            try {
                input = new FileInputStream(from);
                output = target.startWrite();
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) > 0) {
                    output.write(buffer, 0, read);
                }
                output.flush();
                target.finishWrite(output);
                output = null;
            } catch (Throwable t) {
                if (output != null) {
                    target.failWrite(output);
                    output = null;
                }
                if (t instanceof Exception) throw (Exception) t;
                throw new Exception(t);
            } finally {
                closeQuietly(input);
                closeQuietly(output);
            }
        }
    }

    private static void writeTextFileAtomically(File file, String content) throws Exception {
        synchronized (sTimelineFileLock) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            AtomicFile target = new AtomicFile(file);
            FileOutputStream output = null;
            OutputStreamWriter writer = null;
            try {
                output = target.startWrite();
                writer = new OutputStreamWriter(output, "UTF-8");
                writer.write(content == null ? "" : content);
                writer.flush();
                target.finishWrite(output);
                output = null;
                writer = null;
            } catch (Throwable t) {
                if (output != null) {
                    target.failWrite(output);
                    output = null;
                }
                if (t instanceof Exception) throw (Exception) t;
                throw new Exception(t);
            } finally {
                closeQuietly(writer);
                closeQuietly(output);
            }
        }
    }

    private static void writeTimelineNativeCacheStreaming(Object result, Object gson, File file) throws Exception {
        synchronized (sTimelineFileLock) {
            if (result == null || gson == null || file == null) {
                throw new IllegalStateException("missing timeline cache result, gson, or exact file");
            }
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IllegalStateException("cannot create timeline cache directory");
            }

            File temporary = new File(file.getAbsolutePath() + ".reweibo-new");
            if (temporary.exists() && !temporary.delete()) {
                throw new IllegalStateException("cannot clear stale timeline cache temporary file");
            }
            FileOutputStream output = null;
            BufferedWriter writer = null;
            try {
                output = new FileOutputStream(temporary, false);
                writer = new BufferedWriter(new OutputStreamWriter(output, "UTF-8"), 8192);
                Class<?> gsonClass = gson.getClass();
                Method toJson = gsonClass.getMethod("toJson", Object.class, Appendable.class);
                toJson.invoke(gson, result, writer);
                writer.flush();
                output.getFD().sync();
                writer.close();
                output = null;
                writer = null;
                Os.rename(temporary.getAbsolutePath(), file.getAbsolutePath());
            } catch (Throwable t) {
                closeQuietly(writer);
                writer = null;
                closeQuietly(output);
                output = null;
                if (temporary.exists()) temporary.delete();
                Throwable cause = t.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                if (t instanceof Exception) throw (Exception) t;
                throw new Exception(t);
            } finally {
                closeQuietly(writer);
                closeQuietly(output);
                if (temporary.exists()) temporary.delete();
            }
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
            sTimelinePreloadDoneCacheDays = -1;
            sTimelineRestoredCacheMode = false;
            log("Timeline preload marker cleared source=" + source);
        } catch (Throwable t) {
            sTimelinePreloadDone = Boolean.FALSE;
            sTimelinePreloadDoneCacheDays = -1;
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

    private static boolean restoreTimelineNativeCache(Object presenter, List incomingData, String source) {
        return scheduleTimelineCacheRestore(presenter, incomingData, source, false);
    }

    private static boolean scheduleTimelineCacheRestore(
        final Object presenter,
        List incomingData,
        final String source,
        boolean preferCumulative
    ) {
        try {
            if (sTimelineCacheClearInFlight) {
                log("Timeline cache restore skipped during manual clear source=" + source);
                return false;
            }
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return false;

            ArrayList liveStatuses = snapshotTimelineStatuses(presenter, incomingData);
            ArrayList cumulativeStatuses = null;
            final boolean cumulative;
            final int generation;
            synchronized (sTimelineRestoreLock) {
                TimelineRestoreState state = sTimelineRestoreStates.get(presenter);
                if (state == null) {
                    state = new TimelineRestoreState();
                    sTimelineRestoreStates.put(presenter, state);
                }
                if (state.inFlight) {
                    state.pendingLiveStatuses = liveStatuses;
                    state.pendingSource = source;
                    log("Timeline cache restore coalesced source=" + source
                        + " live=" + liveStatuses.size()
                        + " generation=" + state.generation);
                    return true;
                }
                cumulative = preferCumulative || state.diskRestoreResolved;
                if (cumulative) {
                    synchronized (sTimelineCumulativeStatusesById) {
                        cumulativeStatuses = new ArrayList(sTimelineCumulativeStatusesById.values());
                    }
                    if (cumulativeStatuses.size() < TIMELINE_CACHE_MIN_ITEMS) {
                        return false;
                    }
                }
                state.inFlight = true;
                state.generation++;
                state.pendingLiveStatuses = null;
                state.pendingSource = null;
                generation = state.generation;
            }

            final TimelineRestoreRequest request = new TimelineRestoreRequest(
                presenter,
                liveStatuses,
                cumulativeStatuses,
                source,
                getTimelineCacheDaysSetting(),
                sTimelineCacheDaysSettingConfirmed,
                cumulative,
                generation
            );
            log("Timeline cache restore queued source=" + source
                + " kind=" + (cumulative ? "cumulative" : "disk")
                + " live=" + liveStatuses.size()
                + " cached=" + (cumulativeStatuses == null ? -1 : cumulativeStatuses.size())
                + " generation=" + generation);
            sTimelineRestoreExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    executeTimelineCacheRestore(request);
                }
            });
            return true;
        } catch (Throwable t) {
            log("Timeline cache restore schedule error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static ArrayList snapshotTimelineStatuses(Object presenter, List incomingData) {
        try {
            Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
            if (statusList instanceof List) return new ArrayList((List) statusList);
        } catch (Throwable ignored) {
        }
        if (incomingData != null) return new ArrayList(incomingData);
        return new ArrayList();
    }

    private static void executeTimelineCacheRestore(final TimelineRestoreRequest request) {
        TimelineRestoreResult result = null;
        Throwable error = null;
        TimelineRestoreRequest effectiveRequest = request;
        long startedAt = SystemClock.elapsedRealtime();
        try {
            Object action = getTimelineAction(request.presenter);
            if (action == null) throw new IllegalStateException("timeline action is missing");
            Object builder = getTimelineCacheBuilder(action);
            TimelineCacheLoad load = request.cumulative
                ? new TimelineCacheLoad(request.cumulativeStatuses, "cumulative")
                : loadTimelineCacheSingleSource(action, builder, request.source);
            if (load == null || load.statuses == null || load.statuses.isEmpty()) {
                throw new IllegalStateException("timeline cache is missing");
            }
            long loadedAt = SystemClock.elapsedRealtime();
            effectiveRequest = consumePendingTimelineRestoreRequest(request);
            result = prepareTimelineRestore(effectiveRequest, action, builder, load);
            TimelineRestoreRequest latestRequest = consumePendingTimelineRestoreRequest(effectiveRequest);
            if (latestRequest != effectiveRequest) {
                effectiveRequest = latestRequest;
                result = prepareTimelineRestore(effectiveRequest, action, builder, load);
            }
            long preparedAt = SystemClock.elapsedRealtime();
            log("Timeline cache restore prepared source=" + effectiveRequest.source
                + " cacheSource=" + load.source
                + " count=" + result.statuses.size()
                + " loadMs=" + (loadedAt - startedAt)
                + " prepareMs=" + (preparedAt - loadedAt)
                + " totalMs=" + (preparedAt - startedAt));
        } catch (Throwable t) {
            error = t;
        }

        final TimelineRestoreRequest completedRequest = effectiveRequest;
        final TimelineRestoreResult prepared = result;
        final Throwable failure = error;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                applyTimelineCacheRestore(completedRequest, prepared, failure);
            }
        });
    }

    private static TimelineRestoreRequest consumePendingTimelineRestoreRequest(
        TimelineRestoreRequest current
    ) {
        synchronized (sTimelineRestoreLock) {
            TimelineRestoreState state = sTimelineRestoreStates.get(current.presenter);
            if (state == null || !state.inFlight || state.generation != current.generation
                || state.pendingLiveStatuses == null) {
                return current;
            }
            ArrayList pending = state.pendingLiveStatuses;
            String pendingSource = state.pendingSource;
            state.pendingLiveStatuses = null;
            state.pendingSource = null;
            log("Timeline cache restore adopted coalesced input source=" + pendingSource
                + " live=" + pending.size()
                + " generation=" + current.generation);
            return new TimelineRestoreRequest(
                current.presenter,
                pending,
                current.cumulativeStatuses,
                pendingSource == null ? current.source : pendingSource,
                current.cacheDays,
                current.settingConfirmed,
                current.cumulative,
                current.generation
            );
        }
    }

    private static TimelineCacheLoad loadTimelineCacheSingleSource(
        Object action,
        Object builder,
        String source
    ) throws Exception {
        File nativeFile = getTimelineNativeCacheFile(builder);
        File shadowFile = getTimelineShadowCacheFile();
        int shadowCount = readTimelineShadowCacheCount();
        long nativeSize = nativeFile != null && nativeFile.exists() ? nativeFile.length() : 0L;
        long shadowSize = shadowFile != null && shadowFile.exists() ? shadowFile.length() : 0L;
        boolean shadowFirst = shouldReadTimelineShadowFirst(nativeSize, shadowSize, shadowCount);

        if (shadowFirst) {
            List shadowStatuses = readTimelineShadowStatusesDirect(builder, action, source + "-shadow-first");
            if (shadowStatuses != null && !shadowStatuses.isEmpty()) {
                return new TimelineCacheLoad(shadowStatuses, "shadow");
            }
        }

        Class<?> diskCacheClass = Class.forName(
            "com.weico.diskcache.DiskCache",
            false,
            action.getClass().getClassLoader()
        );
        Object cache = callStaticNoArg(diskCacheClass, "getInstance");
        restoreInterruptedTimelineNativeCacheBackups();
        Object nativeCached = XposedHelpers.callMethod(cache, "get", builder);
        List nativeStatuses = extractTimelineStatuses(nativeCached);
        if (nativeStatuses != null && !nativeStatuses.isEmpty()) {
            if (shadowCount >= 100 && nativeStatuses.size() * 4L < shadowCount) {
                List shadowStatuses = readTimelineShadowStatusesDirect(
                    builder,
                    action,
                    source + "-native-fragment"
                );
                if (shadowStatuses != null && !shadowStatuses.isEmpty()) {
                    return new TimelineCacheLoad(shadowStatuses, "shadow");
                }
            }
            return new TimelineCacheLoad(nativeStatuses, "native");
        }

        if (!shadowFirst) {
            List shadowStatuses = readTimelineShadowStatusesDirect(builder, action, source + "-native-miss");
            if (shadowStatuses != null && !shadowStatuses.isEmpty()) {
                return new TimelineCacheLoad(shadowStatuses, "shadow");
            }
        }
        return null;
    }

    static boolean shouldReadTimelineShadowFirst(long nativeSize, long shadowSize, int shadowCount) {
        if (shadowCount < TIMELINE_CACHE_MIN_ITEMS || shadowSize < 1024L) return false;
        if (nativeSize < 1024L) return true;
        return nativeSize < shadowSize / 4L;
    }

    private static List readTimelineShadowStatusesDirect(
        Object builder,
        Object action,
        String source
    ) {
        Reader reader = null;
        try {
            File shadow = getTimelineShadowCacheFile();
            if (shadow == null || !shadow.exists() || shadow.length() < 1024L) return null;
            Object gson = getTimelineCacheGson(builder);
            Class<?> resultClass = Class.forName(
                "com.weico.international.model.sina.StatusResult",
                false,
                action.getClass().getClassLoader()
            );
            reader = new BufferedReader(new FileReader(shadow), 8192);
            Method fromJson = gson.getClass().getMethod("fromJson", Reader.class, Class.class);
            Object cached = fromJson.invoke(gson, reader, resultClass);
            List statuses = extractTimelineStatuses(cached);
            if (statuses != null) {
                log("Timeline shadow cache read directly source=" + source
                    + " count=" + statuses.size() + " size=" + shadow.length());
            }
            return statuses;
        } catch (Throwable t) {
            log("Timeline shadow cache direct read error source=" + source + ": " + t.getMessage());
            return null;
        } finally {
            closeQuietly(reader);
        }
    }

    private static List extractTimelineStatuses(Object cached) {
        if (cached == null) return null;
        try {
            Object statuses = XposedHelpers.callMethod(cached, "getStatuses");
            return statuses instanceof List ? (List) statuses : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static TimelineRestoreResult prepareTimelineRestore(
        TimelineRestoreRequest request,
        Object action,
        Object builder,
        TimelineCacheLoad load
    ) {
        final long cutoffMs = request.settingConfirmed
            ? timelineCacheCutoffMs(System.currentTimeMillis(), request.cacheDays)
            : Long.MIN_VALUE;
        LinkedHashMap<Long, TimelinePreparedStatus> merged = new LinkedHashMap<>();
        appendTimelinePreparedStatuses(merged, request.liveStatuses, cutoffMs);
        appendTimelinePreparedStatuses(merged, load.statuses, cutoffMs);

        ArrayList<TimelinePreparedStatus> records = new ArrayList<>(merged.values());
        Collections.sort(records, new Comparator<TimelinePreparedStatus>() {
            @Override
            public int compare(TimelinePreparedStatus left, TimelinePreparedStatus right) {
                if (left.id == right.id) return 0;
                return left.id > right.id ? -1 : 1;
            }
        });

        ArrayList statuses = new ArrayList(records.size());
        LinkedHashMap<Long, Object> statusesById = new LinkedHashMap<>();
        TimelineCacheStats stats = new TimelineCacheStats();
        TimelineGapScan gapScan = new TimelineGapScan();
        long previousId = 0L;
        for (int i = 0; i < records.size(); i++) {
            TimelinePreparedStatus record = records.get(i);
            statuses.add(record.status);
            statusesById.put(Long.valueOf(record.id), record.status);
            stats.count++;
            if (record.createdMs > 0L) {
                stats.datedCount++;
                if (stats.newestMs <= 0L || record.createdMs > stats.newestMs) stats.newestMs = record.createdMs;
                if (stats.oldestMs <= 0L || record.createdMs < stats.oldestMs) stats.oldestMs = record.createdMs;
            }
            if (gapScan.gap == null && previousId > record.id) {
                long distance = previousId - record.id;
                if (distance >= TIMELINE_GAP_ID_THRESHOLD) {
                    TimelineGap gap = new TimelineGap();
                    gap.cursorId = previousId;
                    gap.targetId = record.id;
                    gap.distance = distance;
                    gap.index = i;
                    gapScan.gap = gap;
                }
            }
            previousId = record.id;
            if ((i & 0xff) == 0xff) Thread.yield();
        }
        gapScan.count = statuses.size();
        hydrateTimelineStatusText(statuses, "reweibo-cache-background");
        long newestId = records.isEmpty() ? 0L : records.get(0).id;
        String maxId = records.isEmpty() ? "0" : String.valueOf(records.get(records.size() - 1).id);
        return new TimelineRestoreResult(
            action,
            builder,
            statuses,
            statusesById,
            stats,
            gapScan,
            newestId,
            maxId,
            load.source
        );
    }

    private static void appendTimelinePreparedStatuses(
        LinkedHashMap<Long, TimelinePreparedStatus> target,
        List source,
        long cutoffMs
    ) {
        if (source == null) return;
        for (int i = 0; i < source.size(); i++) {
            Object status = unwrapStatus(source.get(i));
            if (status == null) continue;
            long id = getStatusId(status);
            if (id <= 0L || isLoadMoreStatus(status) || isTimelineAdStatus(status)) continue;
            if (!hasTimelineRenderableContent(status)) continue;
            Long key = Long.valueOf(id);
            if (target.containsKey(key)) continue;
            long createdMs = getStatusCreatedAtMillis(status);
            if (cutoffMs != Long.MIN_VALUE && createdMs > 0L && createdMs < cutoffMs) continue;
            target.put(key, new TimelinePreparedStatus(status, id, createdMs));
            if ((i & 0xff) == 0xff) Thread.yield();
        }
    }

    private static void applyTimelineCacheRestore(
        TimelineRestoreRequest request,
        TimelineRestoreResult result,
        Throwable failure
    ) {
        if (sTimelineCacheClearInFlight) return;
        ArrayList lateLiveStatuses = null;
        String lateSource = null;
        synchronized (sTimelineRestoreLock) {
            TimelineRestoreState state = sTimelineRestoreStates.get(request.presenter);
            if (state == null || !state.inFlight || state.generation != request.generation) return;
            state.inFlight = false;
            if (!request.cumulative) state.diskRestoreResolved = true;
            lateLiveStatuses = state.pendingLiveStatuses;
            lateSource = state.pendingSource;
            state.pendingLiveStatuses = null;
            state.pendingSource = null;
        }

        if (failure != null || result == null || result.statuses.size() < TIMELINE_CACHE_MIN_ITEMS) {
            String detail = failure == null
                ? "prepared cache is empty"
                : failure.getClass().getSimpleName() + ": " + failure.getMessage();
            log("Timeline cache restore unavailable source=" + request.source + " " + detail);
            if (!request.cumulative) forgetTimelinePreloadDone("restore-miss");
            scheduleTimelinePreload(request.presenter, request.source + "-restore-unavailable");
            return;
        }
        if (sLastTimelinePresenter != null && sLastTimelinePresenter != request.presenter) {
            log("Timeline cache restore dropped stale presenter source=" + request.source);
            return;
        }

        try {
            replaceTimelineCumulativeStatusesPrepared(result.statusesById, request.source);
            syncTimelineActionMaxId(result.action, result.maxId, "reweibo-cache");
            boolean durationReady = isTimelineCacheDurationReady(
                result.stats,
                request.source + "-restored-ready"
            );
            if (!isTimelinePreloadDone() && durationReady) {
                markTimelinePreloadDone("restore-large-cache", 0, result.statuses.size());
            }
            boolean restoredHeadFresh = isTimelineCacheHeadFresh(
                result.stats.newestMs,
                System.currentTimeMillis()
            );
            boolean completeRestore = durationReady
                || restoredHeadFresh && isTimelinePreloadReady(result.statuses.size(), result.stats.newestMs);
            TimelineGap resumeGap = updateTimelineGapFill(
                request.presenter,
                request.source + "-resume",
                true,
                false,
                result.gapScan
            );

            setTimelineCacheRestoring(request.presenter, true);
            try {
                setPresenterTimelineDataPrepared(
                    request.presenter,
                    result.statuses,
                    result.gapScan.count,
                    "reweibo-cache"
                );
            } finally {
                setTimelineCacheRestoring(request.presenter, false);
            }

            checkpointRestoredTimelineCache(
                result.builder,
                result.action,
                result.statuses,
                result.stats,
                result.newestId,
                result.maxId,
                request.source
            );
            synchronized (sPersistedTimelineCacheCounts) {
                sPersistedTimelineCacheCounts.put(request.presenter, Integer.valueOf(result.statuses.size()));
                sPersistedTimelineCacheNewestIds.put(request.presenter, Long.valueOf(result.newestId));
            }
            sTimelineOldestFirstMode = false;
            sTimelineRestoredCacheMode = completeRestore;
            log("Timeline full cache restored source=" + request.source
                + " cacheSource=" + result.cacheSource
                + " count=" + result.statuses.size()
                + " complete=" + completeRestore);
            if (!scheduleTimelineRefreshAnchorForKnownRecyclerViews("presenter-cache-restore")) {
                beginTimelineTopAnchorForKnownRecyclerViews("presenter-cache-restore", true);
            }
            if (resumeGap != null) {
                scheduleTimelineGapFill(request.presenter, request.source + "-resume");
            }
            scheduleTimelinePreload(request.presenter, request.source + "-restore");
            if (lateLiveStatuses != null) {
                scheduleTimelineCacheRestore(
                    request.presenter,
                    lateLiveStatuses,
                    (lateSource == null ? request.source : lateSource) + "-late-coalesced",
                    true
                );
            }
        } catch (Throwable t) {
            setTimelineCacheRestoring(request.presenter, false);
            log("Timeline cache restore apply error source=" + request.source + ": " + t.getMessage());
            scheduleTimelinePreload(request.presenter, request.source + "-restore-apply-error");
        }
    }

    private static void replaceTimelineCumulativeStatusesPrepared(
        LinkedHashMap<Long, Object> prepared,
        String source
    ) {
        synchronized (sTimelineCumulativeStatusesById) {
            sTimelineCumulativeStatusesById.clear();
            for (Map.Entry<Long, Object> entry : prepared.entrySet()) {
                sTimelineCumulativeStatusesById.put(entry.getKey(), entry.getValue());
            }
            log("Timeline cumulative cache replaced prepared source=" + source
                + " count=" + sTimelineCumulativeStatusesById.size());
        }
    }

    private static void checkpointRestoredTimelineCache(
        Object builder,
        Object action,
        List statuses,
        TimelineCacheStats stats,
        long newestId,
        String maxId,
        String source
    ) {
        try {
            ArrayList checkpointStatuses = statuses instanceof ArrayList
                ? (ArrayList) statuses
                : new ArrayList(statuses);
            Object result = newTimelineStatusResult(action, checkpointStatuses, maxId);
            enqueueTimelineNativeCachePersist(new TimelineNativePersistRequest(
                result,
                getTimelineCacheGson(builder),
                getTimelineNativeCacheFile(builder),
                null,
                statuses.size(),
                newestId,
                maxId,
                source + "-restore-checkpoint",
                true,
                stats
            ));
            log("Timeline restored cache checkpoint queued source=" + source
                + " count=" + statuses.size()
                + " newestMs=" + stats.newestMs
                + " maxId=" + maxId);
        } catch (Throwable t) {
            log("Timeline restored cache checkpoint error source=" + source + ": " + t.getMessage());
        }
    }

    private static void setPresenterTimelineDataPrepared(
        Object presenter,
        List data,
        int expected,
        String source
    ) throws Exception {
        Throwable delegateError = null;
        try {
            Object delegate = getFieldValue(presenter, "statusPresenterDelegate");
            XposedHelpers.callMethod(delegate, "setData", data);
        } catch (Throwable t) {
            delegateError = t;
        }

        if (delegateError != null
            || getTimelinePresenterRawStatusCount(presenter) < Math.min(PRELOAD_DONE_MIN_ITEMS, expected)) {
            try {
                XposedHelpers.callMethod(presenter, "setData", data);
            } catch (Throwable t) {
                if (delegateError != null) throw new Exception(delegateError);
                throw new Exception(t);
            }
        }
        log("Presenter timeline restored from cache source=" + source + " size=" + data.size());
    }

    private static boolean restoreTimelineCumulativeCache(Object presenter, List incomingData, String source) {
        maybeAdvanceRefreshAnchorToIncoming(incomingData, source);
        return scheduleTimelineCacheRestore(presenter, incomingData, source, true);
    }

    private static boolean isTimelineCacheRestoring(Object presenter) {
        synchronized (sRestoringTimelineCaches) {
            return sRestoringTimelineCaches.containsKey(presenter);
        }
    }

    private static boolean isTimelineCacheRestoreInFlight(Object presenter) {
        synchronized (sTimelineRestoreLock) {
            TimelineRestoreState state = sTimelineRestoreStates.get(presenter);
            return state != null && state.inFlight;
        }
    }

    static boolean shouldDeferTimelinePersist(boolean force, boolean restoreInFlight) {
        return !force && restoreInFlight;
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

    private static File getTimelineNativeCacheFile(Object builder) {
        try {
            Object key = XposedHelpers.callMethod(builder, "build");
            Object path = XposedHelpers.callMethod(builder, "diskCachePath");
            Object directory = XposedHelpers.callMethod(path, "getCachePath");
            if (key == null || directory == null) return null;
            return new File(String.valueOf(directory), String.valueOf(key) + ".txt");
        } catch (Throwable t) {
            log("Timeline native cache path resolve error: " + t.getMessage());
            return null;
        }
    }

    private static Object getTimelineCacheGson(Object builder) throws Exception {
        Object persistence = XposedHelpers.callMethod(builder, "persistence");
        if (persistence == null) {
            throw new IllegalStateException("timeline cache persistence is missing");
        }
        Class<?> persistenceClass = persistence.getClass();
        if (!"com.weico.diskcache.impl.GsonPersistence".equals(persistenceClass.getName())) {
            throw new IllegalStateException("unexpected timeline cache persistence "
                + persistenceClass.getName());
        }
        try {
            Field gsonField = persistenceClass.getDeclaredField("gson");
            gsonField.setAccessible(true);
            Object gson = gsonField.get(null);
            if (gson != null) return gson;
        } catch (Throwable ignored) {
        }
        Field companionField = persistenceClass.getDeclaredField("Companion");
        companionField.setAccessible(true);
        Object companion = companionField.get(null);
        Object gson = XposedHelpers.callMethod(companion, "getGson");
        if (gson == null) {
            throw new IllegalStateException("timeline cache gson is missing");
        }
        return gson;
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

    private static void mergeTimelineCumulativeStatusesIncremental(List list, Object owner, String source) {
        if (list == null || !"-1".equals(getTimelineGroupId(owner))) return;
        synchronized (sTimelineCumulativeStatusesById) {
            int before = sTimelineCumulativeStatusesById.size();
            for (int i = 0; i < list.size(); i++) {
                Object status = unwrapStatus(list.get(i));
                long id = getStatusId(status);
                if (status == null || id <= 0L || isLoadMoreStatus(status)
                    || isTimelineAdStatus(status) || isTimelineContentlessStatus(status)) {
                    continue;
                }
                sTimelineCumulativeStatusesById.put(Long.valueOf(id), status);
            }
            if (sTimelineCumulativeStatusesById.size() > getTimelinePreloadMaxItems()) {
                ArrayList snapshot = new ArrayList(sTimelineCumulativeStatusesById.values());
                List sorted = sortTimelineNewestFirst(snapshot, owner, source + "-limit", false);
                List trimmed = trimTimelineStatusesToCacheDays(sorted, owner, source + "-limit");
                replaceTimelineCumulativeStatusesLocked(trimmed);
            }
            int after = sTimelineCumulativeStatusesById.size();
            if (after != before && after >= PRELOAD_DONE_MIN_ITEMS
                && after / TIMELINE_GAP_FILL_CHECKPOINT_ITEMS
                    != before / TIMELINE_GAP_FILL_CHECKPOINT_ITEMS) {
                log("Timeline cumulative cache incremented source=" + source
                    + " count=" + after + " previous=" + before);
            }
        }
    }

    private static boolean shouldCheckpointTimelineCache(Object presenter, List statuses) {
        int count = statuses == null ? 0 : statuses.size();
        long newestId = getNewestTimelineStatusId(statuses);
        synchronized (sPersistedTimelineCacheCounts) {
            Integer lastCount = sPersistedTimelineCacheCounts.get(presenter);
            Long lastNewestId = sPersistedTimelineCacheNewestIds.get(presenter);
            if (lastCount == null || lastNewestId == null) return true;
            if (newestId > lastNewestId.longValue()) return true;
            return count >= lastCount.intValue() + TIMELINE_PRELOAD_CHECKPOINT_ITEMS;
        }
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
            List trimmed = trimTimelineStatusesToCacheDays(sorted, owner, source + "-cumulative");
            replaceTimelineCumulativeStatusesLocked(trimmed);
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
        for (int i = 0; i < list.size(); i++) {
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

    static ArrayList mergeTimelineStatusLists(List first, List second, List third) {
        LinkedHashMap<Long, Object> merged = new LinkedHashMap<>();
        List[] sources = new List[] { first, second, third };
        for (int sourceIndex = 0; sourceIndex < sources.length; sourceIndex++) {
            List source = sources[sourceIndex];
            if (source == null) continue;
            for (int i = 0; i < source.size(); i++) {
                Object status = unwrapStatus(source.get(i));
                long id = getStatusId(status);
                if (status == null || id <= 0L || isLoadMoreStatus(status)) {
                    continue;
                }
                Long key = Long.valueOf(id);
                if (!merged.containsKey(key)) merged.put(key, status);
            }
        }
        return new ArrayList(merged.values());
    }

    private static ArrayList collectTimelineStatuses(List list) {
        ArrayList statuses = new ArrayList();
        HashSet<Long> ids = new HashSet<>();
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
            if (!ids.add(key)) continue;
            statuses.add(status);
        }
        return statuses;
    }

    private static List trimTimelineStatusesToCacheDays(List list, Object owner, String source) {
        if (list == null || list.isEmpty()) return list;
        if (!"-1".equals(getTimelineGroupId(owner))) return list;

        int days = getTimelineCacheDaysSetting();
        if (!sTimelineCacheDaysSettingConfirmed) {
            log("Timeline cache window trim skipped until setting is confirmed source=" + source);
            return list;
        }

        TimelineCacheStats stats = buildTimelineCacheStats(list);
        if (stats.datedCount < 2 || stats.newestMs <= 0L) return list;

        long cutoffMs = timelineCacheCutoffMs(System.currentTimeMillis(), days);
        int removed = 0;
        ArrayList copy = new ArrayList(list.size());
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object status = unwrapStatus(item);
            long createdMs = getStatusCreatedAtMillis(status);
            if (createdMs > 0L && createdMs < cutoffMs) {
                removed++;
                continue;
            }
            copy.add(item);
        }
        if (removed == 0) return list;

        log("Timeline cache window trimmed source=" + source
            + " days=" + days
            + " removed=" + removed
            + " kept=" + copy.size()
            + " spanDays=" + formatTimelineCacheSpanDays(stats.spanMs()));
        return copy;
    }

    static long timelineCacheCutoffMs(long nowMs, int days) {
        return nowMs - (Math.max(1, days) * TIMELINE_CACHE_DAY_MS);
    }

    static boolean isTimelineCacheHeadFresh(long newestMs, long nowMs) {
        if (newestMs <= 0L || nowMs <= 0L) return false;
        if (newestMs > nowMs + TIME_JUMP_FUTURE_GRACE_MS) return false;
        return newestMs >= nowMs - TIMELINE_CACHE_HEAD_MAX_AGE_MS;
    }

    private static TimelineCacheStats buildTimelineCacheStats(List list) {
        TimelineCacheStats stats = new TimelineCacheStats();
        if (list == null) return stats;

        for (int i = 0; i < list.size(); i++) {
            Object status = unwrapStatus(list.get(i));
            if (status == null || getStatusId(status) <= 0L || isLoadMoreStatus(status)
                || isTimelineAdStatus(status) || isTimelineContentlessStatus(status)) {
                continue;
            }
            stats.count++;
            long createdMs = getStatusCreatedAtMillis(status);
            if (createdMs <= 0L) continue;
            stats.datedCount++;
            if (stats.newestMs <= 0L || createdMs > stats.newestMs) stats.newestMs = createdMs;
            if (stats.oldestMs <= 0L || createdMs < stats.oldestMs) stats.oldestMs = createdMs;
        }
        return stats;
    }

    private static TimelineCacheStats buildBestTimelineCacheStats(Object presenter) {
        TimelineCacheStats best = null;
        try {
            if (presenter != null) {
                Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
                if (statusList instanceof List) {
                    best = buildTimelineCacheStats((List) statusList);
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            ArrayList snapshot;
            synchronized (sTimelineCumulativeStatusesById) {
                snapshot = new ArrayList(sTimelineCumulativeStatusesById.values());
            }
            List trimmed = trimTimelineStatusesToCacheDays(snapshot, presenter, "cache-stats");
            if (trimmed != snapshot) {
                snapshot = new ArrayList(trimmed);
                replaceTimelineCumulativeStatuses(snapshot, presenter, "cache-stats");
            }
            TimelineCacheStats cumulative = buildTimelineCacheStats(snapshot);
            if (best == null || cumulative.spanMs() > best.spanMs()
                || (cumulative.spanMs() == best.spanMs() && cumulative.count > best.count)) {
                best = cumulative;
            }
        } catch (Throwable ignored) {
        }

        return best == null ? new TimelineCacheStats() : best;
    }

    private static boolean isTimelineCacheDurationReady(List list, String source) {
        TimelineCacheStats stats = buildTimelineCacheStats(list);
        return isTimelineCacheDurationReady(stats, source);
    }

    private static boolean isTimelineCacheDurationReady(TimelineCacheStats stats, String source) {
        if (stats == null || stats.count < TIMELINE_CACHE_MIN_ITEMS) return false;
        int days = getTimelineCacheDaysSetting();
        if (!sTimelineCacheDaysSettingConfirmed) {
            log("Timeline cache duration deferred until setting is confirmed source=" + source);
            return false;
        }
        long spanMs = stats.spanMs();
        boolean headFresh = isTimelineCacheHeadFresh(stats.newestMs, System.currentTimeMillis());
        boolean ready = isTimelineCacheDurationReadyForSetting(
            stats.count,
            stats.datedCount,
            stats.oldestMs,
            stats.newestMs,
            System.currentTimeMillis(),
            days,
            true
        );
        if (!ready) {
            log("Timeline cache duration not ready source=" + source
                + " count=" + stats.count
                + " dated=" + stats.datedCount
                + " days=" + days
                + " spanDays=" + formatTimelineCacheSpanDays(spanMs)
                + " headFresh=" + headFresh
                + " newestMs=" + stats.newestMs);
        }
        return ready;
    }

    static boolean isTimelineCacheDurationReadyForSetting(
        int count,
        int datedCount,
        long oldestMs,
        long newestMs,
        long nowMs,
        int days,
        boolean settingConfirmed
    ) {
        if (!settingConfirmed || count < TIMELINE_CACHE_MIN_ITEMS || datedCount < 2) return false;
        if (!isTimelineCacheHeadFresh(newestMs, nowMs)) return false;
        if (oldestMs <= 0L || newestMs <= 0L || newestMs < oldestMs) return false;
        int cacheDays = ModuleSettings.clampTimelineCacheDays(days);
        long requiredMs = Math.max(
            0L,
            (cacheDays * TIMELINE_CACHE_DAY_MS) - TIMELINE_CACHE_DAY_TOLERANCE_MS
        );
        return newestMs - oldestMs >= requiredMs;
    }

    private static boolean isTimelineTerminalDoneSource(String source) {
        if (source == null) return false;
        return source.contains("no-more") || source.contains("server-empty");
    }

    private static String formatTimelineCacheSpanDays(long spanMs) {
        if (spanMs <= 0L) return "0.0";
        long tenths = (spanMs * 10L) / TIMELINE_CACHE_DAY_MS;
        return (tenths / 10L) + "." + (tenths % 10L);
    }

    private static List filterTimelineAds(List list, Object owner, String source) {
        if (list == null || list.isEmpty()) return list;

        String groupId = getTimelineGroupId(owner);
        if (!"-1".equals(groupId)) return list;

        ArrayList copy = null;
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object status = unwrapStatus(item);
            if (status != null && isTimelineAdStatus(status)) {
                if (copy == null) {
                    copy = new ArrayList(list.size());
                    copy.addAll(list.subList(0, i));
                }
            } else if (copy != null) {
                copy.add(item);
            }
        }
        if (copy == null) return list;
        int removed = list.size() - copy.size();

        log("Timeline ads filtered source=" + source + " group=" + groupId + " removed=" + removed
            + " size=" + list.size() + " kept=" + copy.size());
        return copy;
    }

    private static List filterTimelineContentless(List list, Object owner, String source) {
        if (list == null || list.isEmpty()) return list;

        String groupId = getTimelineGroupId(owner);
        if (!"-1".equals(groupId)) return list;

        ArrayList copy = null;
        String sampleIds = "";
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            Object status = unwrapStatus(item);
            if (status != null && isTimelineContentlessStatus(status)) {
                int removed = copy == null ? 0 : i - copy.size();
                if (removed < 3) {
                    if (sampleIds.length() > 0) sampleIds += ",";
                    sampleIds += String.valueOf(getStatusId(status));
                }
                if (copy == null) {
                    copy = new ArrayList(list.size());
                    copy.addAll(list.subList(0, i));
                }
            } else if (copy != null) {
                copy.add(item);
            }
        }
        if (copy == null) return list;
        int removed = list.size() - copy.size();

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
        if (recyclerView instanceof View) {
            rememberWeicoContext(((View) recyclerView).getContext(), "timeline-view");
        }
        synchronized (sTopAnchorStates) {
            sTimelineRecyclerViews.put(recyclerView, Boolean.TRUE);
        }
    }

    private static Object getAnyTimelineRecyclerView() {
        synchronized (sTopAnchorStates) {
            ArrayList recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
            Object best = null;
            int bestScore = -1;
            for (int i = 0; i < recyclerViews.size(); i++) {
                Object recyclerView = recyclerViews.get(i);
                if (!isTimelineRecyclerView(recyclerView)) continue;
                int score = getTimelineRecyclerViewDataScore(recyclerView);
                if (best == null || score > bestScore) {
                    best = recyclerView;
                    bestScore = score;
                }
            }
            return best;
        }
    }

    private static int getTimelineRecyclerViewDataScore(Object recyclerView) {
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            int dataCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getCount", -1);
            int itemCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getItemCount", -1);
            return Math.max(dataCount, itemCount);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static void captureTimelineRefreshAnchorIfNeeded(Object owner, String source) {
        if (getActiveTimelineRefreshAnchorStatusId() > 0L) return;
        if (owner == null || !"-1".equals(getTimelineGroupId(owner))) return;
        if (!sTimelineRestoredCacheMode && !isTimelinePreloadDone()) return;
        if (!captureTimelineRefreshAnchorFromPendingHomeTab(source)) {
            captureTimelineRefreshAnchorForKnownRecyclerViews(owner, source);
        }
    }

    private static boolean captureTimelineRefreshAnchorForKnownRecyclerViews(Object owner, String source) {
        if (getActiveTimelineRefreshAnchorStatusId() > 0L) return true;
        if (owner == null || !"-1".equals(getTimelineGroupId(owner))) return false;
        if (captureTimelineRefreshAnchorFromPendingHomeTab(source)) return true;

        ArrayList recyclerViews;
        synchronized (sTopAnchorStates) {
            recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
        }
        for (int i = 0; i < recyclerViews.size(); i++) {
            Object recyclerView = recyclerViews.get(i);
            int[] viewport = new int[3];
            long statusId = getVisibleTimelineStatusId(recyclerView, viewport);
            if (statusId <= 0L) continue;
            long lastReadId = getLastReadStatusId();
            if (lastReadId > 0L && lastReadId != statusId
                && viewport[0] >= 0 && viewport[0] < 20
                && (hasTimelineStatusInCumulativeCache(lastReadId)
                    || getTimelineStatusAdapterPosition(recyclerView, lastReadId) >= 0)) {
                return setTimelineRefreshAnchor(lastReadId, source + "-last-read", -1, viewport[1], viewport[2]);
            }
            return setTimelineRefreshAnchor(statusId, source, viewport[0], viewport[1], viewport[2]);
        }
        return getActiveTimelineRefreshAnchorStatusId() > 0L;
    }

    private static void captureHomeTabPendingRefreshAnchor(long now) {
        try {
            if (sHomeTabPendingAnchorStatusId > 0L
                && now <= sHomeTabPendingAnchorUntilMs
                && sHomeTabLastTapAtMs > 0L
                && now - sHomeTabLastTapAtMs <= HOME_TAB_DOUBLE_TAP_MS) {
                return;
            }
            Object recyclerView = getAnyTimelineRecyclerView();
            int[] viewport = new int[3];
            long statusId = getVisibleTimelineStatusId(recyclerView, viewport);
            if (statusId <= 0L) return;
            sHomeTabPendingAnchorStatusId = statusId;
            sHomeTabPendingAnchorUntilMs = now + REFRESH_ANCHOR_WINDOW_MS;
            sHomeTabPendingAnchorPosition = viewport[0];
            sHomeTabPendingAnchorFirst = viewport[1];
            sHomeTabPendingAnchorLast = viewport[2];
            log("Timeline refresh pending anchor captured source=home-tab-tap"
                + " id=" + statusId
                + " position=" + viewport[0]
                + " visible=" + viewport[1] + ".." + viewport[2]);
        } catch (Throwable ignored) {}
    }

    private static boolean captureTimelineRefreshAnchorFromPendingHomeTab(String source) {
        long now = SystemClock.elapsedRealtime();
        long statusId = sHomeTabPendingAnchorStatusId;
        if (statusId <= 0L || now > sHomeTabPendingAnchorUntilMs) return false;
        boolean captured = setTimelineRefreshAnchor(
            statusId,
            source + "-home-pending",
            sHomeTabPendingAnchorPosition,
            sHomeTabPendingAnchorFirst,
            sHomeTabPendingAnchorLast
        );
        if (captured) {
            sHomeTabPendingAnchorStatusId = 0L;
            sHomeTabPendingAnchorUntilMs = 0L;
            sHomeTabPendingAnchorPosition = -1;
            sHomeTabPendingAnchorFirst = -1;
            sHomeTabPendingAnchorLast = -1;
        }
        return captured;
    }

    private static boolean setTimelineRefreshAnchor(long statusId, String source, int position, int first, int last) {
        if (statusId <= 0L) return false;
        if (getActiveTimelineRefreshAnchorStatusId() > 0L) return true;
        sTimelineRefreshAnchorStatusId = statusId;
        sTimelineRefreshAnchorUntilMs = SystemClock.elapsedRealtime() + REFRESH_ANCHOR_WINDOW_MS;
        sTimelineRefreshAnchorPosition = position;
        sTimelineRefreshAnchorGeneration++;
        cancelTimelineTopAnchorsForRefresh(source);
        log("Timeline refresh anchor captured source=" + source
            + " id=" + statusId
            + " position=" + position
            + " visible=" + first + ".." + last);
        return true;
    }

    private static void maybeAdvanceRefreshAnchorToIncoming(List incomingData, String source) {
        try {
            long activeId = getActiveTimelineRefreshAnchorStatusId();
            if (activeId <= 0L || incomingData == null || sTimelineRefreshAnchorPosition > 3) return;

            long incomingNewestId = getNewestTimelineStatusId(incomingData);
            if (incomingNewestId <= activeId) return;

            sTimelineRefreshAnchorStatusId = incomingNewestId;
            sTimelineRefreshAnchorUntilMs = SystemClock.elapsedRealtime() + REFRESH_ANCHOR_WINDOW_MS;
            sTimelineRefreshAnchorPosition = -1;
            sTimelineRefreshAnchorGeneration++;
            log("Timeline refresh anchor advanced source=" + source
                + " from=" + activeId
                + " to=" + incomingNewestId
                + " reason=newer-incoming");
        } catch (Throwable t) {
            log("Timeline refresh anchor advance error source=" + source + ": " + t.getMessage());
        }
    }

    private static long getNewestTimelineStatusId(List list) {
        long newest = 0L;
        if (list == null) return newest;
        for (int i = 0; i < list.size(); i++) {
            Object status = unwrapStatus(list.get(i));
            long id = getStatusId(status);
            if (status != null && id > newest && !isLoadMoreStatus(status)
                && !isTimelineAdStatus(status) && !isTimelineContentlessStatus(status)) {
                newest = id;
            }
        }
        return newest;
    }

    private static boolean pauseTimelineGapFillOnEmptyPage(Object presenter, List incomingData, List mergedData, String source) {
        try {
            if (presenter == null || incomingData == null || !"-1".equals(getTimelineGroupId(presenter))) return false;
            if (!hasActiveTimelineGapFill()) return false;

            int incoming = countTimelineStatuses(incomingData);
            if (incoming > 1) return false;

            int count = mergedData == null ? getTimelineStatusCount(presenter) : countTimelineStatuses(mergedData);
            log("Timeline gap-fill empty page paused source=" + source
                + " incoming=" + incoming + " count=" + count);
            suppressTimelineGapFillShowData();
            postTimelineGapFillProgress(
                "正在补齐中间微博\n当前游标空页，改用备用位置",
                false,
                source + "-empty-page"
            );
            if (mergedData != null) {
                persistTimelineNativeCacheList(presenter, mergedData, source + "-gap-fill-empty", false);
            }
            if (retryTimelineGapFillWithFallback(presenter, count, source + "-empty-page")) {
                return true;
            }
            if (completeTimelineGapFillFromServerEmptyIfProven(presenter, count, source + "-empty-page")) {
                return true;
            }
            stopTimelineGapFill(source + "-empty-page");
            return true;
        } catch (Throwable t) {
            log("Timeline gap-fill empty-page pause error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static boolean retryTimelineGapFillWithFallback(Object presenter, int count, String source) {
        boolean shouldRetry = false;
        synchronized (sTimelineGapFillState) {
            shouldRetry = prepareTimelineGapFillFallbackLocked(sTimelineGapFillState, count, source);
        }
        if (shouldRetry) {
            scheduleTimelineGapFill(presenter, source + "-fallback");
        }
        return shouldRetry;
    }

    private static boolean completeTimelineGapFillFromServerEmptyIfProven(Object presenter, int count, String source) {
        synchronized (sTimelineGapFillState) {
            if (!hasTimelineGapFillServerEmptyProofLocked(sTimelineGapFillState)) return false;
        }
        completeTimelineGapFillFromServerEmpty(presenter, count, source);
        return true;
    }

    private static void completeTimelineGapFillFromServerEmpty(Object presenter, int count, String source) {
        int pages;
        String proof;
        synchronized (sTimelineGapFillState) {
            GapFillState state = sTimelineGapFillState;
            pages = state.requestedPages;
            proof = describeTimelineGapFillEmptyProofLocked(state);
            log("Timeline gap-fill completed source=" + source
                + " reason=server-empty"
                + " pages=" + pages
                + " count=" + count
                + " " + proof);
            suppressTimelineGapFillShowData();
            postTimelineGapFillProgress(
                "已加载完成\n服务器确认没有更多可补齐",
                true,
                source + "-completed"
            );
            resetTimelineGapFillLocked(state);
        }
        if (presenter != null) {
            persistTimelineNativeCache(presenter, source + "-complete");
            markTimelinePreloadDone("gap-fill-server-empty", pages, count);
        }
    }

    private static boolean prepareTimelineGapFillFallbackLocked(GapFillState state, int count, String source) {
        if (state == null || !state.active) return false;
        if (state.requestedPages >= getTimelinePreloadMaxPages()) return false;
        if (state.fallbackAttempts >= TIMELINE_GAP_FILL_MAX_FALLBACKS) {
            log("Timeline gap-fill fallback exhausted source=" + source
                + " attempts=" + state.fallbackAttempts
                + " gapCursor=" + state.gapCursorId
                + " cursor=" + state.cursorId
                + " target=" + state.targetId
                + " count=" + count
                + " " + describeTimelineGapFillEmptyProofLocked(state));
            return false;
        }

        int nextAttempt = state.fallbackAttempts + 1;
        long nextCursor = computeTimelineGapFallbackCursor(
            state.gapCursorId > 0L ? state.gapCursorId : state.cursorId,
            state.cursorId,
            state.targetId,
            nextAttempt
        );
        if (nextCursor <= 0L || nextCursor == state.cursorId || nextCursor <= state.targetId) {
            log("Timeline gap-fill fallback unavailable source=" + source
                + " attempt=" + nextAttempt
                + " gapCursor=" + state.gapCursorId
                + " cursor=" + state.cursorId
                + " target=" + state.targetId
                + " count=" + count
                + " " + describeTimelineGapFillEmptyProofLocked(state));
            return false;
        }

        state.fallbackAttempts = nextAttempt;
        state.cursorId = nextCursor;
        state.lastCount = count;
        state.inFlight = false;
        state.scheduled = false;
        state.untilElapsedMs = SystemClock.elapsedRealtime() + TIMELINE_GAP_FILL_WINDOW_MS;
        suppressTimelineGapFillShowData();
        log("Timeline gap-fill fallback retry source=" + source
            + " attempt=" + nextAttempt
            + " nextCursor=" + nextCursor
            + " gapCursor=" + state.gapCursorId
            + " target=" + state.targetId
            + " count=" + count);
        postTimelineGapFillProgress(
            "正在补齐中间微博\n备用位置第 " + nextAttempt + " 次 · 已缓存 " + count + " 条",
            false,
            source + "-fallback"
        );
        return true;
    }

    private static long computeTimelineGapFallbackCursor(long gapCursor, long currentCursor, long targetId, int attempt) {
        if (targetId <= 0L) return 0L;
        long base = currentCursor > targetId ? currentCursor : gapCursor;
        if (base <= targetId + 1L) return 0L;
        long remaining = base - targetId;
        long offset;
        if (attempt <= 1) {
            offset = 1L;
        } else if (attempt == 2) {
            offset = Math.min(TIMELINE_GAP_FILL_SMALL_BACKOFF_ID, remaining - 1L);
        } else if (attempt == 3) {
            offset = Math.min(TIMELINE_GAP_ID_THRESHOLD / 2L, remaining - 1L);
        } else {
            offset = Math.max(TIMELINE_GAP_ID_THRESHOLD / 2L, remaining / 2L);
            offset = Math.min(offset, remaining - 1L);
        }
        if (offset <= 0L) offset = 1L;
        if (offset >= remaining) offset = remaining - 1L;
        return base - offset;
    }

    private static void continueTimelineGapFill(Object presenter, List mergedData, String source) {
        if (!hasActiveTimelineGapFill()) return;
        try {
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            List data = mergedData;
            if (data == null) {
                synchronized (sTimelineCumulativeStatusesById) {
                    data = new ArrayList(sTimelineCumulativeStatusesById.values());
                }
                data = sortTimelineNewestFirst(data, presenter, source + "-gap-fill", false);
            }
            TimelineGap gap = updateTimelineGapFill(presenter, data, source, false, true);
            if (gap == null) return;

            Object action = getTimelineAction(presenter);
            if (action != null) {
                syncTimelineActionMaxId(action, String.valueOf(gap.cursorId), source + "-gap-fill");
            }
            scheduleTimelineGapFill(presenter, source + "-gap-fill");
        } catch (Throwable t) {
            log("Timeline gap-fill continue error source=" + source + ": " + t.getMessage());
            stopTimelineGapFill(source + "-error");
        }
    }

    private static void checkpointTimelineGapFillCache(Object presenter, List mergedData, String source) {
        try {
            if (presenter == null || mergedData == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            boolean shouldPersist;
            synchronized (sTimelineGapFillState) {
                GapFillState state = sTimelineGapFillState;
                if (!state.active) {
                    shouldPersist = true;
                } else if (state.requestedPages <= 0) {
                    shouldPersist = false;
                } else {
                    shouldPersist = state.requestedPages - state.lastCheckpointPage >= TIMELINE_GAP_FILL_CHECKPOINT_PAGES
                        || state.lastCount - state.lastCheckpointCount >= TIMELINE_GAP_FILL_CHECKPOINT_ITEMS;
                }
            }
            if (!shouldPersist) return;

            if (persistTimelineNativeCacheList(presenter, mergedData, source + "-gap-fill-checkpoint", false)) {
                synchronized (sTimelineGapFillState) {
                    GapFillState state = sTimelineGapFillState;
                    state.lastCheckpointPage = state.requestedPages;
                    state.lastCheckpointCount = state.lastCount;
                }
            }
        } catch (Throwable t) {
            log("Timeline gap-fill checkpoint error source=" + source + ": " + t.getMessage());
        }
    }

    private static TimelineGap updateTimelineGapFill(
        Object presenter,
        List sorted,
        String source,
        boolean allowStart,
        boolean responseArrived
    ) {
        try {
            if (presenter == null || sorted == null || !"-1".equals(getTimelineGroupId(presenter))) return null;
            return updateTimelineGapFill(
                presenter,
                source,
                allowStart,
                responseArrived,
                scanTimelineGap(sorted)
            );
        } catch (Throwable t) {
            log("Timeline gap-fill scan error source=" + source + ": " + t.getMessage());
            return null;
        }
    }

    private static TimelineGap updateTimelineGapFill(
        Object presenter,
        String source,
        boolean allowStart,
        boolean responseArrived,
        TimelineGapScan scan
    ) {
        try {
            if (presenter == null || scan == null || !"-1".equals(getTimelineGroupId(presenter))) return null;
            TimelineGap gap = scan.gap;
            int count = scan.count;
            synchronized (sTimelineGapFillState) {
                GapFillState state = sTimelineGapFillState;
                long now = SystemClock.elapsedRealtime();
                if (state.active && now > state.untilElapsedMs) {
                    log("Timeline gap-fill expired source=" + source
                        + " cursor=" + state.cursorId + " target=" + state.targetId);
                    postTimelineGapFillProgress(
                        "补齐已暂停\n微博轻享版回到前台后可继续刷新补齐",
                        true,
                        source + "-expired"
                    );
                    resetTimelineGapFillLocked(state);
                }
                if (responseArrived && state.active) {
                    state.inFlight = false;
                    state.scheduled = false;
                }
                if (gap == null) {
                    if (state.active) {
                        log("Timeline gap-fill completed source=" + source
                            + " pages=" + state.requestedPages + " count=" + count);
                        postTimelineGapFillProgress(
                            "中间微博已补齐\n已缓存 " + count + " 条",
                            true,
                            source + "-completed"
                        );
                        resetTimelineGapFillLocked(state);
                    }
                    return null;
                }
                if (!allowStart && !state.active) return null;
                if (responseArrived && state.active
                    && state.gapCursorId == gap.cursorId
                    && state.targetId == gap.targetId) {
                    log("Timeline gap-fill stalled source=" + source
                        + " cursor=" + gap.cursorId
                        + " target=" + gap.targetId
                        + " gap=" + gap.distance
                        + " count=" + count);
                    if (prepareTimelineGapFillFallbackLocked(state, count, source + "-stalled")) {
                        return copyTimelineGapForRequest(gap, state.cursorId);
                    }
                    postTimelineGapFillProgress(
                        "补齐已暂停\n已缓存 " + count + " 条",
                        true,
                        source + "-stalled"
                    );
                    resetTimelineGapFillLocked(state);
                    return null;
                }

                if (!state.active) {
                    state.requestedPages = 0;
                    state.requestToken++;
                    state.lastCheckpointPage = 0;
                    state.lastCheckpointCount = count;
                    state.fallbackAttempts = 0;
                    state.cursorId = gap.cursorId;
                    resetTimelineGapFillEmptyProofLocked(state);
                } else if (state.gapCursorId != gap.cursorId) {
                    state.fallbackAttempts = 0;
                    state.cursorId = gap.cursorId;
                    resetTimelineGapFillEmptyProofLocked(state);
                } else if (state.fallbackAttempts > 0 && state.targetId != gap.targetId) {
                    state.cursorId = gap.targetId;
                    resetTimelineGapFillEmptyProofLocked(state);
                    log("Timeline gap-fill fallback advanced source=" + source
                        + " gapCursor=" + gap.cursorId
                        + " nextCursor=" + state.cursorId
                        + " target=" + gap.targetId
                        + " attempts=" + state.fallbackAttempts);
                } else if (state.fallbackAttempts <= 0) {
                    state.cursorId = gap.cursorId;
                }
                state.active = true;
                state.gapCursorId = gap.cursorId;
                state.targetId = gap.targetId;
                state.distance = gap.distance;
                state.lastCount = count;
                state.untilElapsedMs = now + TIMELINE_GAP_FILL_WINDOW_MS;
                suppressTimelineGapFillShowData();
                log("Timeline gap-fill active source=" + source
                    + " cursor=" + gap.cursorId
                    + " requestCursor=" + state.cursorId
                    + " target=" + gap.targetId
                    + " gap=" + gap.distance
                    + " index=" + gap.index
                    + " count=" + count
                    + " pages=" + state.requestedPages
                    + " fallback=" + state.fallbackAttempts);
                postTimelineGapFillProgress(
                    buildTimelineGapFillProgressText(state.requestedPages, count, gap.distance),
                    false,
                    source + "-active"
                );
                return copyTimelineGapForRequest(gap, state.cursorId);
            }
        } catch (Throwable t) {
            log("Timeline gap-fill update error source=" + source + ": " + t.getMessage());
            return null;
        }
    }

    private static TimelineGap copyTimelineGapForRequest(TimelineGap gap, long requestCursor) {
        if (gap == null) return null;
        TimelineGap copy = new TimelineGap();
        copy.cursorId = requestCursor > 0L ? requestCursor : gap.cursorId;
        copy.targetId = gap.targetId;
        copy.distance = gap.distance;
        copy.index = gap.index;
        return copy;
    }

    private static TimelineGapScan scanTimelineGap(List list) {
        ArrayList statuses = collectTimelineStatuses(list);
        TimelineGapScan scan = new TimelineGapScan();
        scan.count = statuses.size();
        if (statuses.size() < 2) return scan;

        long previousId = 0L;
        for (int i = 0; i < statuses.size(); i++) {
            Object status = unwrapStatus(statuses.get(i));
            long id = getStatusId(status);
            if (id <= 0L) continue;
            if (previousId > id) {
                long distance = previousId - id;
                if (distance >= TIMELINE_GAP_ID_THRESHOLD) {
                    TimelineGap gap = new TimelineGap();
                    gap.cursorId = previousId;
                    gap.targetId = id;
                    gap.distance = distance;
                    gap.index = i;
                    scan.gap = gap;
                    return scan;
                }
            }
            previousId = id;
        }
        return scan;
    }

    private static boolean hasActiveTimelineGapFill() {
        synchronized (sTimelineGapFillState) {
            GapFillState state = sTimelineGapFillState;
            if (!state.active) return false;
            long now = SystemClock.elapsedRealtime();
            if (now > state.untilElapsedMs || state.requestedPages >= getTimelinePreloadMaxPages()) {
                resetTimelineGapFillLocked(state);
                return false;
            }
            return true;
        }
    }

    private static void scheduleTimelineGapFill(final Object presenter, final String source) {
        try {
            if (sTimelineCacheClearInFlight) return;
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            final long delayMs;
            synchronized (sTimelineGapFillState) {
                GapFillState state = sTimelineGapFillState;
                if (!state.active || state.scheduled || state.inFlight) return;
                if (state.requestedPages >= getTimelinePreloadMaxPages()) {
                    log("Timeline gap-fill safety stop source=" + source
                        + " pages=" + state.requestedPages
                        + " cursor=" + state.cursorId
                        + " target=" + state.targetId);
                    postTimelineGapFillProgress(
                        "补齐已暂停\n已到安全页数上限",
                        true,
                        source + "-safety"
                    );
                    resetTimelineGapFillLocked(state);
                    return;
                }
                state.scheduled = true;
                delayMs = getTimelineGapFillDelayMsLocked(state);
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestTimelineGapFill(presenter, source);
                }
            }, delayMs);
        } catch (Throwable t) {
            log("Timeline gap-fill schedule error source=" + source + ": " + t.getMessage());
        }
    }

    private static long getTimelineGapFillDelayMsLocked(GapFillState state) {
        if (state == null || state.fallbackAttempts <= 0) return TIMELINE_GAP_FILL_DELAY_MS;
        int attempts = Math.min(state.fallbackAttempts, 4);
        return TIMELINE_GAP_FILL_DELAY_MS + TIMELINE_GAP_FILL_FALLBACK_DELAY_MS * attempts;
    }

    private static void requestTimelineGapFill(final Object presenter, String source) {
        final int token;
        long cursorId;
        long targetId;
        int page;
        int count;
        int fallbackAttempts;
        try {
            if (presenter == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            synchronized (sTimelineGapFillState) {
                GapFillState state = sTimelineGapFillState;
                state.scheduled = false;
                if (!state.active || state.inFlight) return;
                if (state.requestedPages >= getTimelinePreloadMaxPages()) {
                    log("Timeline gap-fill safety stop source=" + source
                        + " pages=" + state.requestedPages
                        + " cursor=" + state.cursorId
                        + " target=" + state.targetId);
                    postTimelineGapFillProgress(
                        "补齐已暂停\n已到安全页数上限",
                        true,
                        source + "-safety"
                    );
                    resetTimelineGapFillLocked(state);
                    return;
                }
                cursorId = state.cursorId;
                targetId = state.targetId;
                state.inFlight = true;
                state.requestedPages++;
                state.lastCount = getTimelineStatusCount(presenter);
                state.untilElapsedMs = SystemClock.elapsedRealtime() + TIMELINE_GAP_FILL_WINDOW_MS;
                token = ++state.requestToken;
                page = state.requestedPages;
                count = state.lastCount;
                fallbackAttempts = state.fallbackAttempts;
            }

            Object action = getTimelineAction(presenter);
            if (action == null) {
                log("Timeline gap-fill skipped source=" + source + " no action");
                stopTimelineGapFill(source + "-no-action");
                return;
            }
            syncTimelineActionMaxId(action, String.valueOf(cursorId), source + "-request");
            log("Timeline gap-fill loadMore page=" + page
                + " cursor=" + cursorId
                + " target=" + targetId
                + " count=" + count
                + " source=" + source
                + (fallbackAttempts > 0 ? " fallback=" + fallbackAttempts : ""));
            suppressTimelineGapFillShowData();
            XposedHelpers.callMethod(presenter, "loadMore");
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    finishTimelineGapFillWatchdog(presenter, token);
                }
            }, TIMELINE_GAP_FILL_WATCHDOG_MS);
        } catch (Throwable t) {
            log("Timeline gap-fill loadMore error source=" + source + ": " + t.getMessage());
            stopTimelineGapFill(source + "-error");
        }
    }

    private static void finishTimelineGapFillWatchdog(Object presenter, int token) {
        try {
            boolean shouldInspect = false;
            boolean shouldRetryFallback = false;
            boolean shouldCompleteServerEmpty = false;
            int count = 0;
            synchronized (sTimelineGapFillState) {
                GapFillState state = sTimelineGapFillState;
                if (!state.active || state.requestToken != token || !state.inFlight) return;
                count = getTimelineStatusCount(presenter);
                state.inFlight = false;
                state.scheduled = false;
                if (count <= state.lastCount) {
                    log("Timeline gap-fill stalled watchdog cursor=" + state.cursorId
                        + " target=" + state.targetId
                        + " pages=" + state.requestedPages
                        + " count=" + count);
                    shouldRetryFallback = prepareTimelineGapFillFallbackLocked(
                        state,
                        count,
                        "gap-fill-watchdog-stalled"
                    );
                    if (!shouldRetryFallback) {
                        shouldCompleteServerEmpty = hasTimelineGapFillServerEmptyProofLocked(state);
                        if (!shouldCompleteServerEmpty) {
                            postTimelineGapFillProgress(
                                "补齐已暂停\n已缓存 " + count + " 条",
                                true,
                                "gap-fill-watchdog-stalled"
                            );
                            resetTimelineGapFillLocked(state);
                        }
                    }
                } else {
                    state.lastCount = count;
                    shouldInspect = true;
                }
            }
            if (shouldCompleteServerEmpty) {
                completeTimelineGapFillFromServerEmpty(presenter, count, "gap-fill-watchdog-server-empty");
                return;
            }
            if (shouldRetryFallback) {
                scheduleTimelineGapFill(presenter, "gap-fill-watchdog-fallback");
                return;
            }
            if (shouldInspect) {
                Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
                if (statusList instanceof List) {
                    List sorted = sortTimelineNewestFirst((List) statusList, presenter, "gap-fill-watchdog", false);
                    TimelineGap gap = updateTimelineGapFill(presenter, sorted, "gap-fill-watchdog", false, false);
                    if (gap != null) {
                        scheduleTimelineGapFill(presenter, "gap-fill-watchdog");
                    }
                }
            }
        } catch (Throwable t) {
            log("Timeline gap-fill watchdog error: " + t.getMessage());
            stopTimelineGapFill("gap-fill-watchdog-error");
        }
    }

    private static void stopTimelineGapFill(String source) {
        synchronized (sTimelineGapFillState) {
            GapFillState state = sTimelineGapFillState;
            if (state.active || state.scheduled || state.inFlight) {
                log("Timeline gap-fill stopped source=" + source
                    + " pages=" + state.requestedPages
                    + " cursor=" + state.cursorId
                    + " target=" + state.targetId);
                suppressTimelineGapFillShowData();
                postTimelineGapFillProgress(
                    "补齐已暂停\n已补 " + state.requestedPages + " 页",
                    true,
                    source + "-stopped"
                );
            }
            resetTimelineGapFillLocked(state);
        }
    }

    private static void resetTimelineGapFillLocked(GapFillState state) {
        state.active = false;
        state.scheduled = false;
        state.inFlight = false;
        state.requestedPages = 0;
        state.requestToken++;
        state.lastCount = 0;
        state.lastCheckpointPage = 0;
        state.lastCheckpointCount = 0;
        state.fallbackAttempts = 0;
        resetTimelineGapFillEmptyProofLocked(state);
        state.gapCursorId = 0L;
        state.cursorId = 0L;
        state.targetId = 0L;
        state.distance = 0L;
        state.untilElapsedMs = 0L;
    }

    private static void suppressTimelineGapFillShowData() {
        sTimelineGapFillSuppressShowDataUntilMs = SystemClock.elapsedRealtime()
            + TIMELINE_GAP_FILL_SHOWDATA_SUPPRESS_MS;
    }

    private static String buildTimelineGapFillProgressText(int pages, int count, long distance) {
        String pageText = pages <= 0 ? "准备补齐" : "已补 " + pages + " 页";
        String gapText = distance > 0L ? "\n断层正在缩小，保持前台会继续" : "";
        return "正在补齐中间微博\n" + pageText + " · 已缓存 " + count + " 条" + gapText;
    }

    private static void postTimelineGapFillProgress(final String text, final boolean hideSoon, final String source) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    updateTimelineGapFillProgressView(text, hideSoon, source);
                }
            });
        } catch (Throwable t) {
            log("Timeline gap-fill progress post error source=" + source + ": " + t.getMessage());
        }
    }

    private static void updateTimelineGapFillProgressView(String text, boolean hideSoon, String source) {
        try {
            int generation = ++sTimelineGapFillProgressGeneration;
            if (!ensureTimelineGapFillProgressView(source)) return;
            if (sTimelineGapFillProgressText != null) {
                sTimelineGapFillProgressText.setText(text);
            }
            if (sTimelineGapFillProgressBar != null) {
                sTimelineGapFillProgressBar.setVisibility(hideSoon ? View.GONE : View.VISIBLE);
            }
            if (sTimelineGapFillProgressView != null) {
                sTimelineGapFillProgressView.setVisibility(View.VISIBLE);
                sTimelineGapFillProgressView.bringToFront();
            }
            if (hideSoon) {
                final int hideGeneration = generation;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (hideGeneration == sTimelineGapFillProgressGeneration) {
                            removeTimelineGapFillProgressView("hide-delay");
                        }
                    }
                }, TIMELINE_GAP_FILL_PROGRESS_HIDE_MS);
            }
        } catch (Throwable t) {
            log("Timeline gap-fill progress update error source=" + source + ": " + t.getMessage());
        }
    }

    private static boolean ensureTimelineGapFillProgressView(String source) {
        try {
            Object recyclerView = getAnyTimelineRecyclerView();
            if (!(recyclerView instanceof View)) return false;
            View anchor = (View) recyclerView;
            FrameLayout parent = findTimelineOverlayParent(anchor);
            if (parent == null) return false;
            if (sTimelineGapFillProgressView != null && sTimelineGapFillProgressView.getParent() == parent
                && sTimelineGapFillProgressText != null && sTimelineGapFillProgressBar != null) {
                return true;
            }
            removeTimelineGapFillProgressView("reparent");

            Context context = anchor.getContext();
            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setClickable(false);
            card.setFocusable(false);
            int paddingH = dpToPx(anchor, 14);
            int paddingV = dpToPx(anchor, 10);
            card.setPadding(paddingH, paddingV, paddingH, paddingV);

            GradientDrawable background = new GradientDrawable();
            background.setColor(0xEA202124);
            background.setCornerRadius(dpToPx(anchor, 8));
            background.setStroke(Math.max(1, dpToPx(anchor, 1)), 0x33FFFFFF);
            card.setBackground(background);
            card.setAlpha(0.96f);

            TextView label = new TextView(context);
            label.setTextColor(Color.WHITE);
            label.setTextSize(13f);
            label.setGravity(Gravity.CENTER);
            label.setIncludeFontPadding(false);
            card.addView(label, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            ProgressBar progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            progress.setIndeterminate(true);
            progress.setIndeterminateTintList(ColorStateList.valueOf(0xFF4EA1FF));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(0x44FFFFFF));
            LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(3, dpToPx(anchor, 4))
            );
            progressLp.topMargin = dpToPx(anchor, 8);
            card.addView(progress, progressLp);

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            int margin = dpToPx(anchor, 16);
            lp.leftMargin = margin;
            lp.rightMargin = margin;
            lp.topMargin = getTimelineTopOffset(anchor) + dpToPx(anchor, 10);
            lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            parent.addView(card, lp);

            sTimelineGapFillProgressView = card;
            sTimelineGapFillProgressText = label;
            sTimelineGapFillProgressBar = progress;
            log("Timeline gap-fill progress shown source=" + source);
            return true;
        } catch (Throwable t) {
            log("Timeline gap-fill progress create error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static FrameLayout findTimelineOverlayParent(View view) {
        try {
            if (view == null) return null;
            View root = view.getRootView();
            if (root != null) {
                View content = root.findViewById(android.R.id.content);
                if (content instanceof FrameLayout) return (FrameLayout) content;
                if (root instanceof FrameLayout) return (FrameLayout) root;
            }
            return findTimelineMarkerParent(view);
        } catch (Throwable ignored) {
            return findTimelineMarkerParent(view);
        }
    }

    private static void removeTimelineGapFillProgressView(String source) {
        try {
            if (sTimelineGapFillProgressView != null) {
                Object parent = sTimelineGapFillProgressView.getParent();
                if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(sTimelineGapFillProgressView);
                }
            }
        } catch (Throwable t) {
            log("Timeline gap-fill progress remove error source=" + source + ": " + t.getMessage());
        } finally {
            sTimelineGapFillProgressView = null;
            sTimelineGapFillProgressText = null;
            sTimelineGapFillProgressBar = null;
        }
    }

    private static boolean ensureTimelineTimeJumpButton(Object recyclerView, String source) {
        try {
            if (!isTimelineRecyclerView(recyclerView) || !(recyclerView instanceof View)) return false;
            View anchor = (View) recyclerView;
            FrameLayout parent = findTimelineOverlayParent(anchor);
            if (parent == null) return false;
            Activity activity = findHostActivity(anchor.getContext());
            if (sTimelineTimeJumpButton != null && sTimelineTimeJumpActivity == activity) {
                sTimelineTimeJumpRecyclerView = recyclerView;
                sTimelineTimeJumpButton.bringToFront();
                return true;
            }
            removeTimelineTimeJumpButton("reparent");

            Context context = anchor.getContext();
            TextView button = new TextView(context);
            button.setText("跳转");
            button.setTextColor(Color.WHITE);
            button.setTextSize(13f);
            button.setGravity(Gravity.CENTER);
            button.setIncludeFontPadding(false);
            button.setClickable(true);
            button.setFocusable(true);
            button.setContentDescription("ReWeibo 跳转");
            int paddingH = dpToPx(anchor, 12);
            button.setPadding(paddingH, 0, paddingH, 0);

            GradientDrawable background = new GradientDrawable();
            background.setColor(0xE8467BF3);
            background.setCornerRadius(dpToPx(anchor, 18));
            background.setStroke(Math.max(1, dpToPx(anchor, 1)), 0x66FFFFFF);
            button.setBackground(background);
            button.setAlpha(0.96f);
            button.setElevation(dpToPx(anchor, 6));
            button.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getActionMasked();
                    if (action == MotionEvent.ACTION_DOWN) {
                        v.setAlpha(0.76f);
                        return true;
                    }
                    if (action == MotionEvent.ACTION_UP) {
                        v.setAlpha(0.96f);
                        Object currentRecyclerView = isTimelineRecyclerView(sTimelineTimeJumpRecyclerView)
                            ? sTimelineTimeJumpRecyclerView
                            : recyclerView;
                        log("Timeline time-jump button tapped");
                        showTimelineTimeJumpDialog(currentRecyclerView);
                        return true;
                    }
                    if (action == MotionEvent.ACTION_CANCEL) {
                        v.setAlpha(0.96f);
                        return true;
                    }
                    return true;
                }
            });

            if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                try {
                    WindowManager.LayoutParams wparams = new WindowManager.LayoutParams(
                        dpToPx(anchor, 72),
                        dpToPx(anchor, 44),
                        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                    );
                    wparams.gravity = Gravity.TOP | Gravity.START;
                    wparams.x = dpToPx(anchor, 16);
                    wparams.y = dpToPx(anchor, 108);
                    wparams.token = activity.getWindow().getDecorView().getWindowToken();

                    WindowManager wm = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
                    wm.addView(button, wparams);
                    sTimelineTimeJumpButton = button;
                    sTimelineTimeJumpWindowManager = wm;
                    sTimelineTimeJumpActivity = activity;
                    sTimelineTimeJumpRecyclerView = recyclerView;
                    log("Timeline time-jump button shown source=" + source + " via WindowManager");
                    return true;
                } catch (Throwable t) {
                    log("Timeline time-jump WindowManager failed source=" + source + ": " + t.getMessage());
                }
            }

            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                dpToPx(anchor, 72),
                dpToPx(anchor, 44)
            );
            lp.gravity = Gravity.TOP | Gravity.START;
            lp.topMargin = dpToPx(anchor, 108);
            lp.leftMargin = dpToPx(anchor, 16);
            parent.addView(button, lp);
            sTimelineTimeJumpButton = button;
            sTimelineTimeJumpWindowManager = null;
            sTimelineTimeJumpActivity = activity;
            sTimelineTimeJumpRecyclerView = recyclerView;
            log("Timeline time-jump button shown source=" + source + " via parent");
            return true;
        } catch (Throwable t) {
            log("Timeline time-jump button error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static void removeTimelineTimeJumpButton(String source) {
        try {
            if (sTimelineTimeJumpButton != null) {
                if (sTimelineTimeJumpWindowManager != null) {
                    sTimelineTimeJumpWindowManager.removeViewImmediate(sTimelineTimeJumpButton);
                } else {
                    Object parent = sTimelineTimeJumpButton.getParent();
                    if (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).removeView(sTimelineTimeJumpButton);
                    }
                }
            }
        } catch (Throwable t) {
            log("Timeline time-jump button remove error source=" + source + ": " + t.getMessage());
        } finally {
            sTimelineTimeJumpButton = null;
            sTimelineTimeJumpWindowManager = null;
            sTimelineTimeJumpActivity = null;
            sTimelineTimeJumpRecyclerView = null;
        }
    }

    private static void showTimelineTimeJumpDialog(final Object recyclerView) {
        try {
            if (!isTimelineRecyclerView(recyclerView) || !(recyclerView instanceof View)) return;
            View anchor = (View) recyclerView;
            final Activity activity = findHostActivity(anchor.getContext());
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                log("Timeline time-jump dialog skipped no activity");
                return;
            }

            final TimelineCacheStats cacheStats = buildBestTimelineCacheStats(sLastTimelinePresenter);
            final boolean hasCacheRange = hasTimelineTimeJumpCacheRange(cacheStats);
            final String cacheRangeText = formatTimelineTimeJumpCacheRange(cacheStats);

            LinearLayout container = new LinearLayout(activity);
            container.setOrientation(LinearLayout.VERTICAL);
            int padding = dpToPx(anchor, 20);
            container.setPadding(padding, dpToPx(anchor, 6), padding, 0);

            TextView hint = new TextView(activity);
            hint.setText("输入目标微博时间，支持 07-09 18:30、18:30");
            hint.setTextSize(13f);
            hint.setTextColor(0xFFB8BCC6);
            container.addView(hint, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView cacheRange = new TextView(activity);
            cacheRange.setText(hasCacheRange
                ? "当前缓存可跳转范围（" + cacheStats.datedCount + " 条）\n" + cacheRangeText
                : "当前缓存尚未读取到可跳转的时间范围");
            cacheRange.setTextSize(14f);
            cacheRange.setTextColor(0xFFDCE6FF);
            int rangePaddingH = dpToPx(anchor, 12);
            int rangePaddingV = dpToPx(anchor, 10);
            cacheRange.setPadding(rangePaddingH, rangePaddingV, rangePaddingH, rangePaddingV);
            GradientDrawable rangeBackground = new GradientDrawable();
            rangeBackground.setColor(0xFF242936);
            rangeBackground.setCornerRadius(dpToPx(anchor, 8));
            rangeBackground.setStroke(Math.max(1, dpToPx(anchor, 1)), 0xFF536A9F);
            cacheRange.setBackground(rangeBackground);
            LinearLayout.LayoutParams rangeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rangeLp.topMargin = dpToPx(anchor, 10);
            container.addView(cacheRange, rangeLp);

            final EditText input = new EditText(activity);
            input.setSingleLine(true);
            input.setHint(hasCacheRange ? cacheRangeText : "例如 07-09 18:30");
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(0xFF8F96A3);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            inputLp.topMargin = dpToPx(anchor, 8);
            container.addView(input, inputLp);

            final AlertDialog dialog = new AlertDialog.Builder(
                activity,
                android.R.style.Theme_Material_Dialog_Alert
            )
                .setTitle("按时间跳转")
                .setView(container)
                .setPositiveButton("跳转", null)
                .setNegativeButton("取消", null)
                .create();
            dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
                @Override
                public void onShow(android.content.DialogInterface ignored) {
                    dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String text = input.getText() == null ? "" : input.getText().toString();
                                long targetMs = parseTimelineJumpInputMillis(text);
                                if (targetMs <= 0L) {
                                    input.setError("时间格式不对");
                                    return;
                                }
                                if (hasCacheRange
                                    && !isTimelineJumpInputWithinCacheRange(text, targetMs, cacheStats)) {
                                    input.setError("超出缓存范围：" + cacheRangeText);
                                    return;
                                }
                                boolean accepted = jumpTimelineToTime(
                                    recyclerView,
                                    targetMs,
                                    "time-dialog",
                                    0
                                );
                                if (accepted) {
                                    dialog.dismiss();
                                } else {
                                    input.setError("当前缓存里没有这个时间附近的微博");
                                }
                            }
                        });
                }
            });
            dialog.show();
            GradientDrawable dialogBackground = new GradientDrawable();
            dialogBackground.setColor(0xFF17181C);
            dialogBackground.setCornerRadius(dpToPx(anchor, 12));
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(dialogBackground);
            }
            int alertTitleId = activity.getResources().getIdentifier("alertTitle", "id", "android");
            View alertTitle = alertTitleId == 0 ? null : dialog.findViewById(alertTitleId);
            if (alertTitle instanceof TextView) {
                ((TextView) alertTitle).setTextColor(Color.WHITE);
            }
            dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(0xFFFF5A63);
            dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(0xFFFF5A63);
            input.requestFocus();
            log("Timeline time-jump dialog range count=" + cacheStats.datedCount
                + " oldest=" + formatTimelineJumpRangeTime(cacheStats.oldestMs)
                + " newest=" + formatTimelineJumpRangeTime(cacheStats.newestMs));
        } catch (Throwable t) {
            log("Timeline time-jump dialog error: " + t.getMessage());
        }
    }

    private static Activity findHostActivity(Context context) {
        Context current = context;
        while (current != null) {
            if (current instanceof Activity) return (Activity) current;
            if (current instanceof ContextWrapper) {
                current = ((ContextWrapper) current).getBaseContext();
            } else {
                break;
            }
        }
        return null;
    }

    private static long parseTimelineJumpInputMillis(String value) {
        if (!hasMeaningfulString(value)) return 0L;
        String text = normalizeTimelineJumpInput(value);
        long parsed = parseTimelineJumpWithPatterns(text, new String[] {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd"
        });
        if (parsed > 0L) return parsed;

        Calendar now = Calendar.getInstance();
        if (text.startsWith("今天") || text.startsWith("今日")) {
            String time = text.substring(2).trim();
            return parseTimelineJumpWithToday(now, time, false);
        }
        if (text.startsWith("昨天")) {
            String time = text.substring(2).trim();
            return parseTimelineJumpWithToday(now, time, true);
        }
        if (text.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
            return parseTimelineJumpWithToday(now, text, false);
        }
        if (text.matches("\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{2}(:\\d{2})?")) {
            int year = now.get(Calendar.YEAR);
            parsed = parseTimelineJumpWithPatterns(year + "-" + text, new String[] {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm"
            });
            if (parsed > now.getTimeInMillis() + TIMELINE_CACHE_DAY_MS) {
                parsed = parseTimelineJumpWithPatterns((year - 1) + "-" + text, new String[] {
                    "yyyy-MM-dd HH:mm:ss",
                    "yyyy-MM-dd HH:mm"
                });
            }
            return parsed;
        }
        if (text.matches("\\d{1,2}-\\d{1,2}")) {
            int year = now.get(Calendar.YEAR);
            parsed = parseTimelineJumpWithPatterns(year + "-" + text, new String[] {"yyyy-MM-dd"});
            if (parsed > now.getTimeInMillis() + TIMELINE_CACHE_DAY_MS) {
                parsed = parseTimelineJumpWithPatterns((year - 1) + "-" + text, new String[] {"yyyy-MM-dd"});
            }
            return parsed;
        }
        return 0L;
    }

    private static String normalizeTimelineJumpInput(String value) {
        String text = value == null ? "" : value.trim();
        text = text.replace('：', ':')
            .replace('/', '-')
            .replace('.', '-')
            .replace("年", "-")
            .replace("月", "-")
            .replace("日", " ");
        while (text.contains("  ")) {
            text = text.replace("  ", " ");
        }
        return text.trim();
    }

    private static long parseTimelineJumpWithToday(Calendar now, String time, boolean yesterday) {
        if (!hasMeaningfulString(time)) return 0L;
        Calendar day = (Calendar) now.clone();
        if (yesterday) {
            day.add(Calendar.DAY_OF_MONTH, -1);
        }
        String date = formatTimelineJumpDate(day);
        long parsed = parseTimelineJumpWithPatterns(date + " " + time, new String[] {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm"
        });
        if (!yesterday && parsed > now.getTimeInMillis() + TIME_JUMP_FUTURE_GRACE_MS) {
            day.add(Calendar.DAY_OF_MONTH, -1);
            parsed = parseTimelineJumpWithPatterns(formatTimelineJumpDate(day) + " " + time, new String[] {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm"
            });
        }
        return parsed;
    }

    private static String formatTimelineJumpDate(Calendar day) {
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            day.get(Calendar.YEAR),
            day.get(Calendar.MONTH) + 1,
            day.get(Calendar.DAY_OF_MONTH)
        );
    }

    private static long parseTimelineJumpWithPatterns(String text, String[] patterns) {
        if (!hasMeaningfulString(text)) return 0L;
        for (int i = 0; i < patterns.length; i++) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(patterns[i], Locale.CHINA);
                format.setLenient(false);
                ParsePosition position = new ParsePosition(0);
                Date date = format.parse(text, position);
                if (date != null && position.getIndex() == text.length()) {
                    return date.getTime();
                }
            } catch (Throwable ignored) {
            }
        }
        return 0L;
    }

    private static boolean jumpTimelineToTime(
        final Object recyclerView,
        final long targetMs,
        final String source,
        final int attempt
    ) {
        try {
            if (!isTimelineRecyclerView(recyclerView)) return false;
            final TimelineTimeJumpTarget target = findTimelineTimeJumpTarget(recyclerView, targetMs);
            if (target == null || target.statusId <= 0L) {
                showTimelineToast(recyclerView, "当前已加载/缓存中没有可跳转的微博");
                log("Timeline time-jump missed source=" + source + " target=" + targetMs
                    + " " + describeTimelineTimeJumpSearch(recyclerView));
                return false;
            }
            if (target.diffMs > TIME_JUMP_MAX_ACCEPT_DIFF_MS) {
                showTimelineToast(recyclerView, "当前没有这个时间附近的微博");
                log("Timeline time-jump rejected stale target source=" + source
                    + " id=" + target.statusId
                    + " input=" + formatTimelineJumpTime(targetMs)
                    + " actual=" + formatTimelineJumpTime(target.createdMs)
                    + " diffMin=" + (target.diffMs / 60000L)
                    + " " + describeTimelineTimeJumpSearch(recyclerView));
                return false;
            }
            final Object targetRecyclerView = isTimelineRecyclerView(target.recyclerView)
                ? target.recyclerView
                : recyclerView;
            if (target.adapterPosition < 0) {
                if (attempt < TIME_JUMP_MAX_ATTEMPTS) {
                    if (attempt == 0) {
                        restoreTimelineCumulativeCache(sLastTimelinePresenter, null, source + "-restore");
                        showTimelineToast(targetRecyclerView, "正在恢复缓存后跳转");
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            jumpTimelineToTime(targetRecyclerView, targetMs, source + "-retry", attempt + 1);
                        }
                    }, TIME_JUMP_RETRY_MS * 3);
                    return true;
                }
                showTimelineToast(targetRecyclerView, "命中缓存，但当前列表还没恢复");
                log("Timeline time-jump target not in adapter source=" + source
                    + " id=" + target.statusId
                    + " created=" + target.createdMs
                    + " searched=" + target.searchedCount);
                return false;
            }

            sTimelineRefreshAnchorStatusId = 0L;
            sTimelineRefreshAnchorUntilMs = 0L;
            sTimelineRefreshAnchorPosition = -1;
            cancelTimelineTopAnchorsForRefresh(source);

            Object layoutManager = XposedHelpers.callMethod(targetRecyclerView, "getLayoutManager");
            int offset = getTimelineTopOffset(targetRecyclerView);
            boolean usedOffset = false;
            try {
                int layoutOffset = sTimelineRestoredCacheMode
                    ? getTimelineLayoutManagerOffset(targetRecyclerView, layoutManager, offset)
                    : offset;
                XposedHelpers.callMethod(layoutManager, "scrollToPositionWithOffset", target.adapterPosition, layoutOffset);
                usedOffset = true;
            } catch (Throwable ignored) {
                XposedHelpers.callMethod(targetRecyclerView, "scrollToPosition", target.adapterPosition);
            }
            if (!isTimelineTargetVisible(layoutManager, target.adapterPosition)) {
                try {
                    XposedHelpers.callMethod(targetRecyclerView, "scrollToPosition", target.adapterPosition);
                } catch (Throwable ignored) {}
                scrollTimelineTowardTarget(targetRecyclerView, layoutManager, target.adapterPosition);
            }

            boolean visible = isTimelineTargetVisible(layoutManager, target.adapterPosition);
            log("Timeline time-jumped source=" + source
                + " id=" + target.statusId
                + " input=" + formatTimelineJumpTime(targetMs)
                + " actual=" + formatTimelineJumpTime(target.createdMs)
                + " diffMin=" + (target.diffMs / 60000L)
                + " adapterPosition=" + target.adapterPosition
                + " dataPosition=" + target.dataPosition
                + " attempt=" + attempt
                + " offset=" + offset
                + " usedOffset=" + usedOffset
                + " visible=" + visible + " "
                + describeTimelineViewport(targetRecyclerView, layoutManager));
            if (!visible && attempt < TIME_JUMP_MAX_ATTEMPTS) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        jumpTimelineToTime(targetRecyclerView, targetMs, source, attempt + 1);
                    }
                }, TIME_JUMP_RETRY_MS);
            }
            if (attempt == 0) {
                showTimelineToast(targetRecyclerView, "已跳到 " + formatTimelineJumpTime(target.createdMs) + " 附近");
            }
            return true;
        } catch (Throwable t) {
            log("Timeline time-jump error source=" + source + ": " + t.getMessage());
            return false;
        }
    }

    private static String describeTimelineTimeJumpSearch(Object recyclerView) {
        int adapterCount = -1;
        int adapterDated = 0;
        String adapterSample = "-";
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            adapterCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getCount", -1);
            for (int i = 0; i < adapterCount; i++) {
                Object item = XposedHelpers.callMethod(adapter, "getItem", i);
                Object status = unwrapStatus(item);
                long createdMs = getStatusCreatedAtMillis(status);
                if (createdMs > 0L) {
                    adapterDated++;
                    if ("-".equals(adapterSample)) {
                        adapterSample = getStringMethodOrField(status, "getCreated_at", "created_at")
                            + "/" + formatTimelineJumpTime(createdMs);
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        int cumulativeCount;
        int cumulativeDated = 0;
        String cumulativeSample = "-";
        ArrayList snapshot;
        synchronized (sTimelineCumulativeStatusesById) {
            cumulativeCount = sTimelineCumulativeStatusesById.size();
            snapshot = new ArrayList(sTimelineCumulativeStatusesById.values());
        }
        for (int i = 0; i < snapshot.size(); i++) {
            Object status = unwrapStatus(snapshot.get(i));
            long createdMs = getStatusCreatedAtMillis(status);
            if (createdMs > 0L) {
                cumulativeDated++;
                if ("-".equals(cumulativeSample)) {
                    cumulativeSample = getStringMethodOrField(status, "getCreated_at", "created_at")
                        + "/" + formatTimelineJumpTime(createdMs);
                }
            }
        }
        return "adapterCount=" + adapterCount
            + " adapterDated=" + adapterDated
            + " adapterSample=" + adapterSample
            + " cumulativeCount=" + cumulativeCount
            + " cumulativeDated=" + cumulativeDated
            + " cumulativeSample=" + cumulativeSample;
    }

    private static TimelineTimeJumpTarget findTimelineTimeJumpTarget(Object recyclerView, long targetMs) {
        TimelineTimeJumpTarget adapterBest = null;
        TimelineTimeJumpTarget cumulativeBest = null;
        ArrayList recyclerViews = getTimelineTimeJumpRecyclerViews(recyclerView);
        for (int r = 0; r < recyclerViews.size(); r++) {
            Object candidateRecyclerView = recyclerViews.get(r);
            try {
                Object adapter = XposedHelpers.callMethod(candidateRecyclerView, "getAdapter");
                int headerCount = adapter == null ? 0 : callIntMethodSafe(adapter, "getHeaderCount", 0);
                int dataCount = adapter == null ? -1 : callIntMethodSafe(adapter, "getCount", -1);
                for (int i = 0; i < dataCount; i++) {
                    Object item = XposedHelpers.callMethod(adapter, "getItem", i);
                    adapterBest = chooseTimelineTimeJumpTarget(
                        adapterBest,
                        unwrapStatus(item),
                        targetMs,
                        Math.max(0, headerCount) + i,
                        i,
                        candidateRecyclerView
                    );
                }
            } catch (Throwable ignored) {
            }
        }

        ArrayList snapshot;
        synchronized (sTimelineCumulativeStatusesById) {
            snapshot = new ArrayList(sTimelineCumulativeStatusesById.values());
        }
        for (int i = 0; i < snapshot.size(); i++) {
            cumulativeBest = chooseTimelineTimeJumpTarget(
                cumulativeBest,
                unwrapStatus(snapshot.get(i)),
                targetMs,
                -1,
                -1,
                null
            );
        }

        if (cumulativeBest != null && cumulativeBest.adapterPosition < 0) {
            for (int r = 0; r < recyclerViews.size(); r++) {
                Object candidateRecyclerView = recyclerViews.get(r);
                try {
                    Object adapter = XposedHelpers.callMethod(candidateRecyclerView, "getAdapter");
                    int headerCount = adapter == null ? 0 : callIntMethodSafe(adapter, "getHeaderCount", 0);
                    int position = getTimelineStatusAdapterPosition(adapter, headerCount, cumulativeBest.statusId);
                    if (position >= 0) {
                        cumulativeBest.adapterPosition = position;
                        cumulativeBest.dataPosition = position - Math.max(0, headerCount);
                        cumulativeBest.recyclerView = candidateRecyclerView;
                        break;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        if (cumulativeBest != null && cumulativeBest.adapterPosition >= 0) {
            if (adapterBest == null || cumulativeBest.diffMs < adapterBest.diffMs) {
                return cumulativeBest;
            }
        }
        if (cumulativeBest != null && (adapterBest == null || cumulativeBest.diffMs < adapterBest.diffMs)) {
            return cumulativeBest;
        }
        if (adapterBest != null) return adapterBest;
        return cumulativeBest;
    }

    private static ArrayList getTimelineTimeJumpRecyclerViews(Object preferredRecyclerView) {
        ArrayList result = new ArrayList();
        addTimelineTimeJumpRecyclerView(result, preferredRecyclerView);
        synchronized (sTopAnchorStates) {
            ArrayList recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
            for (int i = 0; i < recyclerViews.size(); i++) {
                addTimelineTimeJumpRecyclerView(result, recyclerViews.get(i));
            }
        }
        addTimelineTimeJumpRecyclerView(result, getAnyTimelineRecyclerView());
        return result;
    }

    private static void addTimelineTimeJumpRecyclerView(ArrayList list, Object recyclerView) {
        if (recyclerView == null || !isTimelineRecyclerView(recyclerView)) return;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == recyclerView) return;
        }
        list.add(recyclerView);
    }

    private static TimelineTimeJumpTarget chooseTimelineTimeJumpTarget(
        TimelineTimeJumpTarget best,
        Object status,
        long targetMs,
        int adapterPosition,
        int dataPosition,
        Object recyclerView
    ) {
        if (status == null || isLoadMoreStatus(status) || isTimelineAdStatus(status)) {
            return best;
        }
        long statusId = getStatusId(status);
        long createdMs = getStatusCreatedAtMillis(status);
        if (statusId <= 0L || createdMs <= 0L) return best;
        long diff = createdMs >= targetMs ? createdMs - targetMs : targetMs - createdMs;
        if (best != null) best.searchedCount++;
        if (best != null && diff >= best.diffMs) return best;
        TimelineTimeJumpTarget target = best == null ? new TimelineTimeJumpTarget() : best;
        target.statusId = statusId;
        target.createdMs = createdMs;
        target.diffMs = diff;
        target.adapterPosition = adapterPosition;
        target.dataPosition = dataPosition;
        target.recyclerView = recyclerView;
        target.searchedCount = best == null ? 1 : best.searchedCount;
        return target;
    }

    private static String formatTimelineJumpTime(long millis) {
        if (millis <= 0L) return "-";
        try {
            return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(millis));
        } catch (Throwable ignored) {
            return String.valueOf(millis);
        }
    }

    private static boolean hasTimelineTimeJumpCacheRange(TimelineCacheStats stats) {
        return stats != null
            && stats.datedCount > 0
            && stats.oldestMs > 0L
            && stats.newestMs >= stats.oldestMs;
    }

    private static String formatTimelineTimeJumpCacheRange(TimelineCacheStats stats) {
        if (!hasTimelineTimeJumpCacheRange(stats)) return "暂无可用范围";
        return formatTimelineJumpRangeTime(stats.oldestMs)
            + " ～ "
            + formatTimelineJumpRangeTime(stats.newestMs);
    }

    private static boolean isTimelineJumpInputWithinCacheRange(
        String value,
        long targetMs,
        TimelineCacheStats stats
    ) {
        if (targetMs <= 0L || !hasTimelineTimeJumpCacheRange(stats)) return true;
        String normalized = normalizeTimelineJumpInput(value);
        boolean dateOnly = normalized.matches("\\d{4}-\\d{1,2}-\\d{1,2}")
            || normalized.matches("\\d{1,2}-\\d{1,2}");
        if (dateOnly) {
            Calendar endOfDay = Calendar.getInstance();
            endOfDay.setTimeInMillis(targetMs);
            endOfDay.add(Calendar.DAY_OF_MONTH, 1);
            long dayEndMs = endOfDay.getTimeInMillis() - 1L;
            return targetMs <= stats.newestMs && dayEndMs >= stats.oldestMs;
        }

        long minuteMs = 60000L;
        long rangeStartMs = stats.oldestMs - (stats.oldestMs % minuteMs);
        long rangeEndMs = stats.newestMs - (stats.newestMs % minuteMs) + minuteMs - 1L;
        return targetMs >= rangeStartMs && targetMs <= rangeEndMs;
    }

    private static String formatTimelineJumpRangeTime(long millis) {
        if (millis <= 0L) return "-";
        try {
            return new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date(millis));
        } catch (Throwable ignored) {
            return String.valueOf(millis);
        }
    }

    private static void showTimelineToast(Object recyclerView, final String text) {
        try {
            if (!(recyclerView instanceof View) || !hasMeaningfulString(text)) return;
            final Context context = ((View) recyclerView).getContext();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static long getVisibleTimelineStatusId(Object recyclerView, int[] viewport) {
        try {
            if (!isTimelineRecyclerView(recyclerView)) return 0L;
            Object layoutManager = XposedHelpers.callMethod(recyclerView, "getLayoutManager");
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            int headerCount = adapter == null ? 0 : callIntMethodSafe(adapter, "getHeaderCount", 0);
            int first = callIntMethodSafe(layoutManager, "findFirstVisibleItemPosition", -1);
            int last = callIntMethodSafe(layoutManager, "findLastVisibleItemPosition", -1);
            int position = findVisibleTimelineStatusPosition(adapter, headerCount, first, last);
            if (viewport != null && viewport.length >= 3) {
                viewport[0] = position;
                viewport[1] = first;
                viewport[2] = last;
            }
            if (position < 0) return 0L;
            return getTimelineAdapterDataStatusId(adapter, position - Math.max(0, headerCount));
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static boolean scheduleTimelineRefreshAnchorForKnownRecyclerViews(String source) {
        long statusId = getActiveTimelineRefreshAnchorStatusId();
        if (statusId <= 0L) return false;

        cancelTimelineTopAnchorsForRefresh(source);
        ArrayList recyclerViews;
        synchronized (sTopAnchorStates) {
            recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
        }
        boolean scheduled = false;
        int generation = sTimelineRefreshAnchorGeneration;
        for (int i = 0; i < recyclerViews.size(); i++) {
            Object recyclerView = recyclerViews.get(i);
            if (!isTimelineRecyclerView(recyclerView)) continue;
            scheduled = true;
            scheduleTimelineRefreshAnchor(recyclerView, statusId, generation, source, 0, 50L);
            scheduleTimelineRefreshAnchor(recyclerView, statusId, generation, source, 1, 250L);
            scheduleTimelineRefreshAnchor(recyclerView, statusId, generation, source, 2, 900L);
        }
        if (!scheduled) {
            log("Timeline refresh anchor pending source=" + source + " id=" + statusId + " no recycler");
        }
        return scheduled;
    }

    private static void scheduleTimelineRefreshAnchor(
        final Object recyclerView,
        final long statusId,
        final int generation,
        final String source,
        final int attempt,
        long delayMs
    ) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                anchorTimelineRefreshPosition(recyclerView, statusId, generation, source, attempt);
            }
        }, delayMs);
    }

    private static void anchorTimelineRefreshPosition(
        final Object recyclerView,
        final long statusId,
        final int generation,
        final String source,
        final int attempt
    ) {
        try {
            if (statusId <= 0L || statusId != getActiveTimelineRefreshAnchorStatusId()) return;
            if (generation != sTimelineRefreshAnchorGeneration) return;
            if (!isTimelineRecyclerView(recyclerView)) return;

            int target = getTimelineStatusAdapterPosition(recyclerView, statusId);
            if (target < 0) {
                if (attempt < REFRESH_ANCHOR_MAX_ATTEMPTS) {
                    scheduleTimelineRefreshAnchor(recyclerView, statusId, generation, source, attempt + 1, 180L);
                } else {
                    log("Timeline refresh anchor target missing source=" + source + " id=" + statusId);
                }
                return;
            }

            Object layoutManager = XposedHelpers.callMethod(recyclerView, "getLayoutManager");
            int offset = getTimelineTopOffset(recyclerView);
            boolean usedOffset = false;
            try {
                int layoutOffset = sTimelineRestoredCacheMode
                    ? getTimelineLayoutManagerOffset(recyclerView, layoutManager, offset)
                    : offset;
                XposedHelpers.callMethod(layoutManager, "scrollToPositionWithOffset", target, layoutOffset);
                usedOffset = true;
            } catch (Throwable ignored) {
                XposedHelpers.callMethod(recyclerView, "scrollToPosition", target);
            }

            if (!isTimelineTargetVisible(layoutManager, target)) {
                scrollTimelineTowardTarget(recyclerView, layoutManager, target);
            }

            if (isTimelineTargetVisible(layoutManager, target)) {
                consumeTimelineRefreshAnchor(statusId);
                cancelTimelineTopAnchorsForRefresh(source + "-done");
                log("Timeline refresh anchor restored source=" + source
                    + " id=" + statusId
                    + " target=" + target
                    + " attempt=" + attempt
                    + " offset=" + offset
                    + " usedOffset=" + usedOffset
                    + " " + describeTimelineViewport(recyclerView, layoutManager));
                return;
            }

            if (attempt < REFRESH_ANCHOR_MAX_ATTEMPTS) {
                scheduleTimelineRefreshAnchor(recyclerView, statusId, generation, source, attempt + 1, 180L);
            } else {
                log("Timeline refresh anchor not visible source=" + source
                    + " id=" + statusId
                    + " target=" + target
                    + " " + describeTimelineViewport(recyclerView, layoutManager));
            }
        } catch (Throwable t) {
            log("Timeline refresh anchor error source=" + source + ": " + t.getMessage());
        }
    }

    private static int getTimelineStatusAdapterPosition(Object recyclerView, long statusId) {
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            int headerCount = adapter == null ? 0 : callIntMethodSafe(adapter, "getHeaderCount", 0);
            return getTimelineStatusAdapterPosition(adapter, headerCount, statusId);
        } catch (Throwable ignored) {
            return -1;
        }
    }

    private static int getTimelineStatusAdapterPosition(Object adapter, int headerCount, long statusId) {
        if (adapter == null || statusId <= 0L) return -1;
        int dataCount = callIntMethodSafe(adapter, "getCount", -1);
        if (dataCount < 1) return -1;
        for (int i = 0; i < dataCount; i++) {
            if (getTimelineAdapterDataStatusId(adapter, i) == statusId) {
                return Math.max(0, headerCount) + i;
            }
        }
        return -1;
    }

    private static boolean hasTimelineStatusInCumulativeCache(long statusId) {
        if (statusId <= 0L) return false;
        synchronized (sTimelineCumulativeStatusesById) {
            return sTimelineCumulativeStatusesById.containsKey(Long.valueOf(statusId));
        }
    }

    private static long getActiveTimelineRefreshAnchorStatusId() {
        if (sTimelineRefreshAnchorStatusId <= 0L) return 0L;
        if (SystemClock.elapsedRealtime() <= sTimelineRefreshAnchorUntilMs) {
            return sTimelineRefreshAnchorStatusId;
        }
        sTimelineRefreshAnchorStatusId = 0L;
        sTimelineRefreshAnchorUntilMs = 0L;
        sTimelineRefreshAnchorPosition = -1;
        return 0L;
    }

    private static void consumeTimelineRefreshAnchor(long statusId) {
        if (statusId == sTimelineRefreshAnchorStatusId) {
            sTimelineRefreshAnchorStatusId = 0L;
            sTimelineRefreshAnchorUntilMs = 0L;
            sTimelineRefreshAnchorPosition = -1;
        }
    }

    private static void cancelTimelineTopAnchorsForRefresh(String source) {
        synchronized (sTopAnchorStates) {
            ArrayList recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
            for (int i = 0; i < recyclerViews.size(); i++) {
                Object recyclerView = recyclerViews.get(i);
                TopAnchorState state = getTopAnchorStateLocked(recyclerView);
                state.generation++;
                state.userTouched = true;
                state.untilElapsedMs = 0L;
                state.attempts = TOP_ANCHOR_MAX_ATTEMPTS;
                state.finishing = false;
            }
        }
        sSuppressTimelineLoadMoreUntilMs = 0L;
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

    private static void jumpTimelineToAbsoluteBottomForKnownRecyclerViews(String source) {
        ArrayList recyclerViews;
        synchronized (sTopAnchorStates) {
            recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
        }
        boolean jumped = false;
        for (int i = 0; i < recyclerViews.size(); i++) {
            jumped |= jumpTimelineToAbsoluteBottom(recyclerViews.get(i), source, 0);
        }
        if (!jumped) {
            log("Timeline absolute bottom jump skipped source=" + source + " no recycler");
        }
    }

    private static boolean jumpTimelineToAbsoluteTop(final Object recyclerView, final String source, final int attempt) {
        return jumpTimelineToAbsoluteEdge(recyclerView, source, attempt, true);
    }

    private static boolean jumpTimelineToAbsoluteBottom(final Object recyclerView, final String source, final int attempt) {
        return jumpTimelineToAbsoluteEdge(recyclerView, source, attempt, false);
    }

    private static boolean jumpTimelineToAbsoluteEdge(
        final Object recyclerView,
        final String source,
        final int attempt,
        final boolean top
    ) {
        try {
            if (!isTimelineRecyclerView(recyclerView)) return false;

            int target = top
                ? getTimelineAbsoluteTopAdapterPosition(recyclerView)
                : getTimelineAbsoluteBottomAdapterPosition(recyclerView);
            String edge = top ? "top" : "bottom";
            if (target < 0) {
                log("Timeline absolute " + edge + " jump skipped source=" + source + " no target");
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
            log("Timeline absolute " + edge + " jumped source=" + source + " target=" + target
                + " attempt=" + attempt + " offset=" + offset + " usedOffset=" + usedOffset + " "
                + describeTimelineViewport(recyclerView, layoutManager));
            if (!isTimelineTargetVisible(layoutManager, target) && attempt < TOP_BAR_JUMP_MAX_ATTEMPTS) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        jumpTimelineToAbsoluteEdge(recyclerView, source, attempt + 1, top);
                    }
                }, TOP_BAR_JUMP_RETRY_MS);
            }
            return true;
        } catch (Throwable t) {
            log("Timeline absolute " + (top ? "top" : "bottom")
                + " jump error source=" + source + ": " + t.getMessage());
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

    private static int getTimelineAbsoluteBottomAdapterPosition(Object recyclerView) {
        try {
            Object adapter = XposedHelpers.callMethod(recyclerView, "getAdapter");
            if (adapter == null) return -1;

            int dataCount = callIntMethodSafe(adapter, "getCount", -1);
            int headerCount = callIntMethodSafe(adapter, "getHeaderCount", 0);
            if (dataCount >= TIMELINE_CACHE_MIN_ITEMS) {
                if (sTimelineOldestFirstMode) return Math.max(0, headerCount) + dataCount - 1;
                return Math.max(0, headerCount);
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

    private static void suspendTimelineTopAnchorsForPreload(String source) {
        synchronized (sTopAnchorStates) {
            ArrayList recyclerViews = new ArrayList(sTimelineRecyclerViews.keySet());
            for (int i = 0; i < recyclerViews.size(); i++) {
                TopAnchorState state = getTopAnchorStateLocked(recyclerViews.get(i));
                state.generation++;
                state.attempts = TOP_ANCHOR_MAX_ATTEMPTS;
                state.untilElapsedMs = 0L;
                state.finishing = false;
            }
        }
        sSuppressTimelineLoadMoreUntilMs = 0L;
        log("Timeline top anchors suspended source=" + source);
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
            if (!shouldUseTimelineLastRead(sTimelineRestoredCacheMode, isTimelinePreloadDone())) return -1;
            if (adapter == null) return -1;

            int dataCount = callIntMethodSafe(adapter, "getCount", -1);
            if (dataCount < 1) return -1;
            List<Long> candidates = getTimelineLastReadStatusCandidates();
            if (candidates.isEmpty()) return -1;
            LinkedHashMap<Long, Integer> ranks = new LinkedHashMap<>();
            for (int i = 0; i < candidates.size(); i++) {
                Long id = candidates.get(i);
                if (id != null && id.longValue() > 0L && !ranks.containsKey(id)) {
                    ranks.put(id, Integer.valueOf(i));
                }
            }
            int bestRank = Integer.MAX_VALUE;
            int bestPosition = -1;
            long bestStatusId = 0L;
            for (int i = 0; i < dataCount; i++) {
                long itemId = getTimelineAdapterDataStatusId(adapter, i);
                Integer rank = ranks.get(Long.valueOf(itemId));
                if (rank != null && rank.intValue() < bestRank) {
                    bestRank = rank.intValue();
                    bestPosition = Math.max(0, headerCount) + i;
                    bestStatusId = itemId;
                    if (bestRank == 0) break;
                }
            }
            if (bestPosition >= 0 && bestStatusId != getLastReadStatusId()) {
                sLastReadStatusId = Long.valueOf(bestStatusId);
                log("Timeline last-read history fallback selected id=" + bestStatusId
                    + " rank=" + bestRank + " position=" + bestPosition);
            }
            return bestPosition;
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
            if (!shouldUseTimelineLastRead(sTimelineRestoredCacheMode, isTimelinePreloadDone())) return;
            if (getActiveTimelineRefreshAnchorStatusId() > 0L) {
                log("Timeline last-read save skipped source=" + source + " reason=refresh-anchor");
                return;
            }
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
            long previousStatusId = readTrustedTimelineLastReadStatusId(file);
            if (previousStatusId > 0L && previousStatusId != statusId) {
                appendTimelineLastReadHistory(previousStatusId);
            }
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            StringBuilder content = new StringBuilder();
            content.append("saved_at=").append(ts).append('\n');
            content.append("source=").append(source).append('\n');
            content.append("preload_done=true\n");
            content.append("cache_count=").append(dataCount).append('\n');
            content.append("status_id=").append(statusId).append('\n');
            content.append("adapter_position=").append(position).append('\n');
            content.append("data_position=").append(dataPosition).append('\n');
            writeTextFileAtomically(file, content.toString());

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
        long statusId = readTrustedTimelineLastReadStatusId(file);
        sLastReadStatusId = Long.valueOf(statusId);
        return statusId;
    }

    private static long readTrustedTimelineLastReadStatusId(File file) {
        if (file == null || !file.exists()) return 0L;
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
        return statusId;
    }

    private static List<Long> getTimelineLastReadStatusCandidates() {
        ArrayList<Long> candidates = new ArrayList<>();
        long current = getLastReadStatusId();
        if (current > 0L) candidates.add(Long.valueOf(current));

        File history = getLastReadHistoryFile();
        if (history == null || !history.exists()) return candidates;
        ArrayList<Long> stored = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(history));
            String line;
            while ((line = reader.readLine()) != null) {
                int separator = line.lastIndexOf('|');
                String value = separator >= 0 ? line.substring(separator + 1) : line;
                long id = Long.parseLong(value.trim());
                if (id > 0L) stored.add(Long.valueOf(id));
            }
        } catch (Throwable ignored) {
        } finally {
            closeQuietly(reader);
        }
        for (int i = stored.size() - 1; i >= 0; i--) {
            Long id = stored.get(i);
            if (!candidates.contains(id)) candidates.add(id);
        }
        return candidates;
    }

    private static void appendTimelineLastReadHistory(long statusId) {
        if (statusId <= 0L) return;
        File history = getLastReadHistoryFile();
        ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            if (history.exists()) {
                reader = new BufferedReader(new FileReader(history));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().length() > 0) lines.add(line);
                }
            }
        } catch (Throwable ignored) {
        } finally {
            closeQuietly(reader);
        }

        String suffix = "|" + statusId;
        if (!lines.isEmpty() && lines.get(lines.size() - 1).endsWith(suffix)) return;
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
        lines.add(ts + suffix);
        int from = Math.max(0, lines.size() - LAST_READ_HISTORY_LIMIT);
        try {
            StringBuilder content = new StringBuilder();
            for (int i = from; i < lines.size(); i++) {
                content.append(lines.get(i)).append('\n');
            }
            writeTextFileAtomically(history, content.toString());
        } catch (Throwable t) {
            log("Timeline last-read history write error: " + t.getMessage());
        }
    }

    private static boolean isTimelineLastReadTarget(Object recyclerView, int target) {
        try {
            if (!shouldUseTimelineLastRead(sTimelineRestoredCacheMode, isTimelinePreloadDone())) return false;
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

    private static File getLastReadHistoryFile() {
        return new File("/data/data/com.weico.international/files", LAST_READ_HISTORY_FILE);
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

            long nowMs = SystemClock.elapsedRealtime();
            if (sTimelineNoMoreHandledAtMs > 0L
                && nowMs - sTimelineNoMoreHandledAtMs < TIMELINE_NO_MORE_DEDUP_MS) {
                log("Timeline no-more duplicate ignored source=" + source + " count=" + count);
                return;
            }
            sTimelineNoMoreHandledAtMs = nowMs;

            stopTimelineGapFill(source + "-no-more");
            persistTimelineNativeCache(presenter, "no-more-content");
            TimelineCacheStats stats = buildBestTimelineCacheStats(presenter);
            if (!isTimelineCacheDurationReady(stats, source + "-no-more")) {
                log("Timeline no-more kept incomplete source=" + source
                    + " count=" + count
                    + " spanDays=" + formatTimelineCacheSpanDays(stats.spanMs()));
                return;
            }
            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                state.stopped = true;
                state.scheduled = false;
                state.inFlight = false;
                state.stableRounds = PRELOAD_STABLE_DONE_ROUNDS;
                state.lastCount = count;
            }
            markTimelinePreloadDone("no-more-content", 0, count);
            finishTimelineTopAnchorForKnownRecyclerViews("preload-complete");
            log("Timeline no-more marker detected source=" + source + " count=" + count);
        } catch (Throwable t) {
            log("Timeline no-more marker error source=" + source + ": " + t.getMessage());
        }
    }

    private static void deferTimelineNoMoreContent(final String source) {
        final int generation;
        synchronized (WeiboLiteHook.class) {
            sPendingTimelineNoMoreSource = source;
            generation = ++sPendingTimelineNoMoreGeneration;
        }
        log("Timeline no-more deferred until page applied source=" + source);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (WeiboLiteHook.class) {
                    if (generation != sPendingTimelineNoMoreGeneration
                        || sPendingTimelineNoMoreSource == null) {
                        return;
                    }
                }
                consumePendingTimelineNoMoreContent("timeout");
            }
        }, 1500L);
    }

    private static void consumePendingTimelineNoMoreContent(String appliedBy) {
        String source;
        synchronized (WeiboLiteHook.class) {
            source = sPendingTimelineNoMoreSource;
            if (source == null) return;
            sPendingTimelineNoMoreSource = null;
            sPendingTimelineNoMoreGeneration++;
        }
        markTimelineNoMoreContent(source + "-after-" + appliedBy);
    }

    private static void markTimelineNoMoreIfEmptyPage(Object presenter, List incomingData, String source) {
        try {
            if (presenter == null || incomingData == null || !"-1".equals(getTimelineGroupId(presenter))) return;
            int incoming = countTimelineStatuses(incomingData);
            int count = getTimelineStatusCount(presenter);
            if (incoming == 0 && count >= TIMELINE_CACHE_MIN_ITEMS) {
                rememberTimelinePresenter(presenter);
                log("Timeline no-more empty page source=" + source + " incoming=" + incoming + " count=" + count);
                markTimelineNoMoreContent(source + "-empty");
            }
        } catch (Throwable t) {
            log("Timeline no-more empty-page error source=" + source + ": " + t.getMessage());
        }
    }

    private static boolean isTimelinePreloadStoppedState(Object presenter) {
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
            if (sTimelineCacheClearInFlight) return;
            if (!"-1".equals(getTimelineGroupId(presenter))) return;
            rememberTimelinePresenter(presenter);
            int count = getTimelineStatusCount(presenter);
            if (count < 2) return;
            if (hasActiveTimelineGapFill()) {
                scheduleTimelineGapFill(presenter, source + "-gap-fill");
                return;
            }
            if (isTimelinePreloadReady(presenter, source + "-schedule")) {
                persistTimelineShadowCache(source + "-ready", count, null, true);
                log("Timeline preload skipped warmed source=" + source + " count=" + count);
                return;
            }

            boolean suspendTopAnchors = false;
            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                if (count > state.lastCount) {
                    state.lastCount = count;
                    state.stableRounds = 0;
                    state.inFlight = false;
                }
                if (state.retryScheduled) {
                    state.retryScheduled = false;
                    state.retryToken++;
                }
                if (state.stopped || state.inFlight || state.scheduled) return;
                if (state.requestedPages >= getTimelinePreloadMaxPages()
                    || count >= getTimelinePreloadMaxItems()) {
                    state.stopped = true;
                    log("Timeline preload safety stop source=" + source + " pages=" + state.requestedPages
                        + " count=" + count);
                    return;
                }
                state.scheduled = true;
                if (!state.topAnchorsSuspended) {
                    state.topAnchorsSuspended = true;
                    suspendTopAnchors = true;
                }
            }

            if (suspendTopAnchors) {
                suspendTimelineTopAnchorsForPreload(source);
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
            if (hasActiveTimelineGapFill()) {
                scheduleTimelineGapFill(presenter, source + "-gap-fill");
                return;
            }
            if (isTimelinePreloadReady(presenter, source + "-execute")) {
                persistTimelineShadowCache(source + "-execute-ready", count, null, true);
                return;
            }
            synchronized (sPreloadStates) {
                PreloadState state = getPreloadStateLocked(presenter);
                state.scheduled = false;
                if (state.stopped || state.inFlight) return;
                if (state.requestedPages >= getTimelinePreloadMaxPages()
                    || count >= getTimelinePreloadMaxItems()) {
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
            rememberTimelinePresenter(presenter);
            int count = getTimelineStatusCount(presenter);
            boolean durationReady = isTimelineCacheDurationReady(
                buildBestTimelineCacheStats(presenter),
                "preload-watchdog"
            );
            boolean shouldContinue;
            boolean shouldMarkDone = false;
            long retryDelayMs = 0L;
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
                if (shouldStopTimelinePreload(
                    durationReady,
                    state.requestedPages,
                    getTimelinePreloadMaxPages(),
                    count,
                    getTimelinePreloadMaxItems()
                )) {
                    state.stopped = true;
                    pages = state.requestedPages;
                    shouldMarkDone = durationReady;
                    log("Timeline preload watchdog stop pages=" + state.requestedPages
                        + " stable=" + state.stableRounds
                        + " durationReady=" + durationReady
                        + " count=" + count);
                    shouldContinue = false;
                } else if (state.stableRounds >= PRELOAD_STABLE_DONE_ROUNDS) {
                    retryDelayMs = timelinePreloadRetryDelayMs(state.stableRounds);
                    pages = state.requestedPages;
                    log("Timeline preload watchdog retry pages=" + state.requestedPages
                        + " stable=" + state.stableRounds
                        + " durationReady=false count=" + count
                        + " delayMs=" + retryDelayMs);
                    shouldContinue = false;
                } else {
                    shouldContinue = true;
                }
            }
            if (shouldContinue) {
                scheduleTimelinePreload(presenter, "preload-watchdog");
            } else if (retryDelayMs > 0L) {
                scheduleTimelinePreloadRetry(presenter, retryDelayMs);
            } else {
                if (shouldMarkDone) {
                    markTimelinePreloadDone("preload-watchdog", pages, count);
                }
                finishTimelineTopAnchorForKnownRecyclerViews("preload-watchdog-stop");
            }
        } catch (Throwable t) {
            log("Timeline preload watchdog error: " + t.getMessage());
        }
    }

    static boolean shouldStopTimelinePreload(
        boolean durationReady,
        int requestedPages,
        int maxPages,
        int count,
        int maxItems
    ) {
        return durationReady || requestedPages >= maxPages || count >= maxItems;
    }

    static long timelinePreloadRetryDelayMs(int stableRounds) {
        int excess = Math.max(0, stableRounds - PRELOAD_STABLE_DONE_ROUNDS);
        int shift = Math.min(4, excess / 2);
        return Math.min(PRELOAD_STABLE_RETRY_MAX_MS, PRELOAD_STABLE_RETRY_BASE_MS << shift);
    }

    private static void scheduleTimelinePreloadRetry(final Object presenter, long delayMs) {
        final int retryToken;
        synchronized (sPreloadStates) {
            PreloadState state = getPreloadStateLocked(presenter);
            if (state.stopped || state.retryScheduled) return;
            state.retryScheduled = true;
            retryToken = ++state.retryToken;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                synchronized (sPreloadStates) {
                    PreloadState state = getPreloadStateLocked(presenter);
                    if (state.stopped || !state.retryScheduled || state.retryToken != retryToken) return;
                    state.retryScheduled = false;
                }
                scheduleTimelinePreload(presenter, "preload-stable-retry");
            }
        }, delayMs);
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
            if (hasActiveTimelineGapFill()) return false;
            return isTimelinePreloadReady(presenter, "freeze-network");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean shouldSuppressTimelineLoadMore(Object presenter) {
        try {
            if (hasActiveTimelineGapFill()) return false;
            return presenter != null && "-1".equals(getTimelineGroupId(presenter))
                && isTimelinePreloadReady(presenter, "suppress-loadmore");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean shouldRememberPreloadDone(String source, int pages, int count) {
        if (count < TIMELINE_CACHE_MIN_ITEMS) return false;
        TimelineCacheStats stats = buildBestTimelineCacheStats(sLastTimelinePresenter);
        return isTimelineCacheDurationReady(stats, source);
    }

    private static boolean isTimelinePreloadReady(int count, long newestMs) {
        if (!isTimelinePreloadDone()) return false;
        if (count < TIMELINE_CACHE_MIN_ITEMS) return false;
        if (!isTimelinePreloadMarkerHeadCompatible(newestMs, readPreloadDoneNewestMs())) return false;
        int markerCount = readPreloadDoneCount();
        if (markerCount >= TIMELINE_CACHE_MIN_ITEMS) {
            return count + TIMELINE_CACHE_COUNT_TOLERANCE >= markerCount;
        }
        return count >= PRELOAD_DONE_MIN_ITEMS;
    }

    private static boolean isTimelinePreloadReady(Object presenter, String source) {
        if (!isTimelinePreloadDone()) return false;
        int count = getTimelineStatusCount(presenter);
        if (count < TIMELINE_CACHE_MIN_ITEMS) return false;

        TimelineCacheStats stats = buildBestTimelineCacheStats(presenter);
        if (isTimelineCacheDurationReady(stats, source)) return true;
        if (!isTimelineCacheHeadFresh(stats.newestMs, System.currentTimeMillis())) return false;
        return isTimelinePreloadReady(count, stats.newestMs);
    }

    static boolean isTimelinePreloadMarkerHeadCompatible(long cacheNewestMs, long markerNewestMs) {
        if (cacheNewestMs <= 0L || markerNewestMs <= 0L) return false;
        return cacheNewestMs >= markerNewestMs - TIMELINE_CACHE_RECENCY_TOLERANCE_MS
            && cacheNewestMs <= markerNewestMs + TIMELINE_CACHE_RECENCY_TOLERANCE_MS;
    }

    private static boolean isTimelinePreloadDone() {
        int cacheDays = getTimelineCacheDaysSetting();
        if (!sTimelineCacheDaysSettingConfirmed) {
            return false;
        }
        if (sTimelinePreloadDone != null && sTimelinePreloadDoneCacheDays == cacheDays) {
            return sTimelinePreloadDone.booleanValue();
        }
        try {
            File marker = getPreloadDoneFile();
            boolean done = readPreloadDoneSatisfied(marker);
            sTimelinePreloadDone = Boolean.valueOf(done);
            sTimelinePreloadDoneCacheDays = cacheDays;
            if (!done && marker != null && marker.exists()) {
                marker.delete();
                log("Timeline preload marker ignored because stale or cache-days changed");
            }
        } catch (Throwable ignored) {
            sTimelinePreloadDone = Boolean.FALSE;
            sTimelinePreloadDoneCacheDays = cacheDays;
        }
        return sTimelinePreloadDone.booleanValue();
    }

    private static boolean readPreloadDoneSatisfied(File marker) {
        if (marker == null || !marker.exists()) return false;
        BufferedReader reader = null;
        int count = 0;
        int cacheDays = 0;
        long spanMs = 0L;
        long newestMs = 0L;
        boolean stableDone = false;
        boolean terminalDone = false;
        try {
            reader = new BufferedReader(new FileReader(marker));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("count=")) {
                    count = Integer.parseInt(line.substring("count=".length()).trim());
                } else if (line.startsWith("cache_days=")) {
                    cacheDays = Integer.parseInt(line.substring("cache_days=".length()).trim());
                } else if (line.startsWith("span_ms=")) {
                    spanMs = Long.parseLong(line.substring("span_ms=".length()).trim());
                } else if (line.startsWith("newest_ms=")) {
                    newestMs = Long.parseLong(line.substring("newest_ms=".length()).trim());
                } else if ("terminal_done=true".equals(line)) {
                    terminalDone = true;
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
        int requiredDays = getTimelineCacheDaysSetting();
        return isTimelinePreloadMarkerSatisfied(
            count,
            cacheDays,
            spanMs,
            stableDone,
            terminalDone,
            newestMs,
            System.currentTimeMillis(),
            requiredDays
        );
    }

    static boolean isTimelinePreloadMarkerSatisfied(
        int count,
        int cacheDays,
        long spanMs,
        boolean stableDone,
        boolean terminalDone,
        long newestMs,
        long nowMs,
        int requiredDays
    ) {
        if (!stableDone || count < TIMELINE_CACHE_MIN_ITEMS) return false;
        if (!isTimelineCacheHeadFresh(newestMs, nowMs)) return false;
        if (cacheDays < requiredDays) return false;
        long requiredMs = Math.max(0L, (requiredDays * TIMELINE_CACHE_DAY_MS) - TIMELINE_CACHE_DAY_TOLERANCE_MS);
        return spanMs >= requiredMs;
    }

    private static int readPreloadDoneCount() {
        return (int) readPreloadDoneLong("count");
    }

    private static long readPreloadDoneNewestMs() {
        return readPreloadDoneLong("newest_ms");
    }

    private static long readPreloadDoneLong(String key) {
        File marker = getPreloadDoneFile();
        if (marker == null || !marker.exists()) return 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(marker));
            String line;
            while ((line = reader.readLine()) != null) {
                String prefix = key + "=";
                if (line.startsWith(prefix)) {
                    return Long.parseLong(line.substring(prefix.length()).trim());
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
        return 0;
    }

    private static void markTimelinePreloadDone(String source, int pages, int count) {
        getTimelineCacheDaysSetting();
        if (!sTimelineCacheDaysSettingConfirmed) {
            log("Timeline preload marker deferred until setting is confirmed source=" + source);
            return;
        }
        if (!shouldRememberPreloadDone(source, pages, count)) return;
        boolean terminalDone = isTimelineTerminalDoneSource(source);
        TimelineCacheStats stats = buildBestTimelineCacheStats(sLastTimelinePresenter);
        if (isTimelinePreloadDone()) {
            int existingCount = readPreloadDoneCount();
            long existingNewestMs = readPreloadDoneNewestMs();
            persistTimelineShadowCache(source, count, null, true, stats);
            boolean candidateNewer = stats.newestMs
                > existingNewestMs + TIMELINE_CACHE_RECENCY_TOLERANCE_MS;
            if (!candidateNewer && existingCount >= count) {
                log("Timeline preload marker kept current source=" + source
                    + " count=" + count + " existing=" + existingCount
                    + " newestMs=" + stats.newestMs
                    + " existingNewestMs=" + existingNewestMs);
                return;
            }
        }
        try {
            int cacheDays = getTimelineCacheDaysSetting();
            File marker = getPreloadDoneFile();
            String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
            StringBuilder content = new StringBuilder();
            content.append("done_at=").append(ts).append('\n');
            content.append("source=").append(source).append('\n');
            content.append("done_reason=stable\n");
            content.append("pages=").append(pages).append('\n');
            content.append("count=").append(count).append('\n');
            content.append("cache_days=").append(cacheDays).append('\n');
            content.append("span_ms=").append(stats.spanMs()).append('\n');
            content.append("span_days=").append(formatTimelineCacheSpanDays(stats.spanMs())).append('\n');
            content.append("dated_count=").append(stats.datedCount).append('\n');
            content.append("oldest_ms=").append(stats.oldestMs).append('\n');
            content.append("newest_ms=").append(stats.newestMs).append('\n');
            content.append("terminal_done=").append(terminalDone).append('\n');
            writeTextFileAtomically(marker, content.toString());
            sTimelinePreloadDone = Boolean.TRUE;
            sTimelinePreloadDoneCacheDays = cacheDays;
            persistTimelineShadowCache(source, count, null, true, stats);
            log("Timeline preload remembered source=" + source + " pages=" + pages
                + " count=" + count + " cacheDays=" + cacheDays
                + " spanDays=" + formatTimelineCacheSpanDays(stats.spanMs())
                + " terminal=" + terminalDone);
        } catch (Throwable t) {
            log("Timeline preload remember error source=" + source + ": " + t.getMessage());
        }
    }

    private static File getPreloadDoneFile() {
        return new File("/data/data/com.weico.international/files", PRELOAD_DONE_FILE);
    }

    private static int getTimelineStatusCount(Object presenter) {
        try {
            int count = getTimelinePresenterStatusCount(presenter);
            if ("-1".equals(getTimelineGroupId(presenter))) {
                count = Math.max(count, getTimelineCumulativeStatusCount());
            }
            return count;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int getTimelinePresenterStatusCount(Object presenter) {
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
            return count;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int getTimelinePresenterRawStatusCount(Object presenter) {
        try {
            Object statusList = XposedHelpers.callMethod(presenter, "getStatusList");
            return statusList instanceof List ? ((List) statusList).size() : 0;
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
            long afterFirst = getStatusId(unwrapStatus(items.get(0)));
            long afterLast = getStatusId(unwrapStatus(items.get(items.size() - 1)));
            log("Timeline order sorted newest-first source=" + source + " group=" + groupId + " size=" + list.size()
                + " statuses=" + items.size()
                + " beforeFirst=" + beforeFirst + " beforeLast=" + beforeLast
                + " first=" + afterFirst + " last=" + afterLast
                + " loadNew=" + loadNew);
            return list;
        } catch (Throwable t) {
            ArrayList copy = new ArrayList(list);
            for (int i = 0; i < positions.size(); i++) {
                int index = ((Integer) positions.get(i)).intValue();
                copy.set(index, items.get(i));
            }
            long afterFirst = getStatusId(unwrapStatus(items.get(0)));
            long afterLast = getStatusId(unwrapStatus(items.get(items.size() - 1)));
            log("Timeline order copied+sorted newest-first source=" + source + " group=" + groupId + " size=" + list.size()
                + " statuses=" + items.size()
                + " beforeFirst=" + beforeFirst + " beforeLast=" + beforeLast
                + " first=" + afterFirst + " last=" + afterLast
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
            if (getFieldValueOrNull(status, "promotion") != null) return true;

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
        Object fieldValue = getFieldValueOrNull(status, "isLoadMoreButton");
        if (fieldValue instanceof Boolean) return (Boolean) fieldValue;
        try {
            Object value = callNoArgMethodCached(status, "isLoadMoreButton");
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {}
        return false;
    }

    private static long getStatusId(Object status) {
        if (status == null) return 0;
        try {
            Object value = callNoArgMethodCached(status, "getId");
            if (value instanceof Number) return ((Number) value).longValue();
        } catch (Throwable ignored) {}
        try {
            Object value = callNoArgMethodCached(status, "getIdstr");
            if (value != null) return Long.parseLong(String.valueOf(value));
        } catch (Throwable ignored) {}
        return 0;
    }

    private static Object unwrapStatus(Object item) {
        if (item == null) return null;
        try {
            Object value = callNoArgMethodCached(item, "getStatus");
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
        Object value = getFieldValueOrNull(target, field);
        return value instanceof Boolean ? (Boolean) value : fallback;
    }

    private static boolean callBooleanMethodSafe(Object target, String method) {
        try {
            Object value = callNoArgMethodCached(target, method);
            return value instanceof Boolean && (Boolean) value;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static int callIntMethodSafe(Object target, String method, int fallback) {
        try {
            Object value = callNoArgMethodCached(target, method);
            return value instanceof Number ? ((Number) value).intValue() : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static Object callMethodSafe(Object target, String method) {
        if (target == null || method == null) return null;
        try {
            return callNoArgMethodCached(target, method);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getStringMethodOrField(Object target, String method, String field) {
        Object value = callMethodSafe(target, method);
        if (value != null) return String.valueOf(value);
        value = getFieldValueOrNull(target, field);
        return value == null ? null : String.valueOf(value);
    }

    private static long getStatusCreatedAtMillis(Object status) {
        if (status == null) return 0L;
        String createdAt = getStringMethodOrField(status, "getCreated_at", "created_at");
        if (!hasMeaningfulString(createdAt)) {
            createdAt = getStringMethodOrField(status, "getCreatedAt", "createdAt");
        }
        if (!hasMeaningfulString(createdAt)) {
            createdAt = getStringMethodOrField(status, null, "createdAt");
        }
        return parseStatusCreatedAtMillis(createdAt);
    }

    private static long parseStatusCreatedAtMillis(String value) {
        if (!hasMeaningfulString(value)) return 0L;
        String text = value.trim();
        try {
            long numeric = Long.parseLong(text);
            if (numeric > 100000000000L) return numeric;
            if (numeric > 1000000000L) return numeric * 1000L;
        } catch (Throwable ignored) {
        }

        String[] patterns = new String[] {
            "EEE MMM dd HH:mm:ss Z yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
        };
        for (int i = 0; i < patterns.length; i++) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(patterns[i], Locale.ENGLISH);
                Date date = format.parse(text);
                if (date != null) return date.getTime();
            } catch (Throwable ignored) {
            }
        }
        return 0L;
    }

    private static Object getObjectMethodOrField(Object target, String method, String field) {
        Object value = callMethodSafe(target, method);
        if (value != null) return value;
        if (target == null || field == null) return null;
        return getFieldValueOrNull(target, field);
    }

    private static int getIntMethodOrField(Object target, String method, String field, int fallback) {
        Object value = callMethodSafe(target, method);
        if (value instanceof Number) return ((Number) value).intValue();
        if (target == null || field == null) return fallback;
        value = getFieldValueOrNull(target, field);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private static long getLongMethodOrField(Object target, String method, String field, long fallback) {
        Object value = callMethodSafe(target, method);
        long parsed = parseLongValue(value, Long.MIN_VALUE);
        if (parsed != Long.MIN_VALUE) return parsed;
        if (target == null || field == null) return fallback;
        value = getFieldValueOrNull(target, field);
        parsed = parseLongValue(value, Long.MIN_VALUE);
        return parsed != Long.MIN_VALUE ? parsed : fallback;
    }

    private static long parseLongValue(Object value, long fallback) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof CharSequence) {
            try {
                return Long.parseLong(String.valueOf(value).trim());
            } catch (Throwable ignored) {
                return fallback;
            }
        }
        return fallback;
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

    static Object getFieldValue(Object target, String fieldName) throws Exception {
        if (target == null || fieldName == null) throw new NoSuchFieldException(fieldName);
        Field field = findCachedField(target.getClass(), fieldName);
        if (field == null) throw new NoSuchFieldException(fieldName);
        return field.get(target);
    }

    static Object getFieldValueOrNull(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        try {
            Field field = findCachedField(target.getClass(), fieldName);
            return field == null ? null : field.get(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Field findCachedField(Class<?> targetClass, String fieldName) {
        ConcurrentHashMap<String, FieldLookup> classLookups = sFieldLookups.get(targetClass);
        if (classLookups == null) {
            ConcurrentHashMap<String, FieldLookup> created = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, FieldLookup> existing = sFieldLookups.putIfAbsent(targetClass, created);
            classLookups = existing == null ? created : existing;
        }

        FieldLookup cached = classLookups.get(fieldName);
        if (cached != null) return cached.field;

        Field resolved = null;
        Class<?> clazz = targetClass;
        while (clazz != null) {
            try {
                resolved = clazz.getDeclaredField(fieldName);
                resolved.setAccessible(true);
                break;
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            }
        }

        FieldLookup lookup = new FieldLookup(resolved);
        FieldLookup existing = classLookups.putIfAbsent(fieldName, lookup);
        return existing == null ? resolved : existing.field;
    }

    static Object callNoArgMethodCached(Object target, String methodName) throws Exception {
        if (target == null || methodName == null) return null;
        Method method = findCachedNoArgMethod(target.getClass(), methodName);
        if (method == null) return null;
        return method.invoke(target);
    }

    private static Method findCachedNoArgMethod(Class<?> targetClass, String methodName) {
        ConcurrentHashMap<String, MethodLookup> classLookups = sNoArgMethodLookups.get(targetClass);
        if (classLookups == null) {
            ConcurrentHashMap<String, MethodLookup> created = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, MethodLookup> existing = sNoArgMethodLookups.putIfAbsent(targetClass, created);
            classLookups = existing == null ? created : existing;
        }

        MethodLookup cached = classLookups.get(methodName);
        if (cached != null) return cached.method;

        Method resolved = null;
        try {
            resolved = targetClass.getMethod(methodName);
            resolved.setAccessible(true);
        } catch (Throwable ignored) {
            Class<?> clazz = targetClass;
            while (clazz != null) {
                try {
                    resolved = clazz.getDeclaredMethod(methodName);
                    resolved.setAccessible(true);
                    break;
                } catch (NoSuchMethodException notFound) {
                    clazz = clazz.getSuperclass();
                } catch (Throwable inaccessible) {
                    break;
                }
            }
        }

        MethodLookup lookup = new MethodLookup(resolved);
        MethodLookup existing = classLookups.putIfAbsent(methodName, lookup);
        return existing == null ? resolved : existing.method;
    }

    private static String getTimelineGroupId(Object owner) {
        try {
            Object groupId = callNoArgMethodCached(owner, "getGroupId");
            if (groupId != null) return String.valueOf(groupId);
        } catch (Throwable ignored) {}
        try {
            Object presenter = callNoArgMethodCached(owner, "getPresenter");
            Object groupId = callNoArgMethodCached(presenter, "getGroupId");
            if (groupId != null) return String.valueOf(groupId);
        } catch (Throwable ignored) {}
        return null;
    }

    private static void setFieldValue(Object target, String fieldName, Object value) throws Exception {
        Field field = findCachedField(target.getClass(), fieldName);
        if (field == null) throw new NoSuchFieldException(fieldName);
        field.set(target, value);
    }

    private static final class FieldLookup {
        final Field field;

        FieldLookup(Field field) {
            this.field = field;
        }
    }

    private static final class MethodLookup {
        final Method method;

        MethodLookup(Method method) {
            this.method = method;
        }
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
