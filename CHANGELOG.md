# 更新日志

# Changelog

## 1.0.1

### 中文

- 修复缓存天数在微博轻享版冷启动时回退为默认 3 天的问题，支持在应用内明确保存并读取 1-30 天。
- 将 ReWeibo 设置入口改为稳定的应用内深色弹窗，并移除会写入另一份无效配置的旧数字编辑入口。
- 按 status id 合并实时、原生与 shadow cache，避免刷新、空响应或设置尚未确认时丢失较旧历史。
- 使用微博轻享版自身的 Gson 流式写入缓存，经 `fsync` 后原子替换，降低大缓存序列化时的内存峰值和损坏风险。
- 保留有限的上次阅读历史与回退锚点，在缓存恢复和断层回补期间继续显示“上次读到这里”。
- 扩展缓存窗口和补齐安全上限以支持最多 30 天，并补充相关回归测试。

### English

- Fixes the cache window falling back to the three-day default during a Weibo Lite cold start, with explicit in-app persistence for 1-30 days.
- Replaces the profile entry with a stable dark in-app settings dialog and removes the legacy numeric editor that wrote a conflicting value.
- Merges live, native, and shadow caches by status id so refreshes, empty responses, or unconfirmed settings do not discard older history.
- Streams cache JSON through Weibo Lite's own Gson, calls `fsync`, and atomically replaces the native file to reduce memory spikes and partial-cache risk.
- Keeps bounded last-read history and fallback anchors so the “上次读到这里” marker survives restoration and gap repair.
- Expands cache-window and preload safety limits for up to 30 days and adds regression coverage.

## 1.0

### 中文

- `com.tianqianguai.reweibo` 的第一个官方 LSPosed/Xposed 仓库版本。
- 默认推荐 scope 为微博轻享版（`com.weico.international`）。
- 使用经典 `de.robv.android.xposed` API，`xposedminversion=82`。
- 移除微博轻享版启动页广告和时间线广告路径。
- 强制微博轻享版主页时间线使用反向/新微博在前排序。
- 过滤已加载数据中的广告样式条目和无实际内容条目。
- 按设置的时间跨度补齐、裁剪并恢复首页时间线缓存，检测缓存断层后分段回补、显示进度并保存检查点。
- 在微博轻享版“我的”页增加 ReWeibo 设置入口，用于控制入口显示和首页缓存时间跨度。
- 在头像下方增加“跳转”按钮；深色时间弹窗展示实际缓存条数与 `MM-dd HH:mm` 可跳转范围，并拦截越界输入。
- 在可信缓存恢复后保存并恢复上次阅读位置。
- 增加时间线顶部栏双击快捷操作，用于快速跳转到时间线边缘。
- 在可行时刷新或替换打开时间线视频时遇到的过期缓存视频地址。
- 补全缓存时间线条目的文本和媒体字段，让恢复后的条目保持可渲染。
- 保留新浪微博和 Share 的旧 hook 供手动 scope 使用，但不作为官方默认推荐 scope 发布。

### English

- First official LSPosed/Xposed repository release for package `com.tianqianguai.reweibo`.
- Default recommended scope is Weibo Lite (`com.weico.international`).
- Uses the classic `de.robv.android.xposed` API with `xposedminversion=82`.
- Removes Weibo Lite splash-ad and timeline-ad paths.
- Forces Weibo Lite home timeline reverse/newest-first ordering.
- Filters ad-like and contentless entries from loaded timeline data.
- Fills, trims, and restores the home timeline cache to a configured time span, backfilling detected gaps in stages with progress and checkpoints.
- Adds a ReWeibo entry to Weibo Lite's profile drawer for controlling entry visibility and the home timeline cache window.
- Adds a "跳转" button below the avatar; its dark time dialog shows the actual cached item count and `MM-dd HH:mm` jump range, and rejects out-of-range input.
- Saves and restores the last-read timeline position when the restored cache is trusted.
- Adds timeline top-bar double-tap shortcuts for jumping to timeline edges.
- Refreshes or replaces expired cached video URLs when opening timeline videos where possible.
- Hydrates cached timeline text and media fields so restored entries remain renderable.
- Keeps legacy hooks for Sina Weibo and Share available for manual scopes, but does not publish them as official default recommended scopes.
