package com.tianqianguai.reweibo;

import java.util.function.Function;

final class CompatibilityPolicy {
    private CompatibilityPolicy() {}
    static boolean contains(Object ancestor, Object child, Function<Object, Object> parent) {
        if (ancestor == null || child == null) return false;
        for (Object current = parent.apply(child); current != null; current = parent.apply(current)) {
            if (current == ancestor) return true;
        }
        return false;
    }
    static boolean userOwnsViewport(boolean pointerDown, int scrollState) {
        return pointerDown || scrollState != 0;
    }
}
