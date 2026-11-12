package com.tianqianguai.reweibo;

import java.util.Locale;

/** Explain observed failures without claiming that an application-hiding tool is installed. */
final class SettingsSyncFailure {
    static String explain(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SecurityException) {
                return "模块设置服务拒绝了访问。请确认微博轻享版与 ReWeibo 位于同一用户/应用分身空间，"
                    + "并检查应用隔离或权限规则，再重新保存。";
            }
            String message = cause.getMessage();
            if (message != null && (message.toLowerCase(Locale.ROOT).contains("unknown uri")
                    || message.toLowerCase(Locale.ROOT).contains("failed to find provider"))) {
                return "微博轻享版无法访问 ReWeibo 设置服务。若使用 HMA/应用隐藏，请仅将 ReWeibo "
                    + "设为对微博轻享版可见；同时确认模块已安装且与微博位于同一用户/分身空间。"
                    + "修正后重启微博并重新保存，无需关闭全部隐藏规则或清除数据。";
            }
        }
        return "模块设置服务写入失败，请确认 ReWeibo 已安装且可正常打开，再重试保存。"
            + "若仍失败，请附上发生时间与日志。错误类型："
            + (error == null ? "服务未确认写入" : error.getClass().getSimpleName());
    }

    static String message(String key, Throwable error) {
        return "未能同步设置：" + ModuleSettings.titleFor(key) + "（" + key + "）。\n\n"
            + explain(error) + "\n\n本次选择已写入微博本地副本，但模块端可能仅部分更新，尚未完成同步。"
            + "请保留当前选择并重试；不要将此提示视为保存成功。";
    }
}
