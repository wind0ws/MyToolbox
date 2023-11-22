package com.threshold.permissions;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.threshold.toolbox.R;

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
 *         mPermissionController.init(this); // do permission check
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

    private boolean mIsAllPermissionGranted = false;
    private PermissionManager mPermissionManager;
    private final OnPermissionChangedListener mOnPermissionChangedListener;

    public interface OnPermissionChangedListener {
        void onAllPermissionGranted();

        void onSomePermissionPermanentlyDenied();
    }

    public PermissionController(OnPermissionChangedListener permissionChangedListener) {
        this.mOnPermissionChangedListener = permissionChangedListener;
    }

    // call it Activity onCreate()  (after setContentView() )
    public void init(Activity act) {
        mPermissionManager = new PermissionManager() {
            @Override
            public void ifCancelledAndCannotRequest(Activity activity) {
                //super.ifCancelledAndCannotRequest(activity);
                onSomePermissionPermanentlyDenied(activity);
            }
        };
        if (mPermissionManager.checkAndRequestPermissions(act)) {
            onAllPermissionGranted();
        }
    }

    // call it Activity.onRequestPermissionsResult
    public void resolveRequestPermissionsResult(Activity activity,
                                                int requestCode,
                                                @NonNull String[] permissions,
                                                @NonNull int[] grantResults) {
        mPermissionManager.checkPermissionsResult(activity, requestCode, permissions, grantResults);
        final PermissionManager.PermissionStatus status = mPermissionManager.checkPermission(activity);
        if (status.denied.size() > 0) {
            for (int i = 0; i < status.denied.size(); ++i) {
                Log.e("PermissionController", "you denied: " + status.denied.get(i));
            }
            return;
        }
        onAllPermissionGranted();
    }

    public boolean isAllPermissionGranted() {
        return mIsAllPermissionGranted;
    }

    public PermissionManager.PermissionStatus checkPermission(Activity activity) {
        return mPermissionManager.checkPermission(activity);
    }

    protected void onAllPermissionGranted() {
        mIsAllPermissionGranted = true;
        if (mOnPermissionChangedListener != null) {
            mOnPermissionChangedListener.onAllPermissionGranted();
        }
    }

    protected void onSomePermissionPermanentlyDenied(Activity activity) {
        if (mOnPermissionChangedListener != null) {
            mOnPermissionChangedListener.onSomePermissionPermanentlyDenied();
            return;
        }
        Toast.makeText(activity.getApplicationContext(),
                R.string.tip_no_permission_exit,
                Toast.LENGTH_LONG)
                .show();
        SystemSettingUtils.toPermissionSetting(activity);
        activity.finish();
    }

}
