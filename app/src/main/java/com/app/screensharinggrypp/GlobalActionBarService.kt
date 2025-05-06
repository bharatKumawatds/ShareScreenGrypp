package com.app.screensharinggrypp;

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
                getScreenShareComponent().pauseSession()
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
        buttonParams.setMargins(10, 10, 10, 10) // 10dp margins
        yesButton.layoutParams = buttonParams
        noButton.setOnClickListener { v: View? ->
            run {
                getScreenShareComponent().resumeSession()
                removeDialog()
            }
        }
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