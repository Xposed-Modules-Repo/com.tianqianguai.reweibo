# Reverse Weibo Feed Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create an LSPosed module that reverses Weibo's main feed order (oldest posts on top, newest on bottom)

**Architecture:** Hook `com.sina.weibo.page.CardListAdapter.a(List, boolean, boolean)` method to reverse the data list before it's displayed

**Tech Stack:** Android, Xposed API 82, Java

## Global Constraints

- Target package: `com.sina.weibo`
- Hook class: `com.sina.weibo.page.CardListAdapter`
- Hook method: `a(Ljava/util/List;ZZ)V` (obfuscated name)
- Data field: `c` (List<PageCardInfo>)
- Test device: 192.168.6.17:5555

---

### Task 1: Configure Xposed API Dependencies

**Covers:** Project setup

**Files:**
- Modify: `app/build.gradle.kts`
- Create: `libs/api-82.jar` (download Xposed API)

**Interfaces:**
- Produces: Xposed API available for compilation

- [ ] **Step 1: Download Xposed API jar**

Download `api-82.jar` from https://github.com/libxposed/api/releases and place in `libs/` folder.

- [ ] **Step 2: Update build.gradle.kts**

```kotlin
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.tianqianguai.reweibo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tianqianguai.reweibo"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    compileOnly(files("libs/api-82.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
```

- [ ] **Step 3: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/build.gradle.kts libs/
git commit -m "chore: add Xposed API dependency"
```

---

### Task 2: Create MainHook Entry Point

**Covers:** Module entry point

**Files:**
- Create: `app/src/main/java/com/tianqianguai/reweibo/MainHook.java`

**Interfaces:**
- Produces: `MainHook` class implementing `IXposedHookLoadPackage`

- [ ] **Step 1: Create MainHook.java**

```java
package com.tianqianguai.reweibo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.sina.weibo")) {
            return;
        }
        XposedBridge.log("ReWeibo: Weibo detected, hooking...");
        WeiboFeedHook.hook(lpparam);
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/tianqianguai/reweibo/MainHook.java
git commit -m "feat: add MainHook entry point"
```

---

### Task 3: Create WeiboFeedHook

**Covers:** Feed reversal logic

**Files:**
- Create: `app/src/main/java/com/tianqianguai/reweibo/WeiboFeedHook.java`

**Interfaces:**
- Consumes: `LoadPackageParam` from MainHook
- Produces: Hooked `CardListAdapter` that reverses feed data

- [ ] **Step 1: Create WeiboFeedHook.java**

```java
package com.tianqianguai.reweibo;

import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeiboFeedHook {
    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> cardListAdapterClass = XposedHelpers.findClass(
                "com.sina.weibo.page.CardListAdapter",
                lpparam.classLoader
            );

            XposedHelpers.findAndHookMethod(
                cardListAdapterClass,
                "a",
                List.class,
                boolean.class,
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            Object adapter = param.thisObject;
                            List<?> list = (List<?>) XposedHelpers.getObjectField(adapter, "c");
                            if (list != null && !list.isEmpty()) {
                                Collections.reverse(list);
                                XposedBridge.log("ReWeibo: Reversed feed with " + list.size() + " items");
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("ReWeibo: Error reversing feed: " + t.getMessage());
                        }
                    }
                }
            );

            XposedBridge.log("ReWeibo: Successfully hooked CardListAdapter");
        } catch (Throwable t) {
            XposedBridge.log("ReWeibo: Failed to hook: " + t.getMessage());
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/tianqianguai/reweibo/WeiboFeedHook.java
git commit -m "feat: implement feed reversal hook"
```

---

### Task 4: Configure Xposed Module Metadata

**Covers:** Module declaration

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/assets/xposed_init`

**Interfaces:**
- Produces: Xposed module metadata for LSPosed detection

- [ ] **Step 1: Update AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ReWeibo">

        <meta-data
            android:name="xposedmodule"
            android:value="true" />
        <meta-data
            android:name="xposeddescription"
            android:value="Reverse Weibo feed order" />
        <meta-data
            android:name="xposedminversion"
            android:value="82" />
        <meta-data
            android:name="xposedscope"
            android:resource="@array/xposed_scope" />

    </application>

</manifest>
```

- [ ] **Step 2: Create xposed_init file**

Create `app/src/main/assets/xposed_init` with content:
```
com.tianqianguai.reweibo.MainHook
```

- [ ] **Step 3: Create xposed_scope array**

Create `app/src/main/res/values/arrays.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="xposed_scope">
        <item>com.sina.weibo</item>
    </string-array>
</resources>
```

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/assets/ app/src/main/res/values/arrays.xml
git commit -m "feat: add Xposed module metadata"
```

---

### Task 5: Build and Install to Device

**Covers:** Deployment

**Files:**
- Output: `app/build/outputs/apk/debug/app-debug.apk`

**Interfaces:**
- Produces: Installed APK on test device

- [ ] **Step 1: Build release APK**

Run: `./gradlew assembleRelease`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Connect to device**

Run: `adb connect 192.168.6.17:5555`
Expected: connected to 192.168.6.17:5555

- [ ] **Step 3: Install APK**

Run: `adb -s 192.168.6.17:5555 install -r app/build/outputs/apk/release/app-release.apk`
Expected: Success

- [ ] **Step 4: Verify module activation**

1. Open LSPosed manager
2. Enable ReWeibo module
3. Select Weibo app in scope
4. Force stop Weibo
5. Open Weibo and check feed order

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "chore: build release APK for testing"
```

---

### Task 6: Verify and Debug

**Covers:** Testing and validation

**Files:**
- Log output from `adb logcat`

**Interfaces:**
- Produces: Verified feed reversal working

- [ ] **Step 1: Check Xposed logs**

Run: `adb -s 192.168.6.17:5555 logcat | grep -i "ReWeibo"`
Expected: "ReWeibo: Successfully hooked CardListAdapter"

- [ ] **Step 2: Test feed reversal**

1. Open Weibo
2. Scroll through feed
3. Verify oldest posts are at top
4. Verify newest posts are at bottom

- [ ] **Step 3: Debug if needed**

If not working:
1. Check if module is enabled in LSPosed
2. Check if Weibo is in scope
3. Check logcat for errors
4. Verify hook method signature matches

- [ ] **Step 4: Final commit**

```bash
git add .
git commit -m "feat: complete Weibo feed reversal module"
```
