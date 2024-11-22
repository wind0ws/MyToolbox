package com.threshold.permissions;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.threshold.toolbox.R;

public abstract class AbstractPermissionActivity extends AppCompatActivity {

    protected boolean mIsAllPermissionGranted = false;
    private PermissionManager mPermissionManager;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionManager = new PermissionManager(65535) {
            @Override
            public void ifCancelledAndCannotRequest(Activity activity) {
                //super.ifCancelledAndCannotRequest(activity);
                onSomePermissionPermanentlyDenied();
            }
        };
        if(mPermissionManager.checkAndRequestPermissions(this)){
            mIsAllPermissionGranted = true;
            onAllPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        final PermissionManager.PermissionStatus status = mPermissionManager.checkPermissionsResult(this, requestCode, permissions, grantResults);
        if (null == status || !status.denied.isEmpty()) {
            return;
        }
        mIsAllPermissionGranted = true;
        onAllPermissionGranted();
    }

    protected abstract void onAllPermissionGranted();

    protected void onSomePermissionPermanentlyDenied(){
        Toast.makeText(getApplicationContext(),
                R.string.tip_no_permission_exit,
                Toast.LENGTH_LONG).show();
        SystemSettingUtils.toPermissionSetting(this);
        finish();
    }

}
