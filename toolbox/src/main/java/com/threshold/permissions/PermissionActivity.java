package com.threshold.permissions;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.threshold.toolbox.R;

public abstract class PermissionActivity extends AppCompatActivity {

    protected boolean mIsAllPermissionGranted = false;
    private PermissionManager mPermissionManager;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionManager = new PermissionManager() {
            @Override
            public void ifCancelledAndCannotRequest(Activity activity) {
                super.ifCancelledAndCannotRequest(activity);
                onSomePermissionPermanentlyDenied();
            }
        };
        if(mPermissionManager.checkAndRequestPermissions(this)){
            mIsAllPermissionGranted = true;
            onAllPermissionGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionManager.checkPermissionsResult(this, requestCode, permissions, grantResults);
        final PermissionManager.StatusArray status = mPermissionManager.getStatus(this);
        if (status.denied.size() > 0) {
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
        finish();
    }

}
