package com.tianqianguai.reweibo;

import java.util.Collections;
import java.util.List;

import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam;

public class WeiboFeedHook {
    public static void hook(XposedModule module, PackageLoadedParam param) {
        try {
            Class<?> cardListAdapterClass = param.getDefaultClassLoader().loadClass(
                "com.sina.weibo.page.CardListAdapter"
            );

            module.hook(cardListAdapterClass.getMethod("a",
                List.class, boolean.class, boolean.class))
                .intercept(new CardListAdapterHooker());

            module.log(4, "ReWeibo", "Successfully hooked CardListAdapter");
        } catch (Throwable t) {
            module.log(4, "ReWeibo", "Failed to hook: " + t.getMessage());
        }
    }

    private static class CardListAdapterHooker implements XposedInterface.Hooker {
        @Override
        public Object intercept(XposedInterface.Chain chain) throws Throwable {
            Object result = chain.proceed();
            try {
                Object adapter = chain.getThisObject();
                java.lang.reflect.Field field = adapter.getClass().getDeclaredField("c");
                field.setAccessible(true);
                List<?> list = (List<?>) field.get(adapter);
                if (list != null && !list.isEmpty()) {
                    Collections.reverse(list);
                }
            } catch (Throwable t) {
                // Silently ignore errors
            }
            return result;
        }
    }
}
