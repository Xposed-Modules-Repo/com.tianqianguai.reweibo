package com.tianqianguai.reweibo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Process;

/** The provider is shared with the scoped host and the shell, not arbitrary apps. */
final class IpcAccess {
    static final String COMMAND_PERMISSION = "com.tianqianguai.reweibo.permission.COMMAND";
    private IpcAccess() {}

    static boolean isTrustedUid(int caller, int module, int host) {
        return caller == 0 || caller == 2000 || caller == module || host >= 0 && caller == host;
    }

    static void enforce(Context context) {
        int host = -1;
        if (context != null) {
            try {
                host = context.getPackageManager().getApplicationInfo("com.weico.international", 0).uid;
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        if (!isTrustedUid(Binder.getCallingUid(), Process.myUid(), host)) {
            throw new SecurityException("ReWeibo settings require the module, scoped host, or shell");
        }
    }
}
