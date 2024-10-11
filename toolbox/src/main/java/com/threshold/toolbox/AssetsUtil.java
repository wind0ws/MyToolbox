package com.threshold.toolbox;

import android.content.res.AssetManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.threshold.jni.AssetsJni;

import java.io.*;

@SuppressWarnings({"unused", "IOStreamConstructor", "CallToPrintStackTrace"})
public class AssetsUtil {

    /**
     * copy strategies
     */
    public enum CopyMode {
        // copy it if destination file not exists.
        COPY_IF_DEST_NOT_EXISTS,
        // override it if the destination file size is different from the asset size.
        OVERRIDE_IF_DEST_SIZE_NOT_MATCHED,
        // override it always.
        OVERRIDE_FORCED,
    }

    private static final String TAG = "AssetsUtil";

    private AssetsUtil() {
        throw new IllegalStateException("no instance");
    }

    public static long getAssetSize(final AssetManager assetManager,
                                    final String assetFilePath) {
        int ret;
        final long[] receiveAssetSize = {-1};
        if (0 != (ret = AssetsJni.getResSize(assetManager, assetFilePath, receiveAssetSize))) {
            Log.e(TAG, ret + " <- failed on getAssetSize: " + assetFilePath);
        }
        return receiveAssetSize[0];
    }

    /**
     * copy asset file to target if target file not exists.
     *
     * @param assetManager      {@link AssetManager}
     * @param fromAssetFilePath such as "my_folder/1.txt"
     * @param toPath            such as "/sdcard/my_copy/1.txt"
     * @param copyMode          {@link CopyMode}
     * @return true if copy task execute successfully
     */
    public static boolean copyAssetFile(final AssetManager assetManager,
                                        final String fromAssetFilePath,
                                        final String toPath,
                                        final CopyMode copyMode) {
        InputStream in = null;
        OutputStream out = null;
        final File destinationFile = new File(toPath);

        if (destinationFile.exists()) {
            if (CopyMode.OVERRIDE_FORCED != copyMode) {
                if (CopyMode.COPY_IF_DEST_NOT_EXISTS == copyMode) {
                    Log.v(TAG, String.format("\"%s\" is already exists. no need copy.", toPath));
                    return true;
                }
                if (CopyMode.OVERRIDE_IF_DEST_SIZE_NOT_MATCHED == copyMode) {
                    final long destinationFileLength = destinationFile.length();
                    final long assetFileLength = getAssetSize(assetManager, fromAssetFilePath);
                    if (destinationFileLength == assetFileLength) {
                        Log.v(TAG, String.format("\"%s\" is already exists, size(%d) are same. " +
                                "no need copy", toPath, destinationFileLength));
                        return true;
                    }
                }
            }

            if (!destinationFile.delete()) {
                Log.e(TAG, String.format("\"%s\" exists, but delete it failed!", toPath));
                return false;
            }
        }
        try {
            final boolean isNewFileCreated = destinationFile.createNewFile();
            if (isNewFileCreated) {
                in = assetManager.open(fromAssetFilePath);
                out = new BufferedOutputStream(new FileOutputStream(destinationFile), 32 * 1024);
                copyFile(in, out);
                return true;
            }
            Log.e(TAG, "create file on \"" + toPath + "\" failed.");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeCloseable(out);
            closeCloseable(in);
        }
        return false;
    }

    /**
     * copy asset file to target if target file not exists.
     *
     * @param assetManager      {@link AssetManager}
     * @param fromAssetFilePath such as "my_folder/1.txt"
     * @param toPath            such as "/sdcard/my_copy/1.txt"
     * @return true if copy task execute successfully
     */
    public static boolean copyAssetFile(final AssetManager assetManager,
                                        final String fromAssetFilePath, final String toPath) {
        return copyAssetFile(assetManager, fromAssetFilePath,
                toPath, CopyMode.COPY_IF_DEST_NOT_EXISTS);
    }

    /**
     * copy asset folder (Recursive)
     *
     * @param assetManager          {@link AssetManager}
     * @param fromAssetFolderPath   such as "my_folder"
     * @param destinationFolderPath such as "/sdcard/my_copy/"
     * @param copyMode              {@link CopyMode}
     * @return true if all copy task execute successfully
     */
    public static boolean copyAssetFolder(final AssetManager assetManager,
                                          final String fromAssetFolderPath,
                                          final String destinationFolderPath,
                                          final CopyMode copyMode) {
        try {
            final File destinationFolderFile = new File(destinationFolderPath);
            boolean succeed;
            if (destinationFolderFile.exists()) {
                succeed = true;
            } else {
                succeed = destinationFolderFile.mkdirs();
                if (!succeed) {
                    Log.e(TAG, "error mkdirs on " + destinationFolderPath);
                    return false;
                }
            }
            final String[] files = assetManager.list(fromAssetFolderPath);
            if (null == files || 0 == files.length) {
                Log.w(TAG, fromAssetFolderPath + ".list() is empty");
                return true;
            }
            final boolean endWithSlash = fromAssetFolderPath.endsWith("/");
            for (final String fileName : files) {
                final String fromPath = endWithSlash ?
                        fromAssetFolderPath + fileName :
                        fromAssetFolderPath + "/" + fileName;
                final String toPath = new File(destinationFolderFile, fileName).getPath();
                final String[] subPathFiles = assetManager.list(fromPath);
                if (null == subPathFiles || 0 == subPathFiles.length) {
                    succeed &= copyAssetFile(assetManager,
                            fromPath, toPath, copyMode);
                } else {
                    succeed &= copyAssetFolder(assetManager,
                            fromPath, toPath, copyMode);
                }
            }
            return succeed;
        } catch (Exception e) {
            Log.e(TAG, "error on copyAssetFile.", e);
        }
        return false;
    }

    /**
     * copy asset folder to target folder if target file not exists.
     *
     * @param assetManager          {@link AssetManager}
     * @param fromAssetFolderPath   such as "my_folder"
     * @param destinationFolderPath such as "/sdcard/my_copy/"
     * @return true if all copy task execute successfully
     */
    public static boolean copyAssetFolder(final AssetManager assetManager,
                                          final String fromAssetFolderPath,
                                          final String destinationFolderPath) {
        return copyAssetFolder(assetManager, fromAssetFolderPath,
                destinationFolderPath, CopyMode.COPY_IF_DEST_NOT_EXISTS);
    }

    /**
     * read out all string from asset.
     *
     * @param assetManager {@link AssetManager}
     * @param fileNamePath such as "my_folder/1.txt"
     * @return string of content, null if asset file not exists.
     */
    @Nullable
    public static String readString(AssetManager assetManager, String fileNamePath) {
        BufferedReader bufReader = null;
        try {
            bufReader = new BufferedReader(new InputStreamReader(assetManager.open(fileNamePath)));
            String line;
            final StringBuilder contentBuilder = new StringBuilder(8192);
            while (null != (line = bufReader.readLine())) {
                contentBuilder.append(line).append("\n");
            }
            return contentBuilder.toString();
        } catch (Exception e) {
            Log.e(TAG, "err on read from \"" + fileNamePath + "\"", e);
        } finally {
            closeCloseable(bufReader);
        }
        Log.w(TAG, "nothing read from \"" + fileNamePath + "\"");
        return null;
    }

    @Nullable
    public static byte[] read(AssetManager assetManager, String fileNamePath) {
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(assetManager.open(fileNamePath));
            final int fileLen = stream.available();
            final byte[] fileContentBytes = new byte[fileLen];
            if (fileLen != stream.read(fileContentBytes, 0, fileLen)) {
                Log.e(TAG, "failed on read \"" + fileNamePath + "\", len=" + fileLen);
                return null;
            }
            return fileContentBytes;
        } catch (Exception e) {
            Log.e(TAG, "err on read from \"" + fileNamePath + "\"", e);
        } finally {
            closeCloseable(stream);
        }
        Log.w(TAG, "nothing read from \"" + fileNamePath + "\"");
        return null;
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[8192];
        int readCount;
        while ((readCount = in.read(buffer)) > 0) {
            out.write(buffer, 0, readCount);
        }
    }

    private static void closeCloseable(Closeable closeable) {
        if (null == closeable) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            Log.e(TAG, "failed on close it", e);
        }
    }

}