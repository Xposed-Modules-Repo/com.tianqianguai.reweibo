package com.tianqianguai.reweibo;

import android.app.Application;
import android.content.Context;

import com.tianqianguai.reweibo.compat.XC_LoadPackage;
import com.tianqianguai.reweibo.compat.XposedBridge;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

/** The sole Modern API 102 entry point. Only Weibo Lite is in scope. */
public final class MainHook extends XposedModule {
    private static final String TARGET_PACKAGE = "com.weico.international";

    private String loadedProcessName = "";
    private boolean initialHooksInstalled = false;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        XposedBridge.attach(this);
        loadedProcessName = param.getProcessName();
        XposedBridge.log(
            "ReWeibo: API 102 module loaded process=" + loadedProcessName
                + " framework=" + XposedBridge.frameworkName()
                + " version=" + XposedBridge.frameworkVersion()
                + " api=" + XposedBridge.apiVersion()
        );
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        if (!TARGET_PACKAGE.equals(param.getPackageName())) return;
        if (initialHooksInstalled) return;
        initialHooksInstalled = true;
        WeiboLiteHook.initializeHotReloadRuntime(1L, false);
        XposedBridge.RegistrationReport report = registerHooks(
            param.getPackageName(),
            param.getClassLoader(),
            Collections.emptyList()
        );
        WeiboLiteHook.markHotReloadReady(report.completed);
        logRegistrationReport("initial", report);
    }

    @Override
    public boolean onHotReloading(XposedModuleInterface.HotReloadingParam param) {
        Object savedState = WeiboLiteHook.captureHotReloadState();
        if (savedState == null) {
            XposedBridge.log(
                "ReWeibo: hot reload rejected blocker=" + WeiboLiteHook.hotReloadBlocker()
            );
            return false;
        }
        if (!HotReloadState.isValid(savedState, MainHook.class.getClassLoader())) {
            XposedBridge.log("ReWeibo: hot reload rejected: saved state is not classloader-neutral");
            return false;
        }
        if (!WeiboLiteHook.prepareForHotReload(savedState)) {
            XposedBridge.log(
                "ReWeibo: hot reload rejected during quiesce blocker="
                    + WeiboLiteHook.hotReloadBlocker()
            );
            return false;
        }
        param.setSavedInstanceState(savedState);
        XposedBridge.log(
            "ReWeibo: old generation quiesced process=" + loadedProcessName
                + " generation=" + HotReloadState.previousGeneration(savedState)
        );
        return true;
    }

    @Override
    public void onHotReloaded(XposedModuleInterface.HotReloadedParam param) {
        XposedBridge.attach(this);
        loadedProcessName = param.getProcessName();

        List<XposedBridge.OldHookSnapshot> oldHooks = XposedBridge.snapshotOldHooks(
            param.getOldHookHandles()
        );
        Object savedState = param.getSavedInstanceState();
        long generation = Math.max(2L, HotReloadState.previousGeneration(savedState) + 1L);
        ClassLoader targetClassLoader = resolveTargetClassLoader(savedState, param.getOldHookHandles());
        if (targetClassLoader == null) {
            XposedBridge.log("ReWeibo: hot reload failed: target classloader unavailable");
            return;
        }

        boolean restored = WeiboLiteHook.restoreHotReloadState(savedState, generation);
        XposedBridge.RegistrationReport report = registerHooks(
            TARGET_PACKAGE,
            targetClassLoader,
            oldHooks
        );
        XposedBridge.OldHookReconcileReport reconciliation =
            XposedBridge.reconcileOldHooks(oldHooks, report);
        WeiboLiteHook.markHotReloadReady(report.completed && restored);
        WeiboLiteHook.finishHotReloadRestore();
        logRegistrationReport("reload", report);
        XposedBridge.log(
            "ReWeibo: hot reload generation=" + generation
                + " restored=" + restored
                + " oldHooks=" + oldHooks.size()
                + " atomicallyReplaced=" + reconciliation.atomicallyReplaced
                + " legacyRemoved=" + reconciliation.legacyRemoved
                + " staleRemoved=" + reconciliation.staleRemoved
                + " retainedAfterFailure=" + reconciliation.retainedAfterFailure
        );
    }

    private XposedBridge.RegistrationReport registerHooks(
            String packageName,
            ClassLoader classLoader,
            List<XposedBridge.OldHookSnapshot> oldHooks
    ) {
        XposedBridge.beginRegistration(oldHooks);
        boolean completed = false;
        try {
            WeiboLiteHook.hook(new XC_LoadPackage.LoadPackageParam(packageName, classLoader));
            completed = true;
        } catch (Throwable error) {
            XposedBridge.log(error);
        }
        return XposedBridge.finishRegistration(completed);
    }

    private void logRegistrationReport(
            String phase,
            XposedBridge.RegistrationReport report
    ) {
        XposedBridge.log(
            "ReWeibo: hook registration phase=" + phase
                + " attempted=" + report.attempted.size()
                + " successful=" + report.successful.size()
                + " failures=" + report.failures.size()
                + " groupsComplete=" + report.groupsComplete
                + " completed=" + report.completed
        );
        int limit = Math.min(10, report.failures.size());
        for (int index = 0; index < limit; index++) {
            XposedBridge.RegistrationFailure failure = report.failures.get(index);
            XposedBridge.log(
                "ReWeibo: hook registration failed executable="
                    + failure.identity.executable
                    + " id=" + failure.identity.id
                    + " error=" + failure.message
            );
        }
    }

    private ClassLoader resolveTargetClassLoader(
            Object savedState,
            List<XposedInterface.HookHandle> oldHandles
    ) {
        Application currentApplication = currentApplication();
        if (currentApplication != null && TARGET_PACKAGE.equals(currentApplication.getPackageName())) {
            return currentApplication.getClassLoader();
        }
        Object savedContext = HotReloadState.applicationContext(savedState);
        if (savedContext instanceof Context) {
            ClassLoader classLoader = ((Context) savedContext).getClassLoader();
            if (classLoader != null) return classLoader;
        }
        if (oldHandles != null) {
            for (XposedInterface.HookHandle handle : oldHandles) {
                try {
                    ClassLoader classLoader = handle.getExecutable()
                        .getDeclaringClass()
                        .getClassLoader();
                    if (classLoader != null) return classLoader;
                } catch (Throwable ignored) {}
            }
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader == MainHook.class.getClassLoader()
            ? null
            : contextClassLoader;
    }

    private Application currentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Method method = activityThread.getDeclaredMethod("currentApplication");
            method.setAccessible(true);
            Object value = method.invoke(null);
            return value instanceof Application ? (Application) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
