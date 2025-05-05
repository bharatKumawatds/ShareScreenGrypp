package com.app.screenshare.sharingMain
import android.graphics.Rect
import android.webkit.JavascriptInterface

class WebAppInterface(private val redactionManager: RedactionManager) {
    @JavascriptInterface
    fun onSensitiveFieldDetected(left: Int, top: Int, right: Int, bottom: Int) {
        if (left >= 0 && top >= 0 && right > left && bottom > top) {
            val rect = Rect(left, top, right, bottom)
            redactionManager.redactRect(rect)
            android.util.Log.d("WebAppInterface", "Redacting WebView field at $rect")
        } else {
            android.util.Log.w("WebAppInterface", "Invalid coordinates: $left, $top, $right, $bottom")
        }
    }
}