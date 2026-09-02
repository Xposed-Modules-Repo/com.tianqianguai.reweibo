package com.tianqianguai.reweibo.compat;

/** Replacement callback with the same early-return contract used by the existing hooks. */
public abstract class XC_MethodReplacement extends XC_MethodHook {
    @Override
    protected final void beforeHookedMethod(MethodHookParam param) {
        try {
            param.setResult(replaceHookedMethod(param));
        } catch (Throwable throwable) {
            param.setThrowable(throwable);
        }
    }

    @Override
    protected final void afterHookedMethod(MethodHookParam param) {}

    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    public static XC_MethodReplacement returnConstant(final Object value) {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return value;
            }
        };
    }
}
