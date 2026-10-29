package com.tianqianguai.reweibo.compat

import com.tianqianguai.reweibo.HotReloadState
import com.tianqianguai.reweibo.MainHook
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.error.HookFailedError
import java.lang.reflect.Executable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class XposedHookReconcileTest {
    private class Fixture {
        fun target() = Unit
    }

    private class FakeHandle(
        private val origin: Executable,
        private val hookId: String?,
        var hooker: XposedInterface.Hooker? = null
    ) : XposedInterface.HookHandle {
        var unhooked = false
        var replaceFailure: Throwable? = null
        var replaceCalls = 0

        override fun getExecutable(): Executable = origin
        override fun getId(): String? = hookId
        override fun unhook() {
            unhooked = true
        }

        override fun replaceHook(hooker: XposedInterface.Hooker): XposedInterface.HookHandle {
            replaceCalls++
            replaceFailure?.let { throw it }
            this.hooker = hooker
            return this
        }
    }

    private class FakeInstaller(
        var nextHandle: FakeHandle?,
        var failure: Throwable? = null
    ) : XposedBridge.HookInstaller {
        var calls = 0

        override fun install(
            executable: Executable,
            id: String,
            hooker: XposedInterface.Hooker
        ): XposedInterface.HookHandle {
            calls++
            failure?.let { throw it }
            val handle = nextHandle ?: error("no fake handle configured")
            handle.hooker = hooker
            return handle
        }
    }

    private val method = Fixture::class.java.getDeclaredMethod("target")
    private val executable = XposedBridge.executableIdentity(method)

    @Test
    fun stableIdDependsOnlyOnExecutableAndSlot() {
        val first = XposedBridge.stableIdForTest(method, 0)
        val repeated = XposedBridge.stableIdForTest(method, 0)
        val secondSlot = XposedBridge.stableIdForTest(method, 1)

        assertEquals(first, repeated)
        assertTrue(first.contains(executable))
        assertFalse(first == secondSlot)
    }

    @Test
    fun failedSameIdReplacementRetainsOldHook() {
        val id = XposedBridge.stableIdForTest(method, 0)
        val old = FakeHandle(method, id)
        val identity = XposedBridge.HookIdentity(executable, id)
        val report = XposedBridge.RegistrationReport(
            setOf(identity),
            emptyMap(),
            listOf(XposedBridge.RegistrationFailure(identity, "failed")),
            false
        )

        val result = XposedBridge.reconcileOldHooks(listOf(snapshot(old, id)), report)

        assertFalse(old.unhooked)
        assertEquals(1, result.retainedAfterFailure)
    }

    @Test
    fun successfulSameIdReplacementIsAtomic() {
        val id = XposedBridge.stableIdForTest(method, 0)
        val old = FakeHandle(method, id)
        val replacement = FakeHandle(method, id)
        val identity = XposedBridge.HookIdentity(executable, id)
        val report = XposedBridge.RegistrationReport(
            setOf(identity),
            mapOf(identity to replacement),
            emptyList(),
            true
        )

        val result = XposedBridge.reconcileOldHooks(listOf(snapshot(old, id)), report)

        assertFalse(old.unhooked)
        assertEquals(1, result.atomicallyReplaced)
    }

    @Test
    fun completeGenerationRemovesStaleHandle() {
        val id = XposedBridge.stableIdForTest(method, 3)
        val old = FakeHandle(method, id)
        val report = XposedBridge.RegistrationReport(
            emptySet(),
            emptyMap(),
            emptyList(),
            true
        )

        val result = XposedBridge.reconcileOldHooks(listOf(snapshot(old, id)), report)

        assertTrue(old.unhooked)
        assertEquals(1, result.staleRemoved)
    }

    @Test
    fun lookupFailureBeforeHookMethodRetainsOldHandle() {
        val id = XposedBridge.stableIdForTest(method, 0)
        val old = FakeHandle(method, id)

        XposedBridge.beginRegistration(listOf(snapshot(old, id)))
        XposedBridge.beginRegistrationGroup("fixture")
        XposedBridge.markCurrentRegistrationGroupIncomplete(
            "method lookup failed before hook registration",
            NoSuchMethodException("missing")
        )
        XposedBridge.completeRegistrationGroup("fixture")
        val report = XposedBridge.finishRegistration(true)

        val result = XposedBridge.reconcileOldHooks(listOf(snapshot(old, id)), report)

        assertFalse(report.groupsComplete)
        assertFalse(report.completed)
        assertFalse(old.unhooked)
        assertEquals(1, result.retainedAfterFailure)
    }

    @Test
    fun optionalLookupMissDoesNotPolluteRegistrationGroup() {
        XposedBridge.beginRegistration(emptyList())
        XposedBridge.beginRegistrationGroup("optional")
        XposedBridge.beginOptionalRegistrationLookup()
        var missed = false
        try {
            XposedHelpers.findClass(
                "missing.optional.TimelineAdCandidate",
                javaClass.classLoader
            )
        } catch (_: Throwable) {
            missed = true
        } finally {
            XposedBridge.endOptionalRegistrationLookup()
        }
        XposedBridge.completeRegistrationGroup("optional")
        val report = XposedBridge.finishRegistration(true)

        assertTrue(missed)
        assertTrue(report.groupsComplete)
        assertTrue(report.completed)
        assertTrue(report.failures.isEmpty())
    }

    @Test
    fun replaceHookSucceedsWithoutTryingBuilderAndRunsNewCallback() {
        val old = installInitial()
        val id = XposedBridge.stableIdForTest(method, 0)
        val oldRoutes = XposedBridge.prepareRoutesForHotReload()
        val gen2Calls = AtomicInteger()
        old.replaceFailure = null
        val installer = FakeInstaller(null, HookFailedError("builder must not run"))

        val (handle, report) = register(
            callback(2, gen2Calls),
            listOf(snapshot(old, id)),
            oldRoutes,
            installer
        )

        assertSame(old, handle)
        assertTrue(report.completed)
        assertTrue(report.forwarded.isEmpty())
        assertEquals(0, installer.calls)
        assertEquals("origin:g2", invoke(old, AtomicInteger()))
        assertEquals(1, gen2Calls.get())
    }

    @Test
    fun builderRecoversWhenReplaceHookFails() {
        val old = installInitial()
        val id = XposedBridge.stableIdForTest(method, 0)
        val oldRoutes = XposedBridge.prepareRoutesForHotReload()
        val replacement = FakeHandle(method, id)
        val installer = FakeInstaller(replacement)
        old.replaceFailure = HookFailedError("replace internal error")
        val gen2Calls = AtomicInteger()

        val (handle, report) = register(
            callback(2, gen2Calls),
            listOf(snapshot(old, id)),
            oldRoutes,
            installer
        )

        assertSame(replacement, handle)
        assertTrue(report.completed)
        assertTrue(report.forwarded.isEmpty())
        assertEquals(1, installer.calls)
        assertEquals("origin:g2", invoke(replacement, AtomicInteger()))
        assertEquals(1, gen2Calls.get())
    }

    @Test
    fun doubleHookFailedErrorForwardsOldWrapperToCurrentGeneration() {
        val old = installInitial()
        val id = XposedBridge.stableIdForTest(method, 0)
        val oldRoutes = XposedBridge.prepareRoutesForHotReload()
        val oldRoute = oldRoutes[id]!!
        old.replaceFailure = HookFailedError("replace failed")
        val installer = FakeInstaller(null, HookFailedError("builder failed"))
        val gen2Calls = AtomicInteger()

        val (handle, report) = register(
            callback(2, gen2Calls),
            listOf(snapshot(old, id)),
            oldRoutes,
            installer
        )
        val reconciliation = XposedBridge.reconcileOldHooks(listOf(snapshot(old, id)), report)
        val originals = AtomicInteger()

        assertSame(old, handle)
        assertTrue(report.completed)
        assertTrue(report.forwarded.contains(report.attempted.single()))
        assertEquals(1, reconciliation.forwarded)
        assertEquals(0, reconciliation.atomicallyReplaced)
        assertEquals("origin:g2", invoke(old, originals))
        assertEquals(1, gen2Calls.get())
        assertEquals(1, originals.get())
        assertSame(oldRoute, XposedBridge.currentRouteForTest(id))
    }

    @Test
    fun invalidOldHandleAndFailedBuilderDoNotReportReady() {
        val old = installInitial()
        val id = XposedBridge.stableIdForTest(method, 0)
        val oldRoutes = XposedBridge.prepareRoutesForHotReload()
        old.replaceFailure = IllegalStateException("old handle is invalid")
        val installer = FakeInstaller(null, HookFailedError("builder failed"))

        val (_, report) = register(
            callback(2, AtomicInteger()),
            listOf(snapshot(old, id)),
            oldRoutes,
            installer
        )

        assertFalse(report.completed)
        assertTrue(report.successful.isEmpty())
        assertTrue(report.forwarded.isEmpty())
        assertTrue(report.failures.isNotEmpty())
        assertEquals(1, old.replaceCalls)
        assertEquals(1, installer.calls)
    }

    @Test
    fun twoForwardsCanBeAtomicallyReplacedByLaterGeneration() {
        val old = installInitial()
        val id = XposedBridge.stableIdForTest(method, 0)
        val gen2Calls = AtomicInteger()
        val gen3Calls = AtomicInteger()
        val gen4Calls = AtomicInteger()

        var routes = XposedBridge.prepareRoutesForHotReload()
        old.replaceFailure = HookFailedError("replace generation 2")
        val (_, gen2Report) = register(
            callback(2, gen2Calls), listOf(snapshot(old, id)), routes,
            FakeInstaller(null, HookFailedError("builder generation 2"))
        )
        assertTrue(gen2Report.forwarded.contains(gen2Report.attempted.single()))
        assertEquals("origin:g2", invoke(old, AtomicInteger()))

        routes = XposedBridge.prepareRoutesForHotReload()
        old.replaceFailure = HookFailedError("replace generation 3")
        val (_, gen3Report) = register(
            callback(3, gen3Calls), listOf(snapshot(old, id)), routes,
            FakeInstaller(null, HookFailedError("builder generation 3"))
        )
        assertTrue(gen3Report.forwarded.contains(gen3Report.attempted.single()))
        assertEquals("origin:g3", invoke(old, AtomicInteger()))

        routes = XposedBridge.prepareRoutesForHotReload()
        old.replaceFailure = null
        val (_, gen4Report) = register(
            callback(4, gen4Calls), listOf(snapshot(old, id)), routes,
            FakeInstaller(null, HookFailedError("builder must not run"))
        )
        val reconciliation = XposedBridge.reconcileOldHooks(listOf(snapshot(old, id)), gen4Report)

        assertTrue(gen4Report.completed)
        assertTrue(gen4Report.forwarded.isEmpty())
        assertEquals(0, gen4Report.failures.size)
        assertEquals(1, reconciliation.atomicallyReplaced)
        assertEquals(0, reconciliation.forwarded)
        assertEquals("origin:g4", invoke(old, AtomicInteger()))
        assertEquals(1, gen2Calls.get())
        assertEquals(1, gen3Calls.get())
        assertEquals(1, gen4Calls.get())
    }

    @Test
    fun rejectedReloadPreparationLeavesExistingRouteUntouched() {
        val old = installInitial()
        val id = XposedBridge.stableIdForTest(method, 0)
        val route = XposedBridge.currentRouteForTest(id)!!
        val activeCalls = AtomicInteger()
        val originals = AtomicInteger()
        val activeRoute = XposedInterface.Hooker { chain ->
            activeCalls.incrementAndGet()
            chain.proceed()
        }
        route.set(activeRoute)

        val rejectedState = invokeSaveStateAfterPreparation(
            HotReloadState.compose(null, null, null, null, null, 1L),
            false
        )

        assertNull(rejectedState)
        assertSame(activeRoute, route.get())
        assertEquals("origin", invoke(old, originals))
        assertEquals(1, activeCalls.get())
        assertEquals(1, originals.get())
    }

    private fun installInitial(): FakeHandle {
        val id = XposedBridge.stableIdForTest(method, 0)
        val initial = FakeHandle(method, id)
        val (_, report) = register(
            callback(1, AtomicInteger()),
            emptyList(),
            emptyMap(),
            FakeInstaller(initial)
        )
        assertTrue(report.completed)
        return initial
    }

    private fun register(
        callback: XC_MethodHook,
        oldHooks: List<XposedBridge.OldHookSnapshot>,
        oldRoutes: Map<String, AtomicReference<XposedInterface.Hooker>>,
        installer: FakeInstaller
    ): Pair<FakeHandle?, XposedBridge.RegistrationReport> {
        XposedBridge.beginRegistrationForTest(oldHooks, oldRoutes, installer)
        XposedBridge.beginRegistrationGroup("fixture")
        var handle: FakeHandle? = null
        try {
            handle = XposedBridge.hookMethod(method, callback) as FakeHandle
        } catch (_: Throwable) {
            // Registration records failures at the bridge boundary, as MainHook observes them.
        }
        XposedBridge.completeRegistrationGroup("fixture")
        return handle to XposedBridge.finishRegistration(true)
    }

    private fun callback(generation: Int, calls: AtomicInteger) = object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            calls.incrementAndGet()
            param.result = "${param.result}:g$generation"
        }
    }

    private fun invoke(handle: FakeHandle, originals: AtomicInteger): String {
        val handler = InvocationHandler { _, called, _ ->
            when (called.name) {
                "getExecutable" -> method
                "getThisObject" -> null
                "getArgs" -> emptyList<Any?>()
                "getArg" -> null
                "proceed", "proceedWith" -> {
                    originals.incrementAndGet()
                    "origin"
                }
                else -> error("unexpected chain method ${called.name}")
            }
        }
        val chain = Proxy.newProxyInstance(
            XposedInterface.Chain::class.java.classLoader,
            arrayOf(XposedInterface.Chain::class.java),
            handler
        ) as XposedInterface.Chain
        return handle.hooker!!.intercept(chain) as String
    }

    private fun invokeSaveStateAfterPreparation(state: Any, prepared: Boolean): Any? {
        val method = MainHook::class.java.getDeclaredMethod(
            "saveStateAfterPreparation",
            Any::class.java,
            Boolean::class.javaPrimitiveType
        )
        method.isAccessible = true
        return method.invoke(null, state, prepared)
    }

    private fun snapshot(handle: XposedInterface.HookHandle, id: String?) =
        XposedBridge.OldHookSnapshot(handle, executable, id)
}
