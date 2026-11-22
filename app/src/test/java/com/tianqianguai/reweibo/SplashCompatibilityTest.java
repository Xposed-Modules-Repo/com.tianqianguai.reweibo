package com.tianqianguai.reweibo;

import com.tianqianguai.reweibo.compat.HookTestDriver;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import static org.junit.Assert.*;

public class SplashCompatibilityTest {
    static class Router702 { private boolean shouldShowSplashAd() { return true; } }
    static class Logo702 {}
    static class Current701 { private boolean shouldShowSplashAd() { return true; } }
    static class Legacy { private String doWhatNext() { return "ad"; } }
    static class WrongRouter { private String shouldShowSplashAd() { return "ad"; } }

    @Test public void routerDecisionTakesPriorityAndWorksWithoutLogoDecisionMethods() throws Throwable {
        Method routerDecision = WeiboLiteHook.findSplashDecisionMethod(Router702.class, Logo702.class);
        assertEquals(Router702.class, routerDecision.getDeclaringClass());
        assertFeatureSwitch(routerDecision);

        Method preferredOverLogo = WeiboLiteHook.findSplashDecisionMethod(Router702.class, Current701.class);
        assertEquals(Router702.class, preferredOverLogo.getDeclaringClass());
    }

    @Test public void missingOrInvalidRouterFallsBackToLogoBooleanThenLegacyString() throws Throwable {
        Method logoDecision = WeiboLiteHook.findSplashDecisionMethod(null, Current701.class);
        assertEquals(Current701.class, logoDecision.getDeclaringClass());
        assertFeatureSwitch(logoDecision);

        Method invalidRouterFallback = WeiboLiteHook.findSplashDecisionMethod(WrongRouter.class, Current701.class);
        assertEquals(Current701.class, invalidRouterFallback.getDeclaringClass());

        Method legacyDecision = WeiboLiteHook.findSplashDecisionMethod(null, Legacy.class);
        assertEquals(Legacy.class, legacyDecision.getDeclaringClass());
        assertFeatureSwitch(legacyDecision);

        Method invalidRouterLegacyFallback = WeiboLiteHook.findSplashDecisionMethod(WrongRouter.class, Legacy.class);
        assertEquals(Legacy.class, invalidRouterLegacyFallback.getDeclaringClass());
    }

    private static void assertFeatureSwitch(Method method) throws Throwable {
        Field features = WeiboLiteHook.class.getDeclaredField("sFeatures");
        features.setAccessible(true);
        Object saved = features.get(null);
        try {
            boolean booleanDecision = method.getReturnType() == boolean.class;
            Object original = booleanDecision ? Boolean.TRUE : "ad";
            features.set(null, ModuleSettings.featureSnapshot(Collections.emptyMap()));
            assertEquals(booleanDecision ? Boolean.FALSE : "main", HookTestDriver.invoke(method,
                WeiboLiteHook.splashDecisionHook(booleanDecision), original));
            features.set(null, ModuleSettings.featureSnapshot(Collections.singletonMap(
                ModuleSettings.KEY_WEICO_SPLASH_AD_REMOVAL, false)));
            assertEquals(original, HookTestDriver.invoke(method,
                WeiboLiteHook.splashDecisionHook(booleanDecision), original));
        } finally { features.set(null, saved); }
    }
}
