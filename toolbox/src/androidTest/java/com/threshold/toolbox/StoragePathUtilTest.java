package com.threshold.toolbox;

import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class StoragePathUtilTest {

    @Test
    public void testStorageInfo() {
//        final Context targetContext = InstrumentationRegistry.getTargetContext();
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//            final StoragePathUtil.StoragePathInfo storagePathInfo = StoragePathUtil.check(targetContext);
//            Log.d("StoragePathUtil", "check complete.");
//
//            for (String sdPath : storagePathInfo.getSdPaths()) {
//                Log.d("StoragePathUtil", "sdPath=" + sdPath);
//            }
//            for (String usbPath : storagePathInfo.getUsbPaths()) {
//                Log.d("StoragePathUtil", "usbPath=" + usbPath);
//            }
//        } else {
        final Set<String> externalPaths = StoragePathUtil.getExtStoragePaths();
        for (String externalPath : externalPaths) {
            Log.d("StoragePathUtil", "externalPath=" + externalPath);
        }
//        }
    }


}
