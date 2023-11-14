package com.threshold.toolbox;

import java.io.*;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class FileUtil {

    public interface FileOrDirectoryDeterminer {
        /**
         * determine a named file is a normal file or directory
         *
         * @param name a file name. just name, not full path.
         * @return true if it is a normal file, otherwise it is a directory
         */
        boolean isFile(String name);
    }

    public static class DefaultFileOrDirectoryDeterminer implements FileOrDirectoryDeterminer {

        private static final Pattern sPattern = Pattern.compile("^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+$");

        @Override
        public boolean isFile(final String name) {
            return sPattern.matcher(name).matches();
        }
    }

    @SuppressWarnings("all")
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private FileUtil() {
        throw new IllegalStateException("no instance");
    }

    public static byte[] readFileContent(final File file) {
        if (!file.exists()) {
            return null;
        }
        InputStream fis = null;
        //noinspection all
        try {
            fis = new FileInputStream(file);
            final byte[] fileBytes = new byte[fis.available()];
            //noinspection ResultOfMethodCallIgnored
            fis.read(fileBytes);
            return fileBytes;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
        return null;
    }

    public static String readAllAsUTF8String(final File file) {
        String result = null;
        final byte[] readFileContent = readFileContent(file);
        if (readFileContent != null) {
            result = new String(readFileContent, UTF8);
        }
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteFile(final File file) {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            for (final File f : files) {
                deleteFile(f);
            }
            file.delete();
        } else if (file.exists()) {
            file.delete();
        }
    }

    /**
     * check file exists by path
     * @param path file path
     * @return true if exists
     */
    public static boolean isExists(final String path) {
        if (TextUtil.isEmpty(path)) {
            return false;
        }
        return new File(path).exists();
    }

    private static final DelFileInterceptor S_DEL_FILE_INTERCEPTOR = new DelFileInterceptor();
    public static boolean isDirExists(final String path) {
        return isExists(path, S_DEL_FILE_INTERCEPTOR);
    }

    private static final DelDirInterceptor S_DEL_DIR_INTERCEPTOR = new DelDirInterceptor();
    public static boolean isFileExists(final String path){
        return isExists(path, S_DEL_DIR_INTERCEPTOR);
    }

    private interface FileInterceptor{
        void handleFile(File file);
    }

    private static class DelFileInterceptor implements FileInterceptor {
        @Override
        public void handleFile(final File file) {
            if (file.isFile()) {
                return;
            }
            if (!file.delete()) {
                System.out.printf("can't delete file => %s\n", file);
            }
        }
    }

    private static class DelDirInterceptor implements FileInterceptor {
        @Override
        public void handleFile(final File file) {
            if (file.isDirectory()) {
                return;
            }
            if (!file.delete()) {
                System.out.printf("can't delete dir => %s\n", file);
            }
        }
    }

    private static boolean isExists(final String path, final FileInterceptor fileInterceptor) {
        if (TextUtil.isEmpty(path)) {
            return false;
        }
        final String[] paths = path.split(File.separator);
        final StringBuilder current = new StringBuilder(128);
        for (final String onePath : paths) {
            if (TextUtil.isEmpty(onePath)) {
                continue;
            }
            current.append(File.separator).append(onePath);
            final String currentPath = current.toString();
            final File currentFile = new File(currentPath);
            if (currentFile.exists()) {
                fileInterceptor.handleFile(currentFile);
            }
        }
        return new File(path).exists();
    }

    public static boolean mkdirsIfNotExists(final String fullPath) {
        if (!isExists(fullPath)) {
            return new File(fullPath).mkdirs();
        }
        return true;
    }

    public static boolean writeToFile(final File file, final byte[] content, final int contentLen) {
        OutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(content, 0, contentLen);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean copyFile(final File inputFile, final File outFile) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        //noinspection all
        try {
            inputStream = new FileInputStream(inputFile);
            outputStream = new BufferedOutputStream(new FileOutputStream(outFile), 256 * 1024);
            final byte[] buffer = new byte[32 * 1024];
            int readLen;
            while ((readLen = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, readLen);
            }
            return true;
        } catch (IOException ex) {
            System.out.printf("error on copy file: %s\n", ex.toString());
            ex.printStackTrace();
            return false;
        }finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        }
    }

}
