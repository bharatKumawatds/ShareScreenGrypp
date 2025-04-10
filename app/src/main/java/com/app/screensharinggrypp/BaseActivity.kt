package com.app.screensharinggrypp

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.app.screenshare.sharingMain.SessionStatusListener


open class BaseActivity:AppCompatActivity(), SessionStatusListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainApplication.getScreenShareComponent().attachActivity(this)
        MainApplication.getScreenShareComponent().setSessionStatusListener(this)
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
    // Implement the SessionStatusListener interface
    override fun onSessionStatusChanged(status: String) {
        runOnUiThread {
            println("Session Status: $status")
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        MainApplication.getScreenShareComponent().setSessionStatusListener(null)
    }
}