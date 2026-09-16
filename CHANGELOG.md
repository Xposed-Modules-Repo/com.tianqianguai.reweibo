# 更新日志

# Changelog

## 1.2.2

### 新增与改进

- 新增 ADB 首页时间线状态、原生刷新与加载更多接口，以及预加载和断层补齐状态查询；保留原有忙碌检查、加载上限和无更多内容判断。
- 时间跳转与缓存范围清理支持适合 raw ADB 传参的日期时间格式，继续兼容原有输入与范围校验。
- 增加补齐任务排队与后台准备耗时、回退实际调度延迟、提示更新/移除和边界跳转耗时日志，便于真实使用中定位问题。

### Added And Improved

- Adds ADB home-timeline status, native refresh and load-more actions, plus preload and gap-fill status queries, preserving existing busy checks, loading limits, and end-of-feed handling.
- Supports raw-ADB-friendly date-time formats for timeline jumps and cache-range clearing while retaining existing inputs and range validation.
- Adds logs for completion queue/preparation time, actual fallback scheduling delays, progress-card updates/removal, and edge-navigation timing to aid diagnosis during real use.

### 修复

- 将服务器空页确认后的断层补齐收尾统计、裁剪与排序移到后台，减少大缓存占用主线程造成的卡顿和 ANR；异步结果应用前检查任务与数据变化，避免旧快照覆盖新缓存或清理结果。
- 修复补齐超时或到达页数上限后任务停止但提示仍残留的路径；终态及时更新提示，页面不可用时移除残留卡片，并拦截过期调度与提示回调。
- 优化双击回顶部的边界查找，缓存反射方法解析结果，校正时复用有效目标位置，并取消旧的跳转校正任务，减少大列表重复扫描和连续双击造成的任务叠加。
- 复用微博日期解析器和解析结果，避免普通日期字符串反复触发数字转换异常；减少重复缓存统计与影子缓存写入请求，保留原有缓存天数、容量和功能。

### Fixes

- Moves statistics, trimming, and sorting for gap-fill completion after confirmed empty server responses into background work, reducing large-cache main-thread stalls and ANRs. Task and data checks prevent stale snapshots from overwriting newer caches or clear results.
- Fixes paths where an expired or page-limited gap-fill task stops but leaves its progress card visible. Terminal states update promptly, detached cards are removed, and stale scheduling and progress callbacks are ignored.
- Optimizes double-tap-to-top boundary lookup, caches reflective method resolution, reuses valid positions during correction, and cancels older correction tasks to reduce repeated large-list scans and overlapping work.
- Reuses status-date parsers and parsed values instead of repeatedly throwing numeric-conversion exceptions for ordinary date strings, and reduces duplicate cache statistics and shadow-cache write requests while preserving cache duration, capacity, and features.

## 1.2.1

### 中文

- 增加时间线 addData 入口与回调阶段日志，记录线程、传入及缓存条数、可解析的宿主 Activity/Intent、阶段耗时和回补状态。
- 基于 7.0.0 APK 的 DEX 核对，确认首页、评论和竖屏视频 UVE 广告请求经过现有 UveAdHelper Hook，保留旧 RxApiKt lambda 候选。
- README 新增软件发布频道入口。
- 避免断层回补空页处理路径再次持久化整份缓存，保留正常检查点、回补重试、排序和手动加载。
- 将 addData 计时起点保存在单次调用上下文中，避免不同调用共享计时状态。

### English

- Adds timeline addData entry and callback-stage logs with thread information, incoming/cached counts, resolvable host Activity/Intent context, stage timing, and gap-fill state.
- Confirms through DEX inspection of the 7.0.0 APK that home-feed, comment, and vertical-video UVE ad requests pass through the existing UveAdHelper hook, retaining legacy RxApiKt lambda candidates.
- Adds the software release channel link to the README.
- Avoids persisting the entire cache again in the empty-page gap-fill handler while preserving normal checkpoints, fallback retries, ordering, and manual loading.
- Stores the addData timing start in the individual call context so different calls do not share timing state.

## 1.2.0

### 中文

- 迁移到 Modern libxposed API 102，官方静态作用域仅保留微博轻享版，并通过同 ID Hook 原子替换、状态恢复和忙碌期拒绝机制支持自动热重载。
- 新增可直接通过 `adb shell content call` 使用的完整管理接口，覆盖设置读写、运行状态、时间线跳转、缓存统计与清理、预加载重启、设置重载以及日志读取和导出。
- 新增终端样式日志页，支持自由选择、复制当前预览、按起止日期时间筛选和完整 TXT 流式导出；新版日志记录完整日期，旧版 `HH:mm:ss` 日志继续支持无范围导出。
- 完成微博轻享版 6.9.9 与 LSPosed 2.2.0 运行适配，同时保留旧版 `doLoadData` 等 Hook 候选。
- 统一应用与 GitHub 的红熊猫品牌图标，并补充官方群组、Bug/功能建议 Issue 表单和 Star 入口。
- 修复少数精确类名或参数签名变化导致的 Hook 安装失效，并在 API 102 迁移后保持原有时间线、广告过滤和缓存能力。
- 修复 raw ADB 时间参数无法可靠携带空格或冒号的问题，新增 `yyyy-MM-dd_HH-mm-ss` 格式。
- 修复大型日志扫描占用目标主线程或长时间持有写入锁的问题，并避免失败导出遗留不完整文件。

### English

- Migrates to Modern libxposed API 102, limits the official static scope to Weibo Lite, and supports automatic hot reload through same-ID atomic hook replacement, state restoration, and safe rejection while runtime work is active.
- Adds a complete management interface callable through `adb shell content call`, covering settings, runtime status, timeline navigation, cache statistics and clearing, preload restart, settings reload, and log reading/export.
- Adds a terminal-style log view with free text selection, preview copying, start/end date-time filtering, and full streamed TXT export. New logs include complete dates, while legacy `HH:mm:ss` logs remain available in unbounded exports.
- Adapts the module to Weibo Lite 6.9.9 and LSPosed 2.2.0 while retaining older hook candidates such as `doLoadData`.
- Unifies the application and GitHub red-panda branding, and adds the official group, Bug/feature-request Issue forms, and a Star entry point.
- Fixes hook installation failures caused by exact class-name or parameter-signature changes while preserving timeline, ad-filtering, and cache behavior after the API 102 migration.
- Fixes raw ADB time values being unable to carry spaces or colons reliably by adding the `yyyy-MM-dd_HH-mm-ss` format.
- Fixes large log scans occupying the target main thread or holding the append lock for too long, and prevents failed exports from leaving incomplete files.

## 1.1.2

### 中文

- 兼容微博轻享版 6.9.8 的 V2/V3 时间线数据顺序 hook：支持 R8 生成的 `ExternalSyntheticLambda` 候选和 `invoke(Object)` 签名，同时保留旧版 `doLoadData` lambda 候选。
- 修复微博轻享版 6.9.8 因匿名类名称和参数签名变化导致 V2/V3 时间线数据顺序 hook 未安装的问题，恢复广告过滤、无实际内容条目过滤和新微博在前排序。

### English

- Adds Weibo Lite 6.9.8 V2/V3 timeline data-order compatibility for R8-generated `ExternalSyntheticLambda` candidates and `invoke(Object)` signatures, while keeping older `doLoadData` lambda candidates supported.
- Fixes Weibo Lite 6.9.8 V2/V3 timeline data-order hooks failing to install after anonymous-class names and parameter signatures changed, restoring ad filtering, contentless-entry filtering, and newest-first ordering.

## 1.1.1

### 中文

- 断层回补引入“已验证自然断层”记录：当补齐请求跨过边界、返回的数据不含目标微博时，将该间隔确认为信息流自身的自然断点并持久化保存（按缓存范围隔离，最多记住 128 条）；后续扫描自动跳过这些间隔并继续寻找下一个真实断层。
- 修复打开“清除缓存”时间范围弹窗时在主线程统计大缓存导致的界面卡顿甚至 ANR；缓存范围现在改为后台线程读取并提示“正在读取缓存范围，请稍候”，读取中或清理进行中重复打开会得到相应提示而不再重复加载。
- 补充自然断层判定与跳过逻辑的单元测试。

### English

- Adds verified natural-gap tracking to timeline gap filling: when a refill request crosses the boundary and returns data that does not contain the target status, the interval is confirmed as a natural feed pause and persisted per cache scope (up to 128 entries); later scans skip these intervals and continue searching for the next real gap.
- Fixes UI freezes and ANRs caused by computing large-cache statistics on the main thread when opening the cache-clear range dialog; stats now load on a background thread with a “正在读取缓存范围，请稍候” toast, and reopening while loading or during an active clear shows a notice instead of reloading.
- Adds unit tests covering verified-natural-gap detection and skipping.

## 1.1.0

### 中文

- 将大型时间线缓存的读取、合并、过滤、正文/媒体补齐和断层扫描移出主线程，并合并重复恢复请求，降低大缓存恢复时的 ANR、重复读取和部分写入风险。
- 在微博轻享版内的 ReWeibo 设置中新增按发布时间范围清除缓存微博；既支持日历选择，也支持 `7号`、`7-7`、完整日期时间等松散输入，单独日期按整天处理。清理会同步更新原生缓存、shadow cache 与当前内存时间线，并阻止旧的异步任务回写已删除内容。
- 修复按日期清理后，未被删除日期的当前可见微博只剩作者和互动数、正文区域为空的问题；清理现在优先保留当前已渲染对象，并重新补齐保留微博的正文与媒体字段。
- 时间线“跳转”同步支持 `7号`、`7-11`、中文完整日期等松散日期；仅输入日期时只在当天微博中定位，并优先滚动当前主时间线。
- 在首页增加“跳转”和“删除”快捷按钮，并允许在 ReWeibo 设置中分别控制两个按钮是否显示。
- 修复清理或刷新后旧 Fragment 的短列表仍被误当成首页，以及广告、重复项混入列表尾部导致边界跳错的问题；缓存恢复现在会直接同步当前首页 Adapter，清理会同时刷新所有仍存活的首页实例，双击、日期跳转和阅读位置只允许当前可见首页响应，避免几十条短页或伪尾项提前显示“没有更多内容”。
- 断层补齐新增可见微博锚点捕获与恢复，批量插入历史微博时保持当前阅读位置；补齐进度窗同时避开状态栏和刘海安全区。

### English

- Moves large timeline-cache loading, merging, filtering, text/media hydration, and gap scanning off the main thread, while coalescing duplicate restores to reduce ANRs, duplicate reads, and partial-write risk.
- Adds inclusive publication-time range clearing to the in-app ReWeibo settings, with both calendar selection and loose typed dates (date-only input covers the whole day), while updating native, shadow, and in-memory caches and blocking stale asynchronous writes.
- Fixes retained visible statuses becoming metadata-only rows with blank bodies after clearing another date; range clearing now prefers currently rendered objects and rehydrates text and media for every retained status.
- Extends timeline jump input to loose dates such as `7号`, `7-11`, and full Chinese dates; date-only input searches only that day and targets the primary timeline.
- Adds home timeline “跳转” and “删除” shortcuts, each independently configurable from ReWeibo settings.
- Prevents stale or hidden timeline fragments and duplicate/ad tail rows from creating a false timeline boundary after clearing or refreshing; restored cache data is applied to the visible home adapter, while double-tap, time-jump, and last-read actions are scoped to the current home timeline.
- Captures and restores the visible-status anchor during gap filling so inserted history does not move the current reading position, and keeps the progress card below status-bar and display-cutout insets.

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
