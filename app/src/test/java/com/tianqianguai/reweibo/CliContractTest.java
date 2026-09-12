package com.tianqianguai.reweibo;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
import java.util.Arrays;

public class CliContractTest {
    @Test public void allCommandsRouteToScopedTargetAndKeepExistingNames() {
        String[] legacy = {"status", "logs.status", "logs.read", "logs.export", "timeline.top",
            "timeline.bottom", "timeline.jump", "cache.stats", "cache.clear", "preload.restart", "settings.reload"};
        String[] added = {"timeline.status", "timeline.refresh", "timeline.load_more", "preload.status", "gap.status"};
        for (String[] commands : new String[][] {legacy, added}) {
            for (String local : commands) {
                CliContract.Command command = CliContract.commandFor("weico." + local);
                assertNotNull(command);
                assertEquals(local, command.localName);
                assertEquals("com.weico.international", command.targetPackage);
            }
        }
        assertEquals(16, new HashSet<>(Arrays.asList(CliContract.allCommandNames())).size());
        assertNull(CliContract.commandFor("weico.unknown"));
        assertNull(CliContract.commandFor("weibo.timeline.refresh"));
    }

    @Test public void rawAdbDatesPreserveDateAndTimeComponents() {
        assertEquals("2026-09-12 08:09", CliContract.normalizeDateTimeArgument("2026-09-12_08-09"));
        assertEquals("2026-09-12 08:09:10", CliContract.normalizeDateTimeArgument("2026-09-12_08-09-10"));
        assertEquals("2026-9-2 8:9", CliContract.normalizeDateTimeArgument("2026-9-2_8-9"));
    }

    @Test public void legacyAndInvalidInputsAreLeftForExistingValidation() {
        assertNull(CliContract.normalizeDateTimeArgument(null));
        for (String input : new String[] {"", "7号", "7-11 18:30", "2026-09-12", "2026年9月12日 12:30",
            "2026-09-12 12:30", "2026-09-12_08-09-extra", "2026-09-12_08-09-10garbage"}) {
            assertEquals(input, CliContract.normalizeDateTimeArgument(input));
        }
        // Normalization must not clamp bad calendar values into a valid destructive clear range.
        assertEquals("2026-02-30 25:99", CliContract.normalizeDateTimeArgument("2026-02-30_25-99"));
    }
}
