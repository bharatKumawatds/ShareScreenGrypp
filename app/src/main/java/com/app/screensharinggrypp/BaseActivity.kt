package com.app.screensharinggrypp

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity:AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainApplication.getScreenShareComponent().attachActivity(this)
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
}