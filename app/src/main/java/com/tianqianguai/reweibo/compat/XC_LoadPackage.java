package com.tianqianguai.reweibo.compat;

/** Minimal package-ready parameter retained for the existing hook implementations. */
public final class XC_LoadPackage {
    private XC_LoadPackage() {}

    public static final class LoadPackageParam {
        public final String packageName;
        public final ClassLoader classLoader;

        public LoadPackageParam(String packageName, ClassLoader classLoader) {
            this.packageName = packageName;
            this.classLoader = classLoader;
        }
    }
}
