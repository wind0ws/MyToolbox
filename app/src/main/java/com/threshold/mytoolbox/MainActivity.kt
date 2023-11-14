package com.threshold.mytoolbox

import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import com.threshold.permissions.PermissionController
import com.threshold.toolbox.Density
import kotlinx.android.synthetic.main.activity_main.*
import com.threshold.toolbox.ToastUtil
import com.threshold.toolbox.log.llog.LLog
import com.threshold.toolbox.log.LogTag

@LogTag("MainAct")
class MainActivity : AppCompatActivity(), PermissionController.OnPermissionChangedListener {

//    companion object{
//        @Keep
//        @JvmStatic
//        val TAG = "MainActivity"
//    }

    private val mHandler = Handler()
    private var mToastTimes = 0
    private val mPermissionController = PermissionController(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Density.setupDensity(application, this)
        setContentView(R.layout.activity_main)

        // now request permission from user
        mPermissionController.init(this)

        tv1.text = applicationInfo.nativeLibraryDir
        LLog.i("nativeLibraryDir=${applicationInfo.nativeLibraryDir}")

        val sharedLibraryFiles = applicationInfo.sharedLibraryFiles
        LLog.i("sharedLibraryFiles=$sharedLibraryFiles")
        LLog.i("dataDir=${applicationInfo.dataDir}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LLog.i("deviceProtectedDataDir=${applicationInfo.deviceProtectedDataDir}")
        }

        // Example of a call to a native method
//        sample_text.text = NativeLibJni().stringFromJNI()

//        testHiddenPublicApi()
        testToast()
        SLogTest.test()
    }


    private fun testToast() {
        mHandler.post(object : Runnable {
            override fun run() {
                if (mToastTimes < 5) {
                    ToastUtil.showShort(this@MainActivity, "show $mToastTimes")
                    mHandler.postDelayed(this, 2000)
                } else if (mToastTimes < 10) {
                    ToastUtil.showShortImmediately(
                        this@MainActivity, "showImmediately $mToastTimes",
                        Gravity.TOP, 0, 100
                    )
                    mHandler.postDelayed(this, 1000)
                }
                mToastTimes++
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // let controller handle it
        mPermissionController.resolveRequestPermissionsResult(
            this,
            requestCode,
            permissions,
            grantResults
        )
    }

    override fun onAllPermissionGranted() {
        ToastUtil.showLong(this, "All permission got!")
    }

    override fun onSomePermissionPermanentlyDenied() {
        ToastUtil.showLong(this, "Some Permission Permanently Denied! app can't work properly")
        finish()
    }

    //call public hidden api.  must compileOnly magic android.jar
//    private fun testHiddenPublicApi() {
//        val id:Int = getWindowStackId()
//        LLog.d("getWindowStackId() --> %d",id)
//    }


}
