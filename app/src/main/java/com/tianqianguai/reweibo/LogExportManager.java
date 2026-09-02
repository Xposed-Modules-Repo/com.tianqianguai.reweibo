package com.tianqianguai.reweibo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Writes filtered logs without loading the full file into memory. */
final class LogExportManager {
    static final class ExportResult {
        final String location;
        final String uri;
        final LogFileTools.Result log;

        ExportResult(String location, String uri, LogFileTools.Result log) {
            this.location = location;
            this.uri = uri;
            this.log = log;
        }
    }

    private LogExportManager() {}

    static ExportResult exportForUser(
            Context context,
            LogFileTools.Snapshot source,
            LogFileTools.Range range
    ) throws IOException {
        if (Build.VERSION.SDK_INT >= 29) {
            return exportToDownloads(context, source, range);
        }
        return exportToExternalFiles(context, source, range, "user");
    }

    static ExportResult exportForCli(
            Context context,
            LogFileTools.Snapshot source,
            LogFileTools.Range range
    ) throws IOException {
        return exportToExternalFiles(context, source, range, "cli");
    }

    private static ExportResult exportToDownloads(
            Context context,
            LogFileTools.Snapshot source,
            LogFileTools.Range range
    ) throws IOException {
        if (context == null) throw new IOException("context is unavailable");
        String fileName = newFileName();
        String relativePath = Environment.DIRECTORY_DOWNLOADS + "/ReWeibo";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        values.put(MediaStore.MediaColumns.IS_PENDING, 1);

        ContentResolver resolver = context.getContentResolver();
        Uri collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri destination = resolver.insert(collection, values);
        if (destination == null) throw new IOException("cannot create Downloads export");
        try {
            OutputStream output = resolver.openOutputStream(destination, "w");
            if (output == null) throw new IOException("cannot open Downloads export");
            LogFileTools.Result result = LogFileTools.writeFiltered(source, range, output);
            ContentValues completed = new ContentValues();
            completed.put(MediaStore.MediaColumns.IS_PENDING, 0);
            if (resolver.update(destination, completed, null, null) <= 0) {
                throw new IOException("cannot finalize Downloads export");
            }
            return new ExportResult(relativePath + "/" + fileName, destination.toString(), result);
        } catch (Throwable error) {
            resolver.delete(destination, null, null);
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException(error.getMessage(), error);
        }
    }

    private static ExportResult exportToExternalFiles(
            Context context,
            LogFileTools.Snapshot source,
            LogFileTools.Range range,
            String channel
    ) throws IOException {
        if (context == null) throw new IOException("context is unavailable");
        File root = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (root == null) throw new IOException("external files directory is unavailable");
        File directory = new File(root, "ReWeibo/exports/" + channel);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("cannot create export directory: " + directory.getAbsolutePath());
        }
        File destination = new File(directory, newFileName());
        File temporary = new File(directory, destination.getName() + ".tmp");
        if (temporary.exists() && !temporary.delete()) {
            throw new IOException("cannot remove stale temporary export");
        }
        try {
            OutputStream output = new FileOutputStream(temporary, false);
            LogFileTools.Result result = LogFileTools.writeFiltered(source, range, output);
            if (!temporary.renameTo(destination)) {
                throw new IOException("cannot publish completed export");
            }
            return new ExportResult(
                destination.getAbsolutePath(),
                Uri.fromFile(destination).toString(),
                result
            );
        } catch (Throwable error) {
            temporary.delete();
            if (error instanceof IOException) throw (IOException) error;
            throw new IOException(error.getMessage(), error);
        }
    }

    private static String newFileName() {
        String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US)
            .format(new Date());
        return "ReWeibo-log-" + timestamp + ".txt";
    }
}
