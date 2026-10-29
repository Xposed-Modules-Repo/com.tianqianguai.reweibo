package com.tianqianguai.reweibo;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class CacheSourceSelectionTest {
    @Test public void expiredLargeShadowDoesNotDisplaceRecentNativeCheckpoint() {
        assertFalse(WeiboLiteHook.shouldSupplementTimelineCache(1981, 14_000_000,
            133_992_882, 15120, 1000, 2000));
    }
    @Test public void validOrUndatedShadowStillSupplementsSmallNativeCache() {
        assertTrue(WeiboLiteHook.shouldSupplementTimelineCache(50, 10000, 1000000, 1000, 3000, 2000));
        assertTrue(WeiboLiteHook.shouldSupplementTimelineCache(50, 10000, 1000000, 1000, 0, 2000));
    }
    @Test public void missingNativeRetainsShadowRecoveryEvenWithoutMetadata() {
        assertTrue(WeiboLiteHook.shouldSupplementTimelineCache(0, 0, 10000, 0, 1000, 2000));
    }
    @Test public void supplementPreservesNativeNewPostsAndUpdatedDuplicates() {
        Status recent = new Status(3), updated = new Status(2), stale = new Status(2), old = new Status(1);
        assertEquals(Arrays.asList(recent, updated, old), WeiboLiteHook.mergeTimelineStatusLists(
            Arrays.asList(recent, updated), Arrays.asList(stale, old), null));
        assertEquals(Collections.singletonList(old), WeiboLiteHook.mergeTimelineStatusLists(null,
            Collections.singletonList(old), null));
    }
    public static class Status {
        final long id;
        Status(long id) { this.id = id; }
        public long getId() { return id; }
    }
}
