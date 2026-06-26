package com.tianqianguai.reweibo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class FloatingButton {

    private static TextView sButton = null;
    private static long sLastTap = 0;
    private static final long DOUBLE_TAP_MS = 500;
    private static Activity sLastActivity = null;

    public interface DoubleTapAction {
        void onDoubleTap();
    }

    public static void attachToActivity(Activity activity, DoubleTapAction action) {
        if (sButton != null) return;
        if (activity == sLastActivity) return;
        sLastActivity = activity;
        try {
            String actName = activity.getClass().getName();
            XposedBridge.log("ReWeibo: attachToActivity: " + actName);

            sButton = new TextView(activity);
            sButton.setText("R");
            sButton.setTextColor(Color.WHITE);
            sButton.setTextSize(16);
            sButton.setBackgroundColor(0xDDFF5722);
            sButton.setGravity(Gravity.CENTER);
            sButton.setPadding(32, 32, 32, 32);

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.bottomMargin = 400;
            params.rightMargin = 48;

            sButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    long now = SystemClock.elapsedRealtime();
                    if (now - sLastTap < DOUBLE_TAP_MS) {
                        sLastTap = 0;
                        v.setBackgroundColor(0xDD00AA00);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            v.setBackgroundColor(0xDDFF5722);
                        }, 300);
                        try { action.onDoubleTap(); } catch (Throwable t) {
                            XposedBridge.log("ReWeibo: action error: " + t.getMessage());
                        }
                    } else {
                        sLastTap = now;
                    }
                }
                return true;
            });

            activity.addContentView(sButton, params);
            XposedBridge.log("ReWeibo: FloatingButton added via addContentView");
        } catch (Throwable t) {
            XposedBridge.log("ReWeibo: FloatingButton error: " + t.getMessage());
        }
    }

    public static void hookActivity(ClassLoader cl, DoubleTapAction action) {
        try {
            XposedHelpers.findAndHookMethod("android.app.Activity", cl, "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            attachToActivity(activity, action);
                        }, 1000);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("ReWeibo: hookActivity error: " + t.getMessage());
        }
    }
}
