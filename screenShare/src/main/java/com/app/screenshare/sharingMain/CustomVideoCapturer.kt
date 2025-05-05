package com.app.screenshare.sharingMain

import android.app.Activity
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.lifecycle.AtomicReference
import com.opentok.android.BaseVideoCapturer
import com.vonage.webrtc.CapturerObserver
import java.nio.ByteBuffer

class CustomVideoCapturer(private val activity: Activity) : BaseVideoCapturer() {

    private lateinit var capturerObserver: CapturerObserver
    private val handler = Handler(Looper.getMainLooper())

    private val sensitiveRectsRef = AtomicReference<List<Rect>>(emptyList())

    companion object {
        const val TAG = "CustomVideoCapturer"

        const val pixelFormat = ABGR;
    }

    private var width: Int = 240
    private var height: Int = 320
    private var isCapturing: Boolean = false

    override fun init() {
        Log.d(TAG, "On Init")
    }

    override fun onResume() {
        Log.d(TAG, "On Resume")
    }

    override fun onPause() {
        Log.d(TAG, "On Pause")
    }

    override fun getCaptureSettings(): CaptureSettings {
        val captureSettings = CaptureSettings()
        captureSettings.width = width
        captureSettings.height = height
        captureSettings.fps = 30
        captureSettings.expectedDelay = 0

        return captureSettings
    }

    override fun startCapture(): Int {
        Log.d(TAG, "Start Capture")
//        scheduleSensitiveRectUpdates()
        return 0
    }

    override fun stopCapture(): Int {
        Log.d(TAG, "Stop Capture")
        return 0
    }

    override fun isCaptureStarted(): Boolean {
        return isCapturing
    }

    override fun destroy() {
        Log.d(TAG, "Destroy")
    }

    fun sendFrame(imageBuffer: ByteBuffer, imageWidth: Int, imageHeight: Int) {
        provideBufferFrame(imageBuffer, pixelFormat, imageWidth, imageHeight, 0, false)
    }

    private fun scheduleSensitiveRectUpdates() {
        handler.postDelayed({
            if (isCapturing) {
                updateSensitiveRects()
                scheduleSensitiveRectUpdates()
            }
        }, 2000) // Update sensitive field positions every 2s
    }

    private fun updateSensitiveRects() {
        val root = activity.window.decorView.rootView
        val fields = findSensitiveFields(root)
        val rects = fields.map { getViewCoordinates(it) }
        sensitiveRectsRef.set(rects)
    }

    private fun findSensitiveFields(root: View): List<View> {
        val list = mutableListOf<View>()
        if (root is EditText) {
            val type = root.inputType
            val isPassword =
                (type and InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD
            val hintSensitive = root.hint?.toString()?.lowercase()?.let {
                it.contains("password") || it.contains("card") || it.contains("cvv")
            } == true

            if (isPassword || hintSensitive) {
                list.add(root)
            }
        }

        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                list.addAll(findSensitiveFields(root.getChildAt(i)))
            }
        }
        return list
    }

    private fun getViewCoordinates(view: View): Rect {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        return Rect(loc[0], loc[1], loc[0] + view.width, loc[1] + view.height)
    }

}