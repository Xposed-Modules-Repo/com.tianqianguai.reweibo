# LSPosed/Xposed 仓库发布清单

# LSPosed/Xposed Repository Release Checklist

官方仓库：https://modules.lsposed.org

Official repository: https://modules.lsposed.org

## 模块元数据

- 提交 issue 标题：`[submission] com.tianqianguai.reweibo`
- 仓库名：`com.tianqianguai.reweibo`
- 仓库描述/模块名：`ReWeibo`
- 默认分支中用于索引展示的文件：
  - `README.md`
  - `SUMMARY`
  - `SCOPE`
- 推荐 scope：`["com.weico.international"]`

## Module Metadata

- Submission issue title: `[submission] com.tianqianguai.reweibo`
- Repository name: `com.tianqianguai.reweibo`
- Repository description/module name: `ReWeibo`
- Default branch files required for listing:
  - `README.md`
  - `SUMMARY`
  - `SCOPE`
- Recommended scope: `["com.weico.international"]`

## Release 1.0

- Release 标题：`ReWeibo 1.0`
- Release tag：`1-1.0`
- APK 资产：已签名 `ReWeibo-v1.0.apk`
- Release 内容：使用 `docs/release/1.0.md`，并附 APK SHA-256。

## Release 1.0

- Release title: `ReWeibo 1.0`
- Release tag: `1-1.0`
- APK asset: signed `ReWeibo-v1.0.apk`
- Release content: use `docs/release/1.0.md` and include the APK SHA-256.

## Release 1.0.1

- Release 标题：`ReWeibo 1.0.1`
- Release tag：`2-1.0.1`
- APK 资产：已签名 `ReWeibo-v1.0.1.apk`
- Release 内容：使用 `docs/release/1.0.1.md`，并附 APK SHA-256。

## Release 1.0.1

- Release title: `ReWeibo 1.0.1`
- Release tag: `2-1.0.1`
- APK asset: signed `ReWeibo-v1.0.1.apk`
- Release content: use `docs/release/1.0.1.md` and include the APK SHA-256.

## 构建已签名 APK

在仓库外配置签名值，然后构建：

```powershell
$env:REWEIBO_RELEASE_STORE_FILE="C:\path\to\reweibo-release.jks"
$env:REWEIBO_RELEASE_STORE_PASSWORD="..."
$env:REWEIBO_RELEASE_KEY_ALIAS="..."
$env:REWEIBO_RELEASE_KEY_PASSWORD="..."
.\gradlew.bat clean assembleRelease
```

不要发布 Android debug keystore 签名的 APK。如果没有设置签名值，`assembleRelease` 会故意生成 unsigned APK，仅用于本地校验。

## Build a Signed APK

Set signing values outside the repository, then build:

```powershell
$env:REWEIBO_RELEASE_STORE_FILE="C:\path\to\reweibo-release.jks"
$env:REWEIBO_RELEASE_STORE_PASSWORD="..."
$env:REWEIBO_RELEASE_KEY_ALIAS="..."
$env:REWEIBO_RELEASE_KEY_PASSWORD="..."
.\gradlew.bat clean assembleRelease
```

Do not publish an APK signed with the Android debug keystore. If the signing values are not set, `assembleRelease` intentionally produces an unsigned APK for local verification.

## 提交与发布

通过 https://modules.lsposed.org/submission/ 创建新包提交。官方 bot 创建仓库并授予权限后，推送默认分支内容，并创建带签名 APK 的 `1-1.0` release。

## Submit and Publish

Open https://modules.lsposed.org/submission/ and create the submission for a new package. After the official bot creates the repository and grants access, push the default branch content and create the `1-1.0` release with the signed APK attached.
