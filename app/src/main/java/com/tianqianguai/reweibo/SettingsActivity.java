package com.tianqianguai.reweibo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
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
        addSwitchRow(
                panel,
                "显示“跳转”按钮",
                "在微博轻享版首页显示时间跳转快捷按钮",
                prefs,
                ModuleSettings.KEY_WEICO_TIMELINE_JUMP_BUTTON
        );
        addSwitchRow(
                panel,
                "显示“删除”按钮",
                "在微博轻享版首页显示缓存微博删除快捷按钮",
                prefs,
                ModuleSettings.KEY_WEICO_TIMELINE_CACHE_CLEAR_BUTTON
        );
        addInfoRow(
                panel,
                "首页缓存管理",
                "请在微博轻享版的“我的”页点击 ReWeibo：可保存 1-30 天缓存跨度，也可通过日历或直接输入日期（如 7号）清除指定日期范围的缓存"
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
