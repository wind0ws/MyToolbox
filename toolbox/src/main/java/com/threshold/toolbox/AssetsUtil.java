package com.threshold.toolbox;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threshold.jni.AssetsJni;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;

/** @noinspection BooleanMethodIsAlwaysInverted */
@SuppressWarnings({ "unused", "IOStreamConstructor" })
public class AssetsUtil {

    public enum CopyMode {
        // 仅当目标文件不存在时拷贝.
        COPY_IF_DEST_NOT_EXISTS,
        // 当外部文件的大小不匹配时，覆盖目标文件.
        OVERRIDE_IF_DEST_SIZE_NOT_MATCHED,
        // 强制覆盖目标文件, 全部重新拷贝.
        OVERRIDE_FORCED,
        // 平衡检测: 当外部文件的大小或与apk更新时间不一致时，强制覆盖目标文件; 文件过大时则跳过检测.
        OVERRIDE_IF_MODIFIED,
        // 严格检测模式: 检测外部文件的CRC值, 若与asset资源不一致, 则重新拷贝.
        OVERRIDE_IF_CONTENT_CHANGED
    }

    private static final String TAG = "AssetsUtil";
    private static final int BUFFER_SIZE = 128 * 1024; // 128KB buffer
    private static final long MAX_CRC_VERIFY_SIZE = 5 * 1024 * 1024;
    private static final long LEGACY_TIMESTAMP_OFFSET_MS = 1000L;
    private static final long LEGACY_TIMESTAMP_TOLERANCE_MS = 200L;
    private static final String PREF_ASSET_CACHE = "com.threshold.toolbox.assets";
    private static final String KEY_APK_VERSION_CODE = "apk_version_code";
    private static final String KEY_APK_VERSION_NAME = "apk_version_name";
    private static final String KEY_APK_LAST_UPDATE = "apk_last_update";
    private static final String KEY_PREFIX_CRC = "crc:";
    private static final long INVALID_CACHE_VALUE = -1L;
    private static final AtomicLong sApkLastUpdateTime = new AtomicLong(-1);
    private static volatile int sApkUpdateDetected = -1;

    private AssetsUtil() {
        throw new IllegalStateException("no instance");
    }

    // ======================== Core Methods ========================
    public static long getAssetSize(final AssetManager assetManager, final String assetFilePath) {
        if (assetManager == null || assetFilePath == null || assetFilePath.isEmpty()) {
            Log.e(TAG, "Invalid params for getAssetSize");
            return -1;
        }

        final long[] receiveAssetSize = { -1 };
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
            final boolean computeCrc = (copyMode == CopyMode.OVERRIDE_IF_CONTENT_CHANGED);
            final SharedPreferences prefs = computeCrc ? getAssetPreferences(context) : null;
            if (computeCrc && prefs != null) {
                ensureApkInfoSynced(context, prefs);
            }
            final long assetCrc = copyAssetStream(assetManager, fromAssetFilePath, destFile,
                    getApkLastUpdateTime(context), computeCrc);
            if (computeCrc && prefs != null && assetCrc != INVALID_CACHE_VALUE) {
                cacheAssetCrc(prefs, fromAssetFilePath, assetCrc);
            }
            return true;
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

    // 递归删除目录
    private static void deleteDirectoryRecursively(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectoryRecursively(file);
                }
            }
        }
        if (!dir.delete()) {
            Log.w(TAG, "Failed to delete: " + dir.getAbsolutePath());
        }
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

        final File destDir = new File(destinationFolderPath);
        // Handle OVERRIDE_IF_CONTENT_CHANGED mode: delete existing directory if APK updated.
        if (copyMode == CopyMode.OVERRIDE_IF_CONTENT_CHANGED) {
            SharedPreferences prefs = getAssetPreferences(context);
            if (prefs != null && ensureApkInfoSynced(context, prefs) && destDir.exists()) {
                deleteDirectoryRecursively(destDir);
            }
        }
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

            final String[] assets = assetManager.list(fromAssetFolderPath);
            if (null == assets || 0 == assets.length) {
                Log.w(TAG, "Empty or invalid assets folder: " + fromAssetFolderPath);
                return true; // Treat as success
            }

            boolean allSucceeded = true;
            for (String assetName : assets) {
                final String assetPath = joinAssetPath(fromAssetFolderPath, assetName);
                final String destPath = new File(destDir, assetName).getPath();

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
            Log.e(TAG, "Error copying assets folder: " + fromAssetFolderPath, e);
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
                    if (isLegacyManagedTimestamp(assetModTime, fileModTime)) {
                        Log.v(TAG, "Legacy timestamp detected, verifying: " + destFile.getPath());
                        if (shouldVerifyContent(assetSize)) {
                            if (!verifyContentMatch(assetManager, assetPath, destFile)) {
                                Log.d(TAG, "Overwriting (legacy content changed): " + destFile.getPath());
                                break;
                            }
                            normalizeFileTimestamp(destFile, assetModTime);
                            Log.v(TAG, "Skipping copy (legacy verified): " + destFile.getPath());
                            return true;
                        }

                        Log.d(TAG, "Overwriting legacy large file to ensure integrity: " + destFile.getPath());
                        break;
                    }

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

            case OVERRIDE_IF_CONTENT_CHANGED:
                SharedPreferences prefs = getAssetPreferences(context);
                if (prefs == null) {
                    Log.w(TAG, "Preferences unavailable, forcing copy: " + destFile.getPath());
                    break;
                }

                final boolean apkUpdated = ensureApkInfoSynced(context, prefs);
                if (apkUpdated) {
                    Log.d(TAG, "Overwriting (APK updated): " + destFile.getPath());
                    break;
                }

                final long cachedAssetCrc = getOrComputeAssetCrc(assetManager, assetPath, prefs);
                if (cachedAssetCrc == INVALID_CACHE_VALUE) {
                    Log.d(TAG, "Overwriting (asset CRC unavailable): " + destFile.getPath());
                    break;
                }

                final long destCrc = calculateFileCrc(destFile);
                if (cachedAssetCrc == destCrc) {
                    Log.v(TAG, "Skipping copy (content unchanged): " + destFile.getPath());
                    return true;
                }

                Log.d(TAG, "Overwriting (CRC mismatch): " + destFile.getPath());
                break;
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
        if (lastUpdateTime < 0) {
            if (context == null) {
                Log.w(TAG, "Null context, using current time");
                return System.currentTimeMillis();
            }
            synchronized (AssetsUtil.class) {
                lastUpdateTime = sApkLastUpdateTime.get();
                if (lastUpdateTime < 0) {
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

    private static long copyAssetStream(AssetManager assetManager,
            String assetPath, File destFile,
            long apkUpdateTime,
            boolean computeCrc) throws IOException {
        // Use a temporary file to ensure atomic write
        File tempFile = new File(destFile.getParent(), destFile.getName() + ".tmp");
        // Ensure temp file does not exist
        if (tempFile.exists() && !tempFile.delete()) {
            throw new IOException("Failed to delete temp file: " + tempFile.getPath());
        }

        final CRC32 crc = computeCrc ? new CRC32() : null;
        final byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream in = assetManager.open(assetPath);
                FileOutputStream fos = new FileOutputStream(tempFile);
                BufferedOutputStream out = new BufferedOutputStream(fos, BUFFER_SIZE)) {

            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                if (crc != null) {
                    crc.update(buffer, 0, bytesRead);
                }
            }
            out.flush();

            // Force data and metadata to disk
            try {
                fos.getChannel().force(true);
            } catch (IOException forceError) {
                Log.e(TAG, "Failed to force temp file to disk: " + tempFile.getPath(), forceError);
                throw forceError;
            }

            Log.d(TAG, "Copied asset to temp: " + assetPath + " → " + tempFile.getPath());
        } catch (IOException e) {
            Log.e(TAG, "Error copying asset stream: " + assetPath, e);
            // Clean up temp file
            if (tempFile.exists() && !tempFile.delete()) {
                Log.w(TAG, "Failed to delete temp file: " + tempFile.getPath());
            }
            throw e;
        }

        // Atomically move temp file to destination (on Windows, renameTo may fail if dest exists)
        if (!tempFile.renameTo(destFile)) {
            if (destFile.exists() && destFile.delete() && tempFile.renameTo(destFile)) {
                // Fallback succeeded
            } else {
                if (!tempFile.delete()) {
                    Log.w(TAG, "Failed to delete temp file after rename failure: " + tempFile.getPath());
                }
                throw new IOException("Failed to rename temp file to destination: " + destFile.getPath());
            }
        }

        if (destFile.exists()) {
            normalizeFileTimestamp(destFile, apkUpdateTime);
        }
        return crc != null ? crc.getValue() : INVALID_CACHE_VALUE;
    }

    /** 空目录也视为目录：list(path) 对目录返回非 null（可为空数组），对文件返回 null。 */
    private static boolean isAssetDirectory(AssetManager am, String path) throws IOException {
        final String[] list = am.list(path);
        return list != null;
    }

    /** 规范拼接 asset 路径，避免 "a/b" + "/" + "c" 产生 "a/b//c"。 */
    private static String joinAssetPath(String basePath, String name) {
        if (basePath == null || basePath.isEmpty()) {
            return name;
        }
        final String base = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        return base + "/" + name;
    }

    private static SharedPreferences getAssetPreferences(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREF_ASSET_CACHE, Context.MODE_PRIVATE);
    }

    private static boolean ensureApkInfoSynced(Context context, SharedPreferences prefs) {
        if (-1 != sApkUpdateDetected) {
            return (1 == sApkUpdateDetected);
        }
        if (context == null || prefs == null) {
            return false;
        }

        final PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to read package info for cache", e);
            return false;
        }

        final long versionCode = resolveVersionCode(packageInfo);
        final String versionName = packageInfo.versionName;
        final long lastUpdateTime = getApkLastUpdateTime(context);

        final long storedCode = prefs.getLong(KEY_APK_VERSION_CODE, INVALID_CACHE_VALUE);
        final String storedName = prefs.getString(KEY_APK_VERSION_NAME, null);
        final long storedUpdate = prefs.getLong(KEY_APK_LAST_UPDATE, INVALID_CACHE_VALUE);

        final boolean updated = (storedCode != versionCode)
                || !TextUtils.equals(storedName, versionName)
                || storedUpdate != lastUpdateTime;

        final int detected = (updated || storedCode == INVALID_CACHE_VALUE) ? 1 : 0;
        sApkUpdateDetected = detected;
        if (1 == detected) {
            prefs.edit()
                    .clear()
                    .putLong(KEY_APK_VERSION_CODE, versionCode)
                    .putString(KEY_APK_VERSION_NAME, versionName)
                    .putLong(KEY_APK_LAST_UPDATE, lastUpdateTime)
                    .apply();
        }
        return (detected == 1);
    }

    private static void cacheAssetCrc(SharedPreferences prefs, String assetPath, long crc) {
        if (prefs == null || assetPath == null) {
            return;
        }
        prefs.edit().putLong(buildAssetCrcKey(assetPath), crc).apply();
    }

    private static long getOrComputeAssetCrc(AssetManager assetManager,
            String assetPath,
            SharedPreferences prefs) throws IOException {
        if (assetManager == null || assetPath == null || prefs == null) {
            return INVALID_CACHE_VALUE;
        }

        final String key = buildAssetCrcKey(assetPath);
        final long cached = prefs.getLong(key, INVALID_CACHE_VALUE);
        if (cached != INVALID_CACHE_VALUE) {
            return cached;
        }

        final long computed = calculateAssetCrc(assetManager, assetPath);
        prefs.edit().putLong(key, computed).apply();
        return computed;
    }

    private static String buildAssetCrcKey(String assetPath) {
        return KEY_PREFIX_CRC + assetPath;
    }

    private static long resolveVersionCode(PackageInfo packageInfo) {
        if (packageInfo == null) {
            return INVALID_CACHE_VALUE;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageInfo.getLongVersionCode();
        }
        // noinspection deprecation
        return packageInfo.versionCode;
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

    // Detects if the file modification time indicates a legacy managed timestamp.
    private static boolean isLegacyManagedTimestamp(long assetModTime, long fileModTime) {
        final long delta = fileModTime - assetModTime;
        return delta >= (LEGACY_TIMESTAMP_OFFSET_MS - LEGACY_TIMESTAMP_TOLERANCE_MS)
                && delta <= (LEGACY_TIMESTAMP_OFFSET_MS + LEGACY_TIMESTAMP_TOLERANCE_MS);
    }

    private static void normalizeFileTimestamp(File file, long newTimestamp) {
        if (!file.setLastModified(newTimestamp)) {
            Log.v(TAG, "Failed to normalize timestamp: " + file.getPath());
        }
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
                ByteArrayOutputStream result = new ByteArrayOutputStream(BUFFER_SIZE * 2)) {

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
                ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUFFER_SIZE * 16)) {

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
