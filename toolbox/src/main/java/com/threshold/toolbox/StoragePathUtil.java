package com.threshold.toolbox;

import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StoragePathUtil {

    private static final String TAG = "StoragePathUtil";

    private StoragePathUtil(){
        throw new IllegalStateException("no instance");
    }
    
    public static class StoragePathInfo {

        private final List<String> sdPaths;
        private final List<String> usbPaths;

        public StoragePathInfo(List<String> sdPaths, List<String> usbPaths) {
            this.sdPaths = sdPaths;
            this.usbPaths = usbPaths;
        }

        public List<String> getSdPaths() {
            return sdPaths;
        }

        /**
         * USB路径，结尾没有斜杠
         *
         * @return 例如： /mnt/media_rw/AABE-89B8
         */
        public List<String> getUsbPaths() {
            return usbPaths;
        }
    }

    /**
     * 获取外置存储（SD卡或USB路径）
     * 7.0（N)以上系统才可以使用！
     */
    public static StoragePathInfo getExternalStoragePathInfo(Context context) {
        final List<String> sdPaths = new ArrayList<>();
        final List<String> usbPaths = new ArrayList<>();
        final StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?> storageManagerClz = Class.forName("android.os.storage.StorageManager");
            final Method getVolumesMethod = storageManagerClz.getDeclaredMethod("getVolumes");
            getVolumesMethod.setAccessible(true);
            //noinspection unchecked
            final List<Object> volumesInfo = (List<Object>) getVolumesMethod.invoke(mStorageManager);
            if (volumesInfo != null && volumesInfo.size() > 0) {
                Class<?> volumeInfoClz = Class.forName("android.os.storage.VolumeInfo");
                final Method getDiskMethod = volumeInfoClz.getDeclaredMethod("getDisk");
                getDiskMethod.setAccessible(true);
                final Field pathFiled = volumeInfoClz.getDeclaredField("path");
                pathFiled.setAccessible(true);

                final Class<?> diskInfoClz = Class.forName("android.os.storage.DiskInfo");
                final Method isSdMethod = diskInfoClz.getDeclaredMethod("isSd");
                isSdMethod.setAccessible(true);
                final Method isUsbMethod = diskInfoClz.getDeclaredMethod("isUsb");
                isUsbMethod.setAccessible(true);
                for (Object volumeInfoObj : volumesInfo) {
                    String path = (String) pathFiled.get(volumeInfoObj);
                    Object diskInfoObj = getDiskMethod.invoke(volumeInfoObj);
                    if (diskInfoObj != null) {
                        boolean isSd = (boolean) isSdMethod.invoke(diskInfoObj);
                        boolean isUsb = (boolean) isUsbMethod.invoke(diskInfoObj);
                        Log.d(TAG, "path->" + path + ",isSd=" + isSd + ",isUsb=" + isUsb);
                        if (isSd) {
                            sdPaths.add(path);
                        } else if (isUsb) {
                            usbPaths.add(path);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new StoragePathInfo(sdPaths, usbPaths);
    }

    @Nullable
    public static String getFirstAvailableStoragePath(){
        final Set<String> extStoragePaths = getExtStoragePaths();
        if (!extStoragePaths.isEmpty()) {
            return extStoragePaths.iterator().next();
        }
        return null;
    }

    /**
     * 获取外置SD卡路径以及TF卡的路径
     * 适用于4.4+
     * 例如：/mnt/usb_storage/USB_DISK0/udisk0   或 /mnt/media_rw/AABE-89B8
     *
     * @return 所有可用于存储的外部存储位置，用一个Set来保存
     */
    public static Set<String> getExtStoragePaths() {
        final Set<String> paths = new HashSet<>();
        final File extFile = Environment.getExternalStorageDirectory();
        /*
        String extFileStatus = Environment.getExternalStorageState();
        //首先判断一下外置SD卡的状态，处于挂载状态才能获取的到
        if (extFileStatus.equals(Environment.MEDIA_MOUNTED)
                && extFile.exists() && extFile.isDirectory()
                && extFile.canWrite()) {
            //外置SD卡的路径
            paths.add(extFile.getAbsolutePath());
        }*/
        try {
            // obtain executed result of command line code of 'mount', to judge
            // whether tfCard exists by the result
            final Runtime runtime = Runtime.getRuntime();
            final Process process = runtime.exec("mount");
            final InputStream is = process.getInputStream();
            final InputStreamReader isr = new InputStreamReader(is);
            final BufferedReader br = new BufferedReader(isr);
            String line = null;
            int mountPathIndex = 1;
            while ((line = br.readLine()) != null) {
                // format of sdcard file system: vfat/fuse
                if ((!line.contains("fat") && !line.contains("fuse") &&
                        !line.contains("storage"))
                        || line.startsWith("rootfs")
                        || line.startsWith("tmpfs")
                        || line.startsWith("devpts")
                        || line.startsWith("none")
                        || line.startsWith("proc")
                        || line.startsWith("adb")
                        || line.startsWith("sysfs")
                        || line.startsWith("selinuxfs")
                        || line.startsWith("/sys/kernel/")
                        || line.contains("secure")
                        || line.contains("asec")
                        || line.contains("firmware")
                        || line.contains("shell")
                        || line.contains("obb")
                        || line.contains("legacy")
                        || line.contains("data")) {
                    continue;
                }
                if (line.contains(" on ")) {
                    mountPathIndex = 2;
                }
                final String[] parts = line.split(" ");
                final int length = parts.length;
                if (mountPathIndex >= length) {
                    continue;
                }
                final String mountPath = parts[mountPathIndex];
                if (!mountPath.contains("/") || mountPath.contains("data")
                        || mountPath.contains("Data") || mountPath.contains("system")
                        || mountPath.contains("cache") || mountPath.contains("runtime")) {
                    continue;
                }
                if (!canReadWrite(mountPath)) {
                    continue;
                }
                final boolean equalsToPrimarySD = mountPath.equals(extFile.getAbsolutePath());
                if (equalsToPrimarySD) {
                    continue;
                }
                //扩展存储卡即TF卡或SD卡路径或USB路径
                paths.add(mountPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paths;
    }

    private static boolean canReadWrite(String path) {
        try {
            final File dirFile = new File(path);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return false;
            }
            if (!dirFile.canRead() || !dirFile.canWrite()) {
                Log.e(TAG, path + " can't read or write.");
                return false;
            }
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }


}
