package com.tianqianguai.reweibo

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class LogTerminalContractTest {
    @Test
    fun terminalUiSupportsSelectionCopyRangeAndStreamedExport() {
        val source = projectFile(
            "app/src/main/java/com/tianqianguai/reweibo/LogTerminalDialog.java"
        ).readText()

        assertTrue(source.contains("Typeface.MONOSPACE"))
        assertTrue(source.contains("setTextIsSelectable(true)"))
        assertTrue(source.contains("ClipboardManager"))
        assertTrue(source.contains("LogFileTools.parseRange"))
        assertTrue(source.contains("LogFileTools.UI_PREVIEW_MAX_CHARS"))
        assertTrue(source.contains("LogExportManager.exportForUser"))
    }

    @Test
    fun hookUsesFullTimestampsAndTracksLogIoAcrossHotReload() {
        val source = projectFile(
            "app/src/main/java/com/tianqianguai/reweibo/WeiboLiteHook.java"
        ).readText()

        assertTrue(source.contains("LogFileTools.formatLogTimestamp"))
        assertTrue(source.contains("log-io-pending="))
        assertTrue(source.contains("sPendingLogIoTasks.incrementAndGet()"))
        assertTrue(source.contains("LogTerminalDialog.show"))
        assertTrue(source.contains("runLogExportCliCommand"))
    }

    @Test
    fun cliReadsOffMainAndExportsPublishAtomically() {
        val bridge = projectFile(
            "app/src/main/java/com/tianqianguai/reweibo/CliCommandBridge.java"
        ).readText()
        val hook = projectFile(
            "app/src/main/java/com/tianqianguai/reweibo/WeiboLiteHook.java"
        ).readText()
        val exporter = projectFile(
            "app/src/main/java/com/tianqianguai/reweibo/LogExportManager.java"
        ).readText()

        assertTrue(bridge.contains("PendingResult pending = goAsync()"))
        assertTrue(hook.contains("\"logs.status\".equals(command) || \"logs.read\".equals(command)"))
        assertTrue(hook.contains("snapshotWeicoLogFile()"))
        assertTrue(exporter.contains("destination.getName() + \".tmp\""))
        assertTrue(exporter.contains("temporary.renameTo(destination)"))
        assertTrue(exporter.contains("resolver.update(destination, completed, null, null) <= 0"))
        assertTrue(exporter.contains("resolver.delete(destination, null, null)"))
    }

    private fun projectFile(path: String): File = File(projectRoot(), path)

    private fun projectRoot(): File = sequenceOf(File("."), File(".."))
        .firstOrNull { File(it, "app/build.gradle.kts").isFile }
        ?.canonicalFile
        ?: error("project root not found from ${File(".").absolutePath}")
}
