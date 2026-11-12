package com.tianqianguai.reweibo;

import org.junit.Test;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class Compatibility701Test {
    @Test public void terminalEvidenceDiffersFromUninitialisedCursor() {
        PagingEvidence state = new PagingEvidence();
        assertFalse(state.terminal()); assertTrue(state.mayRestoreCursor());
        int token = state.request(false);
        assertFalse(state.mayRestoreCursor());
        assertTrue(state.response(token, 0)); assertTrue(state.terminal());
        assertTrue(PagingEvidence.shouldWait(0, false));
        assertFalse(PagingEvidence.shouldWait(0, true)); // gap probes have their own cursor
        assertFalse(PagingEvidence.shouldWait(100, false));
        assertFalse(PagingEvidence.shouldWait(-1, false)); // legacy action without V3 cursor
    }

    @Test public void refreshingInvalidatesOldTerminalAndAcceptsNewCursor() {
        PagingEvidence state = new PagingEvidence();
        int previous = state.request(false); state.response(previous, 0);
        int current = state.request(true);
        assertFalse(state.terminal());
        assertFalse(state.response(previous, 0));
        assertTrue(state.response(current, 1234));
        assertEquals(1234, state.nextCursor); assertFalse(state.terminal());
        state.response(current, 0);
        assertFalse(state.terminalFor(previous)); assertTrue(state.terminalFor(current));
    }

    public static class Feed { final Object more; Feed(Object more) {this.more=more;} public Object getMoreInfo(){return more;} }
    public static class More { final Object params; More(Object params){this.params=params;} public Object getParams(){return params;} }
    public static class Params { final long cursor; Params(long cursor){this.cursor=cursor;} public long getMaxId(){return cursor;} }
    @Test public void hostTerminalShapesAreRecognizedButReflectionFailureIsUnknown() throws Exception {
        Method cursor = WeiboLiteHook.class.getDeclaredMethod("timelineNextCursor", Object.class); cursor.setAccessible(true);
        assertEquals(0L,cursor.invoke(null,new Feed(null)));
        assertEquals(0L,cursor.invoke(null,new Feed(new More(null))));
        assertEquals(0L,cursor.invoke(null,new Feed(new More(new Params(0)))));
        assertEquals(200L,cursor.invoke(null,new Feed(new More(new Params(200)))));
        assertEquals(-1L,cursor.invoke(null,new Object()));
    }

    static class Node { final Node parent; Node(Node parent){this.parent=parent;} }
    @Test public void refreshSuppressionOnlyMatchesActualHomeAncestors() {
        Node home = new Node(null), other = new Node(null), list = new Node(new Node(home));
        assertTrue(CompatibilityPolicy.contains(home,list,n->((Node)n).parent));
        assertFalse(CompatibilityPolicy.contains(other,list,n->((Node)n).parent));
        assertFalse(CompatibilityPolicy.contains(null,list,n->((Node)n).parent));
    }

    @Test public void gestureAndFlingPreventAnchorCaptureAndRestore() {
        assertTrue(CompatibilityPolicy.userOwnsViewport(true,0));
        assertTrue(CompatibilityPolicy.userOwnsViewport(false,1));
        assertTrue(CompatibilityPolicy.userOwnsViewport(false,2));
        assertFalse(CompatibilityPolicy.userOwnsViewport(false,0));
    }

    public static class Owner { public String getGroupId() { return "-1"; } }
    @Test public void cursorObservationStaysEnabledWhenOnlyCacheIsEnabled() throws Exception {
        Field features = WeiboLiteHook.class.getDeclaredField("sFeatures"); features.setAccessible(true);
        Object original = features.get(null);
        try {
            Map<String, Boolean> values = new LinkedHashMap<>();
            for (String key : ModuleSettings.booleanKeys()) values.put(key, false);
            values.put(ModuleSettings.KEY_WEICO_TIMELINE_CACHE, true);
            features.set(null, ModuleSettings.featureSnapshot(values));
            Method observe = WeiboLiteHook.class.getDeclaredMethod("shouldLogTimelineNetworkProbe", Object.class);
            observe.setAccessible(true);
            assertEquals(true, observe.invoke(null, new Owner()));
            values.put(ModuleSettings.KEY_WEICO_TIMELINE_CACHE, false);
            features.set(null, ModuleSettings.featureSnapshot(values));
            assertEquals(false, observe.invoke(null, new Owner()));
        } finally { features.set(null, original); }
    }

    @Test public void slowVideoRequestOwnershipDoesNotExpireWithRecursionGuard() {
        VideoRefreshRequests requests = new VideoRefreshRequests();
        Object moduleObserver = new Object(), nativeObserver = new Object();
        Boolean previous = requests.begin(); requests.created(moduleObserver); requests.end(previous);
        requests.created(nativeObserver);
        // No clock-based expiry: completion belongs to the observer even after any playback bypass window.
        assertFalse(requests.complete(nativeObserver));
        assertTrue(requests.complete(moduleObserver));
        assertFalse(requests.complete(moduleObserver));
    }
}
