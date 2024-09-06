package com.threshold.toolbox;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.*;
import java.util.regex.Pattern;

public class AssetsUtil {

    private static final String TAG = "AssetsUtil";

    private AssetsUtil() {
        throw new IllegalStateException("no instance");
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

    public static boolean copyAssetFile(final AssetManager assetManager,
                                        String fromAssetFilePath, String toPath,
                                        final boolean isOverrideIfDestinationFileExists) {
        InputStream in = null;
        OutputStream out = null;
        final File destinationFile = new File(toPath);
        if (destinationFile.exists()) {
            if (!isOverrideIfDestinationFileExists) {
                Log.v(TAG, String.format("\"%s\" is already exists. no need copy.", toPath));
                return true;
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

    public static boolean copyAssetFile(final AssetManager assetManager,
                                        final String fromAssetFilePath, final String toPath) {
        return copyAssetFile(assetManager, fromAssetFilePath, toPath, true);
    }

    /**
     * copy asset folder (Recursive)
     *
     * @param assetManager                      {@link AssetManager}
     * @param fromAssetFolderPath               such as "my_folder"
     * @param destinationFolderPath             such as "/sdcard/my_copy/"
     * @param isOverrideIfDestinationFileExists true if file already exists, we will override it.
     * @return true if all copy task execute successfully
     */
    public static boolean copyAssetFolder(final AssetManager assetManager,
                                          String fromAssetFolderPath,
                                          final String destinationFolderPath,
                                          final boolean isOverrideIfDestinationFileExists) {
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
                Log.w(TAG, "list() from \"" + fromAssetFolderPath + "\" is null");
                return true;
            }
            for (final String fileName : files) {
                final String fromPath = fromAssetFolderPath.endsWith("/") ?
                        fromAssetFolderPath + fileName :
                        fromAssetFolderPath + "/" + fileName;
                final String toPath = new File(destinationFolderFile, fileName).getPath();
                final String[] subPathFiles = assetManager.list(fromPath);
                if (null == subPathFiles || 0 == subPathFiles.length) {
                    succeed &= copyAssetFile(assetManager,
                            fromPath,
                            toPath,
                            isOverrideIfDestinationFileExists);
                } else {
                    succeed &= copyAssetFolder(assetManager,
                            fromPath,
                            toPath,
                            isOverrideIfDestinationFileExists);
                }
            }
            return succeed;
        } catch (Exception e) {
            Log.e(TAG, "error on copyAssetFile.", e);
        }
        return false;
    }

    public static boolean copyAssetFolder(final AssetManager assetManager,
                                          final String fromAssetFolderPath,
                                          final String destinationFolderPath) {
        return copyAssetFolder(assetManager, fromAssetFolderPath,
                destinationFolderPath, true);
    }

    public static String readTextFromAssetsFile(AssetManager assetManager, String fileNamePath) {
        BufferedReader bufReader = null;
        try {
            bufReader = new BufferedReader(new InputStreamReader(assetManager.open(fileNamePath)));
            String line;
            final StringBuilder result = new StringBuilder(8192);
            while ((line = bufReader.readLine()) != null) {
                result.append(line).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "err on read from \"" + fileNamePath + "\"", e);
        } finally {
            closeCloseable(bufReader);
        }
        Log.w(TAG, "nothing read from \"" + fileNamePath + "\"");
        return "";
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        final byte[] buffer = new byte[8192];
        int readCount;
        while ((readCount = in.read(buffer)) > 0) {
            out.write(buffer, 0, readCount);
        }
    }
}