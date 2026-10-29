package com.tianqianguai.reweibo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/** A captured account owns its files even if the active account changes during I/O. */
final class TimelineAccountFiles {
    final String userId;
    final File nativeFile;
    final File filesDir;
    final File directory;
    volatile boolean migrated;
    volatile int shadowCount;

    TimelineAccountFiles(String userId, File nativeFile, File filesDir) {
        if (userId == null || !userId.matches("[0-9]+") || "0".equals(userId)
            || nativeFile == null) throw new IllegalArgumentException("unresolved timeline account");
        this.userId = userId;
        this.nativeFile = nativeFile;
        this.filesDir = filesDir;
        this.directory = new File(new File(filesDir, "reweibo_accounts"), userId);
    }

    File file(String name) { return new File(directory, name); }

    boolean canMigrateLegacyShadow(String shadowName, String metadataName) {
        return !file(shadowName).exists() && !file(metadataName).exists()
            && new File(filesDir, shadowName).isFile() && ownsLegacyShadow(metadataName);
    }

    boolean ownsLegacyShadow(String metadataName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filesDir, metadataName)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("file_name=")) {
                    return nativeFile.getName().equals(line.substring("file_name=".length()).trim());
                }
            }
        } catch (IOException ignored) {}
        return false;
    }
}
