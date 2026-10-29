package com.tianqianguai.reweibo.compat;

import java.lang.reflect.Method;

/** Exercises the production compatibility dispatch, including protective exception fallback. */
public final class HookTestDriver {
    public static Object invoke(Method method, XC_MethodHook hook, Object hostResult) throws Throwable {
        return XposedBridge.invokeLegacyForTest(method, hook, null, new Object[0], args -> hostResult);
    }
}
