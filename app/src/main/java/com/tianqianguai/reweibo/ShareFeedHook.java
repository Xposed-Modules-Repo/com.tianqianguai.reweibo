package com.tianqianguai.reweibo;

import android.app.Application;
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
    private static android.content.Context sContext = null;

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

    private static void startPolling() {
        sScrolling = true;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable poll = new Runnable() {
            @Override
            public void run() {
                if (sDone || sFeedRV == null) return;
                try {
                    Object adapter = XposedHelpers.callMethod(sFeedRV, "getAdapter");
                    if (adapter == null) { handler.postDelayed(this, 500); return; }
                    int count = getItemCount(adapter);

                    if (count > sLastCount) {
                        sLastCount = count;
                        sStableChecks = 0;
                        sListenerFired = false;
                        XposedHelpers.callMethod(sFeedRV, "smoothScrollToPosition", count - 1);
                        log("Scrolled to " + (count - 1));
                        handler.postDelayed(this, 1500);
                    } else {
                        sStableChecks++;
                        if (sStableChecks >= 10 && sLastCount > 500) {
                            sDone = true;
                            sScrolling = false;
                            log("Done at " + (count - 1));
                        } else if (sStableChecks >= 100 && sLastCount > 100) {
                            sDone = true;
                            sScrolling = false;
                            log("Done at " + (count - 1));
                        } else if (sShowMoreListener != null && !sListenerFired) {
                            sListenerFired = true;
                            fireShowMore();
                            handler.postDelayed(this, 3000);
                        } else {
                            XposedHelpers.callMethod(sFeedRV, "smoothScrollToPosition", count - 1);
                            handler.postDelayed(this, 1500);
                        }
                    }
                } catch (Throwable t) {
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.postDelayed(poll, 500);
    }

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Setup log file
            try {
                File dir = new File("/data/data/com.hengye.share/files");
                if (!dir.exists()) dir.mkdirs();
                sLogFile = new File(dir, "reweibo_share.log");
            } catch (Throwable ignored) {}

            // Hook View.setOnClickListener to capture the "show more" listener
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

            Class<?> rvClass = XposedHelpers.findClass(
                "androidx.recyclerview.widget.RecyclerView",
                lpparam.classLoader
            );

            // Hook setLayoutManager to find feed RV and show floating button
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

            // Capture context and attach floating button
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader, "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        sContext = (Application) param.thisObject;
                        FloatingButton.hookActivity(lpparam.classLoader, () -> {
                            log("Double-tap triggered");
                            if (sFeedRV != null && !sDone && !sScrolling) {
                                startPolling();
                            }
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
