package com.tianqianguai.reweibo;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CliCommandBridge {
    private static BroadcastReceiver sReceiver;
    private static String sRegisteredPackage;

    private CliCommandBridge() {}

    public interface Handler {
        Result handle(String command, Bundle args);
    }

    /**
     * Registers the in-process endpoint used by SettingsProvider's ordered broadcasts.
     * Only the target application's main process registers a receiver.
     */
    public static synchronized boolean register(
            Context context,
            String targetPackage,
            Handler handler
    ) {
        if (context == null || targetPackage == null || handler == null) return false;
        Context appContext = context.getApplicationContext();
        if (appContext == null) appContext = context;
        if (!targetPackage.equals(appContext.getPackageName())) return false;
        if (!targetPackage.equals(Application.getProcessName())) return false;
        if (sReceiver != null) return targetPackage.equals(sRegisteredPackage);

        final Handler commandHandler = handler;
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                if (intent == null || !CliContract.ACTION_COMMAND.equals(intent.getAction())) {
                    finish(this, Result.error("invalid command broadcast"));
                    return;
                }
                String command = intent.getStringExtra(CliContract.EXTRA_COMMAND);
                if (command == null || command.trim().isEmpty()) {
                    finish(this, Result.error("missing command"));
                    return;
                }
                Bundle args = intent.getExtras();
                if (args == null) {
                    args = new Bundle();
                } else {
                    args = new Bundle(args);
                    args.remove(CliContract.EXTRA_COMMAND);
                    args.remove(CliContract.EXTRA_FULL_COMMAND);
                }
                try {
                    Result result = commandHandler.handle(command, args);
                    finish(this, result == null ? Result.error("handler returned no result") : result);
                } catch (Throwable t) {
                    String message = t.getMessage();
                    finish(this, Result.error(
                        t.getClass().getSimpleName()
                            + (message == null || message.isEmpty() ? "" : ": " + message)
                    ));
                }
            }
        };

        IntentFilter filter = new IntentFilter(CliContract.ACTION_COMMAND);
        if (Build.VERSION.SDK_INT >= 33) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            appContext.registerReceiver(receiver, filter);
        }
        sReceiver = receiver;
        sRegisteredPackage = targetPackage;
        return true;
    }

    private static void finish(BroadcastReceiver receiver, Result result) {
        if (!receiver.isOrderedBroadcast()) return;
        receiver.setResultCode(result.isError() ? Activity.RESULT_CANCELED : Activity.RESULT_OK);
        receiver.setResultData(result.encode());
        receiver.setResultExtras(result.toBundle());
    }

    public static final class Result {
        public static final String STATUS_READY = "ready";
        public static final String STATUS_ACCEPTED = "accepted";
        public static final String STATUS_ERROR = "error";

        private final String status;
        private final String message;
        private final LinkedHashMap<String, String> details = new LinkedHashMap<>();

        private Result(String status, String message) {
            this.status = status;
            this.message = message == null ? "" : message;
        }

        public static Result ready(String message) {
            return new Result(STATUS_READY, message);
        }

        public static Result accepted(String message) {
            return new Result(STATUS_ACCEPTED, message);
        }

        public static Result error(String message) {
            return new Result(STATUS_ERROR, message);
        }

        public Result with(String key, Object value) {
            if (key != null && !key.isEmpty() && value != null) {
                details.put(key, String.valueOf(value));
            }
            return this;
        }

        public String status() {
            return status;
        }

        public String message() {
            return message;
        }

        public boolean isError() {
            return STATUS_ERROR.equals(status);
        }

        public String encode() {
            StringBuilder output = new StringBuilder();
            appendField(output, "status", status);
            appendField(output, "message", message);
            for (Map.Entry<String, String> detail : details.entrySet()) {
                appendField(output, detail.getKey(), detail.getValue());
            }
            return output.toString();
        }

        public Bundle toBundle() {
            Bundle bundle = new Bundle();
            bundle.putString("status", status);
            bundle.putString("message", message);
            bundle.putString("result", encode());
            for (Map.Entry<String, String> detail : details.entrySet()) {
                bundle.putString(detail.getKey(), detail.getValue());
            }
            return bundle;
        }

        private static void appendField(StringBuilder output, String key, String value) {
            if (output.length() > 0) output.append('\n');
            output.append(key).append('=').append(escape(value));
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        }
    }
}
