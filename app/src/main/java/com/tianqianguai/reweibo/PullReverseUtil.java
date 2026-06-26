package com.tianqianguai.reweibo;

import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PullReverseUtil {

    private static void log(String msg) {
        XposedBridge.log("ReWeibo: " + msg);
    }

    public static void findAndReverse(Object rv, ClassLoader cl) {
        try {
            Object parent = XposedHelpers.callMethod(rv, "getParent");
            int depth = 0;
            while (parent != null && depth < 8) {
                String className = parent.getClass().getName();
                log("Parent[" + depth + "]: " + className);

                // ESwpLayout - SwipeRefreshLayout wrapper
                if (className.contains("ESwpLayout") || className.contains("SwipeRefresh")) {
                    log("Found SwipeRefresh: " + className);
                    hookSwipeRefresh(parent);
                    return;
                }
                // PullDownView - Weibo's custom pull-down
                if (className.contains("PullDownView") || className.contains("EasyDownView")) {
                    log("Found PullDownView: " + className);
                    hookPullDown(parent);
                    return;
                }
                try { parent = XposedHelpers.callMethod(parent, "getParent"); } catch (Throwable e) { break; }
                depth++;
            }
            log("No pull-refresh found");
        } catch (Throwable ignored) {}
    }

    // For SwipeRefreshLayout: hook canChildScrollUp to reverse it
    private static void hookSwipeRefresh(Object swipeLayout) {
        try {
            // SwipeRefreshLayout checks canChildScrollUp() to decide if pull-down should trigger
            // By reversing it, pull-down only works when child is at bottom
            XposedHelpers.findAndHookMethod(swipeLayout.getClass(), "canChildScrollUp",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        boolean result = (boolean) param.getResult();
                        param.setResult(!result);
                    }
                });
            log("Hooked canChildScrollUp");
        } catch (Throwable t) {
            log("canChildScrollUp failed: " + t.getMessage());
            // Fallback: try onInterceptTouchEvent with Y reversal
            hookTouchReverse(swipeLayout);
        }
    }

    // For PullDownView: hook touch events with Y reversal
    private static void hookPullDown(Object pullDownView) {
        hookTouchReverse(pullDownView);
    }

    private static void hookTouchReverse(Object view) {
        try {
            XC_MethodHook reverseHook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        android.view.MotionEvent ev = (android.view.MotionEvent) param.args[0];
                        float h = ((View) param.thisObject).getHeight();
                        if (h <= 0) return;
                        float newY = h - ev.getY();
                        android.view.MotionEvent newEv = android.view.MotionEvent.obtain(
                            ev.getDownTime(), ev.getEventTime(), ev.getAction(),
                            ev.getX(), newY, ev.getMetaState());
                        param.args[0] = newEv;
                    } catch (Throwable ignored) {}
                }
            };

            Class<?> cls = view.getClass();
            boolean hooked = false;
            while (cls != null && cls != View.class && !hooked) {
                try {
                    XposedHelpers.findAndHookMethod(cls, "onInterceptTouchEvent",
                        android.view.MotionEvent.class, reverseHook);
                    XposedHelpers.findAndHookMethod(cls, "onTouchEvent",
                        android.view.MotionEvent.class, reverseHook);
                    log("Touch reverse on " + cls.getSimpleName());
                    hooked = true;
                } catch (Throwable t) {
                    cls = cls.getSuperclass();
                }
            }
            if (!hooked) {
                // Fallback: set OnTouchListener
                final View pdv = (View) view;
                pdv.setOnTouchListener((v, ev) -> {
                    float h = v.getHeight();
                    if (h <= 0) return false;
                    ev.setLocation(ev.getX(), h - ev.getY());
                    return false;
                });
                log("Touch listener set (fallback)");
            }
        } catch (Throwable t) {
            log("hookTouchReverse failed: " + t.getMessage());
        }
    }
}
