package com.tianqianguai.reweibo

import io.github.libxposed.api.XposedInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

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

    @Test
    fun routeSlotStateAcceptsEmptySlotsAcrossLegacyPayloadLengths() {
        val base = HotReloadState.compose(null, null, null, null, null, 3L)
        val emptyRoute = AtomicReference<XposedInterface.Hooker>(null)
        val routed = HotReloadState.withRoutes(base, mapOf("hook-id" to emptyRoute))
        val withFeatures = HotReloadState.withFeatures(routed, booleanArrayOf(true, false))

        assertTrue(HotReloadState.isValid(base, HotReloadState::class.java.classLoader))
        assertTrue(HotReloadState.isValid(HotReloadState.withFeatures(base, booleanArrayOf()),
            HotReloadState::class.java.classLoader))
        assertTrue(HotReloadState.isValid(routed, HotReloadState::class.java.classLoader))
        assertTrue(HotReloadState.isValid(withFeatures, HotReloadState::class.java.classLoader))
        assertEquals(emptyRoute, HotReloadState.routes(routed)["hook-id"])
        assertTrue(HotReloadState.features(withFeatures)!!.contentEquals(booleanArrayOf(true, false)))
    }

    @Test
    fun moduleOwnedObjectInsideAtomicRouteIsRejected() {
        val route = AtomicReference<Any?>(ModuleOwned())
        assertFalse(
            HotReloadState.isClassLoaderNeutral(route, HotReloadState::class.java.classLoader)
        )
    }
}
