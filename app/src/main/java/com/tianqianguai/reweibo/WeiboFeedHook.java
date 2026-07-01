package com.tianqianguai.reweibo;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeiboFeedHook {

    private static int sLastCount = 0;
    private static int sIdleChecks = 0;
    private static boolean sDone = false;
    private static Object sRecyclerView = null;
    private static Object sAdapter = null;
    private static android.content.Context sContext = null;

    private static void log(String msg) {
        XposedBridge.log("ReWeibo: " + msg);
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
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    sContext = (Application) param.thisObject;
                    log("App onCreate");
                }
            });

            Class<?> lmClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.LinearLayoutManager", lpparam.classLoader
            );
            XposedHelpers.findAndHookMethod(lmClass, "setReverseLayout", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) { p.args[0] = true; }
            });
            XposedHelpers.findAndHookMethod(lmClass, "setStackFromEnd", boolean.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam p) { p.args[0] = false; }
            });

            Class<?> rvClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.RecyclerView", lpparam.classLoader
            );
            XposedHelpers.findAndHookMethod(rvClass, "setLayoutManager",
                "androidx.recyclerview.widget.RecyclerView$LayoutManager",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Object lm = param.args[0];
                        if (lm != null) {
                            try { XposedHelpers.callMethod(lm, "setReverseLayout", true); } catch (Throwable ignored) {}
                            try { XposedHelpers.callMethod(lm, "setStackFromEnd", false); } catch (Throwable ignored) {}
                        }
                    }
                });

            // Detect feed RV via onLayout (fires after adapter has data)
            XposedHelpers.findAndHookMethod(rvClass, "onLayout", boolean.class, int.class, int.class, int.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (sRecyclerView != null) return;
                        Object rv = param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            if (sRecyclerView != null) return;
                            try {
                                Object adapter = XposedHelpers.callMethod(rv, "getAdapter");
                                if (adapter == null) return;
                                int count = (int) XposedHelpers.callMethod(adapter, "getItemCount");
                                if (count <= 1) return;
                                // Only capture the main feed adapter
                                String adapterName = adapter.getClass().getName();
                                if (!adapterName.contains("RecyclerViewAdapter")) return;
                                sRecyclerView = rv;
                                sAdapter = adapter;
                                sLastCount = count;
                                log("RV captured, count=" + count);
                                Object ctx = XposedHelpers.callMethod(rv, "getContext");
                                if (ctx instanceof Activity) {
                                    FloatingButton.attachToActivity((Activity) ctx, () -> {
                                        log("Double-tap: start scroll");
                                        if (!sDone) scheduleScroll();
                                    }, () -> {
                                        log("Single-tap: stop scroll");
                                        sDone = true;
                                    });
                                }
                                PullReverseUtil.findAndReverse(rv, lpparam.classLoader);
                            } catch (Throwable ignored) {}
                        }, 500);
                    }
                });

            log("Hooked successfully");
        } catch (Throwable t) {
            log("Failed: " + t.getMessage());
        }
    }
}
