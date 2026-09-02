package com.tianqianguai.reweibo.compat;

import io.github.libxposed.api.XposedInterface;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reflection helpers limited to the surface used by ReWeibo's migrated hooks. */
public final class XposedHelpers {
    private XposedHelpers() {}

    public static Class<?> findClass(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException error) {
            if (!XposedBridge.isCurrentRegistrationLookupOptional()) {
                XposedBridge.markCurrentRegistrationGroupIncomplete(
                    "class lookup failed before hook registration: " + name,
                    error
                );
            }
            return sneakyThrow(error);
        }
    }

    public static Class<?> findClassIfExists(String name, ClassLoader classLoader) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static XposedInterface.HookHandle findAndHookMethod(
            String className,
            ClassLoader classLoader,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        return findAndHookMethod(
            findClass(className, classLoader),
            methodName,
            parameterTypesAndCallback
        );
    }

    public static XposedInterface.HookHandle findAndHookMethod(
            Class<?> clazz,
            String methodName,
            Object... parameterTypesAndCallback
    ) {
        XC_MethodHook callback;
        Method method;
        try {
            if (parameterTypesAndCallback.length == 0
                || !(parameterTypesAndCallback[parameterTypesAndCallback.length - 1] instanceof XC_MethodHook)) {
                throw new IllegalArgumentException("Last argument must be XC_MethodHook");
            }
            callback = (XC_MethodHook) parameterTypesAndCallback[
                parameterTypesAndCallback.length - 1
            ];
            Class<?>[] parameterTypes = new Class<?>[parameterTypesAndCallback.length - 1];
            for (int index = 0; index < parameterTypes.length; index++) {
                Object spec = parameterTypesAndCallback[index];
                if (spec instanceof Class<?>) {
                    parameterTypes[index] = (Class<?>) spec;
                } else if (spec instanceof String) {
                    parameterTypes[index] = findClass((String) spec, clazz.getClassLoader());
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + spec);
                }
            }
            method = findMethodExact(clazz, methodName, parameterTypes);
        } catch (Throwable error) {
            if (!XposedBridge.isCurrentRegistrationLookupOptional()) {
                XposedBridge.markCurrentRegistrationGroupIncomplete(
                    "method lookup failed before hook registration: "
                        + clazz.getName() + "#" + methodName,
                    error
                );
            }
            return sneakyThrow(error);
        }
        return XposedBridge.hookMethod(method, callback);
    }

    public static Object callMethod(Object instance, String methodName, Object... args) {
        if (instance == null) throw new NullPointerException("Cannot call " + methodName + " on null");
        Method method = findBestMethod(instance.getClass(), methodName, args, false);
        try {
            return method.invoke(instance, args);
        } catch (InvocationTargetException error) {
            return sneakyThrow(error.getCause() == null ? error : error.getCause());
        } catch (Throwable error) {
            return sneakyThrow(error);
        }
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        Method method = findBestMethod(clazz, methodName, args, true);
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException error) {
            return sneakyThrow(error.getCause() == null ? error : error.getCause());
        } catch (Throwable error) {
            return sneakyThrow(error);
        }
    }

    public static Object getObjectField(Object instance, String fieldName) {
        if (instance == null) throw new NullPointerException("Cannot read " + fieldName + " from null");
        try {
            return findField(instance.getClass(), fieldName).get(instance);
        } catch (Throwable error) {
            return sneakyThrow(error);
        }
    }

    public static void setObjectField(Object instance, String fieldName, Object value) {
        if (instance == null) throw new NullPointerException("Cannot write " + fieldName + " on null");
        try {
            findField(instance.getClass(), fieldName).set(instance, value);
        } catch (Throwable error) {
            sneakyThrow(error);
        }
    }

    private static Method findMethodExact(Class<?> clazz, String name, Class<?>[] types) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(name, types);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return sneakyThrow(new NoSuchMethodException(methodText(clazz, name, types)));
    }

    static Method resolveBestMethodForTest(
            Class<?> clazz,
            String name,
            Object[] args,
            boolean staticOnly
    ) {
        return findBestMethod(clazz, name, args, staticOnly);
    }

    private static Method findBestMethod(
            Class<?> clazz,
            String name,
            Object[] args,
            boolean staticOnly
    ) {
        List<MethodCandidate> candidates = new ArrayList<>();
        Set<String> seenSignatures = new HashSet<>();
        Class<?> current = clazz;
        int declaringDepth = 0;
        while (current != null) {
            Method[] methods = current.getDeclaredMethods();
            for (Method method : methods) {
                String signature = method.getName() + parameterText(method.getParameterTypes());
                if (!method.getName().equals(name) || !seenSignatures.add(signature)) continue;
                if (Modifier.isStatic(method.getModifiers()) != staticOnly) continue;
                Integer score = conversionScore(method.getParameterTypes(), args);
                if (score != null) candidates.add(new MethodCandidate(method, score, declaringDepth));
            }
            current = current.getSuperclass();
            declaringDepth++;
        }
        if (candidates.isEmpty()) {
            return sneakyThrow(new NoSuchMethodException(clazz.getName() + "#" + name + "/" + args.length));
        }

        int minimumScore = Integer.MAX_VALUE;
        for (MethodCandidate candidate : candidates) {
            minimumScore = Math.min(minimumScore, candidate.conversionScore);
        }
        List<MethodCandidate> closest = new ArrayList<>();
        for (MethodCandidate candidate : candidates) {
            if (candidate.conversionScore == minimumScore) closest.add(candidate);
        }

        List<MethodCandidate> mostSpecific = new ArrayList<>();
        for (MethodCandidate candidate : closest) {
            boolean dominated = false;
            for (MethodCandidate other : closest) {
                if (other != candidate
                    && isMoreSpecific(other.method.getParameterTypes(), candidate.method.getParameterTypes())) {
                    dominated = true;
                    break;
                }
            }
            if (!dominated) mostSpecific.add(candidate);
        }

        int minimumDepth = Integer.MAX_VALUE;
        for (MethodCandidate candidate : mostSpecific) {
            minimumDepth = Math.min(minimumDepth, candidate.declaringDepth);
        }
        List<MethodCandidate> finalists = new ArrayList<>();
        for (MethodCandidate candidate : mostSpecific) {
            if (candidate.declaringDepth == minimumDepth) finalists.add(candidate);
        }
        if (finalists.size() != 1) {
            StringBuilder signatures = new StringBuilder();
            for (MethodCandidate candidate : finalists) {
                if (signatures.length() > 0) signatures.append(", ");
                signatures.append(methodText(
                    candidate.method.getDeclaringClass(),
                    candidate.method.getName(),
                    candidate.method.getParameterTypes()
                ));
            }
            throw new IllegalArgumentException(
                "Ambiguous method " + clazz.getName() + "#" + name + ": " + signatures
            );
        }
        Method selected = finalists.get(0).method;
        selected.setAccessible(true);
        return selected;
    }

    private static Integer conversionScore(Class<?>[] types, Object[] args) {
        if (types.length != args.length) return null;
        int score = 0;
        for (int index = 0; index < types.length; index++) {
            Class<?> expected = boxed(types[index]);
            Object argument = args[index];
            if (argument == null) {
                if (types[index].isPrimitive()) return null;
                continue;
            }
            Class<?> actual = argument.getClass();
            if (!expected.isAssignableFrom(actual)) return null;
            score += hierarchyDistance(actual, expected);
        }
        return score;
    }

    private static int hierarchyDistance(Class<?> actual, Class<?> expected) {
        if (actual == expected) return 0;
        ArrayDeque<TypeDistance> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(new TypeDistance(actual, 0));
        while (!queue.isEmpty()) {
            TypeDistance next = queue.removeFirst();
            if (!visited.add(next.type)) continue;
            if (next.type == expected) return next.distance;
            Class<?> parent = next.type.getSuperclass();
            if (parent != null) queue.add(new TypeDistance(parent, next.distance + 1));
            for (Class<?> iface : next.type.getInterfaces()) {
                queue.add(new TypeDistance(iface, next.distance + 1));
            }
        }
        return Integer.MAX_VALUE / 4;
    }

    private static boolean isMoreSpecific(Class<?>[] first, Class<?>[] second) {
        if (first.length != second.length) return false;
        boolean strictly = false;
        for (int index = 0; index < first.length; index++) {
            Class<?> firstType = boxed(first[index]);
            Class<?> secondType = boxed(second[index]);
            if (!secondType.isAssignableFrom(firstType)) return false;
            if (firstType != secondType) strictly = true;
        }
        return strictly;
    }

    private static Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(clazz.getName() + "#" + name);
    }

    private static String methodText(Class<?> clazz, String name, Class<?>[] types) {
        return clazz.getName() + "#" + name + parameterText(types);
    }

    private static String parameterText(Class<?>[] types) {
        StringBuilder text = new StringBuilder("(");
        for (int index = 0; index < types.length; index++) {
            if (index > 0) text.append(',');
            text.append(types[index].getName());
        }
        return text.append(')').toString();
    }

    private static Class<?> boxed(Class<?> type) {
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == void.class) return Void.class;
        return type;
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }

    private static final class MethodCandidate {
        final Method method;
        final int conversionScore;
        final int declaringDepth;

        MethodCandidate(Method method, int conversionScore, int declaringDepth) {
            this.method = method;
            this.conversionScore = conversionScore;
            this.declaringDepth = declaringDepth;
        }
    }

    private static final class TypeDistance {
        final Class<?> type;
        final int distance;

        TypeDistance(Class<?> type, int distance) {
            this.type = type;
            this.distance = distance;
        }
    }
}
