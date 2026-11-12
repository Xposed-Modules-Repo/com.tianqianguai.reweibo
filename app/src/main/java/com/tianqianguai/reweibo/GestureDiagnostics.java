package com.tianqianguai.reweibo;

import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.lang.ref.WeakReference;
import com.tianqianguai.reweibo.compat.XposedHelpers;

/** Observes one visible home gesture. Two viewport samples, at most one delayed callback per gesture. */
final class GestureDiagnostics {
    private static final GestureEvidence evidence = new GestureEvidence();
    private static WeakReference<Object> target = new WeakReference<>(null);
    private static final int[] location = new int[2];
    private static boolean active, observing;
    private static long started, gesture, parent;
    private static int startPosition, startTop, generation;
    private static float threshold;
    private static String lastMutation = "none";
    private static int lastTarget = -1;
    private static Runnable pending;
    private GestureDiagnostics() {}

    static boolean enabled() { return Diagnostics.enabled() && Looper.myLooper() == Looper.getMainLooper(); }

    static void begin(Object recycler, MotionEvent event) {
        if (!enabled() || !(recycler instanceof View)) return;
        View view = (View) recycler;
        if (!view.isShown() || !view.hasWindowFocus()) return;
        view.getLocationOnScreen(location);
        if (event.getRawX() < location[0] || event.getRawX() > location[0] + view.getWidth()
            || event.getRawY() < location[1] || event.getRawY() > location[1] + view.getHeight()) return;
        finish();
        target = new WeakReference<>(recycler);
        evidence.reset(event.getRawY());
        threshold = 48 * view.getResources().getDisplayMetrics().density;
        started = SystemClock.uptimeMillis();
        gesture++;
        parent = Diagnostics.context();
        startPosition = position(recycler);
        startTop = top(recycler, startPosition);
        lastMutation = "none"; lastTarget = -1;
        observing = active = true;
    }

    static void touch(MotionEvent event) {
        if (!enabled() || !active) return;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_MOVE) evidence.move(event.getRawY());
        else if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_CANCEL) {
            evidence.cancelled = true; finish();
        } else if (action == MotionEvent.ACTION_UP) {
            active = false;
            final int token = ++generation;
            pending = () -> { if (token == generation) finish(); };
            HotReloadRuntime.postDelayed(pending, 1000);
        }
    }

    static void delivered(Object recycler) {
        if (enabled() && observing && target.get() == recycler) evidence.delivered = true;
    }
    static void scroll(Object recycler, int dy) {
        if (enabled() && observing && target.get() == recycler) evidence.scroll(dy);
    }
    static void mutation(String source, int position) {
        if (!enabled() || !observing) return;
        evidence.mutations++;
        lastMutation = source;
        lastTarget = position;
    }
    static void mutation(Object recycler, String source, int position) {
        if (target.get() == recycler) mutation(source, position);
    }
    static void clear() {
        HotReloadRuntime.removeCallbacks(pending); pending = null;
        generation++; active = observing = false; target.clear();
    }
    private static void finish() {
        if (!observing) return;
        Object recycler = target.get();
        if (!(recycler instanceof View) || !((View) recycler).isShown()) { clear(); return; }
        int end = position(recycler), offset = top(recycler, end);
        boolean valid = startPosition >= 0 && end >= 0 && startTop != Integer.MIN_VALUE && offset != Integer.MIN_VALUE;
        boolean same = end == startPosition && Math.abs((long)offset - startTop) <= 2;
        boolean boundary = !((View) recycler).canScrollVertically(evidence.lastY < evidence.startY ? 1 : -1);
        String result = evidence.classify(threshold, boundary, same, valid);
        if ((!result.equals("normal") && !result.equals("boundary")) || Diagnostics.detailed()) {
            if (Diagnostics.allow("gesture:" + result, 2000)) {
                Long previous = Diagnostics.enter(parent);
                try {
                    Diagnostics.event("gesture", result, "gesture=" + gesture + " start_uptime_ms=" + started
                        + " duration_ms=" + (SystemClock.uptimeMillis() - started) + " moves=" + evidence.moves
                        + " finger_px=" + (int)evidence.distance + " delivered=" + evidence.delivered
                        + " scroll_px=" + evidence.travel + " scroll_events=" + evidence.scrolls
                        + " start_position=" + startPosition + " end_position=" + end
                        + " start_top=" + startTop + " end_top=" + offset + " boundary=" + boundary
                        + " cancelled=" + evidence.cancelled + " mutations=" + evidence.mutations
                        + " last_mutation=" + lastMutation + " requested_position=" + lastTarget
                        + " evidence=correlation_not_proof", true);
                } finally { Diagnostics.leave(previous); }
            }
        }
        clear();
    }
    private static int position(Object recycler) {
        try { Object manager = XposedHelpers.callMethod(recycler, "getLayoutManager");
            return (Integer) XposedHelpers.callMethod(manager, "findFirstVisibleItemPosition");
        } catch (Throwable ignored) { return -1; }
    }
    private static int top(Object recycler, int position) {
        if (position < 0) return Integer.MIN_VALUE;
        try { Object manager = XposedHelpers.callMethod(recycler, "getLayoutManager");
            View child = (View) XposedHelpers.callMethod(manager, "findViewByPosition", position);
            return child == null ? Integer.MIN_VALUE : child.getTop();
        } catch (Throwable ignored) { return Integer.MIN_VALUE; }
    }
}
