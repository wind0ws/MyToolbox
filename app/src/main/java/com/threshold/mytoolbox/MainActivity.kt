package com.threshold.mytoolbox

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import com.threshold.mytoolbox.databinding.ActivityMainBinding
import com.threshold.permissions.PermissionController
import com.threshold.permissions.SystemSettingUtils
import com.threshold.toolbox.ToastUtil
import com.threshold.toolbox.log.LogTag
import com.threshold.toolbox.log.SLog

@LogTag("MainAct")
class MainActivity : AppCompatActivity(),
    PermissionController.OnPermissionAuthorizationChangedListener {

    companion object {
//        @Keep
//        @JvmStatic
//        val TAG = "MainAct"

        const val REQUEST_CODE_FOR_PERMISSIONS = 65535
    }

    private val mHandler = Handler(Looper.myLooper()!!)
    private var mToastTimes = 0
    private val mPermissionController = PermissionController(
        this,
        REQUEST_CODE_FOR_PERMISSIONS
    )
    private lateinit var mMainViewBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMainViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mMainViewBinding.root)

        // now request permissions from user
        mPermissionController.checkAndRequestPermissions(this)

        mMainViewBinding.tv1.text = applicationInfo.nativeLibraryDir
        SLog.i("nativeLibraryDir=${applicationInfo.nativeLibraryDir}")

        val sharedLibraryFiles = applicationInfo.sharedLibraryFiles
        SLog.i("sharedLibraryFiles=$sharedLibraryFiles")
        SLog.i("dataDir=${applicationInfo.dataDir}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SLog.i("deviceProtectedDataDir=${applicationInfo.deviceProtectedDataDir}")
        }

        testToast()
        MySLogTest.test()
    }

    private fun testToast() {
        mHandler.post(object : Runnable {
            override fun run() {
                if (mToastTimes < 5) {
                    SLog.i("toast.show $mToastTimes")
                    ToastUtil.showShort(this@MainActivity, "show $mToastTimes")
                    mHandler.postDelayed(this, 3500)
                } else if (mToastTimes < 10) {
                    SLog.i("toast.showImmediately $mToastTimes")
                    ToastUtil.showShortImmediately(
                        this@MainActivity, "showImmediately $mToastTimes",
                        Gravity.TOP, 0, 100
                    )
                    mHandler.postDelayed(this, 2000)
                }
                mToastTimes++
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        SLog.i("onActivityResult: requestCode=%d, resultCode=%d", requestCode, resultCode)
        super.onActivityResult(requestCode, resultCode, data)
        // let permission controller handle it
        mPermissionController.resolveActivityResult(this, requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        SLog.i("onRequestPermissionsResult: requestCode=%d", requestCode)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // let permission controller to handle it
        mPermissionController.resolveRequestPermissionsResult(
            this,
            requestCode,
            permissions,
            grantResults
        )
    }

    override fun onDestroy() {
        SLog.d("onDestroy, bye...")
        super.onDestroy()
    }

    override fun onAllPermissionsGranted() {
        SLog.i("congratulations: onAllPermissionsGranted")
        ToastUtil.showLong(this, "OK, All permissions got!")
    }

    override fun onSomePermissionsPermanentlyDenied() {
        SLog.w("oops: onSomePermissionsPermanentlyDenied")
        ToastUtil.showLong(this, com.threshold.toolbox.R.string.tip_no_permission_exit)
        SystemSettingUtils.toPermissionSetting(this)
        finish()
    }


}
