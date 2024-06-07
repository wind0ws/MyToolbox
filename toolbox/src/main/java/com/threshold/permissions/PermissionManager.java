package com.threshold.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;

import com.threshold.toolbox.R;
import com.threshold.toolbox.ToastUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class PermissionManager {

    public static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    public static class PermissionStatus {
        PermissionStatus(List<String> granted, List<String> denied) {
            this.denied = denied;
            this.granted = granted;
        }

        public List<String> granted;
        public List<String> denied;
    }

    private static final int REQUEST_CODE_PERMISSIONS = 65535;

    public boolean checkAndRequestPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M /*23*/) {
            if (!SystemSettingUtils.isNotificationEnabled(activity)) {
                ToastUtil.showLong(activity, R.string.tip_grant_notification);
                SystemSettingUtils.toNotificationSettings(activity);
            }

            final List<String> listPermissionsNeeded = permissionsWillRequest(activity);
            final List<String> listPermissionsAssign = new ArrayList<>(8);
            for (String per : listPermissionsNeeded) {
                if (ContextCompat.checkSelfPermission(activity.getApplicationContext(), per)
                        != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsAssign.add(per);
                }
            }

            if (!listPermissionsAssign.isEmpty()) {
                ActivityCompat.requestPermissions(activity,
                        listPermissionsAssign.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    public PermissionStatus checkPermission(Activity activity) {
        final ArrayList<String> grant = new ArrayList<>();
        final ArrayList<String> deny = new ArrayList<>();
        final List<String> listPermissionsNeeded = permissionsWillRequest(activity);
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

    // do it check on Activity.onRequestPermissionsResult
    // public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    public void checkPermissionsResult(Activity activity, int requestCode,
                                       String[] permissions, int[] grantResults) {
        if (requestCode != REQUEST_CODE_PERMISSIONS) {
            return;
        }
        final List<String> listPermissionsNeeded = permissionsWillRequest(activity);
        final Map<String, Integer> perms = new HashMap<>();
        for (String permission : listPermissionsNeeded) {
            perms.put(permission, PackageManager.PERMISSION_GRANTED);
        }
        if (grantResults.length == 0) {
            return;
        }
        for (int i = 0; i < permissions.length; i++) {
            if (PERMISSION_POST_NOTIFICATIONS.equalsIgnoreCase(permissions[i]) &&
                    SystemSettingUtils.isNotificationEnabled(activity)) {
                perms.put(permissions[i], PackageManager.PERMISSION_GRANTED);
            } else {
                perms.put(permissions[i], grantResults[i]);
            }
        }
        boolean isAllGranted = true;
        for (String permission : listPermissionsNeeded) {
            final Integer permStatus = perms.get(permission);
            if (permStatus != null && permStatus == PackageManager.PERMISSION_DENIED) {
                isAllGranted = false;
                break;
            }
        }
        if (!isAllGranted) {
            boolean shouldRequest = false;
            for (String permission : listPermissionsNeeded) {
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
        }
    }

    public void ifCancelledAndCanRequest(final Activity activity) {
        showDialogOK(activity,
                "Some Permission required for this app, please grant permission for the same",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                checkAndRequestPermissions(activity);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                // proceed with logic by disabling the related features or quit the app.
                                break;
                            default:
                                break;
                        }
                    }
                });
    }

    public void ifCancelledAndCannotRequest(Activity activity) {
        Toast.makeText(activity.getApplicationContext(),
                "Go to settings and enable permissions", Toast.LENGTH_LONG).show();
        SystemSettingUtils.toPermissionSetting(activity);
    }

    private static void showDialogOK(Activity activity, String message,
                                     DialogInterface.OnClickListener clickListener) {
        new AlertDialog.Builder(activity).setMessage(message)
                .setPositiveButton("OK", clickListener)
                .setNegativeButton("Cancel", clickListener)
                .create().show();
    }


}