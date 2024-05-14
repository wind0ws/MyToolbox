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
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "failed on close in/out stream", e);
            }
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
     * @param fileOrDirectoryDeterminer         determine a file whether is a file or directory
     * @param isOverrideIfDestinationFileExists true if file already exists, we will override it.
     * @return true if all copy task execute successfully
     */
    public static boolean copyAssetFolder(final AssetManager assetManager,
                                          String fromAssetFolderPath,
                                          final String destinationFolderPath,
                                          final FileUtil.FileOrDirectoryDeterminer fileOrDirectoryDeterminer,
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
            if (files != null) {
                for (final String fileName : files) {
                    final String fromPath = fromAssetFolderPath.endsWith("/") ?
                            fromAssetFolderPath + fileName :
                            fromAssetFolderPath + "/" + fileName;
                    final String toPath = new File(destinationFolderFile, fileName).getPath();
                    if (fileOrDirectoryDeterminer.isFile(fileName)) {
                        succeed &= copyAssetFile(assetManager,
                                fromPath,
                                toPath,
                                isOverrideIfDestinationFileExists);
                    } else {
                        succeed &= copyAssetFolder(assetManager,
                                fromPath,
                                toPath,
                                fileOrDirectoryDeterminer,
                                isOverrideIfDestinationFileExists);
                    }
                }
            } else {
                Log.w(TAG, "list() from \"" + fromAssetFolderPath + "\" is null");
            }
            return succeed;
        } catch (Exception e) {
            Log.e(TAG, "error on copyAssetFile.", e);
        }
        return false;
    }

    public static boolean copyAssetFolder(final AssetManager assetManager,
                                          String fromAssetFolderPath,
                                          final String destinationFolderPath,
                                          final FileUtil.FileOrDirectoryDeterminer fileOrDirectoryDeterminer) {
        return copyAssetFolder(assetManager, fromAssetFolderPath, destinationFolderPath,
                fileOrDirectoryDeterminer, true);
    }

    public static boolean copyAssetFolder(final AssetManager assetManager,
                                          String fromAssetFolderPath,
                                          final String destinationFolderPath) {
        return copyAssetFolder(assetManager, fromAssetFolderPath, destinationFolderPath,
                new FileUtil.DefaultFileOrDirectoryDeterminer());
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
            if (bufReader != null) {
                try {
                    bufReader.close();
                } catch (Exception ex) {
                    Log.e(TAG, "err on close stream", ex);
                }
            }
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