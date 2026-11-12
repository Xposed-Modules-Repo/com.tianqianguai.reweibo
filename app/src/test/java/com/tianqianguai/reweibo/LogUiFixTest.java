package com.tianqianguai.reweibo;

import org.junit.Test;
import static org.junit.Assert.*;

public class LogUiFixTest {
    @Test public void fullPreviewCanBePagedWithoutLosingAnyText() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 5000; i++) input.append("2026-11-05 12:17:40.000 日志 ").append(i).append('\n');
        LogPreviewPages pages = new LogPreviewPages(input.toString());
        StringBuilder reconstructed = new StringBuilder();
        assertTrue(pages.size() > 1);
        for (int i = 0; i < pages.size(); i++) {
            assertTrue(pages.page(i).length() <= LogPreviewPages.PAGE_CHARS);
            reconstructed.append(pages.page(i));
        }
        assertEquals(input.toString(), reconstructed.toString());
        assertEquals(input.toString(), pages.all());
    }

    @Test public void longLinesAreBoundedWithoutSplittingSurrogatePairs() {
        String input = "x".repeat(4095) + "😀" + "y".repeat(12000);
        LogPreviewPages pages = new LogPreviewPages(input);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            String page = pages.page(i);
            assertTrue(page.length() <= 4096);
            assertFalse(Character.isHighSurrogate(page.charAt(page.length() - 1)));
            assertFalse(Character.isLowSurrogate(page.charAt(0)));
            result.append(page);
        }
        assertEquals(input, result.toString());
    }

    @Test public void emptyPreviewStillHasOneDisplayPage() {
        LogPreviewPages pages = new LogPreviewPages("");
        assertEquals(1, pages.size());
        assertTrue(pages.page(0).contains("no log"));
        assertEquals("", pages.all());
    }

    @Test public void providerInvisibleExplainsNarrowExceptionAndPartialSave() {
        String message = SettingsSyncFailure.message(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS,
            new IllegalArgumentException("Unknown URI content://com.tianqianguai.reweibo.settings/settings/key"));
        assertTrue(message.contains("若使用 HMA"));
        assertTrue(message.contains("仅将 ReWeibo"));
        assertTrue(message.contains("本地副本"));
        assertTrue(message.contains("尚未完成同步"));
    }

    @Test public void permissionAndOtherErrorsAreNotMisreportedAsHiding() {
        assertTrue(SettingsSyncFailure.explain(new SecurityException()).contains("拒绝"));
        assertFalse(SettingsSyncFailure.explain(new SecurityException()).contains("HMA"));
        assertTrue(SettingsSyncFailure.explain(new IllegalStateException()).contains("IllegalStateException"));
        assertFalse(SettingsSyncFailure.explain(new IllegalStateException()).contains("HMA"));
    }
}
