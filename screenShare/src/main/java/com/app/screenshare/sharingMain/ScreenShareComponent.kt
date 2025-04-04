package com.app.screenshare.sharingMain

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.app.screenshare.model.response.CodeRequestedResponse
import com.app.screenshare.model.response.CreateSessionResponse
import com.app.screenshare.service.RestApiBuilder
import com.app.screenshare.util.ProgressAlertDialog

import com.app.screenshare.util.SessionCodeDialog
import com.app.screenshare.util.Utils
import com.google.gson.Gson
import com.opentok.android.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pub.devrel.easypermissions.EasyPermissions
import java.nio.ByteBuffer

class ScreenShareComponent() : MediaProjectionHandler, DefaultLifecycleObserver {
    var session: Session? = null
    var context: Context? = null
    var context_actvity: Context? = null
    var lifecycle: Lifecycle? = null
    var session_dialog: SessionCodeDialog? = null
    private var circleOverlay: CircleOverlayView? = null
    private var windowManager: WindowManager? = null

    constructor(context: Context, lifecycle1: Lifecycle) : this() {
        Log.e("Calling", "Constructor")
        this.context = context
        lifecycle = lifecycle1
        lifecycle?.addObserver(this)

        // Initialize WindowManager
        if (context != null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            circleOverlay = CircleOverlayView(context)
            // Defer adding the overlay until permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    addOverlayView()
                } else {
                    requestOverlayPermission()
                }
            } else {
                // For pre-Marshmallow, no permission needed
                addOverlayView()
            }
        }
    }

    companion object {
        const val TAG = "ScreenShareComponent"
        const val RC_VIDEO_APP_PERM = 124
        const val RC_SCREEN_CAPTURE = 125
        const val RC_OVERLAY_PERMISSION = 126
        var isCurrentAppIsVisible = false
        var matched_session_code = ""

        var API_KEY = "fd81acbc-dfeb-4e74-b14e-167a1c0fdbe0"
        var SESSION_ID = "2_MX5mZDgxYWNiYy1kZmViLTRlNzQtYjE0ZS0xNjdhMWMwZmRiZTB-fjE3NDM1NzE4NTY5Nzd-bFExRkZtdlVmL3pHeE9pRUx5M21CdEFmfn5-"
        var TOKEN = "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLXVzdzIucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjoyNTM3NjAxOTQwODY1MTMyNzYyMjQyNTY0MjU2NjUxMTAzNjIzODIiLCJ0eXAiOiJKV1QiLCJ4NXUiOiJodHRwczovL2FudWJpcy1jZXJ0cy1jMS11c3cyLnByb2QudjEudm9uYWdlbmV0d29ya3MubmV0L3YxL2NlcnRzLzhkMWM3Yzg4YjdiMjBlZGYyODkzYjk3YWVkYzAzNmY3In0.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXNoaXNoLnRhbndhckBkb3RzcXVhcmVzLmNvbSIsImdpdmVuX25hbWUiOiJBc2hpc2giLCJmYW1pbHlfbmFtZSI6IlRhbndhciIsInBob25lX251bWJlciI6IjkxODA5NDAwMDE3NyIsInBob25lX251bWJlcl9jb3VudHJ5IjoiSU4iLCJvcmdhbml6YXRpb25faWQiOiI5ODE0MTRhOS0yZmQ0LTRkMTgtYjM3Yi00OGUxZDljYTAwN2IiLCJhdXRoZW50aWNhdGlvbk1ldGhvZHMiOlt7ImNvbXBsZXRlZF9hdCI6IjIwMjUtMDQtMDRUMDY6Mjg6MjcuNDAzNDU5OTM3WiIsIm1ldGhvZCI6ImludGVybmFsIn1dLCJpcFJpc2siOnsicmlza19sZXZlbCI6MH0sInRva2VuVHlwZSI6InZpYW0iLCJhdWQiOiJwb3J0dW51cy5pZHAudm9uYWdlLmNvbSIsImV4cCI6MTc0Mzc1OTc1MSwianRpIjoiMjU4ZGQyMmEtOWZjZC00OTAzLWExYjItMTY0MzllY2I2MThiIiwiaWF0IjoxNzQzNzU5NDUxLCJpc3MiOiJWSUFNLUlBUCIsIm5iZiI6MTc0Mzc1OTQzNiwic3ViIjoiNDk2NmNjZDEtNjBlZS00MDExLWExY2EtZDFhNzU3NDZhNmNhIn19LCJmZWRlcmF0ZWRBc3NlcnRpb25zIjp7InZpZGVvLWFwaSI6W3siYXBpS2V5IjoiNzM2NGE4NzgiLCJhcHBsaWNhdGlvbklkIjoiZmQ4MWFjYmMtZGZlYi00ZTc0LWIxNGUtMTY3YTFjMGZkYmUwIiwiZXh0cmFDb25maWciOnsidmlkZW8tYXBpIjp7ImluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3QiOiIiLCJyb2xlIjoibW9kZXJhdG9yIiwic2NvcGUiOiJzZXNzaW9uLmNvbm5lY3QiLCJzZXNzaW9uX2lkIjoiMl9NWDVtWkRneFlXTmlZeTFrWm1WaUxUUmxOelF0WWpFMFpTMHhOamRoTVdNd1ptUmlaVEItZmpFM05ETTFOekU0TlRZNU56ZC1iRkV4UmtadGRsVm1MM3BIZUU5cFJVeDVNMjFDZEVGbWZuNS0ifX19XX0sImF1ZCI6InBvcnR1bnVzLmlkcC52b25hZ2UuY29tIiwiZXhwIjoxNzQ2MzUxNDUxLCJqdGkiOiJkOGZhOTc0Yi0xMGZmLTQ1ZmUtOTkyMy05MDBkYTg2NjE1NjEiLCJpYXQiOjE3NDM3NTk0NTEsImlzcyI6IlZJQU0tSUFQIiwibmJmIjoxNzQzNzU5NDM2LCJzdWIiOiI0OTY2Y2NkMS02MGVlLTQwMTEtYTFjYS1kMWE3NTc0NmE2Y2EifQ.hgJi87EKpI_V-bS4G6r2Tuc_YwesqEMJpbiOxT7d8gmA7_UUjRWsGPvpyR_xcyeyPn81oVItD3zLZEM9sBG19MaT8Nl6WV4FMvgJDkwy40yRWPDuPvo8TCat132EmSeZ0Ar8lb7hVOLP9Z0rP0ksQkpQU8KcuUzXj8QY6tJMhDVG8s3VoCqQbCYgJIuHvNYU5Pm9Zq7P973QcfKuhmYCfJ4kyVH_WdEs3ysNTJieUseAej0ceR3BqkzQsWHWhpVj8EjIpe26JZFaEeSrtb2gOpaNGOS4_vOjlc_02kZk-Yw3kzLxgcLc3bXz3Sw5Zc22ih6GZtGWPyEyrP8OB0zb_g"
    }

    private var publisherScreen: Publisher? = null
    private var customVideoCapturer: CustomVideoCapturer? = null
    private var mediaProjectionServiceIsBound: Boolean = false
    private var mediaProjectionBinder: MediaProjectionBinder? = null
    var pd: ProgressAlertDialog? = null

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service Disconnected")
            mediaProjectionBinder = null
            mediaProjectionServiceIsBound = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service Connected")
            mediaProjectionServiceIsBound = true
            mediaProjectionBinder = service as MediaProjectionBinder
            mediaProjectionBinder?.mediaProjectionHandler = this@ScreenShareComponent
            publishScreen()
        }
    }

    private fun addOverlayView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
        try {
            windowManager?.addView(circleOverlay, params)
            Log.d(TAG, "CircleOverlayView added to WindowManager")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add CircleOverlayView: ${e.message}")
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context_actvity != null) {
            AlertDialog.Builder(context_actvity!!)
                .setTitle("Overlay Permission Required")
                .setMessage("This app needs permission to draw over other apps to enable screen annotations. Please allow this permission in the next screen.")
                .setPositiveButton("Allow") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context?.packageName}")
                    )
                    (context_actvity as AppCompatActivity).startActivityForResult(intent, RC_OVERLAY_PERMISSION)
                    Log.d(TAG, "Overlay permission requested")
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Overlay permission request canceled by user")
                    // Optionally handle cancellation (e.g., inform user feature won't work)
                }
                .setCancelable(false) // Prevents dismissing by tapping outside
                .show()
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RC_SCREEN_CAPTURE -> {
                pd?.show()
                Handler().postDelayed({
                    Log.e("here Checking", isCurrentAppIsVisible.toString())
                    if (isCurrentAppIsVisible) {
                            pd?.hide()
                            val intent = Intent(context, MediaProjectionService::class.java)
                            intent.putExtra("resultCode", resultCode)
                            intent.putExtra("data", data)
                            context?.startService(intent)
                            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    }
                }, 100)
            }
            RC_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
                    Log.d(TAG, "Overlay permission granted")
                    addOverlayView() // Add the overlay now that permission is granted
                } else {
                    Log.e(TAG, "Overlay permission denied")
                    // Optionally inform the user that the feature won't work without permission
                }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(TAG, "Component: onResume")
        isCurrentAppIsVisible = true
        resumeSession()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(TAG, "Component: onPause")
        isCurrentAppIsVisible = false
        pauseSession()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "Component: onStop")
    }

    fun onDestroyService() {
        Log.d(TAG, "Component: onDestroy")
        cleanup()
        lifecycle?.removeObserver(this)
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.FOREGROUND_SERVICE
        )

        if (EasyPermissions.hasPermissions(context_actvity!!, *perms)) {
            initializeComponent()
        } else {
            EasyPermissions.requestPermissions(
                context_actvity as AppCompatActivity,
                "This app needs access to your camera and mic to make video calls",
                RC_VIDEO_APP_PERM,
                *perms
            )
        }
    }

    fun attachActivity(context: Context) {
        this.context_actvity = context
        pd = ProgressAlertDialog(context_actvity!!)
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, context)
        if (requestCode == RC_VIDEO_APP_PERM && EasyPermissions.hasPermissions(context!!, *permissions)) {
            initializeComponent()
        }
    }

    private fun initializeComponent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            requestOverlayPermission()
        } else {
            pd?.setMessage("Please Wait...")
            pd?.show()
            val apiService = RestApiBuilder().service
            CoroutineScope(Dispatchers.IO).launch {
                if(Utils.isNetworkOnline1(context)){

                    try {
                        val response = apiService.createSession(" grypp_live_xK2P9M7a1LqVb3Wz6JtD4RfXyE8Nc0Q5")
                        if (response.isSuccessful) {
                            val data = response.body()
                            withContext(Dispatchers.Main) {
                                pd?.hide()
                                //session = Session.Builder(context, API_KEY, SESSION_ID).build()
                                session = Session.Builder(context, response.body()?.apiKey, response.body()?.sessionId).build()
                                session?.setSessionListener(sessionListener)
                                session?.setSignalListener(signalListener)
                                session?.connect(response.body()?.customerToken)
                                //session?.connect(TOKEN)
                                pd?.show()
                                matched_session_code = response.body()?.sessionCode ?: ""

                                Log.e("this@MainActivity", "Data: $data")
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                pd?.hide()
                                Log.e("this@MainActivity", "WErorr: ${response.code()}")
                                showToast("WErorr: ${response.code()}")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            pd?.hide()
                            Log.e("this@MainActivity", "Error: $e")
                            showToast("Error: $e")
                        }
                    }
                }else{
                    withContext(Dispatchers.Main) {
                        pd?.hide()
                        showToast("No Internet Connection")
                    }
                }

            }
        }
    }

    fun showToast(message: String) {
        if (context_actvity == null) return
        Toast.makeText(context_actvity, message, Toast.LENGTH_SHORT).show()
    }
    fun startPublishScreen() {
        if (publisherScreen == null) {
            Log.d(TAG, "Initiate Screenshare")
            requestScreenCapture()
        }
    }

    fun startScreenShare() {
        requestPermissions()
    }

    fun stopScreenShare() {
        Log.d(TAG, "Ending Screenshare")
        cleanup()
    }

    private fun requestScreenCapture() {
        val projectionManager = context?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        (context_actvity as AppCompatActivity).startActivityForResult(intent, RC_SCREEN_CAPTURE)
    }

    private fun publishScreen() {
        if (customVideoCapturer == null) {
            Log.d(TAG, "Creating Custom Video Capturer")
            customVideoCapturer = CustomVideoCapturer()
        }

        if (publisherScreen == null) {
            Log.d(TAG, "Creating Publisher (Screen)")
            publisherScreen = Publisher.Builder(context)
                .capturer(customVideoCapturer)
                .build()
            publisherScreen!!.publisherVideoType = PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen
            publisherScreen!!.audioFallbackEnabled = false
            publisherScreen!!.setPublisherListener(publisherListener)

            Log.d(TAG, "Publishing Screen")
            session?.publish(publisherScreen)
            session?.sendSignal("ScreenDetails", "{brand: ${android.os.Build.BRAND}, model: ${android.os.Build.MODEL},width: ${Utils.getScreenWidth()},height: ${Utils.getScreenHeight()}}")
        }
    }

    private fun showGlassDialog(sessionCode: String) {
        if (context_actvity != null && sessionCode != "") {
            session_dialog = SessionCodeDialog(
                context = context_actvity!!,
                title = sessionCode,
                message = "Please Share the code to executive",
                cancelText = "Cancel",
                confirmText = "Yes",
                onConfirmClick = { println("Confirmed") },
                onCancelClick = { println("Cancelled") }
            )
            session_dialog?.show()
        }
    }

    private fun unpublishScreen() {
        if (publisherScreen != null) {
            Log.d(TAG, "Unpublishing Screen")
            session?.unpublish(publisherScreen)
            publisherScreen?.capturer?.stopCapture()
            publisherScreen = null
        }
    }

    fun pauseSession() {
        session?.onPause()
    }

    fun resumeSession() {
        session?.onResume()
    }

    fun disconnect() {
        unpublishScreen()
        session?.disconnect()
    }

    fun cleanup() {
        disconnect()
        if (mediaProjectionServiceIsBound) {
            context?.unbindService(connection)
            mediaProjectionServiceIsBound = false
        }
        circleOverlay?.let {
            try {
                windowManager?.removeView(it)
                Log.d(TAG, "CircleOverlayView removed from WindowManager")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing CircleOverlayView: ${e.message}")
            }
            circleOverlay = null
        }
    }

    var sessionListener = object : Session.SessionListener {
        override fun onConnected(p0: Session?) {
            Log.d(TAG, "Session Connected")
            pd?.hide()
            showGlassDialog(matched_session_code)
            //startPublishScreen()
        }

        override fun onDisconnected(p0: Session?) {
            Log.d(TAG, "Session Disconnected")
            pd?.hide()
        }

        override fun onStreamReceived(p0: Session?, p1: Stream?) {
            Log.d(TAG, "Stream Received")
            pd?.hide()
        }

        override fun onStreamDropped(p0: Session?, p1: Stream?) {
            Log.d(TAG, "Stream Dropped")
            pd?.hide()
        }

        override fun onError(p0: Session?, p1: OpentokError?) {
            pd?.hide()
            Log.e(TAG, "Session Error: ${p1?.message ?: "null error message"}")
            Toast.makeText(context_actvity, "Session Error: ${p1?.message ?: "null error message"}", Toast.LENGTH_SHORT).show()
        }
    }

    var signalListener = object : Session.SignalListener {
        override fun onSignalReceived(p0: Session?, p1: String?, p2: String?, p3: Connection?) {
            when (p1) {
                "screenshare" -> {
                    var new_p2 = Gson().fromJson(p2, CodeRequestedResponse::class.java)
                    if (new_p2.value == matched_session_code) {
                        session_dialog?.dismiss()
                        startPublishScreen()
                    }
                }
                "Draw" -> {
                    Log.d(TAG, "Draw signal received with data: $p2")
//                    if (p2 != null && p2 != "") {
//                        try {
//                            val json = Gson().fromJson(p2, DrawData::class.java)
//                            circleOverlay?.let {
//                                it.showOval(
//                                    json.x.toFloat(),
//                                    json.y.toFloat(),
//                                    json.radiusX?.toFloat() ?: 100f,
//                                    json.radiusY?.toFloat() ?: 50f
//                                )
//                                Log.d(TAG, "Oval shown at x:${json.x}, y:${json.y}, radiusX:${json.radiusX ?: 100}, radiusY:${json.radiusY ?: 50}")
//                                Handler(Looper.getMainLooper()).postDelayed({
//                                    it.hideOval()
//                                    Log.d(TAG, "Oval hide triggered after 2 seconds")
//                                }, 2000)
//                            } ?: Log.e(TAG, "CircleOverlay is null when trying to draw - permission might not be granted")
//                        } catch (e: Exception) {
//                            Log.e(TAG, "Error parsing draw data: ${e.message}")
//                        }
//                    }
                }
            }
            Log.e("hereSignalTitle", p1.toString())
            Log.e("hereSignalBody", p2.toString())
        }
    }

    var publisherListener = object : PublisherKit.PublisherListener {
        override fun onStreamCreated(p0: PublisherKit?, p1: Stream?) {
            Log.d(TAG, "Publisher Stream Created")
        }

        override fun onStreamDestroyed(p0: PublisherKit?, p1: Stream?) {
            Log.d(TAG, "Publisher Stream Destroyed")
        }

        override fun onError(p0: PublisherKit?, p1: OpentokError?) {
            Log.d(TAG, "Publisher Error: ${p1?.message ?: "null error message"}")
        }
    }

    override fun sendFrame(imageBuffer: ByteBuffer, width: Int, height: Int) {
        customVideoCapturer?.sendFrame(imageBuffer, width, height)
    }

    override fun deleteService() {
        onDestroyService()
    }

    data class DrawData(
        val x: Int,
        val y: Int,
        val radiusX: Int? = null, // Horizontal radius
        val radiusY: Int? = null  // Vertical radius
    )
}

class CircleOverlayView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private var ovalX = 0f        // Center X of the oval
    private var ovalY = 0f        // Center Y of the oval
    private var radiusX = 100f    // Horizontal radius (half-width)
    private var radiusY = 50f     // Vertical radius (half-height)
    private var shouldDraw = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d("CircleOverlayView", "onDraw called, shouldDraw: $shouldDraw")
        if (shouldDraw) {
            // Calculate the bounding rectangle for the oval
            val left = ovalX - radiusX
            val top = ovalY - radiusY
            val right = ovalX + radiusX
            val bottom = ovalY + radiusY
            canvas.drawOval(left, top, right, bottom, paint)
            Log.d("CircleOverlayView", "Drawing oval at center x:$ovalX, y:$ovalY, radiusX:$radiusX, radiusY:$radiusY")
        } else {
            Log.d("CircleOverlayView", "Not drawing oval")
        }
    }

    fun showOval(x: Float, y: Float, rX: Float = 100f, rY: Float = 50f) {
        ovalX = x
        ovalY = y
        radiusX = rX
        radiusY = rY
        shouldDraw = true
        Log.d("CircleOverlayView", "showOval called with x:$x, y:$y, rX:$rX, rY:$rY")
        invalidate()
    }

    fun hideOval() {
        shouldDraw = false
        Log.d("CircleOverlayView", "hideOval called")
        invalidate()
    }
}