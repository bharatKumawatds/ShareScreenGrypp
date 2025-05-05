package com.app.screensharinggrypp


import android.content.Intent
import android.content.res.Configuration
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.appcompat.app.AppCompatActivity
import com.app.screenshare.sharingMain.SessionStatusListener


open class BaseActivity:AppCompatActivity(), SessionStatusListener {
    var layoutListener: OnGlobalLayoutListener? = null
    private var rootView: ViewGroup? = null

    override fun onResume() {
        super.onResume()
        MainApplication.getScreenShareComponent().setSessionStatusListener(this)
        MainApplication.getScreenShareComponent().attachActivity(this)
        rootView = findViewById(android.R.id.content)
        if (rootView == null) {
            Log.e("BaseActivity", "Root view is null. Ensure setContentView is called.")
            return
        }


        // Apply initial redaction after view is laid out
        rootView!!.post(Runnable {
            if (!isFinishing) {
                MainApplication.getScreenShareComponent().autoDetectAndRedactSensitiveViews(rootView!!)
            }
        })


        // Set up global layout listener for view hierarchy changes
        layoutListener = OnGlobalLayoutListener {
            if (!isFinishing) {
                rootView?.post {
                    if (!isFinishing) {
                        MainApplication.getScreenShareComponent()
                            .autoDetectAndRedactSensitiveViews(rootView!!)
                    }
                }
            }
        }
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onPause() {
        super.onPause()
        val rootView = findViewById<View>(android.R.id.content) as ViewGroup
        if (layoutListener != null) {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        layoutListener = null
        MainApplication.getScreenShareComponent().clearRedactions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MainApplication.getScreenShareComponent().handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        MainApplication.getScreenShareComponent().handleActivityResult(requestCode, resultCode, data)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        MainApplication.getScreenShareComponent().onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        MainApplication.getScreenShareComponent().onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)

    }

    override fun onSessionStatusChanged(status: Int) {
        println("Session Status: $status")
    }
    override fun onDestroy() {
        super.onDestroy()
        MainApplication.getScreenShareComponent().setSessionStatusListener(null)
    }
}