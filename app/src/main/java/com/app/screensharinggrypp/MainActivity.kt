package com.app.screensharinggrypp


import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.app.screenshare.ShareScreen



class MainActivity : AppCompatActivity() {
    var main_content:ConstraintLayout ?= null
    var shareScreen: ShareScreen?= null
    var webViewContainer:WebView ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        main_content = findViewById(R.id.main)
        webViewContainer = findViewById(R.id.webView)

        webViewContainer!!.setWebViewClient(WebViewClient())
        val webSettings: WebSettings = webViewContainer!!.getSettings()
        webSettings.javaScriptEnabled = true
        webViewContainer!!.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        webViewContainer!!.loadUrl("https://www.tokbox.com")
        shareScreen = ShareScreen.Builder()
            .activity(this@MainActivity)
            .context(this)
            .contentView(main_content!!)
            .build()
        shareScreen!!.initializeShareScreen()

    }

    override fun onResume() {
        super.onResume()
        shareScreen!!.connectSession()

    }

    override fun onPause() {
        super.onPause()
        shareScreen!!.disconnectSession()
    }
}