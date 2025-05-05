package com.app.screenshare.sharingMain

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.app.screenshare.model.response.CodeRequestedResponse
import com.app.screenshare.model.response.SignalBaseResponse
import com.app.screenshare.service.RestApiBuilder
import com.app.screenshare.util.ProgressAlertDialog
import com.app.screenshare.util.SessionCodeDialog
import com.app.screenshare.util.Utils
import com.google.gson.Gson
import com.opentok.android.Connection
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream
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
    var context_activity: Activity? = null
    var lifecycle: Lifecycle? = null
    var session_dialog: SessionCodeDialog? = null
    private var circleOverlay: CircleOverlayView? = null
    private var windowManager: WindowManager? = null
    private val drawChunks = mutableMapOf<String, MutableList<DrawEndSignal>>()
    var api_Key = ""
    var widthScreen = 1080
    var heightScreen = 720
    var status_ScreenShare = 0
    var firstTimeCall = 0
    var sessionCodeInterface: ConnectingSession? = null
    private var redactionManager: RedactionManager? = null
    private var overlayContainer: FrameLayout? = null
    private var inputMethodManager: InputMethodManager? = null
    private var keyboardHeightListener: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var isWebViewVisible = false // Flag to track WebView visibility
    private var sensitiveTags: List<String> = emptyList() // New property to store sensitive tags


    constructor(context: Context, lifecycle1: Lifecycle, apiKey: String,sensitiveTags: List<String> = emptyList()) : this() {
        Log.e("Calling", "Constructor")
        this.context = context
        api_Key = apiKey
        lifecycle = lifecycle1
        this.sensitiveTags = sensitiveTags
        lifecycle?.addObserver(this)
        inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager


        // Initialize WindowManager
        if (context != null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        }
    }

    companion object {
        const val TAG = "ScreenShareComponent"
        const val RC_VIDEO_APP_PERM = 124
        const val RC_SCREEN_CAPTURE = 125
        const val RC_OVERLAY_PERMISSION = 126
        var isCurrentAppIsVisible = false
        var matched_session_code = ""
        var keyboardHeight = 0 // Shared variable to store dynamic keyboard height


        var API_KEY = "fd81acbc-dfeb-4e74-b14e-167a1c0fdbe0"
        var SESSION_ID =
            "2_MX5mZDgxYWNiYy1kZmViLTRlNzQtYjE0ZS0xNjdhMWMwZmRiZTB-fjE3NDM1NzE4NTY5Nzd-bFExRkZtdlVmL3pHeE9pRUx5M21CdEFmfn5-"
        var TOKEN =
            "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLXVzdzIucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjoyNTM3NjAxOTQwODY1MTMyNzYyMjQyNTY0MjU2NjUxMTAzNjIzODIiLCJ0eXAiOiJKV1QiLCJ4NXUiOiJodHRwczovL2FudWJpcy1jZXJ0cy1jMS11c3cyLnByb2QudjEudm9uYWdlbmV0d29ya3MubmV0L3YxL2NlcnRzLzhkMWM3Yzg4YjdiMjBlZGYyODkzYjk3YWVkYzAzNmY3In0.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXNoaXNoLnRhbndhckBkb3RzcXVhcmVzLmNvbSIsImdpdmVuX25hbWUiOiJBc2hpc2giLCJmYW1pbHlfbmFtZSI6IlRhbndhciIsInBob25lX251bWJlciI6IjkxODA5NDAwMDE3NyIsInBob25lX251bWJlcl9jb3VudHJ5IjoiSU4iLCJvcmdhbml6YXRpb25faWQiOiI5ODE0MTRhOS0yZmQ0LTRkMTgtYjM3Yi00OGUxZDljYTAwN2IiLCJhdXRoZW50aWNhdGlvbk1ldGhvZHMiOlt7ImNvbXBsZXRlZF9hdCI6IjIwMjUtMDQtMTFUMDU6MjE6NDcuMTk2MjcxNjdaIiwibWV0aG9kIjoiaW50ZXJuYWwifV0sImlwUmlzayI6eyJyaXNrX2xldmVsIjowfSwidG9rZW5UeXBlIjoidmlhbSIsImF1ZCI6InBvcnR1bnVzLmlkcC52b25hZ2UuY29tIiwiZXhwIjoxNzQ0MzQ5MzA1LCJqdGkiOiI3OGYzNmU4Zi02ZGY3LTQyZTMtYmU2YS0xYjIwNmM2OTEwNzkiLCJpYXQiOjE3NDQzNDkwMDUsImlzcyI6IlZJQU0tSUFQIiwibmJmIjoxNzQ0MzQ4OTkwLCJzdWIiOiI0OTY2Y2NkMS02MGVlLTQwMTEtYTFjYS1kMWE3NTc0NmE2Y2EifX0sImZlZGVyYXRlZEFzc2VydGlvbnMiOnsidmlkZW8tYXBpIjpbeyJhcGlLZXkiOiI3MzY0YTg3OCIsImFwcGxpY2F0aW9uSWQiOiJmZDgxYWNiYy1kZmViLTRlNzQtYjE0ZS0xNjdhMWMwZmRiZTAiLCJleHRyYUNvbmZpZyI6eyJ2aWRlby1hcGkiOnsiaW5pdGlhbF9sYXlvdXRfY2xhc3NfbGlzdCI6IiIsInJvbGUiOiJtb2RlcmF0b3IiLCJzY29wZSI6InNlc3Npb24uY29ubmVjdCIsInNlc3Npb25faWQiOiIyX01YNW1aRGd4WVdOaVl5MWtabVZpTFRSbE56UXRZakUwWlMweE5qZGhNV013Wm1SaVpUQi1makUzTkRNMU56RTROVFk1TnpkLWJGRXhSa1p0ZGxWbUwzcEhlRTlwUlV4NU0yMUNkRUZtZm41LSJ9fX1dfSwiYXVkIjoicG9ydHVudXMuaWRwLnZvbmFnZS5jb20iLCJleHAiOjE3NDQzNTA4MTYsImp0aSI6IjI5NDE5ZTRlLWI5YjQtNDY4OS05YzliLTE0OWI0NmM5NmE0YiIsImlhdCI6MTc0NDM0OTAxNiwiaXNzIjoiVklBTS1JQVAiLCJuYmYiOjE3NDQzNDkwMDEsInN1YiI6IjQ5NjZjY2QxLTYwZWUtNDAxMS1hMWNhLWQxYTc1NzQ2YTZjYSJ9.f7Xz38na7JPg_2BlqTYewgQIWdmM8_mYlxp5u-ETbAZowZAc1ARCbJ-rtK-2F48qiKNWNGODRm8wrxMklh7QHiA_25kpw1eJ3IEaI73FGLNZaJM_cGCkF4KpXfbmK0Ye_YJf9MvSHJlGcnNC_l18q3RG0i9RO9CFNjHH4WyvUg1r3K8TWsI-USpN0uzp504_OTAyTHLYdQDpADxnPd2DXFpO6dfHj9hsnnLS5zsFE7zQA3iFvuvqKtpIkrpEc4Nc_akLOI0Q5jjpyyJFB0jc4KtAXmZS4FgXH_-XE0T9DzOsD2kqDxz5uEWNoYJoC9hniX258f8ycapwZPro7vQgLQ"
    }

    private var publisherScreen: Publisher? = null
    private var customVideoCapturer: CustomVideoCapturer? = null
    private var mediaProjectionServiceIsBound: Boolean = false
    private var mediaProjectionBinder: MediaProjectionBinder? = null
    var pd: ProgressAlertDialog? = null
    private var sessionStatusListener: SessionStatusListener? = null


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
            setupKeyboardHeightDetection()
            publishScreen()
        }
    }

    // Method to set the listener
    fun setSessionStatusListener(listener: SessionStatusListener?) {
        if (sessionStatusListener == null) {
            this.sessionStatusListener = listener
        }

    }

    // Notify the listener of status changes
    private fun notifySessionStatus(status: Int) {
        sessionStatusListener?.onSessionStatusChanged(status)
        Log.d(TAG, "Session status notified: $status")
    }

    // Define onTouchEvent directly (no interface needed)
    fun onTouchEvent(event: MotionEvent): Boolean {
        val cursorName = "Local User" // You can customize this (e.g., fetch from a user profile)
        if (mediaProjectionServiceIsBound && isCurrentAppIsVisible) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(TAG, "Touch DOWN at (${event.x}, ${event.y})")
                    circleOverlay?.showCursor(event.x, event.y, cursorName)
                }

                MotionEvent.ACTION_MOVE -> {
                    Log.d(TAG, "Touch MOVE at (${event.x}, ${event.y})")
                    circleOverlay?.showCursor(event.x, event.y, cursorName)
                }

                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "Touch UP at (${event.x}, ${event.y})")
                    Handler(Looper.getMainLooper()).postDelayed({
                        circleOverlay?.hideCursor()
                    }, 2000) // Hide cursor after 2 seconds
                }
            }
            return false // Allow event propagation
        }
        return true

    }

    fun onConfigurationChanged(newConfig: Configuration) {
        notifySessionStatus(status_ScreenShare)
        firstTimeCall = 0
        redactionManager?.updateAllPositions()
        setupKeyboardHeightDetection() // Re-detect keyboard height on configuration change

    }

    private fun addOverlayView() {
        overlayContainer = FrameLayout(context!!)
        circleOverlay = CircleOverlayView(context!!)
        overlayContainer?.addView(circleOverlay)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Handle display cutouts (notches) for API 28+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            // Ensure overlay covers system bars (status and navigation)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            }
        }

        try {
            windowManager?.addView(overlayContainer, params)
            redactionManager = RedactionManager()
            Log.d(TAG, "OverlayContainer added to WindowManager, RedactionManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay container: ${e.message}")
        }
    }
    fun redactSensitiveView(view: View) {
        redactionManager?.redact(view)
    }

    fun clearRedactions() {
        redactionManager?.clear()
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
                    (context_activity as AppCompatActivity).startActivityForResult(
                        intent,
                        RC_OVERLAY_PERMISSION
                    )
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
                if (resultCode == -1) {
                    Handler().postDelayed({
                        Log.e("here Checking", isCurrentAppIsVisible.toString())
                        if (isCurrentAppIsVisible) {

                            val intent = Intent(context, MediaProjectionService::class.java)
                            intent.putExtra("resultCode", resultCode)
                            intent.putExtra("data", data)
                            context?.startService(intent)
                            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                        }
                    }, 100)
                } else {
                    stopScreenShare()
                }
            }

            RC_OVERLAY_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(
                        context
                    )
                ) {
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
    override fun isWebViewVisible(): Boolean {
        return isWebViewVisible
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isWebViewVisible = false
        Log.d(TAG, "Component: onPause")
        isCurrentAppIsVisible = false
        pauseSession()
        circleOverlay?.hidePath()
        circleOverlay?.hideMarker()
        keyboardHeightListener?.let {
            (context_activity as? AppCompatActivity)?.findViewById<ViewGroup>(android.R.id.content)?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            keyboardHeightListener = null
        }

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

    fun attachActivity(context: Activity) {
        context_activity = null
        pd = null
        this.context_activity = context
        pd = ProgressAlertDialog(context_activity!!)


    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, context)
        if (requestCode == RC_VIDEO_APP_PERM && EasyPermissions.hasPermissions(
                context!!,
                *permissions
            )
        ) {
            initializeComponent()
        }
    }

    private fun initializeComponent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            requestOverlayPermission()
        } else {
            circleOverlay = CircleOverlayView(context!!)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    addOverlayView()
                } else {
                    requestOverlayPermission()
                }
            } else {
                addOverlayView()
            }
            try {
                pd?.setMessage("Please Wait...")
                pd?.show()
            } catch (e: Exception) {
                Log.e("here", e.toString())
            }

            val apiService = RestApiBuilder().service
            CoroutineScope(Dispatchers.IO).launch {
                if (Utils.isNetworkOnline1(context)) {
                    try {
                        if (api_Key.isEmpty()) {
                            showToast("Api Key is empty")
                            return@launch
                        }

                        val response = apiService.createSession(api_Key)
                        if (response.isSuccessful) {
                            val data = response.body()
                            withContext(Dispatchers.Main) {

                                session = Session.Builder(
                                    context,
                                    response.body()?.apiKey,
                                    response.body()?.sessionId
                                ).build()

                                //session = Session.Builder(context, API_KEY, SESSION_ID).build()
                                session?.setSessionListener(sessionListener)
                                session?.setConnectionListener(connectionListener)
                                session?.setSignalListener(signalListener)
                                //session?.connect(TOKEN)
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

    /**
     * With Connecting session listener return event like session code get and
     * if user cancel session before start just click on cancel button handle event at app level
     * */
    fun startScreenShare(sessionConnection: ConnectingSession) {
        firstTimeCall = 0
        sessionCodeInterface = sessionConnection
        requestPermissions()
    }

    /**
     * Without Connecting session listener. So, all event like session code get and
     * if user cancel session before start just click on cancel button handle event at sdk level
     * */
    fun startScreenShare() {
        firstTimeCall = 0
        requestPermissions()
    }
    fun autoDetectAndRedactSensitiveViews(rootView: ViewGroup) {
        isWebViewVisible = false
        val sensitiveViews = mutableListOf<View>()
        val webViews = mutableListOf<WebView>()
        detectSensitiveViews(rootView, sensitiveViews, webViews)

        if(!isWebViewVisible){
            redactionManager?.clear()
        }

        sensitiveViews.forEach { redactionManager?.redact(it) }
        webViews.forEach { setupWebViewRedaction(it) }

        setupScrollViewListeners(rootView)
        setupKeyboardHeightDetection()
    }

    private fun detectSensitiveViews(view: View, found: MutableList<View>, webViews: MutableList<WebView>) {
        if (view is EditText) {
            // Existing EditText logic
            val inputType = view.inputType
            val hint = view.hint?.toString()?.lowercase() ?: ""
            val tag = view.tag?.toString()?.lowercase() ?: ""
            val isSensitive = when {
                (inputType and android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 -> true
                (inputType and android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0 -> true
                sensitiveTags.any { hint.contains(it.lowercase()) } -> true // Use sensitiveTags
                sensitiveTags.any { tag.contains(it.lowercase()) } -> true // Use sensitiveTags
                else -> false
            }
            if (isSensitive && !found.contains(view)) {
                found.add(view)
                Log.d(TAG, "Detected sensitive view: $view with hint '$hint' and inputType $inputType")
            }
        }
        if (view is TextView) {
            val inputType = view.inputType
            val hint = view.text?.toString()?.lowercase() ?: ""
            val tag = view.tag?.toString()?.lowercase() ?: ""

            val isSensitive = when {
                sensitiveTags.any { hint.contains(it.lowercase()) } -> true // Use sensitiveTags
                sensitiveTags.any { tag.contains(it.lowercase()) } -> true // Use sensitiveTags
                //(inputType and android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 || hint.contains("email") || tag.contains("email") -> false
                else -> false
            }

            if (isSensitive && !found.contains(view)) {
                found.add(view)
                Log.d(TAG, "Detected sensitive view: $view with hint '$hint' and inputType $inputType")
            }
        }
        if (view is WebView) {
            if (!webViews.contains(view)) {
                isWebViewVisible = true
                webViews.add(view)
                Log.d(TAG, "Detected WebView for redaction: $view")
            }
        }else if (view !is ViewGroup) {
            // Reset flag if no WebView is found in this traversal
            isWebViewVisible = false
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                detectSensitiveViews(view.getChildAt(i), found, webViews)
            }
        }
    }

    private fun setupWebViewRedaction(webView: WebView) {
        if (redactionManager == null) {
            Log.e(TAG, "RedactionManager is null, cannot set up WebView redaction")
            return
        }

        // Enable JavaScript (with caution)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = false

        // Add JavaScript interface
        webView.addJavascriptInterface(WebAppInterface(redactionManager!!), "Android")

        // Get WebView's screen position for coordinate adjustment
        val location = IntArray(2)
        webView.getLocationOnScreen(location)
        val webViewOffsetX = location[0]
        val webViewOffsetY = location[1]

        // Handler for debouncing redaction updates
        val handler = Handler(Looper.getMainLooper())
        var pendingRedactionUpdate: Runnable? = null

        // Set WebViewClient to handle URL changes and inject JavaScript
        webView.setWebViewClient(object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "WebView URL changed (started): $url")

                // Cancel any pending redaction update
                pendingRedactionUpdate?.let { handler.removeCallbacks(it) }

                // Schedule new redaction update
                pendingRedactionUpdate = Runnable {
                    val lowerCaseUrl = url.toLowerCase()
                    if (lowerCaseUrl.contains("stripe") || lowerCaseUrl.contains("payment") || lowerCaseUrl.contains("checkout")) {
                        // Redact entire WebView
                        redactionManager?.redact(view)
                        Log.d(TAG, "Redacting entire WebView due to payment-related URL: $url")
                    } else {
                        // Remove full-WebView redaction if previously applied
                        Handler(Looper.getMainLooper()).postDelayed({
                            redactionManager?.removeRedaction(view)
                        },1500)

                        Log.d(TAG, "Cleared full-WebView redaction for URL: $url")
                    }
                }
                handler.postDelayed(pendingRedactionUpdate!!, 100) // Debounce by 100ms
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WebView page finished loading: $url")

                // Cancel any pending redaction update
                pendingRedactionUpdate?.let { handler.removeCallbacks(it) }

                // Schedule JavaScript injection for non-payment URLs
                pendingRedactionUpdate = Runnable {
                    val lowerCaseUrl = url.toLowerCase()
                    if (!(lowerCaseUrl.contains("stripe") || lowerCaseUrl.contains("payment") || lowerCaseUrl.contains("checkout"))) {
                        view.evaluateJavascript(
                            """
                        function detectSensitiveFields() {
                            // Helper function to get element position relative to document
                            function getElementPosition(element) {
                                const rect = element.getBoundingClientRect();
                                return {
                                    left: rect.left + window.scrollX,
                                    top: rect.top + window.scrollY,
                                    right: rect.right + window.scrollX,
                                    bottom: rect.bottom + window.scrollY
                                };
                            }

                            // Collect sensitive fields
                            const sensitiveFields = [];

                            // 1. Detect standard input fields (password, email, cardholder name)
                            const inputs = document.querySelectorAll('input[type="password"], input[type="email"], input[type="text"]');
                            inputs.forEach(input => {
                                const autocomplete = input.getAttribute('autocomplete') || '';
                                const placeholder = (input.getAttribute('placeholder') || '').toLowerCase();
                                const name = (input.getAttribute('name') || '').toLowerCase();
                                const id = (input.getAttribute('id') || '').toLowerCase();

                                // Detect email, cardholder name, or fields with payment-related keywords
                                if (autocomplete.includes('email') ||
                                    autocomplete.includes('cc-name') ||
                                    placeholder.includes('name on card') ||
                                    name.includes('cardholder') ||
                                    id.includes('cardholder') ||
                                    placeholder.includes('email')) {
                                    const pos = getElementPosition(input);
                                    if (pos.right - pos.left > 0 && pos.bottom - pos.top > 0) {
                                        sensitiveFields.push(pos);
                                    }
                                }
                            });

                            // 2. Detect Stripe Elements (card number, MM/YY, CVC)
                            const stripeElements = document.querySelectorAll('[id*="card-element"], [class*="__PrivateStripeElement"], [data-stripe]');
                            stripeElements.forEach(element => {
                                const pos = getElementPosition(element);
                                if (pos.right - pos.left > 0 && pos.bottom - pos.top > 0) {
                                    sensitiveFields.push(pos);
                                }
                            });

                            // 3. Detect iframes (Stripe often uses iframes for card fields)
                            const iframes = document.querySelectorAll('iframe');
                            iframes.forEach(iframe => {
                                const src = (iframe.getAttribute('src') || '').toLowerCase();
                                const name = (iframe.getAttribute('name') || '').toLowerCase();
                                if (src.includes('stripe') || name.includes('stripe') || name.includes('card')) {
                                    const pos = getElementPosition(iframe);
                                    if (pos.right - pos.left > 0 && pos.bottom - pos.top > 0) {
                                        sensitiveFields.push(pos);
                                    }
                                }
                            });

                            // Send sensitive field positions to Android
                            sensitiveFields.forEach(field => {
                                Android.onSensitiveFieldDetected(
                                    Math.round(field.left + $webViewOffsetX),
                                    Math.round(field.top + $webViewOffsetY),
                                    Math.round(field.right + $webViewOffsetX),
                                    Math.round(field.bottom + $webViewOffsetY)
                                );
                            });
                        }

                        // Run on page load, scroll, resize, and DOM changes
                        window.addEventListener('load', detectSensitiveFields);
                        window.addEventListener('scroll', detectSensitiveFields);
                        window.addEventListener('resize', detectSensitiveFields);
                        document.addEventListener('DOMSubtreeModified', detectSensitiveFields);
                        detectSensitiveFields();
                        """.trimIndent(), null
                        )
                        Log.d(TAG, "Injected JavaScript for WebView redaction at $url")
                    }
                }
                handler.postDelayed(pendingRedactionUpdate!!, 100) // Debounce by 100ms
            }
        })
        webView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                isWebViewVisible = true
                val url = webView.url?.toLowerCase() ?: ""
                if ((url.contains("stripe") || url.contains("payment") || url.contains("checkout"))) {
                    redactionManager?.redact(webView)
                }
                return false
            }
        })

        // Update redactions on WebView scroll
        webView.setOnScrollChangeListener { _, _, _, _, _ ->
            val url = webView.url?.toLowerCase() ?: ""
            if (!(url.contains("stripe") || url.contains("payment") || url.contains("checkout"))) {
                redactionManager?.clear() // Clear previous WebView redactions
                webView.evaluateJavascript("detectSensitiveFields();", null)
                Log.d(TAG, "WebView scrolled, updating redactions")
            }
        }
    }


    private fun setupScrollViewListeners(view: View) {
        if (view is ScrollView) {
            Log.d(TAG, "Found ScrollView: $view")
            view.setOnScrollChangeListener { _, _, _, _, _ ->
                Log.d(TAG, "ScrollView scrolled, updating redactions")
                redactionManager?.updateAllPositions()
            }
        }
        if (view is NestedScrollView) {
            Log.d(TAG, "Found ScrollView: $view")
            view.setOnScrollChangeListener { _, _, _, _, _ ->
                Log.d(TAG, "ScrollView scrolled, updating redactions")
                redactionManager?.updateAllPositions()
            }
        }
        if (view is RecyclerView) {
            Log.d(TAG, "Found RecyclerView: $view")
            view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    Log.d(TAG, "RecyclerView scrolled, updating redactions")
                    redactionManager?.updateAllPositions()
                }
            })
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupScrollViewListeners(view.getChildAt(i))
            }
        }
    }

    private fun setupKeyboardHeightDetection() {
        keyboardHeightListener?.let {
            (context_activity as? AppCompatActivity)?.findViewById<ViewGroup>(android.R.id.content)?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
        }

        val contentView = (context_activity as? AppCompatActivity)?.findViewById<ViewGroup>(android.R.id.content)
        if (contentView != null) {
            keyboardHeightListener = ViewTreeObserver.OnGlobalLayoutListener {
                val rect = Rect()
                contentView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = contentView.rootView.height
                val keypadHeight = screenHeight - rect.bottom

                if (keypadHeight > screenHeight * 0.15) { // Threshold to detect keyboard
                    keyboardHeight = keypadHeight
                    Log.d(TAG, "Keyboard detected, height: $keyboardHeight")
                } else {
                    keyboardHeight = 0
                    Log.d(TAG, "No keyboard detected, height set to 0")
                }
                // Notify service to update keyboard height (if bound)
                if (mediaProjectionServiceIsBound) {
                    MediaProjectionService.keyboardHeight = keyboardHeight
                }
            }
            contentView.viewTreeObserver.addOnGlobalLayoutListener(keyboardHeightListener)
        }
    }

    fun stopScreenShare() {
        Log.d(TAG, "Ending Screenshare")
        cleanup()
        notifySessionStatus(0)
        status_ScreenShare = 0
        firstTimeCall = 0
    }

    private fun requestScreenCapture() {
        val projectionManager =
            context?.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        (context_activity as AppCompatActivity).startActivityForResult(intent, RC_SCREEN_CAPTURE)
    }

    val waitTimer = object : CountDownTimer(4000, 1000) {
        override fun onTick(millisUntilFinished: Long) {

        }

        override fun onFinish() {
            circleOverlay?.hideMarker()
        }
    }

    private fun publishScreen() {
        if (customVideoCapturer == null) {
            Log.d(TAG, "Creating Custom Video Capturer")
            customVideoCapturer = CustomVideoCapturer(context_activity!!)
        }

        if (publisherScreen == null) {
            Log.d(TAG, "Creating Publisher (Screen)")
            publisherScreen = Publisher.Builder(context)
                .capturer(customVideoCapturer)
                .audioTrack(false)
                .frameRate(Publisher.CameraCaptureFrameRate.FPS_30) // Increase frame rate
                .build()
            publisherScreen!!.publisherVideoType =
                PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen
            publisherScreen!!.audioFallbackEnabled = false
            publisherScreen!!.setPublisherListener(publisherListener)

            Log.d(TAG, "Publishing Screen")
            session?.publish(publisherScreen)
            notifySessionStatus(1) // Notify here
            status_ScreenShare = 1

        }
    }

    fun showGlassDialog(sessionCode: String) {
        if (context_activity != null && sessionCode != "") {
            session_dialog = SessionCodeDialog(
                context = context_activity!!,
                title = sessionCode,
                message = "Please Share the code to executive",
                cancelText = "Cancel",
                confirmText = "Yes",
                onConfirmClick = { println("Confirmed") },
                onCancelClick = { stopScreenShare() },
//                    customView = customDialog
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
        if (session != null) {
            notifySessionStatus(1)
        }
    }

    fun disconnect() {
        unpublishScreen()
        try {
            session?.let {
                it.setSessionListener(null)
                it.disconnect() // This might throw, but we catch it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during session disconnect: ${e.localizedMessage}")
        } finally {
            session = null
            Log.d(TAG, "Session forcibly cleaned up")
        }
    }

    fun cleanup() {
        firstTimeCall = 0
        disconnect()
        if (mediaProjectionServiceIsBound) {
            context?.unbindService(connection)
            mediaProjectionServiceIsBound = false
        }
        overlayContainer?.let {
            try {
                windowManager?.removeView(it)
                Log.d(TAG, "Overlay container removed")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay container: ${e.message}")
            }
            overlayContainer = null
            circleOverlay = null
            redactionManager = null
        }
        keyboardHeightListener?.let {
            (context_activity as? AppCompatActivity)?.findViewById<ViewGroup>(android.R.id.content)?.viewTreeObserver?.removeOnGlobalLayoutListener(it)
            keyboardHeightListener = null
        }
    }

    var sessionListener = object : Session.SessionListener {
        override fun onConnected(p0: Session?) {
            Log.d(TAG, "Session Connected")
            pd?.hide()
            if (sessionCodeInterface != null) {
                sessionCodeInterface!!.onSessionCode(matched_session_code)
            } else {
                showGlassDialog(matched_session_code)
            }
            notifySessionStatus(1)
            status_ScreenShare = 1
        }

        override fun onDisconnected(p0: Session?) {
            Log.d(TAG, "Session Disconnected")
            pd?.hide()
            notifySessionStatus(0)
            status_ScreenShare = 0
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
            Toast.makeText(
                context_activity,
                "Session Error: ${p1?.message ?: "null error message"}",
                Toast.LENGTH_SHORT
            ).show()

            // Safe cleanup
            if (session != null) {
                try {
                    session?.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during forced disconnect: ${e.message}")
                }
                session = null
            }
        }
    }

    // Connection listener
    val connectionListener = object : Session.ConnectionListener {
        override fun onConnectionCreated(session: Session?, connection: Connection?) {
            Log.d("OpenTok", "Connection created: ${connection?.connectionId}")
        }

        override fun onConnectionDestroyed(session: Session?, connection: Connection?) {
            Log.d("OpenTok", "Connection destroyed: ${connection?.connectionId}")
            stopScreenShare()
            sessionCodeInterface!!.onSessionDisconnected(true)
        }
    }

    var signalListener = object : Session.SignalListener {
        override fun onSignalReceived(p0: Session?, p1: String?, p2: String?, p3: Connection?) {
            if (isCurrentAppIsVisible) {
                when (p1) {
                    "screenshare" -> {
                        var actionSignal = Gson().fromJson(p2, SignalBaseResponse::class.java)
                        if (actionSignal.action == Utils.codeRequested) {
                            Log.d(TAG, "Code Requested signal received")
                            var code = Gson().fromJson(p2, CodeRequestedResponse::class.java)
                            Log.e("here", code.toString())
                            if (code.value == matched_session_code) {
                                if (sessionCodeInterface != null) {
                                    sessionCodeInterface!!.onSessionConnected(true)
                                } else {
                                    session_dialog?.dismiss()
                                }
                                startPublishScreen()
                            }
                        }
                        if (actionSignal.action == Utils.draw) {
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
                                val drawEndSignal =
                                    Gson().fromJson(drawSignal.value, DrawEndSignal::class.java)
                                Log.d(
                                    TAG,
                                    "DrawEndSignal parsed: action=${drawEndSignal.action}, eventId=${drawEndSignal.eventId}, order=${drawEndSignal.order}, totalChunks=${drawEndSignal.totalChunks}"
                                )
                                val eventId = drawEndSignal.eventId
                                val chunks = drawChunks.getOrPut(eventId) { mutableListOf() }
                                chunks.add(drawEndSignal)
                                Log.d(
                                    TAG,
                                    "Stored chunk ${drawEndSignal.order + 1}/${drawEndSignal.totalChunks} for event $eventId"
                                )


                                if (chunks.size == drawEndSignal.totalChunks) {
                                    val sortedChunks = chunks.sortedBy { it.order }
                                    val combinedBase64 = sortedChunks.joinToString("") { it.value }
                                    val decodedBytes = android.util.Base64.decode(
                                        combinedBase64,
                                        android.util.Base64.DEFAULT
                                    )
                                    val decodedJson = String(decodedBytes, Charsets.UTF_8)
                                    Log.d(TAG, "Combined decoded JSON: $decodedJson")
                                    val pathData =
                                        Gson().fromJson(decodedJson, PathDrawData::class.java)
                                    drawPath(pathData)
                                    drawChunks.remove(eventId)
                                }

                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing draw signal: ${e.message}", e)
                            }
                        }
                        if (actionSignal.action == Utils.MARKER_MOVE) {
                            try {
                                val markerMoveSignal =
                                    Gson().fromJson(p2, MarkerMoveResponse::class.java)
                                Log.e("here", markerMoveSignal.toString())
                                val markerValue = markerMoveSignal.value

                                circleOverlay?.showMarker(
                                    markerValue.x.toFloat(),
                                    markerValue.y.toFloat(),
                                    markerValue.userName,
                                    markerValue.scale
                                )

                                waitTimer.cancel()
                                waitTimer.start()

//                            // Hide marker after 4 seconds
//                                Handler(Looper.getMainLooper()).postDelayed({
//                                    circleOverlay?.hideMarker()
//                                }, 4000)
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
    }

    private fun drawPath(pathData: PathDrawData) {
        circleOverlay?.let {
            if (pathData.type == "path") {
                it.showPath(pathData)
                Log.d(
                    TAG,
                    "Path drawn with stroke: ${pathData.stroke}, path size: ${pathData.path.size}"
                )
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
        if (firstTimeCall == 0) {
            try {
                val payload = JSONObject().apply {
                    put("brand", android.os.Build.BRAND)
                    put("model", android.os.Build.MODEL)
                    put("width", width)
                    put("height", height)
                }
                val signalData = JSONObject().apply {
                    put("type", "ScreenDetails")
                    put("payload", payload)
                }
                val signalString = signalData.toString()
                session?.sendSignal("screenshare", signalString)
                firstTimeCall++
                Log.d(TAG, "Sent signal: $signalString")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating ScreenDetails signal: ${e.message}", e)
            }
        }

        widthScreen = width
        heightScreen = height
    }

    override fun deleteService() {
        onDestroyService()
    }
    override fun getRedactionManager(): RedactionManager? {
        return redactionManager
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

    // Paint for the existing path
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    // Paint for text (used for both marker and cursor names)
    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private val textPaintCursor = Paint().apply {
        color = Color.GREEN
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    // Paint for the existing marker circle
    private val circlePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    // Paint for the new cursor
    private val cursorPaint = Paint().apply {
        color = Color.GREEN // Distinct color for the cursor
        style = Paint.Style.STROKE // Filled circle
        strokeWidth = 5f
        isAntiAlias = true
    }

    // Existing variables for path and marker
    private var path = Path()
    private var shouldDrawPath = false
    private var shouldDrawMarker = false
    private var markerX: Float = 0f
    private var markerY: Float = 0f
    private var markerRadius: Float = 50f
    private var markerName: String = ""

    private var lastCanvasScaleX: Float = 1.0f
    private var lastCanvasScaleY: Float = 1.0f
    private var lastOffsetX: Float = 0.0f
    private var lastOffsetY: Float = 0.0f
    private var lastWebWidth: Float = 0f
    private var lastWebHeight: Float = 0f

    // Variables for the new cursor
    private var shouldDrawCursor = false
    private var cursorX: Float = 0f
    private var cursorY: Float = 0f
    private val cursorRadius: Float = 20f // Smaller radius for the cursor
    private var cursorName: String = "" // Name for the cursor

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw the existing path
        if (shouldDrawPath) {
            canvas.drawPath(path, paint)
            Log.d("CircleOverlayView", "Drawing path on canvas")
        }

        // Draw the existing marker
        if (shouldDrawMarker) {
            canvas.drawCircle(markerX, markerY, markerRadius, circlePaint)
            canvas.drawText(markerName, markerX, markerY - markerRadius - 10f, textPaint)
            Log.d(
                "CircleOverlayView",
                "Drawing marker at (x,y) ($markerX, $markerY) with name: $markerName"
            )
        }

        // Draw the new cursor with its name
        if (shouldDrawCursor) {
            canvas.drawCircle(cursorX, cursorY, cursorRadius, cursorPaint)
            canvas.drawText(cursorName, cursorX, cursorY - cursorRadius - 10f, textPaintCursor)
            Log.d(
                "CircleOverlayView",
                "Drawing cursor at ($cursorX, $cursorY) with name: $cursorName"
            )
        }

        // Log when nothing is drawn
        if (!shouldDrawPath && !shouldDrawMarker && !shouldDrawCursor) {
            Log.d("CircleOverlayView", "Nothing to draw")
        }
    }

    // Existing method to show path
    /*fun showPath(pathData: ScreenShareComponent.PathDrawData) {
        paint.color = Color.parseColor(pathData.stroke ?: "#FF0000")
        paint.strokeWidth = pathData.strokeWidth.toFloat()
        path.reset()
        var scaleX = 0.0f
        var scaleY = 0.0f
        var offsetX = 0.0f
        var offsetY = 0.0f

        if (width > height) {
            scaleX = 1.95f
            scaleY = 1.15f
            // Apply offsets from pathData
            offsetX = pathData.left / scaleX
            offsetY = pathData.top / scaleY
        } else {
            scaleX = 1.95f
            scaleY = 1.05f
            // Apply offsets from pathData
            offsetX = pathData.left * scaleX
            offsetY = pathData.top * scaleY
        }


        Log.d(
            "CircleOverlayView",
            "Showing path: stroke=${pathData.stroke}, strokeWidth=${pathData.strokeWidth}, " +
                    "left=$offsetX, top=$offsetY"
        )

        pathData.path.forEach { command ->
            try {
                when (command[0] as String) {
                    "M" -> {

                        path.moveTo(
                            command[1].toString().toFloat() * scaleX + offsetX,
                            command[2].toString().toFloat() * scaleY + offsetY
                        )
                    }

                    "Q" -> {
                        path.quadTo(
                            command[1].toString().toFloat() * scaleX + offsetX,
                            command[2].toString().toFloat() * scaleY + offsetY,
                            command[3].toString().toFloat() * scaleX + offsetX,
                            command[4].toString().toFloat() * scaleY + offsetY
                        )
                    }

                    "L" -> {
                        path.lineTo(
                            command[1].toString().toFloat() * scaleX + offsetX,
                            command[2].toString().toFloat() * scaleY + offsetY
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "CircleOverlayView",
                    "Error processing path command: $command, ${e.message}",
                    e
                )
            }
        }

        shouldDrawPath = true
        invalidate()
    }*/

    fun showPath(pathData: ScreenShareComponent.PathDrawData) {
        paint.color = Color.parseColor(pathData.stroke ?: "#FF0000")
        paint.strokeWidth = pathData.strokeWidth.toFloat() + 5
        path.reset()

        // Source dimensions (web screen)
        val originalWidth = Utils.originalWith.toFloat()
        val originalHeight = Utils.originalHeight.toFloat()

        // Destination dimensions (Android canvas)
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        // Calculate dynamic scaling factors
        val scaleX = viewWidth / originalWidth
        val scaleY = viewHeight / originalHeight

        // Log dimensions for debugging
        Log.d(
            "CircleOverlayView",
            "Original: $originalWidth x $originalHeight | View: $viewWidth x $viewHeight"
        )
        Log.d("CircleOverlayView", "ScaleX: $scaleX | ScaleY: $scaleY")

        val webWidth = pathData.width
        val webHeight = pathData.height
        val offsetX = pathData.left
        val offsetY = pathData.top

        // Update last used transformation values
        lastCanvasScaleX = scaleX
        lastCanvasScaleY = scaleY
        lastOffsetX = offsetX
        lastOffsetY = offsetY
        lastWebWidth = webWidth
        lastWebHeight = webHeight

        // Transform and draw the path
        pathData.path.forEach { command ->
            try {
                when (command[0] as String) {
                    "M" -> {
                        val x = (command[1] as Number).toFloat() * scaleX
                        val y = (command[2] as Number).toFloat() * scaleY
                        path.moveTo(x, y)
                    }

                    "L" -> {
                        val x = (command[1] as Number).toFloat() * scaleX
                        val y = (command[2] as Number).toFloat() * scaleY
                        path.lineTo(x, y)
                    }

                    "Q" -> {
                        val cx = (command[1] as Number).toFloat() * scaleX
                        val cy = (command[2] as Number).toFloat() * scaleY
                        val x = (command[3] as Number).toFloat() * scaleX
                        val y = (command[4] as Number).toFloat() * scaleY
                        path.quadTo(cx, cy, x, y)
                    }

                    else -> Log.w("CircleOverlayView", "Unsupported path command: ${command[0]}")
                }
            } catch (e: Exception) {
                Log.e("CircleOverlayView", "Error parsing path command: $command", e)
            }
        }

        shouldDrawPath = true
        invalidate()
    }

    // Existing method to show marker
    fun showMarker(x: Float, y: Float, userName: String, scale: Float) {
        markerX = x / scale
        markerY = y / scale
        markerName = userName
        markerRadius = 40f * scale
        shouldDrawMarker = true
        invalidate()
    }

    // Existing method to hide path
    fun hidePath() {
        shouldDrawPath = false
        invalidate()
        Log.d("CircleOverlayView", "Path hidden")
    }

    // Existing method to hide marker
    fun hideMarker() {
        shouldDrawMarker = false
        invalidate()
        Log.d("CircleOverlayView", "Marker hidden")
    }

    // Updated method to show cursor with a name
    fun showCursor(x: Float, y: Float, name: String) {
        cursorX = x
        cursorY = y
        cursorName = name
        shouldDrawCursor = true
        invalidate()
        Log.d("CircleOverlayView", "Showing cursor at ($x, $y) with name: $name")
    }

    // Updated method to hide cursor
    fun hideCursor() {
        shouldDrawCursor = false
        cursorName = "" // Clear the name when hiding
        invalidate()
        Log.d("CircleOverlayView", "Cursor hidden")
    }
}

interface ConnectingSession {
    fun onSessionCode(sessionCode: String)
    fun onSessionConnected(isConnected: Boolean)
    fun onSessionDisconnected(isConnected: Boolean)
}