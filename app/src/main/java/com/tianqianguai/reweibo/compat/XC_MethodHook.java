package com.tianqianguai.reweibo.compat;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.Map;

/** Legacy-style before/after callback surface backed exclusively by libxposed API 102. */
public abstract class XC_MethodHook {
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}

    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    final void callBefore(MethodHookParam param) throws Throwable {
        beforeHookedMethod(param);
    }

    final void callAfter(MethodHookParam param) throws Throwable {
        afterHookedMethod(param);
    }

    public static final class MethodHookParam {
        public final Executable method;
        public Object thisObject;
        public Object[] args;

        private Object result;
        private Throwable throwable;
        private boolean returnEarly;
        private Map<String, Object> extras;

        MethodHookParam(
                Executable method,
                Object thisObject,
                Object[] args
        ) {
            this.method = method;
            this.thisObject = thisObject;
            this.args = args;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object value) {
            result = value;
            throwable = null;
            returnEarly = true;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public void setThrowable(Throwable value) {
            throwable = value;
            if (value != null) result = null;
            returnEarly = true;
        }

        public boolean hasThrowable() {
            return throwable != null;
        }

        public void setObjectExtra(String key, Object value) {
            if (extras == null) extras = new HashMap<>();
            extras.put(key, value);
        }

        public Object getObjectExtra(String key) {
            return extras == null ? null : extras.get(key);
        }

        boolean isReturnEarly() {
            return returnEarly;
        }

        void setResultFromChain(Object value) {
            result = value;
            throwable = null;
            returnEarly = false;
        }

        void setThrowableFromChain(Throwable value) {
            result = null;
            throwable = value;
            returnEarly = false;
        }

        Snapshot snapshot() {
            return new Snapshot(result, throwable, returnEarly);
        }

        void restore(Snapshot snapshot) {
            result = snapshot.result;
            throwable = snapshot.throwable;
            returnEarly = snapshot.returnEarly;
        }

        static final class Snapshot {
            final Object result;
            final Throwable throwable;
            final boolean returnEarly;

            Snapshot(Object result, Throwable throwable, boolean returnEarly) {
                this.result = result;
                this.throwable = throwable;
                this.returnEarly = returnEarly;
            }
        }
    }
}
