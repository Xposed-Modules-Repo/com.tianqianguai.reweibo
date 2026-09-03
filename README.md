<p align="center">
  <img src="docs/assets/reweibo-icon.png" width="168" height="168" alt="ReWeibo red panda icon">
</p>

<h1 align="center">ReWeibo</h1>

<p align="center">
  面向微博轻享版（<code>com.weico.international</code>）的 LSPosed/Xposed 模块
  <br>
  An LSPosed/Xposed module for Weibo Lite
</p>

<p align="center">
  <a href="https://github.com/Xposed-Modules-Repo/com.tianqianguai.reweibo/stargazers"><img src="https://img.shields.io/github/stars/Xposed-Modules-Repo/com.tianqianguai.reweibo?style=for-the-badge&logo=github&label=Star" alt="GitHub Stars"></a>
  <a href="https://t.me/reweibo_offical"><img src="https://img.shields.io/badge/Telegram-Official_Group-26A5E4?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram Official Group"></a>
  <a href="https://t.me/zhongjitianqianguai3"><img src="https://img.shields.io/badge/Telegram-Release_Channel-229ED9?style=for-the-badge&logo=telegram&logoColor=white" alt="Telegram Release Channel"></a>
</p>

<p align="center">
  如果 ReWeibo 对你有帮助，欢迎点一个 Star 支持项目 ⭐
  <br>
  If ReWeibo helps you, please consider leaving a Star.
</p>

## 功能

- 跳过微博轻享版启动页广告和时间线广告路径。
- 强制主页时间线使用更干净的反向/新微博在前排序。
- 过滤广告样式和无实际内容的时间线条目。
- 按设定的时间跨度补齐、裁剪并恢复首页时间线缓存；大型缓存的读取、合并、过滤、正文/媒体补齐和断层扫描在后台处理，降低恢复时的卡顿与 ANR 风险。
- 检测到缓存断层时分段回补并显示进度；批量插入历史微博时会恢复当前可见微博锚点，进度窗也会避开状态栏和刘海区域。
- 在微博轻享版“我的”页提供 ReWeibo 设置入口，可控制入口显示、调整首页缓存时间跨度，并分别控制首页“跳转”和“删除”快捷按钮。
- 可在 ReWeibo 设置中按微博发布时间清除指定范围；既可直接输入日期/时间（如 `7号`、`7-7`、`2026-07-07 12:30`），也可使用日历。仅输入日期时按当天整日处理，并同步清除原生缓存、ReWeibo shadow cache 和当前内存时间线中的匹配微博，同时阻止旧任务写回已删除内容。
- 首页“跳转”会显示实际缓存条数与可跳转时间范围，支持 `7号`、`7-11`、完整日期时间等松散格式；仅输入日期时只在当天微博中定位，并拒绝超出缓存范围的输入。
- 缓存恢复、清理、双击跳转和阅读位置操作只作用于当前可见首页，避免旧或隐藏页面、广告、重复项和伪尾项造成错误边界。
- 按日期清理会优先保留当前已渲染微博，并重新补齐保留微博的正文与媒体字段，避免相邻日期内容变成空白。
- 在可信缓存恢复后保存并恢复上次阅读位置，并显示“上次读到这里”标记。
- 在可行时刷新或替换时间线视频的过期缓存地址。
- 在 ReWeibo 设置中提供终端样式日志页：日志文本可长按自由选择，也可复制当前预览；支持按起止日期时间筛选，并将完整筛选结果流式导出为 TXT，避免把大型日志一次性载入界面。

## Features

- Skips Weibo Lite splash-ad and timeline-ad paths.
- Forces a cleaner reverse/newest-first order for the home timeline.
- Filters ad-like and contentless timeline entries.
- Fills, trims, and restores the home timeline cache to a configured time span. Large-cache loading, merging, filtering, text/media hydration, and gap scanning run in the background to reduce restore-time stalls and ANRs.
- Backfills detected gaps in stages with visible progress, restores the visible-status anchor when history is inserted, and keeps the progress card below status-bar and display-cutout insets.
- Adds a ReWeibo entry to Weibo Lite's profile drawer for controlling entry visibility, the home cache window, and the independent home “跳转” and “删除” shortcuts.
- Clears cached statuses within an inclusive publication-time range across the native, ReWeibo shadow, and in-memory timeline caches. Users can type loose dates (for example `7`, `7-7`, or a full date/time) or use the calendar; date-only input covers the entire day, and stale queued work cannot restore deleted statuses.
- The home “跳转” shortcut shows the real cached item count and jumpable time range. Loose dates such as `7号` and `7-11` are accepted, date-only input searches only that day, and out-of-range input is rejected.
- Cache restoration, clearing, double-tap navigation, and reading-position actions target only the visible home timeline, preventing stale or hidden pages, ads, duplicates, and synthetic tail rows from creating false boundaries.
- Date-range clearing prefers currently rendered statuses and rehydrates retained text and media, preventing adjacent retained dates from becoming blank rows.
- Saves and restores the last-read position after trusted cache restoration, with a visible "上次读到这里" marker.
- Refreshes or replaces expired cached timeline video URLs when possible.
- Adds a terminal-style log view to ReWeibo settings. Log text is freely selectable, the current preview can be copied, and complete results can be filtered by start/end date-time and streamed to a TXT export without loading a large file into the UI.

## 兼容性

- 框架 API：Modern libxposed API `102`（`io.github.libxposed:api:102.0.0`）。
- 最低与目标 Xposed API：`102`。
- 静态 scope：仅 `com.weico.international`。
- 模块包名：`com.tianqianguai.reweibo`。
- 入口：`META-INF/xposed/java_init.list` -> `com.tianqianguai.reweibo.MainHook`。
- 启用 `autoHotReload=true`：更新模块后会在不重启微博轻享版进程的情况下原子替换同 ID hook，并恢复当前 Application、时间线 presenter、RecyclerView 与页面 owner。缓存清理、缓存读写、网络订阅或模块对话框仍活动时会拒绝本次热重载，待任务结束后可重试。
- 模块描述：微博轻享版 LSPosed/Xposed 模块：去广告、反向时间线、阅读位置恢复
- 微博轻享版 6.9.9：V2/V3 时间线数据顺序 hook 支持 R8 `ExternalSyntheticLambda` 候选，旧版 `doLoadData` lambda 候选仍受支持。

## Compatibility

- Framework API: Modern libxposed API `102` (`io.github.libxposed:api:102.0.0`).
- Minimum and target Xposed API: `102`.
- Static scope: `com.weico.international` only.
- Module package: `com.tianqianguai.reweibo`.
- Entry point: `META-INF/xposed/java_init.list` -> `com.tianqianguai.reweibo.MainHook`.
- `autoHotReload=true` is enabled. A module update atomically replaces same-ID hooks without restarting the Weibo Lite process and restores the current Application, timeline presenter, RecyclerView, and page owner. Reload is rejected while cache clearing, cache I/O, network subscriptions, or module dialogs are active; retry after that work finishes.
- Module description: Weibo Lite LSPosed/Xposed module for ad removal, reversed timeline, and reading-position recovery.
- Weibo Lite 6.9.9: V2/V3 timeline data-order hooks support R8 `ExternalSyntheticLambda` candidates, while older `doLoadData` lambda candidates remain supported.

## ADB / CLI

设置、状态查询、日志预览/范围导出、时间跳转、缓存统计与精确范围清理均可直接通过 `adb shell content call` 调用，不需要仓库脚本、截图或坐标点击。命令清单见 [docs/cli.md](docs/cli.md)。

Settings, status queries, log preview/range export, time jumps, cache statistics, and exact-range cache clearing are available directly through `adb shell content call`, without repository scripts, screenshots, or coordinate taps. See [docs/cli.md](docs/cli.md) for the command list.

