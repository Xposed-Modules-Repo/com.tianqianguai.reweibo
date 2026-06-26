package com.tianqianguai.reweibo;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.sina.weibo")) {
            XposedBridge.log("ReWeibo: Weibo detected, hooking...");
            WeiboFeedHook.hook(lpparam);
        } else if (lpparam.packageName.equals("com.hengye.share")) {
            XposedBridge.log("ReWeibo: Share detected, hooking...");
            ShareFeedHook.hook(lpparam);
        } else if (lpparam.packageName.equals("com.weico.international")) {
            XposedBridge.log("ReWeibo: WeiboLite detected, hooking...");
            WeiboLiteHook.hook(lpparam);
        }
    }
}
