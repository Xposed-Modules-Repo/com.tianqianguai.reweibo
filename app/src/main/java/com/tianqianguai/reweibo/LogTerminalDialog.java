package com.tianqianguai.reweibo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;

/** Terminal-styled, selectable log viewer with bounded preview and streamed export. */
final class LogTerminalDialog {
    private static final int COLOR_BG = 0xFF070A08;
    private static final int COLOR_PANEL = 0xFF0D1410;
    private static final int COLOR_BORDER = 0xFF285D35;
    private static final int COLOR_TEXT = 0xFFB6F7BF;
    private static final int COLOR_DIM = 0xFF72A77A;
    private static final int COLOR_ACCENT = 0xFF62E875;

    interface TaskRunner {
        boolean execute(Runnable task);
    }

    private LogTerminalDialog() {}

    static void show(
            Activity activity,
            File logFile,
            Object fileLock,
            TaskRunner taskRunner
    ) {
        if (activity == null || activity.isFinishing() || logFile == null
                || fileLock == null || taskRunner == null) return;

        int padding = dp(activity, 12);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);
        root.setBackground(roundRect(COLOR_BG, COLOR_BORDER, dp(activity, 10)));

        TextView prompt = terminalText(activity, 13f, COLOR_ACCENT, true);
        prompt.setText("$ reweibo logs --interactive");
        root.addView(prompt);

        TextView hint = terminalText(activity, 11f, COLOR_DIM, false);
        hint.setText("# 起止时间可留空；格式 yyyy-MM-dd [HH:mm[:ss]]\n"
            + "# 长按下方输出可自由选择和复制；完整结果请导出 TXT");
        hint.setPadding(0, dp(activity, 5), 0, dp(activity, 10));
        root.addView(hint);

        EditText startInput = terminalInput(activity, "开始时间（留空=最早）");
        EditText endInput = terminalInput(activity, "结束时间（留空=最新）");
        root.addView(startInput, inputParams(activity));
        root.addView(endInput, inputParams(activity));

        LinearLayout shortcuts = new LinearLayout(activity);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.setGravity(Gravity.CENTER_VERTICAL);
        TextView allButton = terminalButton(activity, "全部");
        TextView todayButton = terminalButton(activity, "今天");
        TextView hourButton = terminalButton(activity, "最近 1h");
        TextView refreshButton = terminalButton(activity, "刷新");
        shortcuts.addView(allButton, weightedButtonParams(activity, false));
        shortcuts.addView(todayButton, weightedButtonParams(activity, true));
        shortcuts.addView(hourButton, weightedButtonParams(activity, true));
        shortcuts.addView(refreshButton, weightedButtonParams(activity, true));
        LinearLayout.LayoutParams shortcutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(activity, 40)
        );
        shortcutParams.setMargins(0, dp(activity, 2), 0, dp(activity, 8));
        root.addView(shortcuts, shortcutParams);

        TextView status = terminalText(activity, 11f, COLOR_DIM, false);
        status.setText("$ waiting");
        status.setPadding(0, 0, 0, dp(activity, 6));
        root.addView(status);

        ScrollView outputScroll = new ScrollView(activity);
        outputScroll.setFillViewport(true);
        outputScroll.setBackground(roundRect(COLOR_PANEL, COLOR_BORDER, dp(activity, 6)));
        TextView output = terminalText(activity, 11f, COLOR_TEXT, false);
        output.setText("# loading...");
        output.setTextIsSelectable(true);
        output.setFocusable(true);
        output.setTextDirection(View.TEXT_DIRECTION_LTR);
        output.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10));
        outputScroll.addView(output, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.WRAP_CONTENT
        ));
        root.addView(outputScroll, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ));

        AlertDialog dialog = new AlertDialog.Builder(activity)
            .setTitle("ReWeibo Log Terminal")
            .setView(root)
            .setNegativeButton("关闭", null)
            .setNeutralButton("复制当前", null)
            .setPositiveButton("导出 TXT", null)
            .create();
        final int[] loadToken = new int[] {0};

        View.OnClickListener reload = ignored -> loadPreview(
            activity,
            dialog,
            logFile,
            fileLock,
            taskRunner,
            startInput,
            endInput,
            status,
            output,
            outputScroll,
            loadToken
        );
        refreshButton.setOnClickListener(reload);
        allButton.setOnClickListener(view -> {
            startInput.setText("");
            endInput.setText("");
            reload.onClick(view);
        });
        todayButton.setOnClickListener(view -> {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);
            start.set(Calendar.MILLISECOND, 0);
            Calendar end = (Calendar) start.clone();
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);
            startInput.setText(LogFileTools.formatDisplayTimestamp(start.getTimeInMillis()));
            endInput.setText(LogFileTools.formatDisplayTimestamp(end.getTimeInMillis()));
            reload.onClick(view);
        });
        hourButton.setOnClickListener(view -> {
            long now = System.currentTimeMillis();
            startInput.setText(LogFileTools.formatDisplayTimestamp(now - 60L * 60L * 1000L));
            endInput.setText(LogFileTools.formatDisplayTimestamp(now));
            reload.onClick(view);
        });

        dialog.setOnShowListener(ignored -> {
            styleDialog(dialog, activity);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                CharSequence value = output.getText();
                if (value == null || value.length() == 0) {
                    Toast.makeText(activity, "当前没有可复制日志", Toast.LENGTH_SHORT).show();
                    return;
                }
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(
                    Context.CLIPBOARD_SERVICE
                );
                if (clipboard == null) {
                    Toast.makeText(activity, "剪贴板不可用", Toast.LENGTH_SHORT).show();
                    return;
                }
                clipboard.setPrimaryClip(ClipData.newPlainText("ReWeibo logs", value));
                Toast.makeText(activity, "已复制当前终端内容", Toast.LENGTH_SHORT).show();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                LogFileTools.Range range = parseRange(startInput, endInput);
                if (range == null) return;
                status.setText("$ exporting " + range.describe());
                boolean accepted = taskRunner.execute(() -> {
                    try {
                        LogFileTools.Snapshot snapshot;
                        synchronized (fileLock) {
                            snapshot = LogFileTools.snapshot(logFile);
                        }
                        LogExportManager.ExportResult exported = LogExportManager.exportForUser(
                            activity,
                            snapshot,
                            range
                        );
                        LogExportManager.ExportResult result = exported;
                        HotReloadRuntime.post(() -> {
                            if (!isUsable(activity, dialog)) return;
                            status.setText("$ exported " + result.log.matchedLines
                                + " lines -> " + result.location);
                            Toast.makeText(
                                activity,
                                "已导出到 " + result.location,
                                Toast.LENGTH_LONG
                            ).show();
                        });
                    } catch (Throwable error) {
                        postError(activity, dialog, status, "export failed", error);
                    }
                });
                if (!accepted) status.setText("! hot reload is preparing; retry export");
            });
            loadPreview(
                activity,
                dialog,
                logFile,
                fileLock,
                taskRunner,
                startInput,
                endInput,
                status,
                output,
                outputScroll,
                loadToken
            );
        });
        dialog.show();
        HotReloadRuntime.trackDialog(dialog);
    }

    private static void loadPreview(
            Activity activity,
            AlertDialog dialog,
            File logFile,
            Object fileLock,
            TaskRunner taskRunner,
            EditText startInput,
            EditText endInput,
            TextView status,
            TextView output,
            ScrollView outputScroll,
            int[] loadToken
    ) {
        LogFileTools.Range range = parseRange(startInput, endInput);
        if (range == null) return;
        int token = ++loadToken[0];
        status.setText("$ reading " + range.describe());
        boolean accepted = taskRunner.execute(() -> {
            try {
                LogFileTools.Snapshot snapshot;
                synchronized (fileLock) {
                    snapshot = LogFileTools.snapshot(logFile);
                }
                LogFileTools.Result loaded = LogFileTools.readPreview(
                    snapshot,
                    range,
                    LogFileTools.UI_PREVIEW_MAX_CHARS
                );
                LogFileTools.Result result = loaded;
                HotReloadRuntime.post(() -> {
                    if (token != loadToken[0] || !isUsable(activity, dialog)) return;
                    String text = result.text.isEmpty() ? "# no log lines matched\n" : result.text;
                    output.setText(text);
                    status.setText(formatStatus(result));
                    HotReloadRuntime.post(() -> {
                        if (isUsable(activity, dialog)) {
                            outputScroll.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                });
            } catch (Throwable error) {
                postError(activity, dialog, status, "read failed", error);
            }
        });
        if (!accepted) status.setText("! hot reload is preparing; retry refresh");
    }

    private static LogFileTools.Range parseRange(EditText startInput, EditText endInput) {
        startInput.setError(null);
        endInput.setError(null);
        try {
            return LogFileTools.parseRange(
                textOf(startInput),
                textOf(endInput)
            );
        } catch (IllegalArgumentException error) {
            String message = error.getMessage() == null ? "时间范围无效" : error.getMessage();
            if (message.startsWith("invalid start") || message.startsWith("start must")) {
                startInput.setError(message);
                startInput.requestFocus();
            } else {
                endInput.setError(message);
                endInput.requestFocus();
            }
            return null;
        }
    }

    private static String formatStatus(LogFileTools.Result result) {
        StringBuilder value = new StringBuilder("$ matched=")
            .append(result.matchedLines)
            .append(" total=")
            .append(result.totalLines)
            .append(" file=")
            .append(result.fileBytes / 1024L)
            .append("KiB");
        if (result.truncated) value.append(" preview=tail-256KiB");
        if (result.skippedLegacyLines > 0L) {
            value.append(" legacy-skipped=").append(result.skippedLegacyLines);
        }
        return value.toString();
    }

    private static void postError(
            Activity activity,
            AlertDialog dialog,
            TextView status,
            String action,
            Throwable error
    ) {
        String detail = error == null || error.getMessage() == null
            ? "unknown error"
            : error.getMessage();
        HotReloadRuntime.post(() -> {
            if (!isUsable(activity, dialog)) return;
            status.setText("! " + action + ": " + detail);
            Toast.makeText(activity, action + ": " + detail, Toast.LENGTH_LONG).show();
        });
    }

    private static boolean isUsable(Activity activity, AlertDialog dialog) {
        return activity != null
            && !activity.isFinishing()
            && !activity.isDestroyed()
            && dialog != null
            && dialog.isShowing();
    }

    private static EditText terminalInput(Activity activity, String hint) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(COLOR_DIM);
        input.setTextSize(12f);
        input.setTypeface(Typeface.MONOSPACE);
        input.setSelectAllOnFocus(true);
        input.setPadding(dp(activity, 10), 0, dp(activity, 10), 0);
        input.setBackground(roundRect(COLOR_PANEL, COLOR_BORDER, dp(activity, 5)));
        return input;
    }

    private static LinearLayout.LayoutParams inputParams(Activity activity) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(activity, 42)
        );
        params.setMargins(0, 0, 0, dp(activity, 6));
        return params;
    }

    private static TextView terminalButton(Activity activity, String label) {
        TextView button = terminalText(activity, 11f, COLOR_ACCENT, true);
        button.setText(label);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(roundRect(COLOR_PANEL, COLOR_BORDER, dp(activity, 5)));
        return button;
    }

    private static LinearLayout.LayoutParams weightedButtonParams(Activity activity, boolean margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(activity, 38), 1f);
        if (margin) params.setMargins(dp(activity, 5), 0, 0, 0);
        return params;
    }

    private static TextView terminalText(
            Activity activity,
            float size,
            int color,
            boolean bold
    ) {
        TextView text = new TextView(activity);
        text.setTextColor(color);
        text.setTextSize(size);
        text.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        text.setLineSpacing(0f, 1.12f);
        return text;
    }

    private static GradientDrawable roundRect(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, stroke);
        return drawable;
    }

    private static void styleDialog(AlertDialog dialog, Activity activity) {
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(roundRect(COLOR_BG, COLOR_BORDER, dp(activity, 10)));
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.width = WindowManager.LayoutParams.MATCH_PARENT;
            attributes.height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.90f);
            window.setAttributes(attributes);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        int titleId = activity.getResources().getIdentifier("alertTitle", "id", "android");
        View title = titleId == 0 ? null : dialog.findViewById(titleId);
        if (title instanceof TextView) {
            ((TextView) title).setTextColor(COLOR_TEXT);
            ((TextView) title).setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(COLOR_ACCENT);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(COLOR_ACCENT);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(COLOR_ACCENT);
    }

    private static String textOf(EditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
