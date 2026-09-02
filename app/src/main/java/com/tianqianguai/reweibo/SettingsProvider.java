package com.tianqianguai.reweibo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SettingsProvider extends ContentProvider {
    private static final String COLUMN_KEY = "key";
    private static final String COLUMN_ENABLED = "enabled";
    private static final String COLUMN_VALUE = "value";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_DEFAULT = "default";
    private static final String COLUMN_IS_SET = "is_set";
    private static final String[] ITEM_COLUMNS = new String[] {
        COLUMN_KEY,
        COLUMN_ENABLED,
        COLUMN_VALUE,
        COLUMN_TYPE,
        COLUMN_DEFAULT,
        COLUMN_IS_SET
    };
    private static final String[] ALL_COLUMNS = new String[] {
        COLUMN_KEY,
        COLUMN_TYPE,
        COLUMN_ENABLED,
        COLUMN_VALUE,
        COLUMN_DEFAULT,
        COLUMN_IS_SET
    };
    private static final long COMMAND_TIMEOUT_SECONDS = 5L;
    private static final String NO_RECEIVER_RESULT = "status=error\nmessage=target process is not ready";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        if (isAllSettingsUri(uri)) {
            MatrixCursor cursor = new MatrixCursor(ALL_COLUMNS);
            for (String key : ModuleSettings.allKeys()) {
                Object[] row = settingRow(key);
                cursor.addRow(new Object[] { row[0], row[3], row[1], row[2], row[4], row[5] });
            }
            return cursor;
        }

        // Keep the legacy base-URI behavior: /settings resolves to profile-entry.
        MatrixCursor cursor = new MatrixCursor(ITEM_COLUMNS);
        String key = uri == null ? null : uri.getLastPathSegment();
        if (key == null || ModuleSettings.SETTINGS_PATH.equals(key)) {
            key = ModuleSettings.KEY_WEICO_PROFILE_ENTRY;
        }
        cursor.addRow(settingRow(key));
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String key = requireExactSettingKey(uri);
        boolean committed = getPrefs().edit().remove(key).commit();
        if (!committed) throw new IllegalStateException("failed to reset setting: " + key);
        notifySettingChanged(key);
        return 1;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        String key = requireExactSettingKey(uri);
        if (values == null) throw new IllegalArgumentException("missing values for setting: " + key);
        Object rawValue = values.containsKey(COLUMN_VALUE)
            ? values.get(COLUMN_VALUE)
            : values.get(COLUMN_ENABLED);
        writeSetting(key, rawValue);
        return 1;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        try {
            if ("help".equals(method)) {
                return success("help", "CLI contract", buildHelp());
            }
            if ("settings.list".equals(method)) {
                return success("settings.list", "settings listed", buildSettingsList());
            }
            if ("settings.get".equals(method)) {
                String key = requireKnownKey(arg);
                return success("settings.get", "setting read", formatSetting(key));
            }
            if ("settings.set".equals(method)) {
                String key = requireKnownKey(arg);
                Object value = bundleValue(extras, COLUMN_VALUE, COLUMN_ENABLED);
                writeSetting(key, value);
                return success(
                    "settings.set",
                    "setting updated; use weico.settings.reload to refresh a running target",
                    formatSetting(key)
                );
            }
            if ("settings.reset".equals(method)) {
                String key = requireKnownKey(arg);
                boolean committed = getPrefs().edit().remove(key).commit();
                if (!committed) throw new IllegalStateException("failed to reset setting: " + key);
                notifySettingChanged(key);
                return success(
                    "settings.reset",
                    "setting reset; use weico.settings.reload to refresh a running target",
                    formatSetting(key)
                );
            }
            if ("exec".equals(method)) {
                return executeCommand(arg, extras);
            }
            return failure("unknown_method", "unknown method: " + String.valueOf(method));
        } catch (IllegalArgumentException e) {
            return failure("invalid_argument", e.getMessage());
        } catch (Throwable t) {
            String message = t.getMessage();
            return failure(
                "internal_error",
                t.getClass().getSimpleName()
                    + (message == null || message.isEmpty() ? "" : ": " + message)
            );
        }
    }

    private Bundle executeCommand(String fullName, Bundle extras) throws InterruptedException {
        CliContract.Command command = CliContract.commandFor(fullName);
        if (command == null) {
            return failure("unknown_command", "unknown command: " + String.valueOf(fullName));
        }
        Context context = getContext();
        if (context == null) return failure("provider_unavailable", "provider context is unavailable");

        Intent intent = new Intent(CliContract.ACTION_COMMAND);
        intent.setPackage(command.targetPackage);
        if (extras != null) intent.putExtras(new Bundle(extras));
        intent.putExtra(CliContract.EXTRA_COMMAND, command.localName);
        intent.putExtra(CliContract.EXTRA_FULL_COMMAND, command.fullName);

        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<String> resultData = new AtomicReference<>(NO_RECEIVER_RESULT);
        AtomicReference<Bundle> resultExtras = new AtomicReference<>();
        BroadcastReceiver finalReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent resultIntent) {
                String data = getResultData();
                if (data != null && !data.isEmpty()) resultData.set(data);
                Bundle received = getResultExtras(false);
                if (received != null) resultExtras.set(new Bundle(received));
                completed.countDown();
            }
        };

        context.sendOrderedBroadcast(
            intent,
            null,
            finalReceiver,
            new android.os.Handler(Looper.getMainLooper()),
            Activity.RESULT_CANCELED,
            NO_RECEIVER_RESULT,
            null
        );
        if (!completed.await(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            return failure("timeout", "target did not answer within " + COMMAND_TIMEOUT_SECONDS + " seconds");
        }

        Bundle received = resultExtras.get();
        if (received == null) {
            return failure("target_not_ready", resultData.get());
        }
        String status = received.getString("status", CliCommandBridge.Result.STATUS_ERROR);
        String message = received.getString("message", "");
        Bundle response = new Bundle(received);
        response.putBoolean("ok", !CliCommandBridge.Result.STATUS_ERROR.equals(status));
        response.putString("code", command.fullName);
        response.putString("message", message);
        response.putString("result", resultData.get());
        return response;
    }

    private Object[] settingRow(String key) {
        SharedPreferences prefs = getPrefs();
        boolean isSet = prefs.contains(key);
        if (ModuleSettings.isIntegerKey(key)) {
            int defaultValue = ModuleSettings.defaultIntFor(key);
            int value = readIntCompat(prefs, key, defaultValue);
            value = ModuleSettings.clampTimelineCacheDays(value);
            return new Object[] {
                key, value > 0 ? 1 : 0, value, "integer", defaultValue, isSet ? 1 : 0
            };
        }

        // Unknown keys retain the legacy query behavior (boolean false by default).
        boolean defaultValue = ModuleSettings.defaultFor(key);
        boolean value = readBooleanCompat(prefs, key, defaultValue);
        return new Object[] {
            key, value ? 1 : 0, value ? 1 : 0, "boolean", defaultValue ? 1 : 0, isSet ? 1 : 0
        };
    }

    private String formatSetting(String key) {
        Object[] row = settingRow(key);
        return "key=" + row[0]
            + ";type=" + row[3]
            + ";enabled=" + (((Number) row[1]).intValue() != 0)
            + ";value=" + row[2]
            + ";default=" + row[4]
            + ";is_set=" + (((Number) row[5]).intValue() != 0);
    }

    private String buildSettingsList() {
        StringBuilder output = new StringBuilder();
        for (String key : ModuleSettings.allKeys()) {
            if (output.length() > 0) output.append('\n');
            output.append(formatSetting(key));
        }
        return output.toString();
    }

    private String buildHelp() {
        StringBuilder output = new StringBuilder();
        output.append("methods=help,settings.list,settings.get,settings.set,settings.reset,exec");
        output.append("\nsettings=");
        appendCsv(output, ModuleSettings.allKeys());
        output.append("\ncommands=");
        appendCsv(output, CliContract.allCommandNames());
        output.append("\nsettings.set: arg=<key>, extras value=<true|false|1|0|1..30>");
        output.append("\nexec: arg=<full command>, extras=<command arguments>");
        return output.toString();
    }

    private static void appendCsv(StringBuilder output, String[] values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) output.append(',');
            output.append(values[i]);
        }
    }

    private void writeSetting(String key, Object rawValue) {
        requireKnownKey(key);
        if (rawValue == null) throw new IllegalArgumentException("missing value for setting: " + key);
        SharedPreferences.Editor editor = getPrefs().edit();
        if (ModuleSettings.isBooleanKey(key)) {
            editor.putBoolean(key, parseBoolean(rawValue));
        } else if (ModuleSettings.isIntegerKey(key)) {
            editor.putInt(key, parseCacheDays(rawValue));
        } else {
            throw new IllegalArgumentException("unsupported setting: " + key);
        }
        if (!editor.commit()) throw new IllegalStateException("failed to update setting: " + key);
        notifySettingChanged(key);
    }

    private void notifySettingChanged(String key) {
        Context context = getContext();
        if (context == null) return;
        context.getContentResolver().notifyChange(ModuleSettings.settingsUriFor(key), null);
        context.getContentResolver().notifyChange(
            Uri.parse("content://" + ModuleSettings.PROVIDER_AUTHORITY + "/settings/all"),
            null
        );
    }

    private static boolean parseBoolean(Object rawValue) {
        String value = String.valueOf(rawValue).trim();
        if ("true".equals(value) || "1".equals(value)) return true;
        if ("false".equals(value) || "0".equals(value)) return false;
        throw new IllegalArgumentException("boolean value must be true, false, 1, or 0");
    }

    private static int parseCacheDays(Object rawValue) {
        final int value;
        try {
            value = Integer.parseInt(String.valueOf(rawValue).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("cache days must be an integer from 1 to 30");
        }
        if (value < ModuleSettings.MIN_WEICO_TIMELINE_CACHE_DAYS
                || value > ModuleSettings.MAX_WEICO_TIMELINE_CACHE_DAYS) {
            throw new IllegalArgumentException("cache days must be from 1 to 30");
        }
        return value;
    }

    private static Object bundleValue(Bundle extras, String primary, String fallback) {
        if (extras == null) return null;
        if (extras.containsKey(primary)) return extras.get(primary);
        return extras.get(fallback);
    }

    private static String requireKnownKey(String key) {
        if (!ModuleSettings.isKnownKey(key)) {
            throw new IllegalArgumentException("unknown setting: " + String.valueOf(key));
        }
        return key;
    }

    private static String requireExactSettingKey(Uri uri) {
        if (uri == null) throw new IllegalArgumentException("missing setting URI");
        List<String> segments = uri.getPathSegments();
        if (segments.size() != 2 || !ModuleSettings.SETTINGS_PATH.equals(segments.get(0))) {
            throw new IllegalArgumentException("expected URI /settings/<key>");
        }
        return requireKnownKey(segments.get(1));
    }

    private static boolean isAllSettingsUri(Uri uri) {
        if (uri == null) return false;
        List<String> segments = uri.getPathSegments();
        return segments.size() == 2
            && ModuleSettings.SETTINGS_PATH.equals(segments.get(0))
            && "all".equals(segments.get(1));
    }

    private static boolean readBooleanCompat(
            SharedPreferences prefs,
            String key,
            boolean defaultValue
    ) {
        Object raw = prefs.getAll().get(key);
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        if ("true".equals(raw) || "1".equals(raw)) return true;
        if ("false".equals(raw) || "0".equals(raw)) return false;
        return defaultValue;
    }

    private static int readIntCompat(SharedPreferences prefs, String key, int defaultValue) {
        Object raw = prefs.getAll().get(key);
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof String) {
            try {
                return Integer.parseInt(((String) raw).trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private SharedPreferences getPrefs() {
        Context context = getContext();
        if (context == null) throw new IllegalStateException("provider context is unavailable");
        return context.getSharedPreferences(ModuleSettings.PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static Bundle success(String code, String message, String result) {
        Bundle response = new Bundle();
        response.putBoolean("ok", true);
        response.putString("status", CliCommandBridge.Result.STATUS_READY);
        response.putString("code", code);
        response.putString("message", message);
        response.putString("result", result);
        return response;
    }

    private static Bundle failure(String code, String message) {
        String safeMessage = message == null ? "" : message;
        Bundle response = new Bundle();
        response.putBoolean("ok", false);
        response.putString("status", CliCommandBridge.Result.STATUS_ERROR);
        response.putString("code", code);
        response.putString("message", safeMessage);
        response.putString("result", "status=error\nmessage=" + safeMessage.replace("\n", "\\n"));
        return response;
    }
}
