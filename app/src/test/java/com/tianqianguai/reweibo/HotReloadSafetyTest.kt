package com.tianqianguai.reweibo

import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HotReloadSafetyTest {
    @Test
    fun queuedPersistWorkBlocksReloadUntilTheWorkerFinishes() {
        assertEquals(
            "persist-native-pending",
            WeiboLiteHook.timelinePersistBlockerForState(true, false, false)
        )
        assertEquals(
            "persist-shadow-pending",
            WeiboLiteHook.timelinePersistBlockerForState(false, true, false)
        )
        assertEquals(
            "persist-worker-scheduled",
            WeiboLiteHook.timelinePersistBlockerForState(false, false, true)
        )
        assertNull(WeiboLiteHook.timelinePersistBlockerForState(false, false, false))
    }

    @Test
    fun enqueueRaceIsRejectedAfterReloadPreparationStarts() {
        assertFalse(WeiboLiteHook.shouldAcceptTimelinePersistEnqueue(true, true))
        assertFalse(WeiboLiteHook.shouldAcceptTimelinePersistEnqueue(false, false))
        assertTrue(WeiboLiteHook.shouldAcceptTimelinePersistEnqueue(false, true))
    }

    @Test
    fun failedUiCleanupNeverEntersDestructiveQuiesce() {
        val cliCalls = AtomicInteger()
        val quiesceCalls = AtomicInteger()

        val prepared = HotReloadPreparation.run(
            { false },
            { cliCalls.incrementAndGet(); true },
            { quiesceCalls.incrementAndGet(); true }
        )

        assertFalse(prepared)
        assertEquals(0, cliCalls.get())
        assertEquals(0, quiesceCalls.get())
    }

    @Test
    fun failedCliCleanupLeavesDestructiveQuiesceUntouched() {
        val quiesceCalls = AtomicInteger()

        val prepared = HotReloadPreparation.run(
            { true },
            { false },
            { quiesceCalls.incrementAndGet(); true }
        )

        assertFalse(prepared)
        assertEquals(0, quiesceCalls.get())
    }
}
