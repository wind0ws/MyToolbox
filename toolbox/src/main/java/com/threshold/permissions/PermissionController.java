package com.threshold.permissions;

import android.app.Activity;
import androidx.annotation.NonNull;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.threshold.toolbox.R;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Simple permission controller, help you request app permission from user.
 *
 * simple usage:
 * <code>
 * public class MainActivity extends AppCompatActivity implements View.OnClickListener {
 *
 *     private PermissionManager mPermissionManager;
 *
 *     //@Override
 *     protected void onCreate(@Nullable Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.activity_main);
 *         findViewById(R.id.btn2).setOnClickListener(this);
 *
 *         mPermissionController = new PermissionController(new PermissionController.OnPermissionChangedListener() {
 *             //@Override
 *             public void onAllPermissionGranted() {
 *                 Log.i(TAG, "all permission got! you can do something with permission");
 *             }
 *
 *             //@Override
 *             public void onSomePermissionPermanentlyDenied() {
 *                 Log.e(TAG, "some permission permanently denied. exit...");
 *                 ToastUtil.showLong(getApplicationContext(), "you denied some permission, app cannot work properly");
 *                 finish();
 *             }
 *         });
 *         mPermissionController.checkAndRequestPermission(this); // do permission check
 *     }
 *
 *     //@Override
 *     public void onClick(View v) {
 *         if (!mPermissionController.isAllPermissionGranted()) {
 *             ToastUtil.showLongImmediately(getApplicationContext(),
 *                     "no permission, you should grant app permission first!");
 *             return;
 *         }
 *     }
 *
 * }
 * </code>
 *
 */
public class PermissionController {

    private static final String TAG = "PermissionController";

    private final PermissionManager mPermissionManager;
    private final OnPermissionAuthorizationChangedListener mOnPermissionAuthorizationChangedListener;


    public interface OnPermissionAuthorizationChangedListener {
        void onAllPermissionsGranted();

        void onSomePermissionsPermanentlyDenied();
    }

    public PermissionController(OnPermissionAuthorizationChangedListener permissionChangedListener,
                                int permissionsRequestCode) {
        this.mOnPermissionAuthorizationChangedListener = permissionChangedListener;
        mPermissionManager = new PermissionManager(permissionsRequestCode) {
            @Override
            public void ifCancelledAndCannotRequest(Activity activity) {
                //super.ifCancelledAndCannotRequest(activity);
                onSomePermissionsPermanentlyDenied(activity);
            }
        };
    }

    /**
     *  call it at Activity onCreate()  (after setContentView() )
     *  we will checkAndRequestPermissions immediately.
     *
     * @param act Activity
     */
    public void checkAndRequestPermissions(Activity act) {
        if (mPermissionManager.checkAndRequestPermissions(act)) {
            onAllPermissionsGranted();
        }
    }

    public PermissionManager.PermissionStatus checkPermissions(Activity activity) {
        return mPermissionManager.checkPermissions(activity);
    }

    // call it at Activity.onActivityResult
    public void resolveActivityResult(@NotNull final Activity activity, final int requestCode,
                                      final int resultCode, @Nullable final Intent data) {
        mPermissionManager.checkActivityResult(activity, requestCode, resultCode, data);
    }

    // call it at Activity.onRequestPermissionsResult
    public void resolveRequestPermissionsResult(Activity activity,
                                                int requestCode,
                                                @NonNull String[] permissions,
                                                @NonNull int[] grantResults) {
        final PermissionManager.PermissionStatus status = mPermissionManager
                .checkPermissionsResult(activity, requestCode, permissions, grantResults);
        if (null == status) {
            return;
        }
        if (!status.denied.isEmpty()) {
            for (int i = 0; i < status.denied.size(); ++i) {
                Log.e(TAG, "you denied: " + status.denied.get(i));
            }
            return;
        }
        onAllPermissionsGranted();
    }

    protected void onAllPermissionsGranted() {
        Log.i(TAG, "congratulations: onAllPermissionsGranted");
        if (mOnPermissionAuthorizationChangedListener != null) {
            mOnPermissionAuthorizationChangedListener.onAllPermissionsGranted();
        }
    }

    protected void onSomePermissionsPermanentlyDenied(Activity activity) {
        Log.w(TAG, "oops: onSomePermissionsPermanentlyDenied");
        if (mOnPermissionAuthorizationChangedListener != null) {
            mOnPermissionAuthorizationChangedListener.onSomePermissionsPermanentlyDenied();
            return;
        }
        SystemSettingUtils.toPermissionSetting(activity);
        Toast.makeText(activity.getApplicationContext(),
                R.string.tip_no_permission_exit,
                Toast.LENGTH_LONG)
                .show();
        activity.finish();
    }

}
