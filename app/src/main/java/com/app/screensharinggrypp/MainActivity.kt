package com.app.screensharinggrypp


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.app.screenshare.sharingMain.ScreenShareComponent


class MainActivity : AppCompatActivity() {
    companion object {
        const val TAG = "MainActivity"
    }


    private lateinit var shareScreenButton: Button
    private var screenShareComponent: ScreenShareComponent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shareScreenButton = findViewById(R.id.sharescreen_button)


        MainApplication.getScreenShareComponent().attachActivity(this)
        shareScreenButton.setOnClickListener {
            if(shareScreenButton.text == "Start Screenshare"){

                MainApplication.getScreenShareComponent().startScreenShare()
                shareScreenButton.text = "Stop Screenshare"
            }else{
                shareScreenButton.text = "Start Screenshare"
                MainApplication.getScreenShareComponent().stopScreenShare()

            }

        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MainApplication.getScreenShareComponent().handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        MainApplication.getScreenShareComponent().handleActivityResult(requestCode, resultCode, data)
    }
}