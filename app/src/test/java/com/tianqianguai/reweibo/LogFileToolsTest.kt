package com.tianqianguai.reweibo

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFileToolsTest {
    private val sample = """
        12:00:00 legacy-before-upgrade
        2026-09-02 08:00:00.001 early
        2026-09-02 09:00:00.100 inside-start
          continuation-line
        2026-09-02 10:00:00.999 inside-end-second
        2026-09-02 10:00:01.000 late
    """.trimIndent() + "\n"

    @Test
    fun datedRangeIsInclusiveAndSkipsAmbiguousLegacyLines() = withLog(sample) { file ->
        val range = LogFileTools.parseRange(
            "2026-09-02 09:00:00",
            "2026-09-02 10:00:00"
        )
        val preview = LogFileTools.readPreview(file, range, 4096)

        assertTrue(preview.text.contains("inside-start"))
        assertTrue(preview.text.contains("continuation-line"))
        assertTrue(preview.text.contains("inside-end-second"))
        assertFalse(preview.text.contains("legacy-before-upgrade"))
        assertFalse(preview.text.contains(" early"))
        assertFalse(preview.text.contains(" late"))
        assertEquals(3L, preview.matchedLines)
        assertEquals(1L, preview.skippedLegacyLines)
    }

    @Test
    fun allRangeKeepsLegacyLinesForBackwardCompatibility() = withLog(sample) { file ->
        val preview = LogFileTools.readPreview(
            file,
            LogFileTools.parseRange(null, null),
            4096
        )

        assertTrue(preview.text.contains("legacy-before-upgrade"))
        assertEquals(1L, preview.legacyLines)
        assertEquals(0L, preview.skippedLegacyLines)
        assertEquals(6L, preview.matchedLines)
    }

    @Test
    fun previewKeepsNewestTailWhileExportStreamsTheWholeRange() = withLog(sample) { file ->
        val range = LogFileTools.parseRange(null, null)
        val preview = LogFileTools.readPreview(file, range, 70)
        val output = ByteArrayOutputStream()
        val exported = LogFileTools.writeFiltered(file, range, output)
        val text = output.toString(StandardCharsets.UTF_8.name())

        assertTrue(preview.truncated)
        assertTrue(preview.text.contains("late"))
        assertFalse(preview.text.contains("legacy-before-upgrade"))
        assertEquals(sample, text)
        assertEquals(6L, exported.matchedLines)
        assertEquals(sample.toByteArray(StandardCharsets.UTF_8).size.toLong(), exported.outputBytes)
    }

    @Test
    fun invalidOrReversedRangeIsRejected() {
        assertRejected("not-a-date", null)
        assertRejected("2026-09-03", "2026-09-02")
    }

    @Test
    fun adbSafeAndIsoSeparatorsMatchTheHumanReadableRange() {
        val spaced = LogFileTools.parseRange(
            "2026-09-02 09:00:00",
            "2026-09-02 10:00:00"
        )
        val iso = LogFileTools.parseRange(
            "2026-09-02T09:00:00",
            "2026-09-02T10:00:00"
        )
        val adbSafe = LogFileTools.parseRange(
            "2026-09-02_09-00-00",
            "2026-09-02_10-00-00"
        )

        assertEquals(spaced.startMs, iso.startMs)
        assertEquals(spaced.endMs, iso.endMs)
        assertEquals(spaced.startMs, adbSafe.startMs)
        assertEquals(spaced.endMs, adbSafe.endMs)
    }

    @Test
    fun snapshotStopsAtTheCompleteByteBoundaryWhileTheLogKeepsGrowing() =
        withLog("2026-09-02 09:00:00.000 before\n") { file ->
            val snapshot = LogFileTools.snapshot(file)
            Files.write(
                file.toPath(),
                "2026-09-02 09:01:00.000 after\n".toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.APPEND
            )

            val preview = LogFileTools.readPreview(
                snapshot,
                LogFileTools.parseRange(null, null),
                4096
            )

            assertTrue(preview.text.contains("before"))
            assertFalse(preview.text.contains("after"))
            assertEquals(snapshot.length, preview.fileBytes)
        }

    private fun assertRejected(start: String?, end: String?) {
        try {
            LogFileTools.parseRange(start, end)
            throw AssertionError("range should have been rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private fun withLog(content: String, assertion: (java.io.File) -> Unit) {
        val path = Files.createTempFile("reweibo-log", ".txt")
        try {
            Files.write(path, content.toByteArray(StandardCharsets.UTF_8))
            assertion(path.toFile())
        } finally {
            Files.deleteIfExists(path)
        }
    }
}
