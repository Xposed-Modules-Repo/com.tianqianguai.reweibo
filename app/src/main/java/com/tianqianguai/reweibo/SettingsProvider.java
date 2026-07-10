package com.tianqianguai.reweibo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

public class SettingsProvider extends ContentProvider {
    private static final String COLUMN_KEY = "key";
    private static final String COLUMN_ENABLED = "enabled";
    private static final String COLUMN_VALUE = "value";

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
        MatrixCursor cursor = new MatrixCursor(new String[] { COLUMN_KEY, COLUMN_ENABLED, COLUMN_VALUE });
        String key = uri.getLastPathSegment();
        if (key == null || ModuleSettings.SETTINGS_PATH.equals(key)) {
            key = ModuleSettings.KEY_WEICO_PROFILE_ENTRY;
        }
        if (ModuleSettings.KEY_WEICO_TIMELINE_CACHE_DAYS.equals(key)) {
            int days = getPrefs().getInt(key, ModuleSettings.defaultIntFor(key));
            days = ModuleSettings.clampTimelineCacheDays(days);
            cursor.addRow(new Object[] { key, days > 0 ? 1 : 0, days });
            return cursor;
        }

        boolean enabled = getPrefs().getBoolean(key, ModuleSettings.defaultFor(key));
        cursor.addRow(new Object[] { key, enabled ? 1 : 0, enabled ? 1 : 0 });
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
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private SharedPreferences getPrefs() {
        return getContext().getSharedPreferences(ModuleSettings.PREFS_NAME, Context.MODE_PRIVATE);
    }
}
