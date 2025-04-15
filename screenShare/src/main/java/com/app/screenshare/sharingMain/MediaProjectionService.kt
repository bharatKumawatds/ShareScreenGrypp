package com.app.screenshare.sharingMain

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.app.screenshare.R
import java.nio.ByteBuffer

class MediaProjectionService : Service(), ImageReader.OnImageAvailableListener {
    companion object {
        const val TAG = "MediaProjectionService"

        const val FOREGROUND_SERVICE_ID = 1234
        const val NOTIFICATION_CHANNEL_ID = "com.app.screenshare.sharingMain.MediaProjectionService"
        const val NOTIFICATION_CHANNEL_NAME = "Grypp"

        const val SCREEN_CAPTURE_NAME = "screencapture"
        const val MAX_SCREEN_AXIS = 1280 // Cap resolution to avoid encoder issues
        const val VIRTUAL_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC

        var width = 240
        var height = 320
    }

    private var binder: MediaProjectionBinder? = null
    private var binderIntent: Intent? = null
    private var density = 0
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaProjection: MediaProjection? = null

    override fun onBind(intent: Intent?): IBinder? {
        if (intent == null) {
            Log.e(TAG, "Received null intent in onBind")
            return null
        }

        val data = intent.getParcelableExtra<Intent>("data")
        if (data == null) {
            Log.e(TAG, "No data found in intent")
            return null
        }

        Log.d(TAG, "onBind: Received valid intent with data: $data")
        binder = MediaProjectionBinder()
        binderIntent = intent

        // Start foreground service with notification
        val notification = createNotification()
        startForeground(FOREGROUND_SERVICE_ID, notification)

        // Get MediaProjection
        Log.d(TAG, "Initializing MediaProjection")
        val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED)
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        try {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "Failed to create MediaProjection")
                stopSelf()
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating MediaProjection: ${e.message}", e)
            stopSelf()
            return null
        }

        // Get display metrics
        Log.d(TAG, "Getting display metrics")
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)

        // Get real display size
        val size = Point()
        display.getRealSize(size)
        Log.d(TAG, "Raw display size: ${size.x}x${size.y}")

        // Adjust resolution
        width = size.x
        height = size.y
        adjustResolution()
        density = metrics.densityDpi
        Log.d(TAG, "Adjusted resolution: ${width}x${height}, density: $density")

        // Create virtual display
        createVirtualDisplay()

        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind")
        cleanupResources()
        binderIntent = null
        binder = null
        stopForeground(true)
        stopSelf()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        return START_NOT_STICKY // Prevent automatic restart
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        cleanupResources()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved")
        binder?.mediaProjectionHandler?.deleteService()
        cleanupResources()
        val pid = Process.myPid()
        Process.killProcess(pid)
        super.onTaskRemoved(rootIntent)
    }

    private val mediaProjectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection stopped")
            cleanupResources()
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            Log.d(TAG, "Captured content resized: ${width}x${height}")
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            Log.d(TAG, "Captured content visibility changed: $isVisible")
        }
    }

    private fun adjustResolution() {
        // Ensure resolution is divisible by 16 (required by most hardware encoders)
        width = (width + 15) and 0xFFFFFFF0.toInt()
        height = (height + 15) and 0xFFFFFFF0.toInt()

        // Cap resolution to avoid encoder overload
        if (width > MAX_SCREEN_AXIS || height > MAX_SCREEN_AXIS) {
            val aspectRatio = width.toFloat() / height
            if (width > height) {
                width = MAX_SCREEN_AXIS
                height = (MAX_SCREEN_AXIS / aspectRatio).toInt()
            } else {
                height = MAX_SCREEN_AXIS
                width = (MAX_SCREEN_AXIS * aspectRatio).toInt()
            }
            width = (width + 15) and 0xFFFFFFF0.toInt()
            height = (height + 15) and 0xFFFFFFF0.toInt()
        }
        Log.d(TAG, "Final adjusted resolution: ${width}x${height}")
    }

    private fun createVirtualDisplay() {
        Log.i(TAG, "Creating VirtualDisplay [${width}x${height}]")
        try {
            // Create ImageReader
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            mediaProjection?.registerCallback(mediaProjectionCallback, null)

            // Create VirtualDisplay
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                SCREEN_CAPTURE_NAME,
                width,
                height,
                density,
                VIRTUAL_DISPLAY_FLAGS,
                imageReader?.surface,
                null,
                null
            ) ?: run {
                Log.e(TAG, "Failed to create VirtualDisplay")
                return
            }

            // Set ImageReader listener
            val handler = Handler(Looper.getMainLooper())
            imageReader?.setOnImageAvailableListener(this, handler)
            Log.d(TAG, "VirtualDisplay and ImageReader initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating VirtualDisplay: ${e.message}", e)
            cleanupResources()
        }
    }

    override fun onImageAvailable(reader: ImageReader?) {
        val image = reader?.acquireLatestImage() ?: return
        image.use {
            try {
                val imagePlane = image.planes[0]
                val imageBuffer = imagePlane.buffer
                val rowStride = imagePlane.rowStride
                val pixelStride = imagePlane.pixelStride
                val computedWidth = rowStride / pixelStride
                Log.v(TAG, "Image available: ${computedWidth}x${image.height}, buffer size: ${imageBuffer.remaining()}")

                // Send frame to WebRTC pipeline
                sendFrame(imageBuffer, computedWidth, image.height)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}", e)
            }
        }
    }

    private fun sendFrame(imageBuffer: ByteBuffer, width: Int, height: Int) {
        // Optional: Convert RGBA to YUV if required by encoder
        // val yuvBuffer = convertRgbaToYuv(imageBuffer, width, height)
        binder?.mediaProjectionHandler?.sendFrame(imageBuffer, width, height)
    }

    /*
    // Placeholder for RGBA to YUV conversion (if needed)
    private fun convertRgbaToYuv(rgbaBuffer: ByteBuffer, width: Int, height: Int): ByteBuffer {
        // Implement using libyuv or manual conversion
        // Example: LibYUV.ARGBToI420(rgbaBuffer, width, height, yuvBuffer, ...)
        Log.w(TAG, "RGBA to YUV conversion not implemented")
        return rgbaBuffer // Replace with actual conversion
    }
    */

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Grypp Projection Service"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.grypp)
                .setContentTitle("Grypp")
                .setContentText("Running screenShare")
                .build()
        } else {
            Notification.Builder(this)
                .setSmallIcon(R.drawable.grypp)
                .setContentTitle("Grypp")
                .setContentText("Running screenShare")
                .setPriority(Notification.PRIORITY_DEFAULT)
                .build()
        }
    }

    private fun cleanupResources() {
        Log.d(TAG, "Cleaning up resources")
        try {
            imageReader?.setOnImageAvailableListener(null, null)
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ImageReader: ${e.message}", e)
        }
        try {
            virtualDisplay?.release()
            virtualDisplay = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing VirtualDisplay: ${e.message}", e)
        }
        try {
            mediaProjection?.unregisterCallback(mediaProjectionCallback)
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaProjection: ${e.message}", e)
        }
    }
}