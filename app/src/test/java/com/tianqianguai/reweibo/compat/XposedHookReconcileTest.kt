package com.tianqianguai.reweibo.compat

import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Executable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XposedHookReconcileTest {
    private class Fixture {
        fun target() = Unit
    }

    private class FakeHandle(
        private val origin: Executable,
        private val hookId: String?
    ) : XposedInterface.HookHandle {
        var unhooked = false

        override fun getExecutable(): Executable = origin
        override fun getId(): String? = hookId
        override fun unhook() {
            unhooked = true
        }

        override fun replaceHook(hooker: XposedInterface.Hooker): XposedInterface.HookHandle = this
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

    private fun snapshot(handle: XposedInterface.HookHandle, id: String?) =
        XposedBridge.OldHookSnapshot(handle, executable, id)
}
