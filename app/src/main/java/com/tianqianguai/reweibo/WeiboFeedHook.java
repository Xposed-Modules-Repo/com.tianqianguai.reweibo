package com.tianqianguai.reweibo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class WeiboFeedHook {

    private static final AtomicBoolean sReversed = new AtomicBoolean(false);

    public static void hook(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> adapterClass = XposedHelpers.findClass(
                "com.sina.weibo.streamservice.adapter.RecyclerViewAdapter",
                lpparam.classLoader
            );

            for (Method m : adapterClass.getDeclaredMethods()) {
                if (Modifier.isAbstract(m.getModifiers())) continue;
                try {
                    XposedBridge.hookMethod(m, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (sReversed.get()) return;
                            reverseAdapterList(param.thisObject);
                        }
                    });
                } catch (Throwable ignored) {}
            }

            XposedBridge.log("ReWeibo: Successfully hooked RecyclerViewAdapter");
        } catch (Throwable t) {
            XposedBridge.log("ReWeibo: Failed to hook: " + t.getMessage());
        }
    }

    private static void reverseAdapterList(Object adapter) {
        try {
            Class<?> clazz = adapter.getClass();
            while (clazz != null && clazz != Object.class) {
                for (Field f : clazz.getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(adapter);
                    if (val instanceof List) {
                        List<?> list = (List<?>) val;
                        if (list.size() > 1) {
                            if (sReversed.compareAndSet(false, true)) {
                                Collections.reverse(list);
                                XposedBridge.log("ReWeibo: Reversed feed with " + list.size() + " items");
                            }
                            return;
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Throwable t) {
            XposedBridge.log("ReWeibo: Error reversing feed: " + t.getMessage());
        }
    }
}
