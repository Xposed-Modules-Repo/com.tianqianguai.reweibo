package com.tianqianguai.reweibo;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeiboLiteHook {

    private static int sLastCount = 0;
    private static int sIdleChecks = 0;
    private static boolean sDone = false;
    private static Object sRecyclerView = null;
    private static Object sAdapter = null;
    private static File sLogFile = null;

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

    private static void scheduleScroll() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (sDone || sRecyclerView == null || sAdapter == null) return;
                try {
                    int count = (int) XposedHelpers.callMethod(sAdapter, "getItemCount");
                    if (count <= 1) { handler.postDelayed(this, 500); return; }
                    if (count > sLastCount) {
                        sLastCount = count;
                        sIdleChecks = 0;
                        XposedHelpers.callMethod(sRecyclerView, "smoothScrollToPosition", count - 1);
                        if (count % 200 < 25) log("Scrolled to " + (count - 1));
                    } else {
                        sIdleChecks++;
                        if (sIdleChecks >= 30) {
                            sDone = true;
                            log("Done at " + (count - 1));
                        } else {
                            XposedHelpers.callMethod(sRecyclerView, "smoothScrollToPosition", count - 1);
                        }
                    }
                    if (!sDone) handler.postDelayed(this, 1500);
                } catch (Throwable t) {
                    if (!sDone) handler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            log("WeicoHook start");
            initLogFile();
            removeSplashAd(lpparam.classLoader);
            removeTimelineAd(lpparam.classLoader);

            Class<?> lmClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.LinearLayoutManager",
                lpparam.classLoader
            );
            XposedHelpers.findAndHookMethod(lmClass, "setReverseLayout", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { param.args[0] = true; }
            });
            XposedHelpers.findAndHookMethod(lmClass, "setStackFromEnd", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) { param.args[0] = false; }
            });

            Class<?> rvClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.RecyclerView",
                lpparam.classLoader
            );

            XposedHelpers.findAndHookMethod(rvClass, "setLayoutManager",
                "androidx.recyclerview.widget.RecyclerView$LayoutManager",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object lm = param.args[0];
                        if (lm != null) {
                            try {
                                XposedHelpers.callMethod(lm, "setReverseLayout", true);
                                XposedHelpers.callMethod(lm, "setStackFromEnd", false);
                            } catch (Throwable ignored) {}
                        }
                        if (sRecyclerView == null) {
                            Object rv = param.thisObject;
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (sRecyclerView != null) return;
                                try {
                                    Object adapter = XposedHelpers.callMethod(rv, "getAdapter");
                                    if (adapter == null) return;
                                    int count = (int) XposedHelpers.callMethod(adapter, "getItemCount");
                                    if (count <= 1) return;
                                    // Only capture TimelineAdapter (the actual feed adapter)
                                    String adapterName = adapter.getClass().getName();
                                    if (!adapterName.contains("TimelineAdapter")) return;
                                    sRecyclerView = rv;
                                    sAdapter = adapter;
                                    sLastCount = count;
                                    log("Feed RV captured: " + adapterName + " count=" + count);
                                    Object ctx = XposedHelpers.callMethod(rv, "getContext");
                                    if (ctx instanceof Activity) {
                                        FloatingButton.attachToActivity((Activity) ctx, () -> {
                                            log("Double-tap: start scroll");
                                            try {
                                                XposedHelpers.callMethod(sAdapter, "switch2LoadMore");
                                            } catch (Throwable ignored) {}
                                            sDone = false;
                                            sIdleChecks = 0;
                                            scheduleScroll();
                                        }, () -> {
                                            log("Single-tap: stop scroll");
                                            sDone = true;
                                        });
                                    }
                                    PullReverseUtil.findAndReverse(rv, lpparam.classLoader);
                                    disablePullRefresh(rv, lpparam.classLoader);
                                } catch (Throwable ignored) {}
                            }, 1000);
                        }
                    }
                });

            log("WeicoHook done");
        } catch (Throwable t) {
            log("WeicoHook failed: " + t.getMessage());
        }
    }

    private static void disablePullRefresh(Object rv, ClassLoader cl) {
        try {
            Object parent = XposedHelpers.callMethod(rv, "getParent");
            int depth = 0;
            while (parent != null && depth < 8) {
                String name = parent.getClass().getName();
                if (name.contains("ESwpLayout") || name.contains("SwipeRefresh")) {
                    log("Found pull-refresh: " + name);
                    try {
                        XposedHelpers.findAndHookMethod(
                            "androidx.swiperefreshlayout.widget.SwipeRefreshLayout",
                            cl, "canChildScrollUp",
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) {
                                    param.setResult(true);
                                }
                            });
                        log("  canChildScrollUp → always true");
                    } catch (Throwable t) {
                        log("  canChildScrollUp error: " + t.getMessage());
                    }
                    return;
                }
                try { parent = XposedHelpers.callMethod(parent, "getParent"); } catch (Throwable e) { break; }
                depth++;
            }
        } catch (Throwable t) {
            log("disablePullRefresh error: " + t.getMessage());
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
