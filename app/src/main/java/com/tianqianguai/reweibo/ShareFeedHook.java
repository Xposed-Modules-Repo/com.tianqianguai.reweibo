package com.tianqianguai.reweibo;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class ShareFeedHook {

    private static boolean sDone = false;
    private static Object sFeedRV = null;
    private static int sLastCount = 0;
    private static int sStableChecks = 0;
    private static View.OnClickListener sShowMoreListener = null;
    private static boolean sListenerFired = false;
    private static File sLogFile = null;
    private static boolean sScrolling = false;
    private static int sMarkerPosition = -1;
    private static int sScrollTarget = 0;

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

    private static String findTextInTree(View root) {
        if (root instanceof TextView) {
            CharSequence text = ((TextView) root).getText();
            if (text != null) return text.toString();
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                String result = findTextInTree(vg.getChildAt(i));
                if (result != null && result.contains("显示更多")) return result;
            }
        }
        return null;
    }

    private static int getItemCount(Object adapter) {
        for (Method m : adapter.getClass().getMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType() == int.class) {
                try {
                    Object r = m.invoke(adapter);
                    if (r instanceof Integer && (int) r > 0) return (int) r;
                } catch (Throwable ignored) {}
            }
        }
        return 0;
    }

    private static void fireShowMore() {
        if (sShowMoreListener == null) return;
        try {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    sShowMoreListener.onClick(null);
                    log("onClick fired");
                } catch (Throwable t) {
                    log("onClick error: " + t.getMessage());
                }
            });
        } catch (Throwable t) {
            log("fireShowMore error: " + t.getMessage());
        }
    }

    private static boolean searchForMarker(View root) {
        if (root instanceof TextView) {
            CharSequence text = ((TextView) root).getText();
            if (text != null && text.toString().contains("上次阅读到这里")) {
                return true;
            }
        }
        if (root instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) root;
            for (int i = 0; i < vg.getChildCount(); i++) {
                if (searchForMarker(vg.getChildAt(i))) return true;
            }
        }
        return false;
    }

    private static boolean checkMarkerInView() {
        if (sFeedRV == null) return false;
        try {
            ViewGroup rv = (ViewGroup) sFeedRV;
            for (int i = 0; i < rv.getChildCount(); i++) {
                if (searchForMarker(rv.getChildAt(i))) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static void startPolling() {
        sScrolling = true;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable[] poll = new Runnable[1];
        poll[0] = new Runnable() {
            @Override
            public void run() {
                if (sDone || sFeedRV == null) return;
                try {
                    Object adapter = XposedHelpers.callMethod(sFeedRV, "getAdapter");
                    if (adapter == null) { handler.postDelayed(this, 500); return; }
                    int count = getItemCount(adapter);

                    if (sLastCount == 0) {
                        sLastCount = count;
                        sScrollTarget = 0;
                    }

                    sScrollTarget += 3;
                    if (sScrollTarget >= count) sScrollTarget = count - 1;

                    // Instant jump — marker won't be skipped during animation
                    XposedHelpers.callMethod(sFeedRV, "scrollToPosition", sScrollTarget);

                    // Check after layout settles
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (sDone) return;
                            if (checkMarkerInView()) {
                                log("Marker found at=" + sScrollTarget);
                                sDone = true;
                                sScrolling = false;
                                return;
                            }
                            int newCount = getItemCount(adapter);
                            if (sScrollTarget < newCount - 1) {
                                handler.postDelayed(poll[0], 200);
                            } else if (sShowMoreListener != null && !sListenerFired) {
                                sListenerFired = true;
                                fireShowMore();
                                handler.postDelayed(poll[0], 3000);
                            } else {
                                sDone = true;
                                sScrolling = false;
                                log("Done at " + (newCount - 1));
                            }
                        }
                    }, 100);
                } catch (Throwable t) {
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.postDelayed(poll[0], 500);
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            try {
                File dir = new File("/data/data/com.hengye.share/files");
                if (!dir.exists()) dir.mkdirs();
                sLogFile = new File(dir, "reweibo_share.log");
            } catch (Throwable ignored) {}

            XposedHelpers.findAndHookMethod("android.view.View", lpparam.classLoader, "setOnClickListener",
                View.OnClickListener.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        View view = (View) param.thisObject;
                        Object listener = param.args[0];
                        if (listener == null) return;
                        String text = findTextInTree(view);
                        if (text != null && text.contains("显示更多")) {
                            sShowMoreListener = (View.OnClickListener) listener;
                            log("Captured showMore: " + listener.getClass().getName());
                        }
                    }
                });

            try {
                XposedHelpers.findAndHookMethod("android.widget.TextView", lpparam.classLoader, "setText",
                    CharSequence.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (sMarkerPosition >= 0) return;
                            CharSequence text = (CharSequence) param.args[0];
                            if (text != null && text.toString().contains("点击刷新")) {
                                View tv = (View) param.thisObject;
                                View rvChild = tv;
                                while (rvChild != null && rvChild.getParent() instanceof View) {
                                    View parent = (View) rvChild.getParent();
                                    if (parent == sFeedRV) break;
                                    rvChild = parent;
                                }
                                if (rvChild != null && sFeedRV != null) {
                                    try {
                                        int pos = (int) XposedHelpers.callMethod(sFeedRV, "getChildAdapterPosition", rvChild);
                                        if (pos >= 0) {
                                            sMarkerPosition = pos;
                                            log("Marker at position " + pos);
                                        }
                                    } catch (Throwable ignored) {}
                                }
                            }
                        }
                    });
            } catch (Throwable ignored) {}

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
                        if (lm == null) return;
                        String name = lm.getClass().getSimpleName();
                        if (!"CustomLinearLayoutManager".equals(name)) return;
                        sFeedRV = param.thisObject;
                        log("Feed RV found");
                    }
                });

            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        FloatingButton.hookActivity(lpparam.classLoader, () -> {
                            log("Double-tap: start scroll, marker=" + sMarkerPosition);
                            sDone = false;
                            sScrolling = false;
                            sStableChecks = 0;
                            sLastCount = 0;
                            sListenerFired = false;
                            sScrollTarget = 0;
                            if (sFeedRV != null) {
                                startPolling();
                            }
                        }, () -> {
                            log("Single-tap: stop");
                            sDone = true;
                            sScrolling = false;
                        });
                        log("App onCreate");
                    }
                });

            log("Hooked successfully");
        } catch (Throwable t) {
            log("Failed: " + t.getMessage());
        }
    }
}
