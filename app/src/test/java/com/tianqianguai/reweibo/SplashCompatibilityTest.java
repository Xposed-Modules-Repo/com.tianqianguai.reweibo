package com.tianqianguai.reweibo;

import com.tianqianguai.reweibo.compat.HookTestDriver;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import static org.junit.Assert.*;

public class SplashCompatibilityTest {
    static class Current { private boolean shouldShowSplashAd() { return true; } }
    static class Legacy { private String doWhatNext() { return "ad"; } }
    @Test public void currentAndLegacyDecisionsHonorTheFeatureSwitch() throws Throwable {
        Field features = WeiboLiteHook.class.getDeclaredField("sFeatures");
        features.setAccessible(true);
        Object saved = features.get(null);
        try {
            for (Class<?> type : new Class<?>[]{Current.class, Legacy.class}) {
                Method method = WeiboLiteHook.findSplashDecisionMethod(type);
                boolean current = method.getReturnType() == boolean.class;
                Object original = current ? Boolean.TRUE : "ad";
                features.set(null, ModuleSettings.featureSnapshot(Collections.emptyMap()));
                assertEquals(current ? Boolean.FALSE : "main", HookTestDriver.invoke(method,
                    WeiboLiteHook.splashDecisionHook(current), original));
                features.set(null, ModuleSettings.featureSnapshot(Collections.singletonMap(
                    ModuleSettings.KEY_WEICO_SPLASH_AD_REMOVAL, false)));
                assertEquals(original, HookTestDriver.invoke(method,
                    WeiboLiteHook.splashDecisionHook(current), original));
            }
        } finally { features.set(null, saved); }
    }
}
