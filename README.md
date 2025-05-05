Steps To Integrate Screen Sharing Using Grypp

Step - 1 implementation "com.github.bharatKumawatds:ShareScreenGrypp:v1.0.0" in app level build.gradle file

Step - 2 in Manifest define config Changes in Mainfest 
![Screenshot 2025-05-05 at 3 38 06 PM](https://github.com/user-attachments/assets/c1778d7e-db48-44f3-aa39-0f50632def82)

Step - 3 Create a Application Class and Intlize ScreenShareComponent

*like this*
![Screenshot 2025-05-05 at 5 21 26 PM](https://github.com/user-attachments/assets/2a4bc08b-3fbd-48ff-a19a-1d79398343af)

class MainApplication : Application() {
    var screenShareComponent: ScreenShareComponent? = null
    companion object {
        private lateinit var instance: MainApplication

        fun getScreenShareComponent(): ScreenShareComponent {
            return instance.screenShareComponent
                ?: throw IllegalStateException("ScreenShareComponent not initialized")
        }
    }
    override fun onCreate() {
        super.onCreate()
        instance  = this
        val sensitiveTags = listOf(
            "password", "pass", "credit", "card",
            "stripe", "payment", "checkout"
        )
        //paste the grypp key 
        screenShareComponent = ScreenShareComponent(this, ProcessLifecycleOwner.get().lifecycle,"grypp_live_xK2P9M234qVb3Wz6JtD4RfXyE8Nc320Q5",sensitiveTags)
    }
}


Step - 4 Create Base Actvity
Note - All Activity Must be inherit with BaseActivity

*like this*


open class BaseActivity:AppCompatActivity(), SessionStatusListener {
    var layoutListener: OnGlobalLayoutListener? = null
    private var rootView: ViewGroup? = null

    override fun onResume() {
        super.onResume()
        MainApplication.getScreenShareComponent().setSessionStatusListener(this)
        MainApplication.getScreenShareComponent().attachActivity(this)
        rootView = findViewById(android.R.id.content)
        if (rootView == null) {
            Log.e("BaseActivity", "Root view is null. Ensure setContentView is called.")
            return
        }


        // Apply initial redaction after view is laid out
        rootView!!.post(Runnable {
            if (!isFinishing) {
                MainApplication.getScreenShareComponent().autoDetectAndRedactSensitiveViews(rootView!!)
            }
        })


        // Set up global layout listener for view hierarchy changes
        layoutListener = OnGlobalLayoutListener {
            if (!isFinishing) {
                rootView?.post {
                    if (!isFinishing) {
                        MainApplication.getScreenShareComponent()
                            .autoDetectAndRedactSensitiveViews(rootView!!)
                    }
                }
            }
        }
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onPause() {
        super.onPause()
        val rootView = findViewById<View>(android.R.id.content) as ViewGroup
        if (layoutListener != null) {
            rootView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
        layoutListener = null
        MainApplication.getScreenShareComponent().clearRedactions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MainApplication.getScreenShareComponent().handlePermissionResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        MainApplication.getScreenShareComponent().handleActivityResult(requestCode, resultCode, data)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        MainApplication.getScreenShareComponent().onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        MainApplication.getScreenShareComponent().onConfigurationChanged(newConfig)
        super.onConfigurationChanged(newConfig)

    }

    override fun onSessionStatusChanged(status: Int) {
        println("Session Status: $status")
    }
    override fun onDestroy() {
        super.onDestroy()
        MainApplication.getScreenShareComponent().setSessionStatusListener(null)
    }
}

Step - 5 Create a overlay button(Can Also Customize this overlay button)
Note - Mention the service in mainfest like this 
<service
            android:name=".GlobalActionBarService"
            android:enabled="true"
            android:exported="false" />

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ProcessLifecycleOwner
import com.app.screenshare.sharingMain.ConnectingSession
import com.app.screensharinggrypp.MainApplication.Companion.getScreenShareComponent
import kotlin.math.abs


class GlobalActionBarService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayButton: Button? = null
    private var params: WindowManager.LayoutParams? = null
    private var dialogLayout: LinearLayout? = null
    private var dialogParams: WindowManager.LayoutParams? = null
    private var lifecycleObserver: AppLifecycleObserver? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize lifecycle observer to stop service when app is in background
        lifecycleObserver = AppLifecycleObserver(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver!!)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayButton = Button(this)
        overlayButton!!.text = "GRYPP"
        overlayButton!!.setTextColor(-0x1) // White text
        overlayButton!!.setPadding(20, 5, 20, 5)
        overlayButton!!.setBackgroundResource(R.drawable.round_button_bg)
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params!!.gravity = Gravity.TOP or Gravity.START
        params!!.x = 180
        params!!.y = 20

        // Make the button draggable
        overlayButton!!.setOnTouchListener(object : OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params!!.x
                        initialY = params!!.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        params!!.x = initialX + (event.rawX - initialTouchX).toInt()
                        params!!.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager!!.updateViewLayout(overlayButton, params)
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        if (abs((event.rawX - initialTouchX).toDouble()) < 5 && abs((event.rawY - initialTouchY).toDouble()) < 5) {
                            v.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        // Add click action
        overlayButton!!.setOnClickListener { v: View? ->
            if (getScreenShareComponent().status_ScreenShare == 0) {
                // Start screen sharing and show session code dialog
                val session: ConnectingSession = object : ConnectingSession {
                    override fun onSessionDisconnected(isConnected: Boolean) {
                        overlayButton!!.text = "GRYPP"
                    }

                    override fun onSessionConnected(isConnected: Boolean) {
                        // Remove dialog when session is connected
                        removeDialog()
                        overlayButton!!.text = "Capturing Screen"
                    }

                    override fun onSessionCode(sessionCode: String) {
                        showSessionCodeDialog(sessionCode)
                    }
                }
                getScreenShareComponent().startScreenShare(session)
            } else {
                // Show end session dialog
                showEndSessionDialog()
            }
        }

        windowManager!!.addView(overlayButton, params)
    }

    private fun showSessionCodeDialog(sessionCode: String) {
        // Create dialog layout
        dialogLayout = LinearLayout(this)
        dialogLayout!!.orientation = LinearLayout.VERTICAL
        dialogLayout!!.setBackgroundColor(-0x1) // White background
        dialogLayout!!.setPadding(20, 20, 20, 20)

        // Title
        val title = TextView(this)
        title.text = "Connect Session"
        title.setTextColor(-0x1000000)
        title.textSize = 20f
        title.setPadding(0, 0, 0, 20)
        dialogLayout!!.addView(title)

        // Message
        val message = TextView(this)
        message.text = "Your session code is $sessionCode"
        message.setTextColor(-0x1000000)
        message.setPadding(0, 0, 0, 20)
        dialogLayout!!.addView(message)

        // Cancel button
        val cancelButton = Button(this)
        cancelButton.text = "Cancel"
        cancelButton.setTextColor(-0x1)
        cancelButton.setBackgroundResource(R.drawable.round_btn_red)
        cancelButton.setPadding(10, 10, 10, 20)
        cancelButton.setOnClickListener { v: View? ->
            getScreenShareComponent().stopScreenShare()
            removeDialog()
        }
        dialogLayout!!.addView(cancelButton)

        // Dialog layout params
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )

        dialogParams!!.gravity = Gravity.CENTER
        dialogParams!!.dimAmount = 0.5f // Dim the background

        // Add dialog to window
        windowManager!!.addView(dialogLayout, dialogParams)
    }

    private fun showEndSessionDialog() {
        // Create dialog layout
        dialogLayout = LinearLayout(this)
        dialogLayout!!.orientation = LinearLayout.VERTICAL
        dialogLayout!!.setBackgroundColor(-0x1) // White background
        dialogLayout!!.setPadding(20, 20, 20, 20)

        // Title
        val title = TextView(this)
        title.text = "End Session"
        title.setTextColor(-0x1000000)
        title.textSize = 20f
        title.setPadding(20, 10, 20, 20)
        dialogLayout!!.addView(title)

        // Message
        val message = TextView(this)
        message.text = "Do you want to end the current session?"
        message.setTextColor(-0x1000000)
        dialogLayout!!.addView(message)

        // Buttons layout
        val buttonLayout = LinearLayout(this)
        buttonLayout.orientation = LinearLayout.HORIZONTAL
        buttonLayout.gravity = Gravity.END

        // Yes button
        val yesButton = Button(this)
        yesButton.text = "Yes"
        yesButton.setTextColor(-0x1)
        yesButton.setBackgroundResource(R.drawable.round_button_bg)
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        buttonParams.setMargins(10, 10, 10, 10) // 10dp margins
        yesButton.layoutParams = buttonParams
        yesButton.setOnClickListener { v: View? ->
            overlayButton!!.text = "GRYPP"
            getScreenShareComponent().stopScreenShare()
            removeDialog()
        }
        buttonLayout.addView(yesButton)

        // No button
        val noButton = Button(this)
        noButton.text = "No"
        noButton.setTextColor(-0x1)
        noButton.setBackgroundResource(R.drawable.round_btn_red)
        noButton.setPadding(40, 10, 10, 20)
        noButton.setOnClickListener { v: View? -> removeDialog() }
        noButton.layoutParams = buttonParams
        buttonLayout.addView(noButton)

        dialogLayout!!.addView(buttonLayout)

        // Dialog layout params
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        dialogParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )

        dialogParams!!.gravity = Gravity.CENTER
        dialogParams!!.dimAmount = 0.5f // Dim the background

        // Add dialog to window
        windowManager!!.addView(dialogLayout, dialogParams)
    }

    private fun removeDialog() {
        if (dialogLayout != null) {
            windowManager!!.removeView(dialogLayout)
            dialogLayout = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayButton != null) {
            windowManager!!.removeView(overlayButton)
            overlayButton = null
        }
        removeDialog() // Ensure dialog is removed when service stops
        windowManager = null
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }


    fun hideOverlayButton() {
        removeDialog()

        if (overlayButton != null && overlayButton!!.windowToken != null) {
            overlayButton!!.visibility = View.GONE
        }
    }

    fun showOverlayButton() {
        if (overlayButton != null) {
            overlayButton!!.visibility = View.VISIBLE
        }
    }
} 

Step - 6 Create an AppLifeCycle Observer to hide or show overlay button 


import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner


class AppLifecycleObserver(private val globalService: GlobalActionBarService?) :
    DefaultLifecycleObserver {
    override fun onStop(owner: LifecycleOwner) {
        globalService?.hideOverlayButton()
    }

    override fun onStart(owner: LifecycleOwner) {
        globalService?.showOverlayButton()
    }
}

Step - 7 Ask Permission For Overlay and start GlobalActionBarService 


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


For Draw Annotation and Senstive View All actvity must inherit with BaseActivity

