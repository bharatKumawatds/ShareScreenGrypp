package com.app.screensharinggrypp



import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.app.screenshare.sharingMain.MediaProjectionService
import com.app.screenshare.sharingMain.ScreenShareComponent
import com.app.screenshare.sharingMain.ScreenShareComponent.Companion
import com.app.screenshare.sharingMain.ScreenShareComponent.Companion.RC_OVERLAY_PERMISSION
import com.app.screenshare.sharingMain.ScreenShareComponent.Companion.RC_SCREEN_CAPTURE
import com.app.screenshare.sharingMain.ScreenShareComponent.Companion.isCurrentAppIsVisible


class MainActivity : BaseActivity() {
    companion object {
        const val TAG = "MainActivity"
    }

    val OVERLAY_PERMISSION_REQUEST_CODE: Int = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (checkOverlayPermission()) {
            startOverlayService()
        } else {
            requestOverlayPermission()
        }


    }
    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this)
        }
        return true // Permission not required below Marshmallow
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage("This app needs permission to draw over other apps to enable screen annotations. Please allow this permission in the next screen.")
            .setPositiveButton("Allow") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${this.packageName}")
                )
                (this as AppCompatActivity).startActivityForResult(
                    intent,
                    OVERLAY_PERMISSION_REQUEST_CODE
                )
                Log.d(ScreenShareComponent.TAG, "Overlay permission requested")
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Log.d(ScreenShareComponent.TAG, "Overlay permission request canceled by user")
            }
            .setCancelable(false)
            .show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {

            OVERLAY_PERMISSION_REQUEST_CODE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(
                        this
                    )
                ) {
                    Log.d(ScreenShareComponent.TAG, "Overlay permission granted")
                    startOverlayService()
                } else {
                    Log.e(ScreenShareComponent.TAG, "Overlay permission denied")
                }
            }
        }
    }

    private fun startOverlayService() {
        val intent = Intent(
            this,
            GlobalActionBarService::class.java
        )
        startService(intent)
    }



}