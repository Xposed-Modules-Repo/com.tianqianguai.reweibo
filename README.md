# ReWeibo

ReWeibo is an LSPosed/Xposed module for Weibo Lite (`com.weico.international`).

## Features

- Skip Weibo Lite splash and timeline ads.
- Reverse the timeline feed and keep reading-position recovery behavior.
- Refresh stale cached image and video media paths when possible.
- Keep legacy hooks for Sina Weibo and Share available for manual LSPosed scopes, while the default published scope only recommends Weibo Lite.

## Compatibility

- Framework API: legacy XposedBridge API, compatible with LSPosed/Xposed API v82+.
- Default scope: `com.weico.international`.
- Module package: `com.tianqianguai.reweibo`.

## Install

1. Install the APK.
2. Enable the module in LSPosed.
3. Apply the recommended scope for Weibo Lite.
4. Force stop and reopen Weibo Lite.

## Build

Release builds intended for public distribution should be signed with a private release keystore:

```powershell
$env:REWEIBO_RELEASE_STORE_FILE="C:\path\to\reweibo-release.jks"
$env:REWEIBO_RELEASE_STORE_PASSWORD="..."
$env:REWEIBO_RELEASE_KEY_ALIAS="..."
$env:REWEIBO_RELEASE_KEY_PASSWORD="..."
.\gradlew.bat clean assembleRelease
```

Without these signing values, Gradle produces an unsigned release APK for local verification only.
