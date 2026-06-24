package com.tianqianguai.reweibo;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

public class MainHook extends XposedModule {

    private static final int LOG_LEVEL_INFO = 4;

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        if (!param.getPackageName().equals("com.sina.weibo")) {
            return;
        }
        log(LOG_LEVEL_INFO, "ReWeibo", "Weibo detected, hooking...");
        WeiboFeedHook.hook(this, param);
    }
}
