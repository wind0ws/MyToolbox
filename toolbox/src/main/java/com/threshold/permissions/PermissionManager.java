package com.threshold.permissions;

import static com.threshold.permissions.SystemSettingUtils.hasManageExternalStoragePermission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.threshold.toolbox.R;
import com.threshold.toolbox.ToastUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class PermissionManager {

    private static final String TAG = "PermissionManager";

    public static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    public static class PermissionStatus {
        PermissionStatus(List<String> granted, List<String> denied) {
            this.denied = denied;
            this.granted = granted;
        }

        public List<String> granted;
        public List<String> denied;
    }

    private final int mPermissionsRequestCode;
    private final int mPermissionRequestCodeForManageExternalStorage;

    public PermissionManager(int permissionsRequestCode,
                             int permissionRequestCodeForManageExternalStorage){
        this.mPermissionsRequestCode = permissionsRequestCode;
        this.mPermissionRequestCodeForManageExternalStorage = permissionRequestCodeForManageExternalStorage;
    }

    public PermissionManager(int permissionsRequestCode) {
        this(permissionsRequestCode, permissionsRequestCode - 100);
    }

    private void requestManageExternalStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.e(TAG, "no need request permission MANAGE_EXTERNAL_STORAGE on SDK:"+ Build.VERSION.SDK_INT );
            return;
        }
        final Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + activity.getApplicationContext().getPackageName()));
        activity.startActivityForResult(intent, mPermissionRequestCodeForManageExternalStorage);
    }

    /**
     * check and request permissions immediately.
     * internal flow:
     *   1. if (need storage permission), request it by SDK_INT.
     *      1.1 if (SDK_INT >= R), request MANAGE_EXTERNAL_STORAGE by startActivity with Intent
     *      1.2 otherwise, request normal WRITE_EXTERNAL_STORAGE by checkSelfPermission()
     *   2. if request permission by 1.1, then we request remain permissions onActivityResult by call this method again.
     *   3. we will notify authorization permissions by listener to you.
     *
     * @param activity your activity
     * @return true for all permissions got, false means we are request permissions.
     */
    public boolean checkAndRequestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M /*23*/) {
            return true;
        }

        final List<String> listPermissionsNeeded = permissionsWillRequest(activity);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean needsStorage = listPermissionsNeeded.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    || listPermissionsNeeded.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || listPermissionsNeeded.contains(Manifest.permission.READ_EXTERNAL_STORAGE);
            if (needsStorage && !hasManageExternalStoragePermission(activity)) {
                ToastUtil.showLong(activity, R.string.tip_grant_file_manager);
                requestManageExternalStoragePermission(activity);
                return false;
            }
        }

        if (!SystemSettingUtils.isNotificationEnabled(activity)) {
            ToastUtil.showLong(activity, R.string.tip_grant_notification);
            SystemSettingUtils.toNotificationSettings(activity);
        }

        final List<String> listPermissionsAssign = new ArrayList<>(8);
        for (String per : listPermissionsNeeded) {
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), per)
                    != PackageManager.PERMISSION_GRANTED) {
                listPermissionsAssign.add(per);
            }
        }

        if (!listPermissionsAssign.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    listPermissionsAssign.toArray(new String[0]), mPermissionsRequestCode);
            return false;
        }
        return true;
    }

    public PermissionStatus checkPermissions(Activity activity) {
        final List<String> grant = new ArrayList<>(8);
        final List<String> deny = new ArrayList<>(8);
        final List<String> listPermissionsNeeded = permissionsWillRequest(activity);
        Log.i(TAG, "Package Permissions = " + listPermissionsNeeded);
        for (String per : listPermissionsNeeded) {
            if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), per)
                    == PackageManager.PERMISSION_GRANTED) {
                grant.add(per);
            } else {
                if (per.equalsIgnoreCase(PERMISSION_POST_NOTIFICATIONS)
                        && SystemSettingUtils.isNotificationEnabled(activity)) {
                    continue;
                }
                deny.add(per);
            }
        }
        // MANAGE_EXTERNAL_STORAGE is getting from startActivityForResult with intent
        // if you got it, which means you have permission with EXTERNAL_STORAGE.
        if (!deny.isEmpty() && hasManageExternalStoragePermission(activity)) {
            final List<String> storagePermissions = Arrays.asList(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE);
            deny.removeAll(storagePermissions);
            grant.addAll(storagePermissions);
        }
        return new PermissionStatus(grant, deny);
    }

    public List<String> permissionsWillRequest(Activity activity) {
        final List<String> per = new ArrayList<>(8);
        try {
            final Context applicationContext = activity.getApplicationContext();
            final PackageManager pm = applicationContext.getPackageManager();
            final PackageInfo pi = pm.getPackageInfo(applicationContext.getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            final String[] permissionInfo = pi.requestedPermissions;
            Collections.addAll(per, permissionInfo);
        } catch (Exception e) {
            //ignore
        }
        return per;
    }

    // do it check on Activity.onActivityResult
    // protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    protected void checkActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == mPermissionRequestCodeForManageExternalStorage) {
            if (hasManageExternalStoragePermission(activity)) {
                checkAndRequestPermissions(activity); // continuous request remain permissions.
            } else {
                ifCancelledAndCannotRequest(activity); // no Manage Storage Permission
            }
        }
    }

    // do it check on Activity.onRequestPermissionsResult
    // public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    public PermissionStatus checkPermissionsResult(Activity activity, int requestCode,
                                       String[] permissions, int[] grantResults) {
        if (requestCode != mPermissionsRequestCode) {
            return null;
        }

        final PermissionStatus permissionStatus = checkPermissions(activity);
        if (permissionStatus.denied.isEmpty()) {
            return permissionStatus;
        }
        boolean shouldRequest = false;
        for (String permission : permissionStatus.denied) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldRequest = true;
                break;
            }
        }
        if (shouldRequest) {
            ifCancelledAndCanRequest(activity);
        } else {
            ifCancelledAndCannotRequest(activity);
        }
        return permissionStatus;
    }

    public void ifCancelledAndCanRequest(final Activity activity) {
        showDialogOK(activity, activity.getString(R.string.tip_permission_required),
                (dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        checkAndRequestPermissions(activity);
                    }
                });
    }

    public void ifCancelledAndCannotRequest(Activity activity) {
        Toast.makeText(activity.getApplicationContext(),
                R.string.tip_go_to_settings, Toast.LENGTH_LONG).show();
        SystemSettingUtils.toPermissionSetting(activity);
    }

    private static void showDialogOK(Activity activity, String message,
                                     DialogInterface.OnClickListener clickListener) {
        new AlertDialog.Builder(activity).setMessage(message)
                .setPositiveButton(android.R.string.ok, clickListener)
                .setNegativeButton(android.R.string.cancel, clickListener)
                .create().show();
    }


}