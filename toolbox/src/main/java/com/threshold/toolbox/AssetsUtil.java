package com.threshold.toolbox;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threshold.jni.AssetsJni;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

@SuppressWarnings({"unused", "IOStreamConstructor"})
public class AssetsUtil {

    public enum CopyMode {
        COPY_IF_DEST_NOT_EXISTS,
        OVERRIDE_IF_DEST_SIZE_NOT_MATCHED,
        OVERRIDE_FORCED,
        OVERRIDE_IF_MODIFIED
    }

    private static final String TAG = "AssetsUtil";
    private static final int BUFFER_SIZE = 128 * 1024; // 128KB buffer
    private static final long MAX_CRC_VERIFY_SIZE = 10 * 1024 * 1024; // 10MB
    private static final AtomicLong sApkLastUpdateTime = new AtomicLong(-1);

    private AssetsUtil() {
        throw new IllegalStateException("no instance");
    }

    // ======================== Core Methods ========================
    public static long getAssetSize(final AssetManager assetManager, final String assetFilePath) {
        if (assetManager == null || assetFilePath == null || assetFilePath.isEmpty()) {
            Log.e(TAG, "Invalid params for getAssetSize");
            return -1;
        }

        final long[] receiveAssetSize = {-1};
        int ret = AssetsJni.getResSize(assetManager, assetFilePath, receiveAssetSize);
        if (0 != ret) {
            Log.e(TAG, "Failed to get asset size: " + assetFilePath + ", error=" + ret);
        }
        return receiveAssetSize[0];
    }

    public static boolean copyAssetFile(@NonNull Context context,
                                        @NonNull CopyMode copyMode,
                                        @NonNull String fromAssetFilePath,
                                        @NonNull String toFilePath) {
        return copyAssetFile(context, context.getAssets(), copyMode, fromAssetFilePath, toFilePath);
    }

    public static boolean copyAssetFile(@NonNull Context context,
                                        @NonNull AssetManager assetManager,
                                        @NonNull CopyMode copyMode,
                                        @NonNull String fromAssetFilePath,
                                        @NonNull String toFilePath) {
        if (fromAssetFilePath.isEmpty() || toFilePath.isEmpty()) {
            Log.e(TAG, "Empty paths not allowed");
            return false;
        }

        final File destFile = new File(toFilePath);
        try {
            // Check if we should skip copying
            if (shouldSkipCopy(context, assetManager, fromAssetFilePath, destFile, copyMode)) {
                return true;
            }

            // Ensure parent directories exist
            final File parentDir = destFile.getParentFile();
            if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                Log.e(TAG, "Failed to create parent directories: " + parentDir);
                return false;
            }

            // Perform file copy
            return copyAssetStream(assetManager, fromAssetFilePath, destFile,
                    getApkLastUpdateTime(context));
        } catch (IOException e) {
            Log.e(TAG, "Error copying asset: " + fromAssetFilePath + " to " + toFilePath, e);
            // Clean up incomplete file
            if (destFile.exists() && !destFile.delete()) {
                Log.w(TAG, "Failed to delete incomplete file: " + destFile.getPath());
            }
            return false;
        }
    }

    // ======================== Folder Operations ========================
    public static boolean copyAssetFolder(@NonNull Context context,
                                          @NonNull CopyMode copyMode,
                                          @NonNull String fromAssetFolderPath,
                                          @NonNull String destinationFolderPath) {
        return copyAssetFolder(context, context.getAssets(), copyMode,
                fromAssetFolderPath, destinationFolderPath);
    }

    public static boolean copyAssetFolder(@NonNull Context context,
                                          @NonNull AssetManager assetManager,
                                          @NonNull CopyMode copyMode,
                                          @NonNull String fromAssetFolderPath,
                                          @NonNull String destinationFolderPath) {
        if (fromAssetFolderPath.isEmpty() || destinationFolderPath.isEmpty()) {
            Log.e(TAG, "Empty paths not allowed");
            return false;
        }

        File destDir = new File(destinationFolderPath);
        // Check existing directory type
        if (destDir.exists() && !destDir.isDirectory()) {
            Log.e(TAG, "Destination exists but is not a directory: " + destinationFolderPath);
            return false;
        }

        try {
            // Create directory only if doesn't exist
            if (!destDir.exists() && !destDir.mkdirs()) {
                Log.e(TAG, "Failed to create destination directory: " + destinationFolderPath);
                return false;
            }

            String[] assets = assetManager.list(fromAssetFolderPath);
            if (assets == null || assets.length == 0) {
                Log.w(TAG, "Empty or invalid asset folder: " + fromAssetFolderPath);
                return true; // Treat as success
            }

            boolean allSucceeded = true;
            for (String assetName : assets) {
                String assetPath = fromAssetFolderPath + "/" + assetName;
                String destPath = new File(destDir, assetName).getPath();

                if (isAssetDirectory(assetManager, assetPath)) {
                    allSucceeded &= copyAssetFolder(context, assetManager, copyMode,
                            assetPath, destPath);
                } else {
                    allSucceeded &= copyAssetFile(context, assetManager, copyMode,
                            assetPath, destPath);
                }
            }
            return allSucceeded;
        } catch (IOException e) {
            Log.e(TAG, "Error copying asset folder: " + fromAssetFolderPath, e);
            return false;
        }
    }

    // ======================== Helper Methods ========================
    private static boolean shouldSkipCopy(Context context,
                                          AssetManager assetManager,
                                          String assetPath,
                                          File destFile,
                                          CopyMode mode) throws IOException {
        if (!destFile.exists()) {
            return false; // Need to copy
        }

        final long assetSize = getAssetSize(assetManager, assetPath);
        final long fileSize = getFileSize(destFile);

        switch (mode) {
            case COPY_IF_DEST_NOT_EXISTS:
                Log.v(TAG, "Skipping copy (file exists): " + destFile.getPath());
                return true;

            case OVERRIDE_IF_DEST_SIZE_NOT_MATCHED:
                if (assetSize == fileSize) {
                    Log.v(TAG, "Skipping copy (size matched): " + destFile.getPath());
                    return true;
                }
                Log.d(TAG, "Overwriting (size mismatch): " + destFile.getPath());
                break;

            case OVERRIDE_FORCED:
                Log.d(TAG, "Forced overwrite: " + destFile.getPath());
                break;

            case OVERRIDE_IF_MODIFIED:
                // 1. Size check (fastest)
                if (assetSize != fileSize) {
                    Log.d(TAG, "Overwriting (size mismatch): " + destFile.getPath());
                    break;
                }

                // 2. Modification time check
                final long assetModTime = getApkLastUpdateTime(context);
                final long fileModTime = getFileModificationTime(destFile);
                Log.v(TAG, destFile.getPath() + " : assetModTime = " + assetModTime
                        + ", fileModTime = " + fileModTime);

                // Case 1: Asset is newer -> overwrite
                if (assetModTime > fileModTime) {
                    Log.d(TAG, "Overwriting (asset newer): " + destFile.getPath());
                    break;
                }
                // Case 2: File is newer -> skip (assume user-modified)
                if (fileModTime > assetModTime) {
                    Log.v(TAG, "Skipping copy (file newer): " + destFile.getPath());
                    return true;
                }

                // Case 3: Times equal -> CRC check for small files
                if (shouldVerifyContent(assetSize)) {
                    if (!verifyContentMatch(assetManager, assetPath, destFile)) {
                        Log.d(TAG, "Overwriting (content changed): " + destFile.getPath());
                        break;
                    }
                } else {
                    Log.v(TAG, "Skipping CRC for large file: " + destFile.getPath());
                }

                Log.v(TAG, "Skipping copy (verified): " + destFile.getPath());
                return true;
        }
        return false; // Proceed with copy
    }

    private static boolean verifyContentMatch(AssetManager am,
                                              String assetPath, File file) throws IOException {
        final long assetCrc = calculateAssetCrc(am, assetPath);
        final long fileCrc = calculateFileCrc(file);
        return assetCrc == fileCrc;
    }

    private static boolean shouldVerifyContent(long fileSize) {
        return fileSize > 0 && fileSize < MAX_CRC_VERIFY_SIZE;
    }

    private static long getFileSize(File file) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.size(file.toPath());
        } else {
            return file.length();
        }
    }

    private static long getApkLastUpdateTime(Context context) {
        long lastUpdateTime = sApkLastUpdateTime.get();
        if (lastUpdateTime == -1) {
            if (context == null) {
                Log.w(TAG, "Null context, using current time");
                return System.currentTimeMillis();
            }
            synchronized (AssetsUtil.class) {
                lastUpdateTime = sApkLastUpdateTime.get();
                if (lastUpdateTime == -1) {
                    try {
                        final PackageInfo pi = context.getPackageManager()
                                .getPackageInfo(context.getPackageName(), 0);
                        lastUpdateTime = pi.lastUpdateTime;
                        sApkLastUpdateTime.set(lastUpdateTime);
                        Log.v(TAG, "sApkLastUpdateTime = " + lastUpdateTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Failed to get package info", e);
                        lastUpdateTime = System.currentTimeMillis();
                        sApkLastUpdateTime.set(lastUpdateTime);
                    }
                }
            }
        }
        return lastUpdateTime;
    }

    private static long getFileModificationTime(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(
                        file.toPath(), BasicFileAttributes.class);
                return attrs.lastModifiedTime().toMillis();
            } catch (IOException e) {
                Log.w(TAG, "Failed to get file mod time: " + file, e);
            }
        }
        return file.lastModified();
    }

    private static boolean copyAssetStream(AssetManager assetManager,
                                           String assetPath, File destFile,
                                           long apkUpdateTime) {
        boolean succeed = false;
        if (destFile.exists() && !destFile.delete()) {
            Log.w(TAG, "Failed to delete existing file: " + destFile);
        }

        try (InputStream in = assetManager.open(assetPath);
             OutputStream out = new BufferedOutputStream(
                     new FileOutputStream(destFile), BUFFER_SIZE)) {

            copyStream(in, out);
            Log.d(TAG, "Copied asset: " + assetPath + " → " + destFile.getPath());
            succeed = true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying asset stream: " + assetPath, e);
        }

        if (succeed && destFile.exists()) {
            // Set to APK update time +1s to ensure it's newer than original
            long newTime = apkUpdateTime + 1000;
            if (!destFile.setLastModified(newTime)) {
                Log.v(TAG, "Failed to set mod time: " + destFile.getPath());
            }
        }
        return succeed;
    }

    private static boolean isAssetDirectory(AssetManager am, String path) throws IOException {
        final String[] list = am.list(path);
        return list != null && list.length > 0;
    }

    // ======================== CRC Calculation ========================
    private static long calculateAssetCrc(AssetManager am, String assetPath) throws IOException {
        try (InputStream in = am.open(assetPath)) {
            return calculateCrc(in);
        }
    }

    private static long calculateFileCrc(File file) throws IOException {
        try (InputStream in = new BufferedInputStream(
                new FileInputStream(file), BUFFER_SIZE)) {
            return calculateCrc(in);
        }
    }

    private static long calculateCrc(InputStream in) throws IOException {
        final CRC32 crc = new CRC32();
        final byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;

        while ((bytesRead = in.read(buffer)) != -1) {
            crc.update(buffer, 0, bytesRead);
        }
        return crc.getValue();
    }

    // ======================== Read Operations ========================
    @Nullable
    public static String readString(AssetManager assetManager, String fileNamePath) {
        return readString(assetManager, fileNamePath, "UTF-8");
    }

    @Nullable
    public static String readString(AssetManager assetManager,
                                    String fileNamePath,
                                    String charset) {
        if (assetManager == null || fileNamePath == null || charset == null) {
            Log.e(TAG, "Invalid params for readString");
            return null;
        }

        try (InputStream is = assetManager.open(fileNamePath);
             ByteArrayOutputStream result = new ByteArrayOutputStream()) {

            copyStream(is, result);
            return result.toString(charset);

        } catch (IOException e) {
            Log.e(TAG, "Error reading asset: " + fileNamePath, e);
            return null;
        }
    }

    @Nullable
    public static byte[] read(AssetManager assetManager, String fileNamePath) {
        if (assetManager == null || fileNamePath == null) {
            Log.e(TAG, "Invalid params for read");
            return null;
        }

        try (InputStream is = new BufferedInputStream(
                assetManager.open(fileNamePath));
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {

            copyStream(is, buffer);
            return buffer.toByteArray();

        } catch (IOException e) {
            Log.e(TAG, "Error reading asset bytes: " + fileNamePath, e);
            return null;
        }
    }

    // ======================== Utils ========================
    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.flush();
    }
}