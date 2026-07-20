package com.tianqianguai.reweibo

import org.junit.Test

import org.junit.Assert.*
import java.util.Calendar

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    private open class ParentStatus {
        @JvmField
        val promotion: String = "ad"

        fun getStatusId(): Long = 42L
    }

    private class ChildStatus : ParentStatus()

    private class TestStatus(private val id: Long, val source: String) {
        fun getId(): Long = id
    }

    @Test
    fun timelineCacheSettingAcceptsThirtyDays() {
        assertEquals(1, ModuleSettings.clampTimelineCacheDays(0))
        assertEquals(3, ModuleSettings.clampTimelineCacheDays(3))
        assertEquals(30, ModuleSettings.clampTimelineCacheDays(30))
        assertEquals(30, ModuleSettings.clampTimelineCacheDays(31))
    }

    @Test
    fun thirtyDayCacheExpandsPreloadSafetyLimits() {
        assertEquals(240, WeiboLiteHook.timelinePreloadMaxPagesForCacheDays(3))
        assertEquals(10_000, WeiboLiteHook.timelinePreloadMaxItemsForCacheDays(3))
        assertEquals(1_200, WeiboLiteHook.timelinePreloadMaxPagesForCacheDays(30))
        assertEquals(30_000, WeiboLiteHook.timelinePreloadMaxItemsForCacheDays(30))
    }

    @Test
    fun cacheDurationCannotCompleteBeforeSettingIsConfirmed() {
        val dayMs = 24L * 60L * 60L * 1000L
        val nowMs = 1_800_000_000_000L
        val newestMs = nowMs - 60_000L
        val oldestMs = newestMs - 8L * dayMs

        assertFalse(
            WeiboLiteHook.isTimelineCacheDurationReadyForSetting(
                3500, 3500, oldestMs, newestMs, nowMs, 3, false
            )
        )
        assertTrue(
            WeiboLiteHook.isTimelineCacheDurationReadyForSetting(
                3500, 3500, oldestMs, newestMs, nowMs, 3, true
            )
        )
        assertFalse(
            WeiboLiteHook.isTimelineCacheDurationReadyForSetting(
                3500, 3500, oldestMs, newestMs, nowMs, 30, true
            )
        )
    }

    @Test
    fun stableEmptyRoundsRetryUntilDurationOrSafetyLimit() {
        assertFalse(WeiboLiteHook.shouldStopTimelinePreload(false, 8, 1200, 3500, 30000))
        assertTrue(WeiboLiteHook.shouldStopTimelinePreload(true, 8, 1200, 3500, 30000))
        assertTrue(WeiboLiteHook.shouldStopTimelinePreload(false, 1200, 1200, 3500, 30000))
        assertTrue(WeiboLiteHook.shouldStopTimelinePreload(false, 8, 1200, 30000, 30000))
        assertTrue(WeiboLiteHook.timelinePreloadRetryDelayMs(8) > 0L)
        assertTrue(
            WeiboLiteHook.timelinePreloadRetryDelayMs(30) >=
                WeiboLiteHook.timelinePreloadRetryDelayMs(8)
        )
    }

    @Test
    fun cacheSourcesMergeByStatusIdWithoutDroppingHistory() {
        val live = TestStatus(3L, "live")
        val nativeDuplicate = TestStatus(3L, "native")
        val native = TestStatus(2L, "native")
        val shadowDuplicate = TestStatus(2L, "shadow")
        val shadow = TestStatus(1L, "shadow")

        val merged = WeiboLiteHook.mergeTimelineStatusLists(
            listOf(live),
            listOf(nativeDuplicate, native),
            listOf(shadowDuplicate, shadow)
        ).map { it as TestStatus }

        assertEquals(listOf(3L, 2L, 1L), merged.map { it.getId() })
        assertEquals(listOf("live", "native", "shadow"), merged.map { it.source })
    }

    @Test
    fun disjointFreshCacheCannotOverwriteHistoricalShadow() {
        val now = 1_800_000_000_000L
        val cutoff = now - 30 * 24 * 60 * 60 * 1000L

        assertFalse(
            WeiboLiteHook.shouldReplaceTimelineShadowCache(
                1880, now - 8 * 24 * 60 * 60 * 1000L, now - 5 * 24 * 60 * 60 * 1000L,
                480, now - 3 * 24 * 60 * 60 * 1000L, now - 30 * 60 * 1000L,
                cutoff
            )
        )
        assertTrue(
            WeiboLiteHook.shouldReplaceTimelineShadowCache(
                1880, now - 8 * 24 * 60 * 60 * 1000L, now - 5 * 24 * 60 * 60 * 1000L,
                2360, now - 8 * 24 * 60 * 60 * 1000L, now - 30 * 60 * 1000L,
                cutoff
            )
        )
        assertFalse(
            WeiboLiteHook.shouldReplaceTimelineShadowCache(
                1880, now - 40 * 24 * 60 * 60 * 1000L, now - 5 * 24 * 60 * 60 * 1000L,
                480, now - 3 * 24 * 60 * 60 * 1000L, now - 30 * 60 * 1000L,
                cutoff
            )
        )
        assertTrue(
            WeiboLiteHook.shouldReplaceTimelineShadowCache(
                1880, now - 40 * 24 * 60 * 60 * 1000L, now - 5 * 24 * 60 * 60 * 1000L,
                480, now - 3 * 24 * 60 * 60 * 1000L, now - 30 * 60 * 1000L,
                now - 3 * 24 * 60 * 60 * 1000L
            )
        )
    }

    @Test
    fun cacheCompletionRequiresARecentHead() {
        val now = 1_800_000_000_000L

        assertTrue(WeiboLiteHook.isTimelineCacheHeadFresh(now - 60 * 60 * 1000L, now))
        assertFalse(WeiboLiteHook.isTimelineCacheHeadFresh(now - 5 * 24 * 60 * 60 * 1000L, now))
        assertFalse(WeiboLiteHook.isTimelineCacheHeadFresh(now + 10 * 60 * 1000L, now))
    }

    @Test
    fun cacheWindowIsAnchoredToWallClock() {
        val now = 1_800_000_000_000L

        assertEquals(
            now - 3 * 24 * 60 * 60 * 1000L,
            WeiboLiteHook.timelineCacheCutoffMs(now, 3)
        )
    }

    @Test
    fun cacheClearRangeIsInclusiveAndKeepsUnknownTimestamps() {
        val start = 1_800_000_000_000L
        val end = start + 60_000L

        assertTrue(WeiboLiteHook.shouldClearTimelineStatusCreatedAt(start, start, end))
        assertTrue(WeiboLiteHook.shouldClearTimelineStatusCreatedAt(end, start, end))
        assertTrue(WeiboLiteHook.shouldClearTimelineStatusCreatedAt(start + 30_000L, start, end))
        assertFalse(WeiboLiteHook.shouldClearTimelineStatusCreatedAt(start - 1L, start, end))
        assertFalse(WeiboLiteHook.shouldClearTimelineStatusCreatedAt(end + 1L, start, end))
        assertFalse(WeiboLiteHook.shouldClearTimelineStatusCreatedAt(0L, start, end))
        assertFalse(WeiboLiteHook.shouldClearTimelineStatusCreatedAt(start, end, start))
    }

    @Test
    fun looseSingleDayInputExpandsToTheWholeLocalDay() {
        val reference = Calendar.getInstance()
        val start = WeiboLiteHook.parseTimelineClearDateOnlyInput("7号", false)
        val end = WeiboLiteHook.parseTimelineClearDateOnlyInput("7日", true)
        val startValue = Calendar.getInstance().apply { timeInMillis = start }
        val endValue = Calendar.getInstance().apply { timeInMillis = end }

        assertTrue(start > 0L)
        assertTrue(end > start)
        assertEquals(reference.get(Calendar.YEAR), startValue.get(Calendar.YEAR))
        assertEquals(reference.get(Calendar.MONTH), startValue.get(Calendar.MONTH))
        assertEquals(7, startValue.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startValue.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startValue.get(Calendar.MINUTE))
        assertEquals(0, startValue.get(Calendar.SECOND))
        assertEquals(0, startValue.get(Calendar.MILLISECOND))
        assertEquals(7, endValue.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endValue.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endValue.get(Calendar.MINUTE))
        assertEquals(59, endValue.get(Calendar.SECOND))
        assertEquals(999, endValue.get(Calendar.MILLISECOND))
        assertEquals(start, WeiboLiteHook.parseTimelineClearDateOnlyInput("7", false))
        assertEquals(
            start,
            WeiboLiteHook.parseTimelineClearDateOnlyInput(
                "${reference.get(Calendar.MONTH) + 1}-7",
                false
            )
        )
    }

    @Test
    fun looseDateInputSupportsFullDatesAndRejectsImpossibleDates() {
        val dayStart = WeiboLiteHook.parseTimelineClearTimeInput("2026-07-07", false)
        val dayEnd = WeiboLiteHook.parseTimelineClearTimeInput("20260707", true)
        val preciseStart = WeiboLiteHook.parseTimelineClearTimeInput("2026年7月7日 12:34", false)
        val preciseEnd = WeiboLiteHook.parseTimelineClearTimeInput("2026/7/7 12:34", true)

        assertTrue(dayStart > 0L)
        assertTrue(dayEnd > dayStart)
        assertEquals(59_999L, preciseEnd - preciseStart)
        assertEquals(-1L, WeiboLiteHook.parseTimelineClearDateOnlyInput("2月30日", false))
        assertEquals(-1L, WeiboLiteHook.parseTimelineClearTimeInput("2026-02-30 10:00", false))
        assertEquals(-1L, WeiboLiteHook.parseTimelineClearDateOnlyInput("32号", false))
    }

    @Test
    fun staleTerminalMarkerCannotCompletePreloadByCountAlone() {
        val now = 1_800_000_000_000L

        assertFalse(
            WeiboLiteHook.isTimelinePreloadMarkerSatisfied(
                1880, 3, 3 * 24 * 60 * 60 * 1000L,
                true, true,
                now - 5 * 24 * 60 * 60 * 1000L,
                now, 3
            )
        )
        assertTrue(
            WeiboLiteHook.isTimelinePreloadMarkerSatisfied(
                1880, 3, 3 * 24 * 60 * 60 * 1000L,
                true, true,
                now - 60 * 60 * 1000L,
                now, 3
            )
        )
        assertFalse(
            WeiboLiteHook.isTimelinePreloadMarkerSatisfied(
                1880, 3, 3 * 24 * 60 * 60 * 1000L,
                true, true,
                now - 60 * 60 * 1000L,
                now, 30
            )
        )
    }

    @Test
    fun preloadMarkerCountCannotStopARefillAfterTheCacheHeadAdvances() {
        val markerNewest = 1_800_000_000_000L

        assertTrue(
            WeiboLiteHook.isTimelinePreloadMarkerHeadCompatible(
                markerNewest + 30 * 60 * 1000L,
                markerNewest
            )
        )
        assertFalse(
            WeiboLiteHook.isTimelinePreloadMarkerHeadCompatible(
                markerNewest + 4 * 60 * 60 * 1000L,
                markerNewest
            )
        )
        assertFalse(
            WeiboLiteHook.isTimelinePreloadMarkerHeadCompatible(
                markerNewest - 2 * 60 * 60 * 1000L,
                markerNewest
            )
        )
    }

    @Test
    fun incompleteNativePayloadCannotOverwriteAFullShadowCache() {
        assertFalse(
            WeiboLiteHook.isTimelineShadowPayloadPlausible(
                1_642, 14_500_000L,
                1_642, 218_325L
            )
        )
        assertTrue(
            WeiboLiteHook.isTimelineShadowPayloadPlausible(
                1_642, 14_500_000L,
                1_666, 14_690_765L
            )
        )
    }

    @Test
    fun inheritedFieldsRemainReadableAcrossRepeatedLookups() {
        val status = ChildStatus()

        repeat(1_000) {
            assertEquals("ad", WeiboLiteHook.getFieldValue(status, "promotion"))
            assertEquals(42L, WeiboLiteHook.callNoArgMethodCached(status, "getStatusId"))
        }
    }

    @Test
    fun missingReflectionLookupsStayOnTheNullFastPath() {
        val status = ChildStatus()

        repeat(10_000) {
            assertNull(WeiboLiteHook.getFieldValueOrNull(status, "missingField"))
            assertNull(WeiboLiteHook.callNoArgMethodCached(status, "missingMethod"))
        }
    }

    @Test
    fun fullNativeCacheWinsWithoutReadingAnEquivalentShadowCopy() {
        assertFalse(
            WeiboLiteHook.shouldReadTimelineShadowFirst(
                47_717_754L, 47_717_754L, 5_480
            )
        )
        assertTrue(
            WeiboLiteHook.shouldReadTimelineShadowFirst(
                200_000L, 47_717_754L, 5_480
            )
        )
        assertFalse(
            WeiboLiteHook.shouldReadTimelineShadowFirst(
                47_717_754L, 0L, 0
            )
        )
    }

    @Test
    fun partialCacheWritesWaitForAnInFlightRestore() {
        assertTrue(WeiboLiteHook.shouldDeferTimelinePersist(false, true))
        assertFalse(WeiboLiteHook.shouldDeferTimelinePersist(true, true))
        assertFalse(WeiboLiteHook.shouldDeferTimelinePersist(false, false))
    }

    @Test
    fun completeRestoreCanUseLastReadWithoutStrictPreloadMarker() {
        assertTrue(WeiboLiteHook.shouldUseTimelineLastRead(true, false))
        assertTrue(WeiboLiteHook.shouldUseTimelineLastRead(false, false))
    }
}
