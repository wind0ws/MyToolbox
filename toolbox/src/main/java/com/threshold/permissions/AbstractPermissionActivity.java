package com.threshold.permissions;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.threshold.toolbox.R;

/**
 * Activity 基类：内部使用 {@link PermissionController} 统一处理权限请求与回调。
 * 子类只需实现 {@link #onAllPermissionGranted()}，并可按需重写 {@link #onSomePermissionPermanentlyDenied()}。
 */
public abstract class AbstractPermissionActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 0xFFFF;

    protected boolean mIsAllPermissionGranted = false;
    private PermissionController mPermissionController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPermissionController = new PermissionController(
                new PermissionController.OnPermissionAuthorizationChangedListener() {
                    @Override
                    public void onAllPermissionsGranted() {
                        mIsAllPermissionGranted = true;
                        onAllPermissionGranted();
                    }

                    @Override
                    public void onSomePermissionsPermanentlyDenied() {
                        onSomePermissionPermanentlyDenied();
                    }
                },
                REQUEST_CODE_PERMISSIONS);

        mPermissionController.checkAndRequestPermissions(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPermissionController.resolveActivityResult(this, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPermissionController.resolveRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    /** 所有权限已授予时回调，子类必须实现。 */
    protected abstract void onAllPermissionGranted();

    /** 部分权限被永久拒绝时的默认行为：Toast 提示并跳转设置后 finish，子类可重写。 */
    protected void onSomePermissionPermanentlyDenied() {
        Toast.makeText(getApplicationContext(),
                R.string.tip_no_permission_exit,
                Toast.LENGTH_LONG).show();
        SystemSettingUtils.toPermissionSetting(this);
        finish();
    }

    /** 是否已获得全部所需权限。 */
    public boolean isAllPermissionGranted() {
        return mPermissionController != null && mPermissionController.isAllPermissionGranted(this);
    }

}
