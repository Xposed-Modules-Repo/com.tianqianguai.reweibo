# ReWeibo

ReWeibo 是一个面向微博轻享版（`com.weico.international`）的 LSPosed/Xposed 模块。

ReWeibo is an LSPosed/Xposed module for Weibo Lite (`com.weico.international`).

## 功能

- 跳过微博轻享版启动页广告和时间线广告路径。
- 强制主页时间线使用更干净的反向/新微博在前排序。
- 过滤广告样式和无实际内容的时间线条目。
- 持久化并恢复扩展后的时间线缓存，降低刷新或缓存丢失后的阅读位置丢失概率。
- 在可信缓存恢复后保存并恢复上次阅读位置，并显示“上次读到这里”标记。
- 在可行时刷新或替换时间线视频的过期缓存地址。
- 保留新浪微博和 Share 的旧 hook 供手动 scope 使用，但官方默认推荐 scope 只包含微博轻享版。

## Features

- Skips Weibo Lite splash-ad and timeline-ad paths.
- Forces a cleaner reverse/newest-first order for the home timeline.
- Filters ad-like and contentless timeline entries.
- Persists and restores an expanded timeline cache to reduce reading-position loss after refreshes or cache eviction.
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

## 构建

面向公开分发的 release 构建必须使用私有 release keystore 签名：

```powershell
$env:REWEIBO_RELEASE_STORE_FILE="C:\path\to\reweibo-release.jks"
$env:REWEIBO_RELEASE_STORE_PASSWORD="..."
$env:REWEIBO_RELEASE_KEY_ALIAS="..."
$env:REWEIBO_RELEASE_KEY_PASSWORD="..."
.\gradlew.bat clean assembleRelease
```

如果没有配置这些签名值，Gradle 只会生成用于本地校验的 unsigned release APK。

## Build

Release builds intended for public distribution must be signed with a private release keystore:

```powershell
$env:REWEIBO_RELEASE_STORE_FILE="C:\path\to\reweibo-release.jks"
$env:REWEIBO_RELEASE_STORE_PASSWORD="..."
$env:REWEIBO_RELEASE_KEY_ALIAS="..."
$env:REWEIBO_RELEASE_KEY_PASSWORD="..."
.\gradlew.bat clean assembleRelease
```

Without these signing values, Gradle produces an unsigned release APK for local verification only.
