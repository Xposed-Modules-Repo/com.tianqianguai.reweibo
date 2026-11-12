package com.tianqianguai.reweibo;

/** Constant-space per-gesture counters; no allocation, logging or reflection on MOVE/scroll. */
final class GestureEvidence {
    float startY, lastY, distance;
    long travel;
    int moves, scrolls, mutations;
    boolean delivered, cancelled;
    void reset(float y) {
        startY = lastY = y; distance = 0; travel = 0;
        moves = scrolls = mutations = 0; delivered = cancelled = false;
    }
    void move(float y) { lastY = y; distance = Math.max(distance, Math.abs(y - startY)); moves++; }
    void scroll(int dy) { travel += Math.abs((long) dy); if (dy != 0) scrolls++; }
    String classify(float threshold, boolean boundary, boolean sameViewport, boolean validViewport) {
        if (cancelled || distance < threshold || moves < 2) return "normal";
        if (!delivered) return "gesture_not_delivered";
        if (travel == 0 && sameViewport && validViewport) return boundary ? "boundary" : "gesture_no_scroll";
        if (mutations > 0 && travel >= threshold && sameViewport && validViewport) return "possible_anchor_pullback";
        if (mutations > 0) return "module_during_gesture";
        return "normal";
    }
}
