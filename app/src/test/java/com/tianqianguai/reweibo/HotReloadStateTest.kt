package com.tianqianguai.reweibo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HotReloadStateTest {
    private class ModuleOwned

    @Test
    fun savedPayloadUsesBootArrayAndContainsNoModuleOwnedObject() {
        val state = HotReloadState.compose(
            "context",
            "presenter",
            "recycler",
            "fragment",
            "activity",
            7L
        )

        assertEquals(Array<Any>::class.java, state.javaClass)
        assertTrue(HotReloadState.isValid(state, HotReloadState::class.java.classLoader))
        assertEquals(7L, HotReloadState.previousGeneration(state))
    }

    @Test
    fun moduleClassloaderObjectIsRejected() {
        val contaminated = HotReloadState.compose(
            ModuleOwned(),
            null,
            null,
            null,
            null,
            1L
        )

        assertFalse(
            HotReloadState.isValid(contaminated, HotReloadState::class.java.classLoader)
        )
    }
}
