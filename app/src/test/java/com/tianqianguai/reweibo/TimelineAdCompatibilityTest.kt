package com.tianqianguai.reweibo

import com.tianqianguai.reweibo.compat.XposedBridge
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineAdCompatibilityTest {
    private class FakeObservable

    private class FakeUveAdHelper {
        @Suppress("UNUSED_PARAMETER")
        fun queryUveAdRequest(
            params: Map<*, *>,
            first: String,
            second: String,
            values: List<*>
        ): FakeObservable = FakeObservable()

        @Suppress("UNUSED_PARAMETER")
        fun queryUveAdRequest(params: Map<*, *>, first: String): FakeObservable =
            FakeObservable()
    }

    @Test
    fun discoversThe699ObservableQueryBySignature() {
        val method = WeiboLiteHook.findUveAdQueryMethod(
            FakeUveAdHelper::class.java,
            FakeObservable::class.java
        )

        assertNotNull(method)
        assertEquals("queryUveAdRequest", method.name)
        assertEquals(4, method.parameterCount)
        assertEquals(FakeObservable::class.java, method.returnType)
    }

    @Test
    fun zeroEffectiveTimelineAdHooksMarksRequiredGroupIncomplete() {
        XposedBridge.beginRegistration(emptyList())
        XposedBridge.beginRegistrationGroup("timeline-ad")
        WeiboLiteHook.recordTimelineAdRegistrationResult(0, "none")
        XposedBridge.completeRegistrationGroup("timeline-ad")
        val report = XposedBridge.finishRegistration(true)

        assertFalse(report.groupsComplete)
        assertFalse(report.completed)
        assertTrue(report.failures.any {
            it.message.contains("zero effective hooks")
        })
    }

    @Test
    fun sourceKeepsLegacyCandidatesAndReturnsAnEmptyObservableFor699() {
        val source = projectFile(
            "app/src/main/java/com/tianqianguai/reweibo/WeiboLiteHook.java"
        ).readText()

        assertTrue(source.contains("com.weico.international.manager.uvead.UveAdHelper"))
        assertTrue(source.contains("queryUveAdRequest\$lambda\$151"))
        assertTrue(source.contains("queryUveAdRequest\$lambda\$152"))
        assertTrue(source.contains("queryUveAdRequest\$lambda\$153"))
        assertTrue(source.contains("observableClass.getMethod(\"just\", Object.class)"))
        assertTrue(source.contains("justMethod.invoke(null, Collections.emptyList())"))
        assertTrue(source.contains("if (listCandidate || List.class.isAssignableFrom"))
        assertTrue(source.contains("param.setResult(Collections.emptyList())"))
        assertTrue(source.contains(".with(\"timeline_ad_hooks\", sTimelineAdHookCount)"))
        assertTrue(source.contains(".with(\"timeline_ad_strategy\", sTimelineAdHookStrategy)"))
    }

    private fun projectFile(path: String): File = File(projectRoot(), path)

    private fun projectRoot(): File = sequenceOf(File("."), File(".."))
        .firstOrNull { File(it, "app/build.gradle.kts").isFile }
        ?.canonicalFile
        ?: error("project root not found from ${File(".").absolutePath}")
}
