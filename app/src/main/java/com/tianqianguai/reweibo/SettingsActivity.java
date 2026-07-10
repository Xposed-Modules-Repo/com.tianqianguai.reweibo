package com.tianqianguai.reweibo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

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
        subtitle.setPadding(0, dp(6), 0, dp(22));
        root.addView(subtitle);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(16), dp(8), dp(16), dp(8));
        panel.setBackground(makeRoundRect(COLOR_PANEL, COLOR_LINE, dp(8)));
        root.addView(panel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        addSwitchRow(
                panel,
                "我的页 ReWeibo 入口",
                "在微博轻享版我的页显示模块配置入口",
                prefs,
                ModuleSettings.KEY_WEICO_PROFILE_ENTRY
        );
        addNumberRow(
                panel,
                "首页缓存天数",
                "按微博时间跨度缓存首页时间线，补齐完成也按这个天数判断",
                prefs,
                ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS,
                ModuleSettings.MIN_WEICO_TIMELINE_CACHE_DAYS,
                ModuleSettings.MAX_WEICO_TIMELINE_CACHE_DAYS
        );

        setContentView(scrollView);
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
        toggle.setChecked(prefs.getBoolean(key, ModuleSettings.defaultFor(key)));
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

    private void addNumberRow(
            LinearLayout parent,
            String title,
            String subtitle,
            SharedPreferences prefs,
            String key,
            int min,
            int max
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
        subtitleView.setText(subtitle + "（" + min + "-" + max + " 天）");
        subtitleView.setTextColor(COLOR_SUBTEXT);
        subtitleView.setTextSize(13);
        subtitleView.setPadding(0, dp(4), dp(12), 0);
        texts.addView(subtitleView);

        row.addView(texts, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        LinearLayout inputBox = new LinearLayout(this);
        inputBox.setOrientation(LinearLayout.HORIZONTAL);
        inputBox.setGravity(Gravity.CENTER_VERTICAL);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setSelectAllOnFocus(true);
        input.setGravity(Gravity.CENTER);
        input.setTextColor(COLOR_TEXT);
        input.setTextSize(16);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[] { new InputFilter.LengthFilter(2) });
        input.setBackground(makeRoundRect(Color.rgb(28, 36, 47), COLOR_LINE, dp(6)));
        int current = prefs.getInt(key, ModuleSettings.defaultIntFor(key));
        current = ModuleSettings.clampTimelineCacheDays(current);
        input.setText(String.valueOf(current));
        inputBox.addView(input, new LinearLayout.LayoutParams(dp(58), dp(42)));

        TextView unit = new TextView(this);
        unit.setText("天");
        unit.setTextColor(COLOR_SUBTEXT);
        unit.setTextSize(14);
        unit.setPadding(dp(8), 0, 0, 0);
        inputBox.addView(unit);

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s == null ? "" : s.toString().trim();
                if (text.length() == 0) return;
                try {
                    int days = ModuleSettings.clampTimelineCacheDays(Integer.parseInt(text));
                    prefs.edit().putInt(key, days).apply();
                } catch (Throwable ignored) {
                }
            }
        });
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) return;
                String text = input.getText() == null ? "" : input.getText().toString().trim();
                int days = ModuleSettings.defaultIntFor(key);
                try {
                    if (text.length() > 0) {
                        days = Integer.parseInt(text);
                    }
                } catch (Throwable ignored) {
                }
                days = ModuleSettings.clampTimelineCacheDays(days);
                prefs.edit().putInt(key, days).apply();
                input.setText(String.valueOf(days));
            }
        });

        row.addView(inputBox);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                input.requestFocus();
                input.selectAll();
            }
        });

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
