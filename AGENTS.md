# AGENTS.md — ReWeibo

## What this is

LSPosed/Xposed module (legacy `de.robv.android.xposed.*` API, **not** libxposed v2). Hooks into three target apps to modify feed layout and remove ads.

| Target | Package | Hook class | Function |
|--------|---------|------------|----------|
| 微博 | `com.sina.weibo` | `WeiboFeedHook` | Reverse feed + auto-scroll to top |
| Share | `com.hengye.share` | `ShareFeedHook` | Reverse feed layout |
| 微博轻享版 | `com.weico.international` | `WeiboLiteHook` | Remove splash + timeline ads |

Entry point: `MainHook` implements `IXposedHookLoadPackage`, dispatches by `lpparam.packageName`.

## Build & deploy

```bash
.\gradlew.bat assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Install + restart target (example for Weibo):
```bash
adb -s 192.168.6.17:5555 install -r app/build/outputs/apk/release/app-release.apk
adb -s 192.168.6.17:5555 shell am force-stop com.sina.weibo
adb -s 192.168.6.17:5555 logcat -c
adb -s 192.168.6.17:5555 shell monkey -p com.sina.weibo -c android.intent.category.LAUNCHER 1
```

Device: `192.168.6.17:5555` (Xiaomi, Android 16). Reconnect with `adb connect 192.168.6.17:5555` if dropped.

## Xposed config

- `assets/xposed_init` → `com.tianqianguai.reweibo.MainHook`
- `AndroidManifest.xml` meta-data: `xposedmodule=true`, `xposedminversion=82`
- Scope: `res/values/arrays.xml` `xposed_scope` array
- Dependency: `compileOnly(files("libs/xposed-bridge-api.jar"))` — **compileOnly**, not implementation

After changing scope or xposed_init, user must re-enable module in LSPosed manager and restart target app.

## Architecture

**Single module, 4 Java files, no frameworks.** All hooks use `XposedHelpers.findAndHookMethod` / `XposedHelpers.findAndHookConstructor`. No Kotlin, no AndroidX Room/DataBinding in the module itself.

### WeiboFeedHook (most complex)

- Hooks `LinearLayoutManager.setReverseLayout` / `setStackFromEnd` to force reverse layout
- Hooks `RecyclerView.setLayoutManager` to set reverseLayout via reflection after layout manager is assigned
- Auto-scroll: time-driven loop with `smoothScrollToPosition` (triggers `onScrolled` → load-more). `scrollToPosition` does NOT trigger load-more.
- First open: ~6 min auto-scroll (~1900 items). Subsequent: ~3.5 min (optimized delays).
- SharedPreferences flag (`context.getSharedPreferences("reweibo", ...)`) via `Application.onCreate` hook.

### ShareFeedHook

- Share's `ShareRecyclerView` extends `OOo0oO` (obfuscated), NOT standard `RecyclerView`
- Inner RecyclerView accessible via `getRecyclerView()` method
- `RecyclerView.onLayout` hook does NOT fire for the inner RV — must use polling approach
- `setLayoutManager` hook on `RecyclerView` fires when `CustomLinearLayoutManager` is assigned — `param.thisObject` IS the inner RV
- `O000000o(true)` on `CustomLinearLayoutManager` = `setReverseLayout(true)`
- `O0000oo` field on parent `LinearLayoutManager` = `mReverseLayout`
- Adapter count method: find via reflection (returns int, 0 params)

### WeiboLiteHook

- Splash ad: hook `LogoActivity.doWhatNext` → return "main"
- Timeline ad: hook `RxApiKt` lambda methods + `isWeiboUVEAd` + `Setting.loadBoolean/loadInt/loadString`
- Lambda names vary by version (6.1.7/6.2.6/6.3.8) — default uses 6.2.6 names
- Reference: https://github.com/wangyuan0217/MyXposed `WeicoHook.java`

## Critical gotchas

- **`smoothScrollToPosition` is the only way to trigger load-more.** `scrollToPosition` and `scrollBy` do NOT trigger `onScrolled` callbacks.
- **Xposed cannot hook abstract methods.** Must hook concrete implementation classes.
- **`$` in class names** (e.g. `RecyclerView$Adapter`): `XposedHelpers.findClass` converts `$` → `.` causing ClassNotFoundException. Use `Class.forName(name, false, classLoader)` or pass `Class<?>` objects.
- **Share's AndroidX is obfuscated.** Standard method/field names like `setReverseLayout`, `mReverseLayout` don't exist. Fields are `O0000xx` format.
- **Weibo uses `AqtsHttpClient`** (interface, not class) for networking. Standard OkHttp/URLConnection hooks don't capture feed API responses.
- **Weibo Room database (`feed_database`) has fixed 76-item cache.** Cannot be expanded via DELETE blocking — Room uses `SupportSQLiteDatabase` interface, not raw `SQLiteDatabase`.
- **Xposed `#exact` mode** fails for inherited methods on subclasses — hook the base class instead.
- **`onLayout` fires during `smoothScrollToPosition` animation** every frame — don't use layout callbacks as completion detection. Use idle-count polling.
- **SharedPreferences works across app restarts** when obtained via target app's `Application.onCreate` context.
- **Android scoped storage** blocks writing to `/sdcard/` from target app process. Use `context.getDir()` or `context.getSharedPreferences()`.
- **LSPosed processes**: Weibo hooks fire in 3 processes (`com.sina.weibo`, `:remote`, `.image`). Share fires in 2 (`:remote`). Module code must be safe for concurrent multi-process execution.

## What doesn't work (don't retry)

- Hooking `SQLiteDatabase.delete/execSQL/insertWithOnConflict` for Room databases
- Hooking `URLConnection.getInputStream` for Weibo API (OkHttp bypasses it)
- Hooking `ResponseBody.string/bytes/source` (Weibo uses AqtsHttpClient wrapper)
- Serializing Weibo ViewModels (all fields obfuscated single-letter, no toJson)
- `scrollBy(0, -999999)` to trigger load-more (doesn't fire onScrolled)
- Caching Weibo feed data for instant reload (no viable serialization path found)
