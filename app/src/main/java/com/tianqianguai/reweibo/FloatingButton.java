package com.tianqianguai.reweibo;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
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
    private static Runnable sPendingSingleTap = null;
    private static boolean sDoubleTapped = false;

    public interface DoubleTapAction {
        void onDoubleTap();
    }

    public interface SingleTapAction {
        void onSingleTap();
    }

    private static TextView createButton(Activity activity) {
        TextView btn = new TextView(activity);
        btn.setText("R");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(16);
        btn.setBackgroundColor(0xDDFF5722);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(32, 32, 32, 32);
        return btn;
    }

    private static void setupTouch(TextView btn, DoubleTapAction doubleAction, SingleTapAction singleAction) {
        btn.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                long now = SystemClock.elapsedRealtime();
                if (now - sLastTap < DOUBLE_TAP_MS) {
                    // Double tap
                    sLastTap = 0;
                    sDoubleTapped = true;
                    if (sPendingSingleTap != null) {
                        new Handler(Looper.getMainLooper()).removeCallbacks(sPendingSingleTap);
                        sPendingSingleTap = null;
                    }
                    v.setBackgroundColor(0xDD00AA00);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        v.setBackgroundColor(0xDDFF5722);
                    }, 300);
                    try { doubleAction.onDoubleTap(); } catch (Throwable t) {
                        XposedBridge.log("ReWeibo: doubleTap error: " + t.getMessage());
                    }
                    // Reset flag after a delay
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        sDoubleTapped = false;
                    }, DOUBLE_TAP_MS);
                } else {
                    // First tap — wait to see if double tap follows
                    sLastTap = now;
                    sDoubleTapped = false;
                    if (sPendingSingleTap != null) {
                        new Handler(Looper.getMainLooper()).removeCallbacks(sPendingSingleTap);
                    }
                    sPendingSingleTap = () -> {
                        sPendingSingleTap = null;
                        if (!sDoubleTapped && singleAction != null) {
                            try { singleAction.onSingleTap(); } catch (Throwable t) {
                                XposedBridge.log("ReWeibo: singleTap error: " + t.getMessage());
                            }
                        }
                    };
                    new Handler(Looper.getMainLooper()).postDelayed(sPendingSingleTap, DOUBLE_TAP_MS);
                }
            }
            return true;
        });
    }

    private static void removeButton() {
        if (sButton == null) return;
        try {
            if (sButton.getParent() instanceof WindowManager) {
                ((WindowManager) sButton.getParent()).removeViewImmediate(sButton);
            } else if (sButton.getParent() instanceof FrameLayout) {
                ((FrameLayout) sButton.getParent()).removeView(sButton);
            }
        } catch (Throwable ignored) {}
        sButton = null;
    }

    public static void attachToActivity(Activity activity, DoubleTapAction doubleAction, SingleTapAction singleAction) {
        if (activity == sLastActivity && sButton != null) return;
        if (activity != sLastActivity) {
            removeButton();
        }
        sLastActivity = activity;
        try {
            XposedBridge.log("ReWeibo: attachToActivity: " + activity.getClass().getName());
            sButton = createButton(activity);
            setupTouch(sButton, doubleAction, singleAction);

            // Try WindowManager first (works above all views)
            try {
                WindowManager.LayoutParams wparams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                );
                wparams.gravity = Gravity.BOTTOM | Gravity.END;
                wparams.x = 48;
                wparams.y = 400;
                wparams.token = activity.getWindow().getDecorView().getWindowToken();

                WindowManager wm = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
                wm.addView(sButton, wparams);
                XposedBridge.log("ReWeibo: FloatingButton via WindowManager");
                return;
            } catch (Throwable t) {
                XposedBridge.log("ReWeibo: WindowManager failed: " + t.getMessage());
                sButton = null;
            }

            // Fallback: addContentView
            sButton = createButton(activity);
            setupTouch(sButton, doubleAction, singleAction);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.bottomMargin = 400;
            params.rightMargin = 48;
            activity.addContentView(sButton, params);
            XposedBridge.log("ReWeibo: FloatingButton via addContentView");
        } catch (Throwable t) {
            XposedBridge.log("ReWeibo: FloatingButton error: " + t.getMessage());
            sButton = null;
        }
    }

    public static void hookActivity(ClassLoader cl, DoubleTapAction doubleAction, SingleTapAction singleAction) {
        try {
            XposedHelpers.findAndHookMethod("android.app.Activity", cl, "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            attachToActivity(activity, doubleAction, singleAction);
                        }, 1000);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("ReWeibo: hookActivity error: " + t.getMessage());
        }
    }
}
