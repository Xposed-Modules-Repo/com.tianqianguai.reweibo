# ReWeibo

ReWeibo 是一个面向微博轻享版（`com.weico.international`）的 LSPosed/Xposed 模块。

ReWeibo is an LSPosed/Xposed module for Weibo Lite (`com.weico.international`).

## 功能

- 跳过微博轻享版启动页广告和时间线广告路径。
- 强制主页时间线使用更干净的反向/新微博在前排序。
- 过滤广告样式和无实际内容的时间线条目。
- 按设定的时间跨度补齐、裁剪并恢复首页时间线缓存；检测到缓存断层时分段回补并显示进度，降低刷新或缓存丢失后的内容缺口。
- 在微博轻享版“我的”页提供 ReWeibo 设置入口，可控制入口显示并调整首页缓存时间跨度。
- 可在 ReWeibo 设置中按微博发布时间清除指定范围；既可直接输入日期/时间（如 `7号`、`7-7`、`2026-07-07 12:30`），也可使用日历。仅输入日期时按当天整日处理，并同步清除原生缓存、ReWeibo shadow cache 和当前内存时间线中的匹配微博。
- 在头像下方提供“跳转”按钮；深色弹窗会显示实际缓存条数与可跳转时间范围。输入支持 `7号`、`7-11`、完整日期时间等松散格式；仅输入日期时只在当天微博中定位，并拒绝超出缓存范围的输入。
- 在可信缓存恢复后保存并恢复上次阅读位置，并显示“上次读到这里”标记。
- 在可行时刷新或替换时间线视频的过期缓存地址。
- 保留新浪微博和 Share 的旧 hook 供手动 scope 使用，但官方默认推荐 scope 只包含微博轻享版。

## Features

- Skips Weibo Lite splash-ad and timeline-ad paths.
- Forces a cleaner reverse/newest-first order for the home timeline.
- Filters ad-like and contentless timeline entries.
- Fills, trims, and restores the home timeline cache to a configured time span, backfilling detected gaps in stages with visible progress.
- Adds a ReWeibo entry to Weibo Lite's profile drawer for controlling entry visibility and the home timeline cache window.
- Clears cached statuses within an inclusive publication-time range across the native, ReWeibo shadow, and in-memory timeline caches. Users can type loose dates (for example `7`, `7-7`, or a full date/time) or use the calendar; date-only input covers the entire day.
- Adds a "跳转" button below the avatar; its dark dialog shows the real cached item count and jumpable time range. Loose dates such as `7号` and `7-11` are accepted, date-only input searches only that day, and out-of-range input is rejected.
- Saves and restores the last-read position after trusted cache restoration, with a visible "上次读到这里" marker.
- Refreshes or replaces expired cached timeline video URLs when possible.
- Keeps legacy hooks for Sina Weibo and Share available for manual scopes, while the official recommended scope only includes Weibo Lite.

## 兼容性

- 框架 API：经典 `de.robv.android.xposed` / XposedBridge API，不是 libxposed v2。
- 最低 Xposed API：`82`。
- 默认推荐 scope：`com.weico.international`。
- 模块包名：`com.tianqianguai.reweibo`。
- 入口：`assets/xposed_init` -> `com.tianqianguai.reweibo.MainHook`。
- 模块描述：微博轻享版 LSPosed/Xposed 模块：去广告、反向时间线、阅读位置恢复

## Compatibility

- Framework API: classic `de.robv.android.xposed` / XposedBridge API, not libxposed v2.
- Minimum Xposed API: `82`.
- Default recommended scope: `com.weico.international`.
- Module package: `com.tianqianguai.reweibo`.
- Entry point: `assets/xposed_init` -> `com.tianqianguai.reweibo.MainHook`.
- Module description: Weibo Lite LSPosed/Xposed module for ad removal, reversed timeline, and reading-position recovery.

## 安装

1. 安装 APK。
2. 在 LSPosed 中启用模块。
3. 应用微博轻享版的推荐 scope。
4. 强制停止并重新打开微博轻享版。

## Install

1. Install the APK.
2. Enable the module in LSPosed.
3. Apply the recommended scope for Weibo Lite.
4. Force stop and reopen Weibo Lite.


