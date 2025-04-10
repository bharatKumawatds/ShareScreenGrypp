package com.app.screensharinggrypp



import android.os.Bundle
import android.util.Log
import android.widget.Button


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
            }else{
                MainApplication.getScreenShareComponent().stopScreenShare()

            }
        }

    }

    override fun onSessionStatusChanged(status: Int) {
        super.onSessionStatusChanged(status)
        Log.e("here MainActvity Come",status.toString())
        runOnUiThread {
          if(status == 1){
              shareScreenButton.text = "Stop Screenshare"
          }else {
              shareScreenButton.text = "Start Screenshare"
          }
        }

    }


}