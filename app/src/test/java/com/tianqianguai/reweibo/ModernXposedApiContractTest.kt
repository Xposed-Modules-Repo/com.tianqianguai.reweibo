package com.tianqianguai.reweibo

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernXposedApiContractTest {
    @Test
    fun api102MetadataIsCompleteAndLegacyMetadataIsGone() {
        val build = source("app/build.gradle.kts")
        assertTrue(build.contains("compileOnly(\"io.github.libxposed:api:102.0.0\")"))
        assertTrue(build.contains("testImplementation(\"io.github.libxposed:api:102.0.0\")"))
        assertFalse(projectFile("app/libs/xposed-bridge-api.jar").exists())

        val metadataRoot = projectFile("app/src/main/resources/META-INF/xposed")
        assertEquals(
            setOf("java_init.list", "module.prop", "scope.list"),
            metadataRoot.listFiles().orEmpty().map { it.name }.toSet()
        )
        assertEquals(
            listOf("com.tianqianguai.reweibo.MainHook"),
            source("app/src/main/resources/META-INF/xposed/java_init.list")
                .lineSequence().filter(String::isNotBlank).toList()
        )
        assertEquals(
            listOf("com.weico.international"),
            source("app/src/main/resources/META-INF/xposed/scope.list")
                .lineSequence().filter(String::isNotBlank).toList()
        )
        val properties = source("app/src/main/resources/META-INF/xposed/module.prop")
        listOf(
            "minApiVersion=102",
            "targetApiVersion=102",
            "staticScope=true",
            "exceptionMode=protective",
            "autoHotReload=true"
        ).forEach { assertTrue("missing $it", properties.contains(it)) }

        assertFalse(projectFile("app/src/main/assets/xposed_init").exists())
        assertFalse(projectFile("assets/xposed_init").exists())
        val manifest = source("app/src/main/AndroidManifest.xml")
        assertFalse(manifest.contains("xposedmodule"))
        assertFalse(manifest.contains("xposedminversion"))
        assertFalse(manifest.contains("xposedscope"))
    }

    @Test
    fun modernEntryAndRuntimeImplementRealHotReloadLifecycle() {
        val entry = source("app/src/main/java/com/tianqianguai/reweibo/MainHook.java")
        assertTrue(entry.contains("extends XposedModule"))
        assertTrue(entry.contains("onModuleLoaded"))
        assertTrue(entry.contains("onPackageReady"))
        assertTrue(entry.contains("onHotReloading"))
        assertTrue(entry.contains("onHotReloaded"))
        assertTrue(entry.contains("groupsComplete="))
        assertFalse(entry.contains("WeiboFeedHook.hook"))
        assertFalse(entry.contains("ShareFeedHook.hook"))

        val bridge = source(
            "app/src/main/java/com/tianqianguai/reweibo/compat/XposedBridge.java"
        )
        assertTrue(bridge.contains("oldHandle.replaceHook(hooker)"))
        assertTrue(bridge.contains("stableId(executable"))
        assertFalse(bridge.contains("deactivateGeneration"))

        val runtime = source("app/src/main/java/com/tianqianguai/reweibo/HotReloadRuntime.java")
        assertTrue(runtime.contains("shutdownNow"))
        assertTrue(runtime.contains("removeCallbacks"))
        assertTrue(runtime.contains("runMainCleanup"))
        assertTrue(runtime.contains("retireFully"))
        assertFalse(runtime.contains("return stopped"))

        val weico = source(
            "app/src/main/java/com/tianqianguai/reweibo/WeiboLiteHook.java"
        )
        assertTrue(weico.contains("HotReloadPreparation.run("))
        assertTrue(weico.contains(".with(\"hot_reload_blocker\", hotReloadBlocker())"))
        assertTrue(weico.contains("timelinePersistBlockerForState("))

        val cli = source("app/src/main/java/com/tianqianguai/reweibo/CliCommandBridge.java")
        assertTrue(cli.contains("boolean unregister()"))
        assertTrue(cli.contains("unregisterReceiver"))
    }

    @Test
    fun productionSourcesHaveNoLegacyApiReferences() {
        val sourceRoot = projectFile("app/src/main/java")
        val legacy = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .filter { it.readText().contains("de.robv.android.xposed") }
            .map { it.relativeTo(projectRoot()).invariantSeparatorsPath }
            .toList()
        assertTrue("legacy references remain: $legacy", legacy.isEmpty())
    }

    private fun source(path: String): String = projectFile(path).readText()

    private fun projectFile(path: String): File = File(projectRoot(), path)

    private fun projectRoot(): File = sequenceOf(File("."), File(".."))
        .firstOrNull { File(it, "app/build.gradle.kts").isFile }
        ?.canonicalFile
        ?: error("project root not found from ${File(".").absolutePath}")
}
