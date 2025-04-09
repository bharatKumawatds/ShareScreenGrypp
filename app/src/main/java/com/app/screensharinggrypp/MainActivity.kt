package com.app.screensharinggrypp


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.app.screenshare.sharingMain.ScreenShareComponent


class MainActivity : BaseActivity() {
    companion object {
        const val TAG = "MainActivity"
    }


    private lateinit var shareScreenButton: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        shareScreenButton = findViewById(R.id.sharescreen_button)

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

}