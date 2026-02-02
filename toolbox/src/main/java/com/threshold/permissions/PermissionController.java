package com.threshold.permissions;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.threshold.toolbox.R;

/**
 * Simple permission controller, help you request app permission from user.
 * <p>
 * simple usage: <p>
 * <code>
 * public class MainActivity extends AppCompatActivity implements View.OnClickListener {
 * <p>
 *     private PermissionManager mPermissionManager;
 * <p>
 *     //@Override
 * <p>    protected void onCreate(@Nullable Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.activity_main);
 *         findViewById(R.id.btn2).setOnClickListener(this);
 * <p>
 *         mPermissionController = new PermissionController(new PermissionController.OnPermissionChangedListener() {
 * <p>        //@Override
 *             public void onAllPermissionGranted() {
 *                 Log.i(TAG, "all permission got! you can do something with permission");
 *             }
 * <p>
 * <p>         //@Override
 *             public void onSomePermissionPermanentlyDenied() {
 *                 Log.e(TAG, "some permission permanently denied. exit...");
 *                 ToastUtil.showLong(getApplicationContext(), "you denied some permission, app cannot work properly");
 *                 finish();
 *             }
 *         });
 *         mPermissionController.checkAndRequestPermission(this); // do permission check
 *     }
 * <p>
 * <p>    //@Override
 *     public void onClick(View v) {
 *         if (!mPermissionController.isAllPermissionGranted()) {
 *             ToastUtil.showLongImmediately(getApplicationContext(),
 *                     "no permission, you should grant app permission first!");
 *             return;
 *         }
 *     }
 * <p>
 * }
 * </code>
 *
 */
@SuppressWarnings("unused")
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

    /** Returns true if all permissions that will be requested are already granted. */
    public boolean isAllPermissionGranted(Activity activity) {
        PermissionManager.PermissionStatus status = mPermissionManager.checkPermissions(activity);
        return status != null && status.denied.isEmpty();
    }

    /** Call from Activity.onActivityResult. */
    public void resolveActivityResult(@NonNull final Activity activity, final int requestCode,
                                      final int resultCode, @Nullable final Intent data) {
        if (activity.isFinishing()) {
            return;
        }
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
            for (String denied : status.denied) {
                Log.e(TAG, "you denied: " + denied);
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
