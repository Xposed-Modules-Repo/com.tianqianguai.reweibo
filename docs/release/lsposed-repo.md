# LSPosed/Xposed Repository Release Checklist

Official repository: https://modules.lsposed.org

## Module metadata

- Submission issue title: `[submission] com.tianqianguai.reweibo`
- Repository name: `com.tianqianguai.reweibo`
- Repository description/module name: `ReWeibo`
- Default branch files required for listing:
  - `README.md`
  - `SUMMARY`
  - `SCOPE`
- Recommended scope: `["com.weico.international"]`

## Release 1.0

- Release title: `1.0`
- Release tag: `1-1.0`
- APK asset: signed `app-release.apk`
- Release content: copy the `1.0` section from `CHANGELOG.md`.

## Build a signed APK

Set signing values outside the repository, then build:

```powershell
$env:REWEIBO_RELEASE_STORE_FILE="C:\path\to\reweibo-release.jks"
$env:REWEIBO_RELEASE_STORE_PASSWORD="..."
$env:REWEIBO_RELEASE_KEY_ALIAS="..."
$env:REWEIBO_RELEASE_KEY_PASSWORD="..."
.\gradlew.bat clean assembleRelease
```

Do not publish an APK signed with the Android debug keystore. If the signing values are not set, `assembleRelease` intentionally produces an unsigned APK for local verification.

## Submit

Open https://modules.lsposed.org/submission/ and create the prefilled GitHub issue for a new package. After the bot creates the repository and grants admin access, push this default branch content and create the `1-1.0` release with the signed APK attached.
