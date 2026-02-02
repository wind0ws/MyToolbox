package com.threshold.toolbox;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @noinspection IOStreamConstructor, ResultOfMethodCallIgnored
 */
public class NativeLibHelper {

    private static final String TAG = "NativeLibHelper";
    private static final String CACHE_TIMESTAMP_PREFIX = "ts_";
    private static final String CACHE_DIR_NAME = "native_lib_cache";

    // 双重缓存结构: <abi, <libName, abi>>
    private static final Map<String, Map<String, String>> abiCache = new ConcurrentHashMap<>();
    private static final Map<String, Object> extractionLocks = new ConcurrentHashMap<>();

    /**
     * 检测是否禁用SO库提取
     */
    public static boolean isExtractNativeLibsDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return (context.getApplicationInfo().flags
                    & ApplicationInfo.FLAG_EXTRACT_NATIVE_LIBS) == 0;
        }
        return false;
    }

    /**
     * 获取SO库路径（自动适配提取模式）
     */
    public static String getNativeLibPath(Context context, String libSoFileName) throws IOException {
        ApplicationInfo appInfo = context.getApplicationInfo();
        File normalPath = new File(appInfo.nativeLibraryDir, libSoFileName);

        if (!isExtractNativeLibsDisabled(context) && normalPath.exists()) {
            return normalPath.getAbsolutePath();
        }

        Log.i(TAG, "Using cached extraction for: " + libSoFileName);
        return getCachedSoPath(context, libSoFileName);
    }

    private static Object getExtractionLock(String libSoFileName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return extractionLocks.computeIfAbsent(libSoFileName, k -> new Object());
        } else {
            Object lock;
            synchronized (extractionLocks) {
                lock = extractionLocks.get(libSoFileName);
                //noinspection Java8MapApi
                if (lock == null) {
                    lock = new Object();
                    extractionLocks.put(libSoFileName, lock);
                }
            }
            return lock;
        }
    }

    /**
     * 获取缓存 SO路径（带ABI回退机制）
     */
    private static String getCachedSoPath(Context context, String libSoFileName) throws IOException {
        final String deviceAbiKey = getAbiCacheKey();
        final File cacheRoot = getCacheDir(context);
        final long apkModified = getApkLastModified(context);

        // 获取文件级锁确保线程安全（同进程与跨进程）
        final Object lock = getExtractionLock(libSoFileName);
        synchronized (lock) {
            // per-library process-level lock file
            final File lockFile = new File(cacheRoot, libSoFileName + ".lock");
            try (RandomAccessFile raf = new RandomAccessFile(lockFile, "rw");
                 FileChannel lockChannel = raf.getChannel();
                 java.nio.channels.FileLock fileLock = lockChannel.lock()) {

                try {
                    // 1. 尝试从内存缓存获取ABI
                    String targetAbi = getCachedAbi(deviceAbiKey, libSoFileName);

                    // 2. 检查缓存有效性（含内容完整性）
                    File soFile;
                    if (targetAbi != null) {
                        soFile = getSoCacheFile(cacheRoot, targetAbi, libSoFileName);
                        final File timestampFile = getTimestampFile(cacheRoot, targetAbi);
                        if (isCacheValid(soFile, timestampFile, apkModified)) {
                            // 额外校验内容与APK一致（crc或ELF头）
                            if (validateSoWithApk(context, soFile, "lib/" + targetAbi + "/" + libSoFileName)) {
                                return ensureExecutable(soFile).getAbsolutePath();
                            } else {
                                Log.w(TAG, "Cache exists but failed content validation. Deleting: " + soFile);
                                // 尝试删除损坏的缓存文件，再继续走重提取流程
                                if (!soFile.delete()) {
                                    Log.w(TAG, "Failed to delete corrupted cache file: " + soFile);
                                }
                            }
                        }
                    }

                    // 3. 查找有效ABI并更新缓存
                    targetAbi = findSupportedAbi(context, libSoFileName);
                    cacheAbiMatch(deviceAbiKey, libSoFileName, targetAbi);

                    // 4. 更新文件缓存（原子提取 + 校验）
                    soFile = getSoCacheFile(cacheRoot, targetAbi, libSoFileName);
                    final File timestampFile = getTimestampFile(cacheRoot, targetAbi);
                    if (!isCacheValid(soFile, timestampFile, apkModified) ||
                            !validateSoWithApk(context, soFile, "lib/" + targetAbi + "/" + libSoFileName)) {
                        // 如果存在且损坏，删除旧文件再提取
                        if (soFile.exists() && !soFile.delete()) {
                            Log.w(TAG, "Could not delete existing corrupted soFile: " + soFile);
                        }
                        extractSoWithAtomicWriteAndValidation(context, "lib/" + targetAbi + "/" + libSoFileName, soFile);
                        writeTimestamp(timestampFile, apkModified);
                        Log.d(TAG, "Extracted: \"" + libSoFileName + "\" for ABI: " + targetAbi);
                    }
                    return ensureExecutable(soFile).getAbsolutePath();
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get cached SO: " + libSoFileName, e);
                    throw new IOException("SO extraction failed: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 查找支持的ABI（带回退机制）
     */
    @VisibleForTesting
    static String findSupportedAbi(Context context, String libName) throws IOException {
        final String[] supportedAbis = getSupportedAbis();
        final File apkFile = new File(context.getApplicationInfo().sourceDir);

        try (ZipFile zip = new ZipFile(apkFile)) {
            for (String abi : supportedAbis) {
                final String entryPath = "lib/" + abi + "/" + libName;
                if (zip.getEntry(entryPath) != null) {
                    Log.d(TAG, "Found \"" + libName + "\" for ABI: " + abi);
                    return abi;
                }
            }
        }
        throw new FileNotFoundException("No ABI support found for: " + libName);
    }

    /**
     * 使用文件流复制文件（兼容低版本 API）
     */
    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (destFile.exists()) {
            if (!destFile.delete()) {
                throw new IOException("Failed to delete existing file: " + destFile);
            }
        }

        final byte[] buffer = new byte[32768];
        try (FileInputStream in = new FileInputStream(sourceFile);
             FileOutputStream out = new FileOutputStream(destFile)) {
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }

    /**
     * 提取SO文件到缓存 —— 带原子写入 & CRC 校验
     */
    private static void extractSoWithAtomicWriteAndValidation(Context context, String entryPath, File outputFile)
            throws IOException {
        final File apkFile = new File(context.getApplicationInfo().sourceDir);
        final File parentDir = outputFile.getParentFile();

        // 确保目录存在
        if ((null == parentDir) || (!parentDir.exists() && !parentDir.mkdirs())) {
            throw new IOException("Cannot create directory: " +
                    ((null == parentDir) ? "" : parentDir.getAbsolutePath()));
        }

        // 临时文件 (与目标同目录)
        final File tmpFile = new File(parentDir, outputFile.getName() + ".tmp-" + System.nanoTime());

        // 从APK提取文件并计算CRC
        try (ZipFile zip = new ZipFile(apkFile)) {
            final ZipEntry entry = zip.getEntry(entryPath);
            if (entry == null) {
                throw new FileNotFoundException("Entry not found: " + entryPath);
            }

            // 如果目标文件存在且大小与zip entry相等并crc一致，则可以直接复制（尽量避免重复提取）
            // 但这里仍然走写临时文件逻辑以保证原子性
            try (InputStream in = zip.getInputStream(entry);
                 FileOutputStream fos = new FileOutputStream(tmpFile);
                 FileChannel fc = fos.getChannel()) {

                final CRC32 crc = new CRC32();
                final byte[] buffer = new byte[65536];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    crc.update(buffer, 0, bytesRead);
                }
                // flush to underlying channel
                fos.getFD().sync();

                // 强制文件通道落盘
                try {
                    fc.force(true);
                } catch (IOException ignored) {
                    // 某些文件系统上可能不支持，但尽量调用
                }

                final long computedCrc = crc.getValue();
                final long entryCrc = entry.getCrc(); // -1 if unknown
                if (entryCrc != -1 && computedCrc != entryCrc) {
                    throw new IOException("CRC mismatch after extraction: computed=" + computedCrc + ", entry=" + entryCrc);
                }

                // 基本 ELF 头检查（强烈建议，但不能替代完整ELF解析）
                if (!looksLikeElf(tmpFile)) {
                    throw new IOException("Extracted file does not look like ELF: " + tmpFile);
                }
            }

            // 原子重命名 tmp -> final
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    // prefer atomic move
                    Files.move(tmpFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (UnsupportedOperationException | IOException e) {
                    // 如果文件系统不支持 ATOMIC_MOVE，退回到普通的 replace
                    try {
                        Files.move(tmpFile.toPath(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException re) {
                        // 确保临时文件被删除
                        tmpFile.delete();
                        throw new IOException("Failed to move tmp file to final destination", re);
                    }
                }
            } else {
                if (tmpFile.renameTo(outputFile)) {
                    // 移动成功
                    Log.d(TAG, "succeed move: " + tmpFile.getAbsolutePath() + " --> " + outputFile.getAbsolutePath());
                } else {
                    // 如果 renameTo 失败，使用传统的文件复制方式
                    copyFile(tmpFile, outputFile);
                }
            }
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }

        // 最终校验：文件非空且可读
        if (!outputFile.exists() || outputFile.length() == 0) {
            throw new IOException("Extracted empty file: " + outputFile);
        }
    }

    /**
     * 快速判断是否是 ELF 文件（只检查 magic bytes）
     */
    private static boolean looksLikeElf(File f) {
        if (!f.exists() || f.length() < 4) return false;
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            byte[] magic = new byte[4];
            raf.readFully(magic);
            return magic[0] == 0x7f && magic[1] == 'E' && magic[2] == 'L' && magic[3] == 'F';
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 验证已有的 so 文件是否与 APK 中的 entry 一致（crc 或 ELF 头）
     */
    private static boolean validateSoWithApk(Context context, File soFile, String entryPath) {
        if (soFile == null || !soFile.exists() || soFile.length() == 0) {
            return false;
        }
        final File apkFile = new File(context.getApplicationInfo().sourceDir);
        try (ZipFile zip = new ZipFile(apkFile)) {
            final ZipEntry entry = zip.getEntry(entryPath);
            if (entry == null) {
                // 无条目，则认为不一致
                return false;
            }
            long entryCrc = entry.getCrc();
            long entrySize = entry.getSize();

            // 如果 apk 中提供了 CRC（正常大多数 case），用 CRC 校验
            if (entryCrc != -1) {
                CRC32 crc = new CRC32();
                try (InputStream in = new FileInputStream(soFile)) {
                    final byte[] buffer = new byte[65536];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        crc.update(buffer, 0, bytesRead);
                    }
                }
                if (crc.getValue() != entryCrc) {
                    Log.w(TAG, "CRC mismatch: fileCRC=" + crc.getValue() + " apkEntryCRC=" + entryCrc);
                    return false;
                } else {
                    return true;
                }
            } else {
                // 没有CRC时退而检查文件尺寸和 ELF magic
                if (entrySize != -1 && entrySize != soFile.length()) {
                    Log.w(TAG, "Size mismatch: fileSize=" + soFile.length() + " apkEntrySize=" + entrySize);
                    return false;
                }
                return looksLikeElf(soFile);
            }
        } catch (Exception e) {
            Log.w(TAG, "validateSoWithApk failed", e);
            return false;
        }
    }

    // 缓存目录管理 --------------------------------------------------

    private static File getCacheDir(Context context) throws IOException {
        final File cacheDir = new File(context.getCodeCacheDir() != null
                ? context.getCodeCacheDir()
                : context.getFilesDir(),
                CACHE_DIR_NAME
        );

        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("Cannot create cache: " + cacheDir);
        }
        return cacheDir;
    }

    private static File getSoCacheFile(File cacheRoot, String abi, String libName) {
        return new File(new File(cacheRoot, abi), libName);
    }

    private static File getTimestampFile(File cacheRoot, String abi) {
        return new File(new File(cacheRoot, abi), CACHE_TIMESTAMP_PREFIX + abi);
    }

    // 缓存验证 -----------------------------------------------------

    private static boolean isCacheValid(File soFile, File timestampFile, long currentTimestamp) {
        // 1. 检查SO文件是否存在
        if (!soFile.exists() || soFile.length() == 0) {
            Log.d(TAG, "Invalid cache: Missing or empty SO file");
            return false;
        }

        // 2. 检查时间戳文件
        if (!timestampFile.exists()) {
            Log.d(TAG, "Invalid cache: Timestamp missing");
            return false;
        }

        try {
            long cachedTimestamp = readTimestamp(timestampFile);
            if (cachedTimestamp < currentTimestamp) {
                Log.d(TAG, "Invalid cache: APK updated (" + currentTimestamp + " > " + cachedTimestamp + ")");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Cache validation failed", e);
            return false;
        }
    }

    private static File ensureExecutable(File file) {
        if (!file.setExecutable(true, false)) {
            Log.w(TAG, "Set executable failed: " + file);
        }
        return file;
    }

    // 时间戳管理 ---------------------------------------------------

    private static void writeTimestamp(File file, long timestamp) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeLong(timestamp);
        }
    }

    private static long readTimestamp(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            return dis.readLong();
        }
    }

    // ABI管理 ------------------------------------------------------

    private static String getAbiCacheKey() {
        return Build.SUPPORTED_ABIS[0] + "|" + Build.SUPPORTED_ABIS.length;
    }

    @NonNull
    private static String[] getSupportedAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS;
        } else {
            //noinspection deprecation
            return new String[]{Build.CPU_ABI};
        }
    }

    // ABI内存缓存 --------------------------------------------------

    private static String getCachedAbi(String deviceKey, String libName) {
        Map<String, String> libCache = abiCache.get(deviceKey);
        return libCache != null ? libCache.get(libName) : null;
    }

    private static void cacheAbiMatch(String deviceKey, String libName, String abi) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            abiCache.computeIfAbsent(deviceKey, k -> new ConcurrentHashMap<>())
                    .put(libName, abi);
        } else {
            Map<String, String> deviceMap;
            synchronized (abiCache) {
                deviceMap = abiCache.get(deviceKey);
                if (deviceMap == null) {
                    deviceMap = new ConcurrentHashMap<>();
                    abiCache.put(deviceKey, deviceMap);
                }
                deviceMap.put(libName, abi);
            }
        }
    }

    // APK版本管理 --------------------------------------------------

    private static long getApkLastModified(Context context) {
        try {
            final File apkFile = new File(context.getApplicationInfo().sourceDir);
            if (apkFile.exists()) {
                return apkFile.lastModified();
            }

            final PackageInfo pkgInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return pkgInfo.lastUpdateTime;
        } catch (Exception e) {
            Log.w(TAG, "Using fallback timestamp", e);
            return System.currentTimeMillis();
        }
    }
}
