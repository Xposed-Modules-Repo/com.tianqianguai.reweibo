package com.tianqianguai.reweibo;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.github.libxposed.api.XposedInterface;

/** Builds a saved-state payload containing only boot-classpath containers and target objects. */
public final class HotReloadState {
    private static final String MAGIC = "reweibo-hot-reload-v1";
    private static final int CONTEXT = 1;
    private static final int PRESENTER = 2;
    private static final int RECYCLER_VIEW = 3;
    private static final int OWNER_FRAGMENT = 4;
    private static final int ACTIVITY = 5;
    private static final int GENERATION = 6;
    private static final int BASE_SIZE = 7;
    private static final int FEATURES = 7;
    private static final int ROUTES = 8;

    private HotReloadState() {}

    public static Object compose(
            Object applicationContext,
            Object presenter,
            Object recyclerView,
            Object ownerFragment,
            Object activity,
            long generation
    ) {
        return new Object[] {
            MAGIC,
            applicationContext,
            presenter,
            recyclerView,
            ownerFragment,
            activity,
            Long.valueOf(generation)
        };
    }

    public static boolean isValid(Object state, ClassLoader moduleClassLoader) {
        if (!(state instanceof Object[])) return false;
        Object[] values = (Object[]) state;
        if ((values.length < BASE_SIZE || values.length > ROUTES + 1)
            || !MAGIC.equals(values[0])) return false;
        return isClassLoaderNeutral(state, moduleClassLoader);
    }

    public static boolean isClassLoaderNeutral(Object value, ClassLoader moduleClassLoader) {
        if (value == null) return true;
        Class<?> type = value.getClass();
        if (type.getClassLoader() == moduleClassLoader) return false;
        if (type.isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                if (!isClassLoaderNeutral(Array.get(value, index), moduleClassLoader)) return false;
            }
        } else if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                if (!isClassLoaderNeutral(item, moduleClassLoader)) return false;
            }
        } else if (value instanceof Map<?, ?>) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                if (!isClassLoaderNeutral(entry.getKey(), moduleClassLoader)
                    || !isClassLoaderNeutral(entry.getValue(), moduleClassLoader)) return false;
            }
        } else if (value instanceof AtomicReference<?>) {
            return isClassLoaderNeutral(
                ((AtomicReference<?>) value).get(),
                moduleClassLoader
            );
        }
        return true;
    }

    public static Object applicationContext(Object state) {
        return value(state, CONTEXT);
    }

    public static Object withFeatures(Object state, boolean[] features) {
        Object[] values = (Object[]) state;
        Object[] copy = java.util.Arrays.copyOf(values, Math.max(values.length, FEATURES + 1));
        copy[FEATURES] = features.clone();
        return copy;
    }

    public static Object withRoutes(
            Object state,
            Map<String, AtomicReference<XposedInterface.Hooker>> routes
    ) {
        Object[] values = (Object[]) state;
        Object[] copy = java.util.Arrays.copyOf(values, Math.max(values.length, ROUTES + 1));
        copy[ROUTES] = routes;
        return copy;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, AtomicReference<XposedInterface.Hooker>> routes(Object state) {
        Object stored = value(state, ROUTES);
        return stored instanceof Map<?, ?>
            ? (Map<String, AtomicReference<XposedInterface.Hooker>>) stored
            : java.util.Collections.emptyMap();
    }

    public static boolean[] features(Object state) {
        Object stored = value(state, FEATURES);
        return stored instanceof boolean[] ? ((boolean[]) stored).clone() : null;
    }

    public static Object presenter(Object state) {
        return value(state, PRESENTER);
    }

    public static Object recyclerView(Object state) {
        return value(state, RECYCLER_VIEW);
    }

    public static Object ownerFragment(Object state) {
        return value(state, OWNER_FRAGMENT);
    }

    public static Object activity(Object state) {
        return value(state, ACTIVITY);
    }

    public static long previousGeneration(Object state) {
        Object value = value(state, GENERATION);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static Object value(Object state, int index) {
        if (!(state instanceof Object[])) return null;
        Object[] values = (Object[]) state;
        return values.length > index ? values[index] : null;
    }
}
