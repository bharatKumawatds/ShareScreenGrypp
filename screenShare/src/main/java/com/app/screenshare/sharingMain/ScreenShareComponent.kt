package com.app.screenshare.sharingMain

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
import com.app.screenshare.model.response.SignalBaseResponse
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
import org.json.JSONObject
import pub.devrel.easypermissions.EasyPermissions
import java.nio.ByteBuffer

class ScreenShareComponent() : MediaProjectionHandler, DefaultLifecycleObserver {
    var session: Session? = null
    var context: Context? = null
    var context_activity: Context? = null
    var lifecycle: Lifecycle? = null
    var session_dialog: SessionCodeDialog? = null
    private var circleOverlay: CircleOverlayView? = null
    private var windowManager: WindowManager? = null
    private val drawChunks = mutableMapOf<String, MutableList<DrawEndSignal>>()
    var api_Key = ""

    constructor(context: Context, lifecycle1: Lifecycle,apiKey:String) : this() {
        Log.e("Calling", "Constructor")
        this.context = context
        api_Key = apiKey
        lifecycle = lifecycle1
        lifecycle?.addObserver(this)

        // Initialize WindowManager
        if (context != null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            circleOverlay = CircleOverlayView(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    addOverlayView()
                } else {
                    requestOverlayPermission()
                }
            } else {
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
        var TOKEN = "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLXVzdzIucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjoyNTM3NjAxOTQwODY1MTMyNzYyMjQyNTY0MjU2NjUxMTAzNjIzODIiLCJ0eXAiOiJKV1QiLCJ4NXUiOiJodHRwczovL2FudWJpcy1jZXJ0cy1jMS11c3cyLnByb2QudjEudm9uYWdlbmV0d29ya3MubmV0L3YxL2NlcnRzLzhkMWM3Yzg4YjdiMjBlZGYyODkzYjk3YWVkYzAzNmY3In0.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXNoaXNoLnRhbndhckBkb3RzcXVhcmVzLmNvbSIsImdpdmVuX25hbWUiOiJBc2hpc2giLCJmYW1pbHlfbmFtZSI6IlRhbndhciIsInBob25lX251bWJlciI6IjkxODA5NDAwMDE3NyIsInBob25lX251bWJlcl9jb3VudHJ5IjoiSU4iLCJvcmdhbml6YXRpb25faWQiOiI5ODE0MTRhOS0yZmQ0LTRkMTgtYjM3Yi00OGUxZDljYTAwN2IiLCJhdXRoZW50aWNhdGlvbk1ldGhvZHMiOlt7ImNvbXBsZXRlZF9hdCI6IjIwMjUtMDQtMDRUMDY6Mjg6MjcuNDAzNDU5OTM3WiIsIm1ldGhvZCI6ImludGVybmFsIn1dLCJpcFJpc2siOnsicmlza19sZXZlbCI6MH0sInRva2VuVHlwZSI6InZpYW0iLCJhdWQiOiJwb3J0dW51cy5pZHAudm9uYWdlLmNvbSIsImV4cCI6MTc0Mzc1OTc1MSwianRpIjoiMjU4ZGQyMmEtOWZjZC00OTAzLWExYjItMTY0MzllY2I2MThiIiwiaWF0IjoxNzQzNzU5NDUxLCJpc3MiOiJWSUFNLUlBUCIsIm5iZiI6MTc0Mzc1OTQzNiwic3ViIjoiNDk2NmNjZDEtNjBlZS00MDExLWExY2EtZDFhNzU3NDZhNmNhIn19LCJmZWRlcmF0ZWRBc3NlcnRpb25zIjp7InZpZGVvLWFwaSI6W3siYXBpS2V5IjoiNzM2NGE4NzgiLCJhcHBsaWNhdGlvbklkIjoiZmQ4MWFjYmMtZGZlYi00ZTc0LWIxNGUtMTY3YTFjMGZkYmUwIiwiZXh0cmFDb25maWciOnsidmlkZW8tYXBpIjp7ImluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3QiOiIiLCJyb2xlIjoibW9kZXJhdG9yIiwic2NvcGUiOiJzZXNzaW9uLmNvbm5lY3QiLCJzZXNzaW9uX2lkIjoiMl9NWDVtWkRneFlXTmlZeTFrWm1WaUxUUmxOelF0WWpFMFpTMHhOamRhTVdNd1ptUmlaVEItZmpFM05ETTFOekU0TlRZNU56ZC1iRkV4UmtadGRsVm1MM3BIZUU5cFJVeDVNMjFDZEVGbWZuNS0ifX19XX0sImF1ZCI6InBvcnR1bnVzLmlkcC52b25hZ2UuY29tIiwiZXhwIjoxNzQ2MzUxNDUxLCJqdGkiOiJkOGZhOTc0Yi0xMGZmLTQ1ZmUtOTkyMy05MDBkYTg2NjE1NjEiLCJpYXQiOjE3NDM3NTk0NTEsImlzcyI6IlZJQU0tSUFQIiwibmJmIjoxNzQzNzU5NDM2LCJzdWIiOiI0OTY2Y2NkMS02MGVlLTQwMTEtYTFjYS1kMWE3NTc0NmE2Y2EifQ.hgJi87EKpI_V-bS4G6r2Tuc_YwesqEMJpbiOxT7d8gmA7_UUjRWsGPvpyR_xcyeyPn81oVItD3zLZEM9sBG19MaT8Nl6WV4FMvgJDkwy40yRWPDuPvo8TCat132EmSeZ0Ar8lb7hVOLP9Z0rP0ksQkpQU8KcuUzXj8QY6tJMhDVG8s3VoCqQbCYgJIuHvNYU5Pm9Zq7P973QcfKuhmYCfJ4kyVH_WdEs3ysNTJieUseAej0ceR3BqkzQsWHWhpVj8EjIpe26JZFaEeSrtb2gOpaNGOS4_vOjlc_02kZk-Yw3kzLxgcLc3bXz3Sw5Zc22ih6GZtGWPyEyrP8OB0zb_g"
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context_activity != null) {
            AlertDialog.Builder(context_activity!!)
                .setTitle("Overlay Permission Required")
                .setMessage("This app needs permission to draw over other apps to enable screen annotations. Please allow this permission in the next screen.")
                .setPositiveButton("Allow") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context?.packageName}")
                    )
                    (context_activity as AppCompatActivity).startActivityForResult(intent, RC_OVERLAY_PERMISSION)
                    Log.d(TAG, "Overlay permission requested")
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    Log.d(TAG, "Overlay permission request canceled by user")
                }
                .setCancelable(false)
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
                    addOverlayView()
                } else {
                    Log.e(TAG, "Overlay permission denied")
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
        drawChunks.clear()
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.FOREGROUND_SERVICE
        )

        if (EasyPermissions.hasPermissions(context_activity!!, *perms)) {
            initializeComponent()
        } else {
            EasyPermissions.requestPermissions(
                context_activity as AppCompatActivity,
                "This app needs access to your camera and mic to make video calls",
                RC_VIDEO_APP_PERM,
                *perms
            )
        }
    }

    fun attachActivity(context: Context) {
        this.context_activity = context
        pd = ProgressAlertDialog(context_activity!!)
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
                if (Utils.isNetworkOnline1(context)) {
                    try {
                        if(api_Key.isEmpty()){
                            showToast("Api Key is empty")
                            return@launch
                        }

                        val response = apiService.createSession(api_Key)
                        if (response.isSuccessful) {
                            val data = response.body()
                            withContext(Dispatchers.Main) {

                                session = Session.Builder(context, response.body()?.apiKey, response.body()?.sessionId).build()
                                session?.setSessionListener(sessionListener)
                                session?.setSignalListener(signalListener)
                                session?.connect(response.body()?.customerToken)
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
                } else {
                    withContext(Dispatchers.Main) {
                        pd?.hide()
                        showToast("No Internet Connection")
                    }
                }
            }
        }
    }

    fun showToast(message: String) {
        if (context_activity == null) return
        Toast.makeText(context_activity, message, Toast.LENGTH_SHORT).show()
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
        (context_activity as AppCompatActivity).startActivityForResult(intent, RC_SCREEN_CAPTURE)
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

            try {
                val payload = JSONObject().apply {
                    put("brand", android.os.Build.BRAND)
                    put("model", android.os.Build.MODEL)
                    put("width", MediaProjectionService.width)
                    put("height", MediaProjectionService.height)
                }
                val signalData = JSONObject().apply {
                    put("type", "ScreenDetails")
                    put("payload", payload)
                }
                val signalString = signalData.toString()
                session?.sendSignal("screenshare", signalString)
                Log.d(TAG, "Sent signal: $signalString")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating ScreenDetails signal: ${e.message}", e)
            }
        }
    }

    private fun showGlassDialog(sessionCode: String) {
        if (context_activity != null && sessionCode != "") {
            session_dialog = SessionCodeDialog(
                context = context_activity!!,
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
            Toast.makeText(context_activity, "Session Error: ${p1?.message ?: "null error message"}", Toast.LENGTH_SHORT).show()
        }
    }

    var signalListener = object : Session.SignalListener {
        override fun onSignalReceived(p0: Session?, p1: String?, p2: String?, p3: Connection?) {
            when (p1) {
                "screenshare" -> {
                    var actionSignal = Gson().fromJson(p2, SignalBaseResponse::class.java)
                    if (actionSignal.action == Utils.codeRequested) {
                        Log.d(TAG, "Code Requested signal received")
                        var code = Gson().fromJson(p2, CodeRequestedResponse::class.java)
                        Log.e("here",code.toString())
                        if (code.value == matched_session_code) {
                            session_dialog?.dismiss()
                            startPublishScreen()
                        }
                    }
                    if(actionSignal.action == Utils.draw){
                        Log.d(TAG, "Draw signal received with data: $p2")
                        if (p2.isNullOrEmpty()) {
                            Log.e(TAG, "Draw signal data is null or empty")
                            return
                        }
                        try {
                            // Step 1: Parse outer JSON into DrawSignal
                            val drawSignal = Gson().fromJson(p2, DrawSignal::class.java)
                            Log.d(TAG, "DrawSignal parsed: $drawSignal")

                            // Step 2: Parse nested JSON into DrawEndSignal
                            val drawEndSignal = Gson().fromJson(drawSignal.value, DrawEndSignal::class.java)
                            Log.d(TAG, "DrawEndSignal parsed: action=${drawEndSignal.action}, eventId=${drawEndSignal.eventId}, order=${drawEndSignal.order}, totalChunks=${drawEndSignal.totalChunks}")
                            val eventId = drawEndSignal.eventId
                            val chunks = drawChunks.getOrPut(eventId) { mutableListOf() }
                            chunks.add(drawEndSignal)
                            Log.d(TAG, "Stored chunk ${drawEndSignal.order + 1}/${drawEndSignal.totalChunks} for event $eventId")


                            if (chunks.size == drawEndSignal.totalChunks) {
                                val sortedChunks = chunks.sortedBy { it.order }
                                val combinedBase64 = sortedChunks.joinToString("") { it.value }
                                val decodedBytes = android.util.Base64.decode(combinedBase64, android.util.Base64.DEFAULT)
                                val decodedJson = String(decodedBytes, Charsets.UTF_8)
                                Log.d(TAG, "Combined decoded JSON: $decodedJson")
                                val pathData = Gson().fromJson(decodedJson, PathDrawData::class.java)
                                drawPath(pathData)
                                drawChunks.remove(eventId)
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing draw signal: ${e.message}", e)
                        }
                    }
                    if(actionSignal.action == Utils.MARKER_MOVE){
//                        var markerMoveSignal = Gson().fromJson(p2, MarkerMoveResponse::class.java)
//                        Log.e("here",markerMoveSignal.toString())
                        try {
                            val markerMoveSignal = Gson().fromJson(p2, MarkerMoveResponse::class.java)
                            val markerValue = markerMoveSignal.value
                            circleOverlay?.showMarker(
                                markerValue.x.toFloat(),
                                markerValue.y.toFloat(),
                                markerValue.userName,
                                markerValue.scale
                            )
//                            // Hide marker after 4 seconds
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                circleOverlay?.hideMarker()
//                            }, 4000)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing marker move signal: ${e.message}", e)
                        }

                    }

                }

            }
            Log.e("hereSignalTitle", p1.toString())
            Log.e("hereSignalBody", p2.toString())
        }
    }
    private fun drawPath(pathData: PathDrawData) {
        circleOverlay?.let {
            if (pathData.type == "path") {
                it.showPath(pathData)
                Log.d(TAG, "Path drawn with stroke: ${pathData.stroke}, path size: ${pathData.path.size}")
                Handler(Looper.getMainLooper()).postDelayed({
                    it.hidePath()
                    Log.d(TAG, "Path hidden after 2 seconds")
                }, 4000)
            } else {
                Log.e(TAG, "PathData type is not 'path': ${pathData.type}")
            }
        } ?: Log.e(TAG, "CircleOverlay is null - ensure overlay permission is granted")
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

    data class DrawSignal(
        val action: String,
        val value: String
    )

    data class DrawEndSignal(
        val action: String,
        val value: String,
        val eventId: String,
        val order: Int,
        val totalChunks: Int
    )

    data class PathDrawData(
        val type: String,
        val version: String,
        val originX: String,
        val originY: String,
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
        val fill: String?,
        val stroke: String,
        val strokeWidth: Int,
        val strokeDashArray: List<Float>?,
        val strokeLineCap: String,
        val strokeDashOffset: Int,
        val strokeLineJoin: String,
        val strokeUniform: Boolean,
        val strokeMiterLimit: Int,
        val scaleX: Float,
        val scaleY: Float,
        val angle: Float,
        val flipX: Boolean,
        val flipY: Boolean,
        val opacity: Float,
        val shadow: String?,
        val visible: Boolean,
        val backgroundColor: String,
        val fillRule: String,
        val paintFirst: String,
        val globalCompositeOperation: String,
        val skewX: Float,
        val skewY: Float,
        val selectable: Boolean,
        val path: List<List<Any>>
    )
    data class MarkerMoveResponse(
        val action: String,
        val value: MarkerValue
    )

    data class MarkerValue(
        val userName: String,
        val x: Int,
        val y: Int,
        val scale: Float
    )
}

class CircleOverlayView(context: Context) : View(context) {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER

    }
    private val circlePaint = Paint().apply {
        color = Color.RED // Circle color
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private var path = Path()
    private var shouldDrawPath = false
    private var shouldDrawMarker = false
    private var markerX: Float = 0f
    private var markerY: Float = 0f
    private var markerRadius: Float = 50f
    private var markerName: String = ""

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (shouldDrawPath) {
            canvas.drawPath(path, paint)
            Log.d("CircleOverlayView", "Drawing path on canvas")
        }
        if (shouldDrawMarker) {
            canvas.drawCircle(markerX, markerY, markerRadius, circlePaint)
            canvas.drawText(markerName, markerX, markerY - markerRadius - 10f, textPaint)
            Log.d("CircleOverlayView", "Drawing marker at ($markerX, $markerY) with name: $markerName")
        }
        if (!shouldDrawPath && !shouldDrawMarker) {
            Log.d("CircleOverlayView", "Nothing to draw")
        }
    }

    fun showPath(pathData: ScreenShareComponent.PathDrawData) {
        paint.color = Color.parseColor(pathData.stroke ?: "#FF0000")
        paint.strokeWidth = pathData.strokeWidth.toFloat()
        path.reset()
        Log.d("CircleOverlayView", "Showing path with stroke: ${pathData.stroke}, width: ${pathData.strokeWidth}")
        pathData.path.forEach { command ->
            try {
                when (command[0] as String) {
                    "M" -> path.moveTo(command[1].toString().toFloat(), command[2].toString().toFloat())
                    "Q" -> path.quadTo(
                        command[1].toString().toFloat(), command[2].toString().toFloat(),
                        command[3].toString().toFloat(), command[4].toString().toFloat()
                    )
                    "L" -> path.lineTo(command[1].toString().toFloat(), command[2].toString().toFloat())
                }
            } catch (e: Exception) {
                Log.e("CircleOverlayView", "Error processing path command: $command, ${e.message}", e)
            }
        }
        shouldDrawPath = true
        // Removed shouldDrawMarker = false to allow both
        invalidate()
    }

    fun showMarker(x: Float, y: Float, userName: String, scale: Float) {
        markerX = x
        markerY = y
        markerName = userName
        markerRadius = 50f * scale
        shouldDrawMarker = true
        // Removed shouldDrawPath = false to allow both
        invalidate()
        Log.d("CircleOverlayView", "Showing marker for $userName at ($x, $y) with scale $scale")
    }

    fun hidePath() {
        shouldDrawPath = false
        invalidate()
        Log.d("CircleOverlayView", "Path hidden")
    }

    fun hideMarker() {
        shouldDrawMarker = false
        invalidate()
        Log.d("CircleOverlayView", "Marker hidden")
    }
}