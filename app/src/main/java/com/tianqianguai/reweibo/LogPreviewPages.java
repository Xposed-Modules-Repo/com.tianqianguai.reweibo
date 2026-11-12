package com.tianqianguai.reweibo;

import java.util.ArrayList;
import java.util.List;

/** Keeps the existing preview window, but bounds each selectable TextView layout. */
final class LogPreviewPages {
    static final int PAGE_CHARS = 4096;
    private final List<String> pages = new ArrayList<>();
    private final String text;

    LogPreviewPages(String text) {
        this.text = text;
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + PAGE_CHARS);
            if (end < text.length()) {
                int newline = text.lastIndexOf('\n', end - 1);
                if (newline >= start) end = newline + 1;
                else if (Character.isHighSurrogate(text.charAt(end - 1))
                        && Character.isLowSurrogate(text.charAt(end))) end--;
            }
            pages.add(text.substring(start, end));
            start = end;
        }
        if (pages.isEmpty()) pages.add("# no log lines matched\n");
    }

    int size() { return pages.size(); }
    String page(int index) { return pages.get(index); }
    String all() { return text; }
}
