# AGENTS.md — ReWeibo

## What this is

LSPosed module using Modern `io.github.libxposed:api:102.0.0`. Its static scope contains only Weibo Lite. The older Weibo/Share hook source files remain for source compatibility, but `MainHook` does not dispatch them.

| Target | Package | Hook class | Function |
|--------|---------|------------|----------|
| 微博轻享版 | `com.weico.international` | `WeiboLiteHook` | Remove splash + timeline ads + reverse feed + auto-scroll |

Entry point: the sole Java `MainHook extends XposedModule`, dispatched from `onPackageReady`. Hot reload is implemented through `onHotReloading` / `onHotReloaded`, not by replaying package callbacks.

## Build & deploy

```bash
.\gradlew.bat assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Install + restart target (when an explicit device deployment is requested):
```bash
adb -s 192.168.6.17:5555 install -r app/build/outputs/apk/release/app-release.apk
adb -s 192.168.6.17:5555 shell am force-stop com.weico.international
adb -s 192.168.6.17:5555 logcat -c
adb -s 192.168.6.17:5555 shell monkey -p com.weico.international -c android.intent.category.LAUNCHER 1
```

Device: `192.168.6.17:5555` (Xiaomi, Android 16). Reconnect with `adb connect 192.168.6.17:5555` if dropped.

**When testing, simulate interactions yourself via adb.** Don't ask the user to tap buttons. Use `adb shell input tap x y` for taps, `adb shell input swipe` for scrolls. First confirm the target app is in the foreground with `adb shell dumpsys activity activities | grep mFocusedApp`.

## Agent ADB command interface

For supported ReWeibo operations, use raw `adb shell content call` first. Do not use repository wrapper scripts, screenshots, UI dumps, or coordinate taps when a command exists.

Provider URI: `content://com.tianqianguai.reweibo.settings/settings`

```bash
# Activate Weibo Lite first; Xiaomi may freeze background broadcast receivers.
adb -s 192.168.6.17:5555 shell am start -n com.weico.international/.appicon_white

# Discover the live contract and settings.
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method help
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.list

# Read, set, or reset one setting. Run weico.settings.reload after changing a running target.
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.get --arg weico_timeline_cache_days
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.set --arg weico_timeline_cache_days --extra value:i:30
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method settings.reset --arg weico_timeline_cache_days

# Runtime status and actions.
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.status
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.logs.status
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.logs.read --extra start:s:2026-09-02_09-00-00 --extra end:s:2026-09-02_11-30-00 --extra max_chars:i:48000
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.logs.export --extra start:s:2026-09-02_09-00-00 --extra end:s:2026-09-02_11-30-00
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.top
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.bottom
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.timeline.jump --extra "value:s:7-11 18:30"
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.stats
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.cache.clear --extra day:s:2026-07-07
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.preload.restart
adb -s 192.168.6.17:5555 shell content call --uri content://com.tianqianguai.reweibo.settings/settings --method exec --arg weico.settings.reload
```

`weico.logs.export`, `weico.cache.stats`, and `weico.cache.clear` are asynchronous. Poll `weico.status` and read `last_operation_state` until it becomes `completed` or `error`. A completed log export reports a pullable path in `last_log_export_path`. Use `yyyy-MM-dd_HH-mm-ss` for raw ADB ranges because Android `content` bindings do not reliably preserve spaces or colons; the app UI also accepts the human-readable space and ISO `T` forms.

## Release notes

When preparing a release:

- Compare the previous release tag with the release commit and cover the complete interval, including any final uncommitted fixes that will ship.
- Keep `CHANGELOG.md`, `docs/release/<version>.md`, README feature descriptions, and the GitHub Release body consistent.
- Write release notes in both Chinese and English. The Chinese and English sections must describe the same shipped behavior.
- Use the template below. Omit `修复` / `Fixes` only when the release contains no fixes.
- Do not add `模块元数据` / `Module Metadata`, `验证` / `Verification`, `APK 校验` / `APK Checksum`, or `已知限制` / `Known Limit` sections unless the user explicitly requests them.
- Use `docs/release/<version>.md` verbatim as the GitHub Release body.

```markdown
# ReWeibo <version>

<中文版本摘要>

<English release summary>

## 新增与改进

- <覆盖上个 release tag 至本次版本的中文更新内容>

## Added And Improved

- <English updates covering the same release interval>

## 修复

- <中文修复内容>

## Fixes

- <English fixes matching the Chinese section>
```

## Debugging

- **Use persistent file logging, NOT logcat buffer.** `XposedBridge.log()` goes to logcat which can overflow or get cleared. For durable logs, write to target app's internal storage via `context.getDir()` or `context.getFilesDir()`. Example: `new File(context.getFilesDir(), "reweibo.log")`.
- ShareFeedHook: `/data/data/com.hengye.share/files/reweibo_share.log`
- WeiboLiteHook: `/data/data/com.weico.international/files/reweibo_weico.log`
- To read persistent logs: `adb shell cat /data/data/<package>/files/<logfile>`
- New entries use `yyyy-MM-dd HH:mm:ss.SSS`. Legacy `HH:mm:ss` entries remain available in an unbounded export but are skipped for exact date ranges because their calendar date cannot be recovered safely.

## Xposed config

- `META-INF/xposed/java_init.list` → `com.tianqianguai.reweibo.MainHook`
- `META-INF/xposed/module.prop`: API 102, static scope, protective exception mode, automatic hot reload
- `META-INF/xposed/scope.list`: only `com.weico.international`
- Dependency: `compileOnly("io.github.libxposed:api:102.0.0")`; the API must not be packaged into the APK
- Legacy `assets/xposed_init`, Manifest Xposed metadata, and the API 82 jar do not exist

`autoHotReload=true` updates an already-running scoped process. If a cache clear, background cache task, network probe/subscription, or module dialog is active, the old generation rejects reload and stays usable; retry after it becomes idle.

## Architecture

**Single Java module, no app-side framework migration.** Existing hook code keeps its `XC_MethodHook` / `XposedHelpers` calling style through the local `com.tianqianguai.reweibo.compat` bridge, while production code and the APK contain no legacy API references.

### API 102 hot reload

- Every hook receives a deterministic executable-and-slot ID. `onHotReloaded` uses `HookHandle.replaceHook` for same-ID atomic replacement; a failed replacement retains the old handle, and a complete generation removes stale handles.
- `HotReloadRuntime` owns main-thread callbacks and the cache executors. Reload cancels queued callbacks, stops executors, unregisters the CLI receiver, removes shortcut/progress views and listeners, and clears target-object caches.
- Saved state is an `Object[]` containing only boot-classpath containers and target-classloader objects: Application context, current presenter, RecyclerView, owner fragment when present, Activity, and generation number.
- Restored state re-registers the raw ADB CLI and recreates timeline shortcuts without restarting the target process. `weico.status` reports API, framework, generation, readiness, active tasks, and observable-probe evidence.

### FloatingButton

This helper is retained for the non-dispatched Weibo/Share source files; it is not installed in the current static Weibo Lite scope.

- Primary: `WindowManager` + `TYPE_APPLICATION_PANEL` + `activity.getWindow().getDecorView().getWindowToken()` — no `SYSTEM_ALERT_WINDOW` needed
- Fallback: `activity.addContentView()` — works for Weibo/Share, but Weico's view hierarchy sometimes intercepts touches
- Hook `Activity.onResume` to attach, `sLastActivity` dedup prevents re-adding
- Double-tap detection: 500ms window, `SystemClock.elapsedRealtime()`, `onTouchListener(return true)`
- Single-tap stops scrolling: 500ms delayed callback, cancelled if double-tap detected. `SingleTapAction` interface

### WeiboFeedHook

Retained source only; `MainHook` does not dispatch `com.sina.weibo`.

- `onLayout` hook on `RecyclerView` fires intermittently for main process — filter by adapter class name containing `RecyclerViewAdapter`
- `setLayoutManager` hook fires on background thread — `postDelayed` callbacks may find adapter already consumed
- Auto-scroll: `smoothScrollToPosition` triggers `onScrolled` → load-more. `scrollToPosition` does NOT.
- Idle-count polling: 1.5s interval, 30 checks for first-time, 10 for subsequent opens
- SharedPreferences flag via `Application.onCreate` context

### ShareFeedHook

Retained source only; `MainHook` does not dispatch `com.hengye.share`.

- Share's `ShareRecyclerView` extends `OOo0oO` (obfuscated), NOT standard `RecyclerView`
- `RecyclerView.onLayout` hook fires for the inner RV (standard RecyclerView)
- `setLayoutManager` hook on `RecyclerView` fires when `CustomLinearLayoutManager` is assigned
- `O000000o(true)` on `CustomLinearLayoutManager` = `setReverseLayout(true)`
- "显示更多" button: `FrameLayout` (not `TextView`), hook `View.setOnClickListener` to capture listener
- Listener gets recycled by RecyclerView — must re-capture on each bind, call `onClick(null)` async via Handler
- "上次阅读到这里，点击刷新" marker: search visible TextViews during scroll for "上次"/"阅读"/"刷新"/"已读". Stop scrolling when found. If not found, scroll to bottom.
- Stable check: 10 rounds if count>500, 100 rounds if count>100, then scroll to top

### WeiboLiteHook

- Ad removal: hook `LogoActivity.doWhatNext` → "main", `RxApiKt` lambdas + `isWeiboUVEAd` + `Setting.load*`
- Lambda names vary by version (6.1.7/6.2.6/6.3.8) — default uses 6.2.6 names
- Feed reversal: same as WeiboFeedHook pattern (setReverseLayout + onLayout + auto-scroll)
- RV captured via `setLayoutManager` hook, filter by adapter class name containing `TimelineAdapter` (NOT `MainProfileAdapter` which is drawer items)
- Feed adapter: `com.weico.international.adapter.TimelineAdapter` — actual feed data. `MainProfileAdapter` = drawer items (19 DrawerInfo + 1 footer = 20)
- Load-more: Weico uses `EasyRecyclerView` library — `smoothScrollToPosition` does NOT trigger load-more. Must call `adapter.switch2LoadMore()` directly. But `switch2LoadMore()` alone also doesn't trigger — it needs `smoothScrollToPosition` to be called alongside (scroll state triggers the load-more callback internally)
- Pull-refresh disable: hook `SwipeRefreshLayout.canChildScrollUp` → return true (hook on base class, NOT on ESwpLayout which fails with exact error)
- Auto-load without user scroll: NOT possible — EasyRecyclerView only activates load-more after real user scroll interaction. All programmatic approaches (dispatchTouchEvent, scrollBy, smoothScrollToPosition, onScrolled) fail to trigger it. User must double-tap R button manually.
- Reference: https://github.com/wangyuan0217/MyXposed `WeicoHook.java`

## Critical gotchas

- **`smoothScrollToPosition` is the only way to trigger load-more.** `scrollToPosition` and `scrollBy` do NOT trigger `onScrolled` callbacks.
- **Weico is different** — uses `EasyRecyclerView` library. `smoothScrollToPosition` does NOT trigger its load-more. Must call `adapter.switch2LoadMore()` directly. But `switch2LoadMore()` alone also doesn't trigger — it needs `smoothScrollToPosition` to be called alongside (scroll state triggers the load-more callback internally).
- **`canChildScrollUp` hook on ESwpLayout fails** — must hook on base class `SwipeRefreshLayout` instead. ESwpLayout is obfuscated SwipeRefreshLayout subclass, exact mode fails.
- **Weico auto-load blocked** — `switch2LoadMore()` without user scroll doesn't trigger data loading. EasyRecyclerView's `OnMoreListener` registration path is unknown (`setMore` hook never fires). See MEMORY.md for investigation status.
- **Xposed cannot hook abstract methods.** Must hook concrete implementation classes.
- **`$` in class names** (e.g. `RecyclerView$Adapter`): `XposedHelpers.findClass` converts `$` → `.` causing ClassNotFoundException. Use `Class.forName(name, false, classLoader)` or pass `Class<?>` objects.
- **Share's AndroidX is obfuscated.** Standard method/field names like `setReverseLayout`, `mReverseLayout` don't exist. Fields are `O0000xx` format.
- **Weibo uses `AqtsHttpClient`** (interface, not class) for networking. Standard OkHttp/URLConnection hooks don't capture feed API responses.
- **Weibo Room database (`feed_database`) has fixed 76-item cache.** Cannot be expanded.
- **Xposed `#exact` mode** fails for inherited methods on subclasses — hook the base class instead.
- **`onLayout` fires during `smoothScrollToPosition` animation** every frame — use idle-count polling, not layout callbacks.
- **SharedPreferences works across app restarts** when obtained via target app's `Application.onCreate` context.
- **`View.setOnClickListener` hook captures recycled listeners** — RecyclerView recycles ViewHolder, clearing and re-setting listeners on each bind.
- **Static scope is Weibo Lite only.** Do not add another package to `scope.list` or dispatch another hook without a new explicit task.

## What doesn't work (don't retry)

- Hooking `SQLiteDatabase.delete/execSQL/insertWithOnConflict` for Room databases
- Hooking `URLConnection.getInputStream` for Weibo API
- Hooking `ResponseBody.string/bytes/source` (Weibo uses AqtsHttpClient wrapper)
- Serializing Weibo ViewModels (all fields obfuscated, no toJson)
- `scrollBy(0, -999999)` to trigger load-more (doesn't fire onScrolled)
- `WindowManager.addView` with `TYPE_APPLICATION_OVERLAY` (needs permission)
- `PullDownView.onInterceptTouchEvent` / `onTouchEvent` hook (NoSuchMethodError on all hierarchy levels)
- `canChildScrollUp` hook on `ESwpLayout` (exact error — cannot hook)
- `setOnTouchListener` with `return false` on PullDownView (events consumed internally)
- `switch2LoadMore()` without user scroll on Weico (EasyRecyclerView requires OnMoreListener to be registered first)
