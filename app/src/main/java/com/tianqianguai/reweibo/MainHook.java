package com.tianqianguai.reweibo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.sina.weibo")) {
            return;
        }
        XposedBridge.log("ReWeibo: Weibo detected, hooking...");
        WeiboFeedHook.hook(lpparam);
    }
}
