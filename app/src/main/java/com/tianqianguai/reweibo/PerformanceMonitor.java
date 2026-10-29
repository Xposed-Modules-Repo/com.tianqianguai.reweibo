package com.tianqianguai.reweibo;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.ApplicationExitInfo;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.view.FrameMetrics;
import android.view.Window;
import java.lang.ref.WeakReference;
import java.util.List;

/** Foreground-only heartbeat and aggregated frame metrics; no per-frame log writes. */
final class PerformanceMonitor implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    private static PerformanceMonitor instance;
    private final Application application;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HandlerThread thread = new HandlerThread("ReWeibo-performance", android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private final Handler background;
    private volatile boolean closed;
    private volatile boolean foreground;
    private volatile long heartbeatSent;
    private volatile boolean heartbeatPending;
    private volatile String currentPage = "none";
    private volatile long frameBudgetNs = 16_666_667L;
    private volatile long resumedAt;
    private WeakReference<Window> window = new WeakReference<>(null);
    private WeakReference<Activity> current = new WeakReference<>(null);
    private long frames, slowFrames, longestFrameNs, metricsLost;
    private long windowStarted, memoryAt, lastSampleAt;
    private int stallSamples;
    private final Runnable acknowledgement = () -> { heartbeatPending = false; };
    private final Window.OnFrameMetricsAvailableListener metrics = (w, m, lost) -> {
        if (closed || !foreground) return;
        long duration = m.getMetric(FrameMetrics.TOTAL_DURATION);
        frames++;
        if (isSlowFrame(duration, frameBudgetNs)) slowFrames++;
        longestFrameNs = Math.max(longestFrameNs, duration);
        metricsLost += Math.max(0, lost);
        if (m.getMetric(FrameMetrics.FIRST_DRAW_FRAME) == 1) {
            Diagnostics.event("ui", "first_frame", "activity=" + currentPage
                + " frame_ms=" + duration / 1_000_000L
                + " since_resume_ms=" + (SystemClock.uptimeMillis() - resumedAt), true);
        }
    };

    static synchronized void start(Context context) {
        if (instance != null || context == null || !Diagnostics.enabled()) return;
        Context app = context.getApplicationContext();
        if (!(app instanceof Application)) return;
        instance = new PerformanceMonitor((Application) app);
    }

    static synchronized void stop() {
        if (instance == null) return;
        instance.close();
        instance = null;
    }

    static synchronized void restoreActivity(Activity activity) {
        if (instance != null && activity != null && !activity.isFinishing()) instance.onActivityResumed(activity);
    }

    private PerformanceMonitor(Application application) {
        this.application = application;
        thread.start();
        background = new Handler(thread.getLooper());
        application.registerActivityLifecycleCallbacks(this);
        application.registerComponentCallbacks(this);
        background.post(this::startupEvidence);
        background.postDelayed(tick, 1000);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (closed) return;
            long now = SystemClock.uptimeMillis();
            if (foreground) {
                if (heartbeatPending) {
                    long delay = now - heartbeatSent;
                    if (shouldSampleStall(delay, stallSamples, now - lastSampleAt)) {
                        stallSamples++;
                        lastSampleAt = now;
                        StringBuilder stack = new StringBuilder();
                        StackTraceElement[] trace = Looper.getMainLooper().getThread().getStackTrace();
                        for (int i = 0; i < Math.min(trace.length, 24); i++) stack.append(trace[i]).append(" <- ");
                        Diagnostics.event("performance", "main_stall", "delay_ms=" + delay
                            + " sample=" + stallSamples + " activity=" + currentPage + " stack=" + stack, true);
                    }
                } else {
                    if (stallSamples > 0) Diagnostics.event("performance", "main_recovered",
                        "delay_ms=" + (now - heartbeatSent) + " samples=" + stallSamples, true);
                    stallSamples = 0;
                    heartbeatSent = now;
                    heartbeatPending = true;
                    main.post(acknowledgement);
                }
                if (now - windowStarted >= 5000) reportFrames(now);
                if (now - memoryAt >= 30000) {
                    memoryAt = now;
                    Runtime runtime = Runtime.getRuntime();
                    long used = runtime.totalMemory() - runtime.freeMemory();
                    boolean pressure = used > runtime.maxMemory() * 4 / 5;
                    Diagnostics.event("performance", "heap", "used_bytes=" + used
                        + " max_bytes=" + runtime.maxMemory(), pressure);
                    Diagnostics.event("logger", "health", Diagnostics.status(), true);
                }
            }
            background.postDelayed(this, 1000);
        }
    };

    private void reportFrames(long now) {
        if (slowFrames > 0 || Diagnostics.detailed()) Diagnostics.event("performance", "frames",
            "window_ms=" + (now - windowStarted) + " frames=" + frames + " slow_frames=" + slowFrames
                + " max_frame_ms=" + longestFrameNs / 1_000_000L + " metrics_lost=" + metricsLost
                + " budget_us=" + frameBudgetNs / 1000, true);
        frames = slowFrames = longestFrameNs = metricsLost = 0;
        windowStarted = now;
    }

    static boolean shouldSampleStall(long delayMs, int samples, long sinceSampleMs) {
        return delayMs >= 1000 && samples < 3 && sinceSampleMs >= 2000;
    }

    static boolean isSlowFrame(long frameNanos, long budgetNanos) {
        return frameNanos > budgetNanos * 3 / 2;
    }

    private void startupEvidence() {
        try {
            String target = application.getPackageManager().getPackageInfo(application.getPackageName(), 0).versionName;
            String module = application.getPackageManager().getPackageInfo("com.tianqianguai.reweibo", 0).versionName;
            Diagnostics.event("session", "environment", "target=" + target + " module_version=" + module
                + " sdk=" + Build.VERSION.SDK_INT + " pid=" + android.os.Process.myPid()
                + " framework=" + com.tianqianguai.reweibo.compat.XposedBridge.frameworkName()
                + " framework_version=" + com.tianqianguai.reweibo.compat.XposedBridge.frameworkVersion(), true);
            if (Build.VERSION.SDK_INT >= 30) {
                ActivityManager manager = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
                List<ApplicationExitInfo> exits = manager.getHistoricalProcessExitReasons(application.getPackageName(), 0, 5);
                for (ApplicationExitInfo exit : exits) {
                    if (System.currentTimeMillis() - exit.getTimestamp() > 24 * 60 * 60 * 1000L) continue;
                    Diagnostics.event("process", "previous_exit", "timestamp=" + exit.getTimestamp()
                        + " pid=" + exit.getPid() + " reason=" + exit.getReason()
                        + " status=" + exit.getStatus() + " importance=" + exit.getImportance()
                        + " pss_kb=" + exit.getPss() + " rss_kb=" + exit.getRss(), true);
                }
            }
        } catch (Exception error) { Diagnostics.event("session", "evidence_unavailable", error.getClass().getName(), true); }
    }

    static void intent(Activity activity, Intent intent, String source) {
        if (!Diagnostics.enabled()) return;
        Diagnostics.interaction(source);
        // Deliberately omit extras, URI contents, notification text, account IDs and tokens.
        Diagnostics.event("ui", source, "activity=" + activity.getClass().getName()
            + " action=" + (intent == null ? "none" : Diagnostics.clean(intent.getAction()))
            + " has_data=" + (intent != null && intent.getData() != null)
            + " flags=" + (intent == null ? 0 : intent.getFlags()), true);
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) { intent(activity, activity.getIntent(), "created"); }
    @Override public void onActivityResumed(Activity activity) {
        detachWindow();
        current = new WeakReference<>(activity);
        currentPage = activity.getClass().getName();
        resumedAt = SystemClock.uptimeMillis();
        Diagnostics.page(currentPage, "resumed");
        foreground = true;
        try {
            float hz = activity.getWindowManager().getDefaultDisplay().getRefreshRate();
            frameBudgetNs = (long) (1_000_000_000L / Math.max(30f, hz));
            Window activeWindow = activity.getWindow();
            activeWindow.addOnFrameMetricsAvailableListener(metrics, background);
            window = new WeakReference<>(activeWindow);
        } catch (RuntimeException error) {
            Diagnostics.event("performance", "frame_metrics_unavailable", error.getClass().getName(), true);
        }
        background.post(() -> { frames = slowFrames = longestFrameNs = metricsLost = 0; windowStarted = SystemClock.uptimeMillis(); });
    }
    @Override public void onActivityPaused(Activity activity) {
        if (current.get() != activity) return;
        Diagnostics.page(activity.getClass().getName(), "paused");
        foreground = false;
        main.removeCallbacks(acknowledgement);
        heartbeatPending = false;
        detachWindow();
        background.post(() -> reportFrames(SystemClock.uptimeMillis()));
    }
    @Override public void onActivityDestroyed(Activity activity) {
        if (current.get() == activity) { onActivityPaused(activity); current.clear(); }
    }
    @Override public void onActivityStarted(Activity activity) {}
    @Override public void onActivityStopped(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onLowMemory() { Diagnostics.event("performance", "low_memory", "", true); }
    @Override public void onTrimMemory(int level) { Diagnostics.event("performance", "trim_memory", "level=" + level, true); }
    @Override public void onConfigurationChanged(Configuration configuration) {}

    private void detachWindow() {
        Window previous = window.get();
        if (previous != null) {
            try { previous.removeOnFrameMetricsAvailableListener(metrics); }
            catch (RuntimeException error) { Diagnostics.event("performance", "frame_detach_error", error.getClass().getName(), true); }
        }
        window.clear();
    }

    private void close() {
        closed = true;
        foreground = false;
        application.unregisterActivityLifecycleCallbacks(this);
        application.unregisterComponentCallbacks(this);
        detachWindow();
        main.removeCallbacks(acknowledgement);
        background.removeCallbacksAndMessages(null);
        thread.quitSafely();
    }
}
