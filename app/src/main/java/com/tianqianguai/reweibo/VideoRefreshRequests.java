package com.tianqianguai.reweibo;

import java.util.WeakHashMap;

/** Associates the actual observer, rather than a timeout or unrelated video, with module requests. */
final class VideoRefreshRequests {
    private final ThreadLocal<Boolean> creating = new ThreadLocal<>();
    private final WeakHashMap<Object, Boolean> observers = new WeakHashMap<>();
    Boolean begin() { Boolean previous = creating.get(); creating.set(true); return previous; }
    void end(Boolean previous) { if (previous == null) creating.remove(); else creating.set(previous); }
    synchronized void created(Object observer) { if (Boolean.TRUE.equals(creating.get())) observers.put(observer, true); }
    synchronized boolean complete(Object observer) { return observers.remove(observer) != null; }
    synchronized void clear() { observers.clear(); creating.remove(); }
}
