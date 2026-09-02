package com.tianqianguai.reweibo.compat;

import android.util.Log;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;

import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Modern API 102 bridge preserving ReWeibo's existing before/after callback behavior. */
public final class XposedBridge {
    private static final String TAG = "ReWeibo";
    private static final String HOOK_ID_PREFIX = "reweibo:";

    private static volatile XposedModule sModule;
    private static final ThreadLocal<MutableRegistration> REGISTRATION = new ThreadLocal<>();
    private static final ConcurrentHashMap<String, AtomicInteger> RUNTIME_SLOTS =
        new ConcurrentHashMap<>();

    private XposedBridge() {}

    public static void attach(XposedModule module) {
        sModule = module;
    }

    public static int apiVersion() {
        XposedModule module = sModule;
        if (module == null) return 102;
        try {
            return module.getApiVersion();
        } catch (Throwable ignored) {
            return 102;
        }
    }

    public static String frameworkName() {
        XposedModule module = sModule;
        if (module == null) return "unattached";
        try {
            return module.getFrameworkName();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public static String frameworkVersion() {
        XposedModule module = sModule;
        if (module == null) return "";
        try {
            return module.getFrameworkVersion();
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static void log(String message) {
        XposedModule module = sModule;
        if (module != null) {
            try {
                module.log(Log.INFO, TAG, message);
                return;
            } catch (Throwable ignored) {}
        }
        try {
            Log.i(TAG, message);
        } catch (Throwable ignored) {}
    }

    public static void log(Throwable throwable) {
        XposedModule module = sModule;
        String message = throwable == null || throwable.getMessage() == null
            ? "hook callback failed"
            : throwable.getMessage();
        if (module != null) {
            try {
                module.log(Log.ERROR, TAG, message, throwable);
                return;
            } catch (Throwable ignored) {}
        }
        try {
            Log.e(TAG, message, throwable);
        } catch (Throwable ignored) {}
    }

    public static void beginRegistration(List<OldHookSnapshot> oldHooks) {
        if (REGISTRATION.get() != null) {
            throw new IllegalStateException("A hook registration session is already active");
        }
        REGISTRATION.set(new MutableRegistration(oldHooks));
    }

    public static void beginRegistrationGroup(String name) {
        MutableRegistration registration = REGISTRATION.get();
        if (registration == null) {
            throw new IllegalStateException("No hook registration session is active");
        }
        registration.beginGroup(name);
    }

    public static void completeRegistrationGroup(String name) {
        MutableRegistration registration = REGISTRATION.get();
        if (registration == null) return;
        registration.completeGroup(name);
    }

    public static void markCurrentRegistrationGroupIncomplete(
            String detail,
            Throwable error
    ) {
        MutableRegistration registration = REGISTRATION.get();
        if (registration == null) return;
        registration.markCurrentGroupIncomplete(detail, error);
    }

    public static void beginOptionalRegistrationLookup() {
        MutableRegistration registration = REGISTRATION.get();
        if (registration != null) registration.optionalLookupDepth++;
    }

    public static void endOptionalRegistrationLookup() {
        MutableRegistration registration = REGISTRATION.get();
        if (registration != null && registration.optionalLookupDepth > 0) {
            registration.optionalLookupDepth--;
        }
    }

    public static boolean isCurrentRegistrationLookupOptional() {
        MutableRegistration registration = REGISTRATION.get();
        return registration != null && registration.optionalLookupDepth > 0;
    }

    public static RegistrationReport finishRegistration(boolean dispatchCompleted) {
        MutableRegistration mutable = REGISTRATION.get();
        REGISTRATION.remove();
        if (mutable == null) mutable = new MutableRegistration(Collections.emptyList());
        mutable.finishOpenGroup();
        boolean groupsComplete = mutable.allGroupsCompleted();
        return new RegistrationReport(
            mutable.attempted,
            mutable.successful,
            mutable.failures,
            dispatchCompleted && mutable.failures.isEmpty() && groupsComplete,
            groupsComplete
        );
    }

    public static XposedInterface.HookHandle hookMethod(Member member, XC_MethodHook callback) {
        if (!(member instanceof Executable)) {
            throw new IllegalArgumentException("Only methods and constructors can be hooked: " + member);
        }
        XposedModule module = sModule;
        if (module == null) throw new IllegalStateException("Modern Xposed module is not attached");
        Executable executable = (Executable) member;
        executable.setAccessible(true);

        String executableKey = executableIdentity(executable);
        MutableRegistration session = REGISTRATION.get();
        HookIdentity identity;
        if (session != null) {
            identity = session.nextIdentity(executableKey);
        } else {
            int slot = RUNTIME_SLOTS
                .computeIfAbsent(executableKey, key -> new AtomicInteger())
                .getAndIncrement();
            identity = new HookIdentity(executableKey, stableId(executableKey, slot));
        }
        if (session != null) session.attempted.add(identity);

        XposedInterface.Hooker hooker = chain -> invokeLegacyCallback(
            executable,
            callback,
            chain.getThisObject(),
            chain.getArgs().toArray(new Object[0]),
            chain::proceed
        );

        try {
            XposedInterface.HookHandle oldHandle = session == null
                ? null
                : session.oldStableHooks.get(identity);
            XposedInterface.HookHandle handle;
            if (oldHandle != null) {
                handle = oldHandle.replaceHook(hooker);
            } else {
                handle = module.hook(executable)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                    .setId(identity.id)
                    .intercept(hooker);
            }
            if (session != null) session.successful.put(identity, handle);
            return handle;
        } catch (Throwable error) {
            if (session != null) {
                session.failures.add(new RegistrationFailure(
                    identity,
                    error.getMessage() == null ? error.getClass().getName() : error.getMessage()
                ));
                session.markCurrentGroupIncomplete(
                    "hook installation failed: " + identity.executable,
                    error
                );
            }
            return sneakyThrow(error);
        }
    }

    public static List<OldHookSnapshot> snapshotOldHooks(
            List<XposedInterface.HookHandle> handles
    ) {
        if (handles == null || handles.isEmpty()) return Collections.emptyList();
        ArrayList<OldHookSnapshot> snapshots = new ArrayList<>();
        for (XposedInterface.HookHandle handle : handles) {
            if (handle == null) continue;
            try {
                snapshots.add(new OldHookSnapshot(
                    handle,
                    executableIdentity(handle.getExecutable()),
                    handle.getId()
                ));
            } catch (Throwable ignored) {}
        }
        return snapshots;
    }

    /**
     * Same-ID hooks were replaced atomically during registration. Failed replacements retain their
     * old handle; only a complete generation removes handles that are now stale.
     */
    public static OldHookReconcileReport reconcileOldHooks(
            List<OldHookSnapshot> oldHooks,
            RegistrationReport report
    ) {
        Map<String, List<OldHookSnapshot>> legacyByExecutable = new LinkedHashMap<>();
        Map<String, Integer> successfulByExecutable = new LinkedHashMap<>();
        for (Map.Entry<HookIdentity, XposedInterface.HookHandle> entry : report.successful.entrySet()) {
            String executable = entry.getKey().executable;
            successfulByExecutable.put(
                executable,
                successfulByExecutable.containsKey(executable)
                    ? successfulByExecutable.get(executable) + 1
                    : 1
            );
        }
        for (OldHookSnapshot snapshot : oldHooks) {
            if (snapshot.id == null) {
                legacyByExecutable
                    .computeIfAbsent(snapshot.executable, key -> new ArrayList<>())
                    .add(snapshot);
            }
        }

        int atomicallyReplaced = 0;
        int legacyRemoved = 0;
        int staleRemoved = 0;
        int retainedAfterFailure = 0;

        for (Map.Entry<String, List<OldHookSnapshot>> entry : legacyByExecutable.entrySet()) {
            int replacements = successfulByExecutable.containsKey(entry.getKey())
                ? successfulByExecutable.get(entry.getKey())
                : 0;
            boolean safeToRemove = report.completed || replacements >= entry.getValue().size();
            if (safeToRemove) {
                for (OldHookSnapshot snapshot : entry.getValue()) {
                    safeUnhook(snapshot.handle);
                    legacyRemoved++;
                }
            } else {
                retainedAfterFailure += entry.getValue().size();
            }
        }

        for (OldHookSnapshot snapshot : oldHooks) {
            if (snapshot.id == null) continue;
            HookIdentity identity = new HookIdentity(snapshot.executable, snapshot.id);
            if (report.successful.containsKey(identity)) {
                atomicallyReplaced++;
            } else if (report.attempted.contains(identity)) {
                retainedAfterFailure++;
            } else if (report.completed) {
                safeUnhook(snapshot.handle);
                staleRemoved++;
            } else {
                retainedAfterFailure++;
            }
        }
        return new OldHookReconcileReport(
            atomicallyReplaced,
            legacyRemoved,
            staleRemoved,
            retainedAfterFailure
        );
    }

    private static void safeUnhook(XposedInterface.HookHandle handle) {
        try {
            handle.unhook();
        } catch (Throwable ignored) {}
    }

    private static Object invokeLegacyCallback(
            Executable executable,
            XC_MethodHook callback,
            Object thisObject,
            Object[] args,
            Proceeding proceeding
    ) throws Throwable {
        Object[] originalArgs = args.clone();
        XC_MethodHook.MethodHookParam param = new XC_MethodHook.MethodHookParam(
            executable,
            thisObject,
            args
        );
        try {
            callback.callBefore(param);
        } catch (Throwable error) {
            log(error);
            return proceeding.proceed(originalArgs);
        }

        if (!param.isReturnEarly()) {
            try {
                param.setResultFromChain(proceeding.proceed(param.args));
            } catch (Throwable error) {
                param.setThrowableFromChain(error);
            }
        }

        XC_MethodHook.MethodHookParam.Snapshot beforeAfter = param.snapshot();
        try {
            callback.callAfter(param);
        } catch (Throwable error) {
            log(error);
            param.restore(beforeAfter);
        }
        if (param.getThrowable() != null) throw param.getThrowable();
        return param.getResult();
    }

    static Object invokeLegacyForTest(
            Executable executable,
            XC_MethodHook callback,
            Object thisObject,
            Object[] args,
            Proceeding proceeding
    ) throws Throwable {
        return invokeLegacyCallback(executable, callback, thisObject, args, proceeding);
    }

    static String executableIdentity(Executable executable) {
        StringBuilder text = new StringBuilder()
            .append(executable.getDeclaringClass().getName())
            .append('#')
            .append(executable.getName())
            .append('(');
        Class<?>[] parameters = executable.getParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            if (index > 0) text.append(',');
            text.append(parameters[index].getName());
        }
        return text.append(')').toString();
    }

    static String stableIdForTest(Executable executable, int slot) {
        return stableId(executableIdentity(executable), slot);
    }

    private static String stableId(String executable, int slot) {
        return HOOK_ID_PREFIX + executable + ":slot:" + slot;
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T sneakyThrow(Throwable throwable) throws E {
        throw (E) throwable;
    }

    interface Proceeding {
        Object proceed(Object[] args) throws Throwable;
    }

    public static final class HookIdentity {
        public final String executable;
        public final String id;

        public HookIdentity(String executable, String id) {
            this.executable = executable;
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof HookIdentity)) return false;
            HookIdentity identity = (HookIdentity) other;
            return executable.equals(identity.executable) && id.equals(identity.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(executable, id);
        }
    }

    public static final class RegistrationFailure {
        public final HookIdentity identity;
        public final String message;

        public RegistrationFailure(HookIdentity identity, String message) {
            this.identity = identity;
            this.message = message;
        }
    }

    public static final class RegistrationReport {
        public final Set<HookIdentity> attempted;
        public final Map<HookIdentity, XposedInterface.HookHandle> successful;
        public final List<RegistrationFailure> failures;
        public final boolean completed;
        public final boolean groupsComplete;

        public RegistrationReport(
                Set<HookIdentity> attempted,
                Map<HookIdentity, XposedInterface.HookHandle> successful,
                List<RegistrationFailure> failures,
                boolean completed
        ) {
            this(attempted, successful, failures, completed, true);
        }

        public RegistrationReport(
                Set<HookIdentity> attempted,
                Map<HookIdentity, XposedInterface.HookHandle> successful,
                List<RegistrationFailure> failures,
                boolean completed,
                boolean groupsComplete
        ) {
            this.attempted = Collections.unmodifiableSet(new LinkedHashSet<>(attempted));
            this.successful = Collections.unmodifiableMap(new LinkedHashMap<>(successful));
            this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
            this.completed = completed;
            this.groupsComplete = groupsComplete;
        }
    }

    public static final class OldHookSnapshot {
        public final XposedInterface.HookHandle handle;
        public final String executable;
        public final String id;

        public OldHookSnapshot(
                XposedInterface.HookHandle handle,
                String executable,
                String id
        ) {
            this.handle = handle;
            this.executable = executable;
            this.id = id;
        }
    }

    public static final class OldHookReconcileReport {
        public final int atomicallyReplaced;
        public final int legacyRemoved;
        public final int staleRemoved;
        public final int retainedAfterFailure;

        OldHookReconcileReport(
                int atomicallyReplaced,
                int legacyRemoved,
                int staleRemoved,
                int retainedAfterFailure
        ) {
            this.atomicallyReplaced = atomicallyReplaced;
            this.legacyRemoved = legacyRemoved;
            this.staleRemoved = staleRemoved;
            this.retainedAfterFailure = retainedAfterFailure;
        }
    }

    private static final class MutableRegistration {
        final Set<HookIdentity> attempted = new LinkedHashSet<>();
        final Map<HookIdentity, XposedInterface.HookHandle> successful = new LinkedHashMap<>();
        final List<RegistrationFailure> failures = new ArrayList<>();
        final Map<String, Integer> nextSlotByExecutable = new LinkedHashMap<>();
        final Map<HookIdentity, XposedInterface.HookHandle> oldStableHooks = new LinkedHashMap<>();
        final Set<String> startedGroups = new LinkedHashSet<>();
        final Set<String> completedGroups = new LinkedHashSet<>();
        final Set<String> incompleteGroups = new LinkedHashSet<>();
        String currentGroup;
        int optionalLookupDepth;

        MutableRegistration(List<OldHookSnapshot> oldHooks) {
            for (OldHookSnapshot snapshot : oldHooks) {
                if (snapshot.id != null) {
                    oldStableHooks.put(
                        new HookIdentity(snapshot.executable, snapshot.id),
                        snapshot.handle
                    );
                }
            }
        }

        HookIdentity nextIdentity(String executable) {
            int slot = nextSlotByExecutable.containsKey(executable)
                ? nextSlotByExecutable.get(executable)
                : 0;
            nextSlotByExecutable.put(executable, slot + 1);
            return new HookIdentity(executable, stableId(executable, slot));
        }

        void beginGroup(String name) {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Registration group name is required");
            }
            if (currentGroup != null) {
                markCurrentGroupIncomplete("nested registration group: " + name, null);
                throw new IllegalStateException("Registration group already active: " + currentGroup);
            }
            currentGroup = name;
            startedGroups.add(name);
        }

        void completeGroup(String name) {
            if (!Objects.equals(currentGroup, name)) {
                markCurrentGroupIncomplete("registration group mismatch: " + name, null);
                currentGroup = null;
                return;
            }
            if (!incompleteGroups.contains(name)) completedGroups.add(name);
            currentGroup = null;
        }

        void markCurrentGroupIncomplete(String detail, Throwable error) {
            String group = currentGroup;
            if (group == null || !incompleteGroups.add(group)) return;
            String message = detail == null || detail.isEmpty()
                ? "registration group incomplete"
                : detail;
            if (error != null) {
                String errorText = error.getMessage();
                message += ": " + error.getClass().getSimpleName()
                    + (errorText == null || errorText.isEmpty() ? "" : ": " + errorText);
            }
            failures.add(new RegistrationFailure(
                new HookIdentity("registration-group:" + group, "reweibo:group:" + group),
                message
            ));
        }

        void finishOpenGroup() {
            if (currentGroup == null) return;
            markCurrentGroupIncomplete("registration group did not complete", null);
            currentGroup = null;
        }

        boolean allGroupsCompleted() {
            return incompleteGroups.isEmpty()
                && completedGroups.containsAll(startedGroups)
                && startedGroups.containsAll(completedGroups);
        }
    }
}
