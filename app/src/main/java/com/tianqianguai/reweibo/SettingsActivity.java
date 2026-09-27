package com.tianqianguai.reweibo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;

public class SettingsActivity extends Activity {
    private static final int COLOR_BG = Color.rgb(12, 16, 22);
    private static final int COLOR_PANEL = Color.rgb(20, 26, 34);
    private static final int COLOR_LINE = Color.rgb(46, 56, 69);
    private static final int COLOR_TEXT = Color.rgb(235, 241, 247);
    private static final int COLOR_SUBTEXT = Color.rgb(142, 155, 171);
    private static final int COLOR_ACCENT = Color.rgb(76, 166, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(COLOR_BG);
        getWindow().setNavigationBarColor(COLOR_BG);

        SharedPreferences prefs = getSharedPreferences(ModuleSettings.PREFS_NAME, MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(COLOR_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView title = new TextView(this);
        title.setText("ReWeibo");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("微博轻享版模块设置");
        subtitle.setTextColor(COLOR_SUBTEXT);
        subtitle.setTextSize(14);
        subtitle.setPadding(0, dp(6), 0, dp(16));
        root.addView(subtitle);

        Button onlyAds = new Button(this);
        onlyAds.setText("仅去广告");
        onlyAds.setAllCaps(false);
        onlyAds.setTextColor(COLOR_TEXT);
        onlyAds.setBackground(makeRoundRect(COLOR_ACCENT, COLOR_ACCENT, dp(8)));
        onlyAds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = prefs.edit();
                for (Map.Entry<String, Boolean> entry : ModuleSettings.onlyAdsPreset().entrySet()) {
                    editor.putBoolean(entry.getKey(), entry.getValue().booleanValue());
                }
                if (editor.commit()) {
                    Toast.makeText(SettingsActivity.this, "已设置为仅去广告；重启微博轻享版后生效", Toast.LENGTH_LONG).show();
                    recreate();
                } else {
                    Toast.makeText(SettingsActivity.this, "设置保存失败", Toast.LENGTH_LONG).show();
                }
            }
        });
        root.addView(onlyAds, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView note = new TextView(this);
        note.setText("开关在微博轻享版下次启动时生效。关闭“我的页 ReWeibo 入口”后，可从桌面的 ReWeibo 应用图标重新打开本页。");
        note.setTextColor(COLOR_SUBTEXT);
        note.setTextSize(13);
        note.setPadding(0, dp(8), 0, dp(16));
        root.addView(note);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(8));
        panel.setBackground(makeRoundRect(COLOR_PANEL, COLOR_LINE, dp(8)));
        root.addView(panel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        addSectionTitle(panel, "入口与广告");
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_PROFILE_ENTRY),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_PROFILE_ENTRY), prefs, ModuleSettings.KEY_WEICO_PROFILE_ENTRY);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_SPLASH_AD_REMOVAL),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_SPLASH_AD_REMOVAL), prefs, ModuleSettings.KEY_WEICO_SPLASH_AD_REMOVAL);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_AD_REMOVAL),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_AD_REMOVAL), prefs, ModuleSettings.KEY_WEICO_TIMELINE_AD_REMOVAL);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_CONTENTLESS_FILTER),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_CONTENTLESS_FILTER), prefs, ModuleSettings.KEY_WEICO_CONTENTLESS_FILTER);

        addSectionTitle(panel, "信息流行为");
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_FEED_REVERSE),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_FEED_REVERSE), prefs, ModuleSettings.KEY_WEICO_FEED_REVERSE);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_CACHE),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_CACHE), prefs, ModuleSettings.KEY_WEICO_TIMELINE_CACHE);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_PRELOAD),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_PRELOAD), prefs, ModuleSettings.KEY_WEICO_TIMELINE_PRELOAD);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_GAP_FILL),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_GAP_FILL), prefs, ModuleSettings.KEY_WEICO_TIMELINE_GAP_FILL);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_LAST_READ),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_LAST_READ), prefs, ModuleSettings.KEY_WEICO_TIMELINE_LAST_READ);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_JUMP),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_JUMP), prefs, ModuleSettings.KEY_WEICO_TIMELINE_JUMP);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_STATUS_HYDRATION),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_STATUS_HYDRATION), prefs, ModuleSettings.KEY_WEICO_STATUS_HYDRATION);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_VIDEO_REFRESH),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_VIDEO_REFRESH), prefs, ModuleSettings.KEY_WEICO_VIDEO_REFRESH);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_DISABLE_PULL_REFRESH),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_DISABLE_PULL_REFRESH), prefs, ModuleSettings.KEY_WEICO_DISABLE_PULL_REFRESH);

        addSectionTitle(panel, "模块入口与诊断");
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_JUMP_BUTTON),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_JUMP_BUTTON), prefs, ModuleSettings.KEY_WEICO_TIMELINE_JUMP_BUTTON);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON), prefs, ModuleSettings.KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON);
        addSwitchRow(panel, ModuleSettings.titleFor(ModuleSettings.KEY_WEICO_PERSISTENT_LOGS),
                ModuleSettings.descriptionFor(ModuleSettings.KEY_WEICO_PERSISTENT_LOGS), prefs, ModuleSettings.KEY_WEICO_PERSISTENT_LOGS);
        addCacheDaysRow(panel, prefs);

        addInfoRow(
                panel,
                "缓存跨度与依赖",
                "缓存跨度可设为 1-30 天。关闭扩展缓存会暂时停用预加载、断层补齐和阅读位置恢复，但会保留这些开关的选择；重新开启缓存后继续按原选择工作。双击跳转可独立于缓存使用。"
        );

        setContentView(scrollView);
    }

    private void addSectionTitle(LinearLayout parent, String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextColor(COLOR_ACCENT);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, dp(16), 0, dp(4));
        parent.addView(title);
    }

    private void addSwitchRow(
            LinearLayout parent,
            String title,
            String subtitle,
            SharedPreferences prefs,
            String key
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(COLOR_TEXT);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        texts.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(COLOR_SUBTEXT);
        subtitleView.setTextSize(13);
        subtitleView.setPadding(0, dp(4), dp(12), 0);
        texts.addView(subtitleView);

        row.addView(texts, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        Switch toggle = new Switch(this);
        toggle.setChecked(readBoolean(prefs, key));
        toggle.setTextColor(COLOR_ACCENT);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(key, isChecked).apply();
            }
        });
        row.addView(toggle);

        parent.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle.setChecked(!toggle.isChecked());
            }
        });
    }

    private void addCacheDaysRow(LinearLayout parent, SharedPreferences prefs) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView title = new TextView(this);
        title.setText("缓存跨度（天）");
        title.setTextColor(COLOR_TEXT);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(title);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSingleLine(true);
        input.setText(String.valueOf(prefs.getInt(
                ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS,
                ModuleSettings.DEFAULT_WEICO_TIMELINE_CACHE_DAYS
        )));
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(COLOR_SUBTEXT);
        input.setHint("1-30");
        inputRow.addView(input, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button save = new Button(this);
        save.setText("保存");
        save.setAllCaps(false);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int days;
                try {
                    days = Integer.parseInt(input.getText().toString().trim());
                } catch (NumberFormatException error) {
                    showCacheDaysError();
                    return;
                }
                if (days < ModuleSettings.MIN_WEICO_TIMELINE_CACHE_DAYS
                        || days > ModuleSettings.MAX_WEICO_TIMELINE_CACHE_DAYS) {
                    showCacheDaysError();
                    return;
                }
                boolean saved = prefs.edit()
                        .putInt(ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS, days)
                        .commit();
                Toast.makeText(
                        SettingsActivity.this,
                        saved ? "缓存跨度已保存" : "保存失败",
                        Toast.LENGTH_LONG
                ).show();
            }
        });
        inputRow.addView(save, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(48)
        ));
        row.addView(inputRow);
        parent.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    private void showCacheDaysError() {
        Toast.makeText(this, "请输入 1 到 30 之间的天数", Toast.LENGTH_LONG).show();
    }

    private boolean readBoolean(SharedPreferences prefs, String key) {
        Object raw = prefs.getAll().get(key);
        if (raw instanceof Boolean) return ((Boolean) raw).booleanValue();
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        return ModuleSettings.defaultFor(key);
    }

    private void addInfoRow(
            LinearLayout parent,
            String title,
            String subtitle
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, dp(12), 0, dp(12));

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(COLOR_TEXT);
        titleView.setTextSize(16);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitle);
        subtitleView.setTextColor(COLOR_SUBTEXT);
        subtitleView.setTextSize(13);
        subtitleView.setPadding(0, dp(4), dp(12), 0);
        row.addView(subtitleView);

        parent.addView(row, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
    }

    private GradientDrawable makeRoundRect(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, stroke);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
