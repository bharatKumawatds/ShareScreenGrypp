package com.app.screenshare.sharingMain

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
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
        const val MAX_SCREEN_AXIS = 1024
        const val VIRTUAL_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
    }

    private var binder: MediaProjectionBinder? = null
    private var binderIntent: Intent? = null

    private var width = 240
    private var height = 320
    private var density = 0

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private var mediaProjection: MediaProjection ?= null

    override fun onBind(intent: Intent?): IBinder? {
        if(intent != null){
            val data = intent.getParcelableExtra("data") ?: Intent()
            if (data == null) {
                println("No data found (null)")
                // Handle the null case
                return null
            } else if (data is Intent && data.extras == null && data.action == null) {
                println("Received an empty Intent: $data")
                // Handle the empty Intent case
                return null
            } else {
                println("Received valid data: $data")
                Log.d(TAG, "On Bind")
                binder = MediaProjectionBinder()
                binderIntent = intent

                val notification = createNotification()
                startForeground(FOREGROUND_SERVICE_ID, notification)

                // Get Media Projection
                Log.d(TAG, "Getting Media Projection")
                val resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: 0


                val projectionManager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                mediaProjection = projectionManager.getMediaProjection(resultCode, data)

                // Get Display
                Log.d(TAG, "Getting Display")
                val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val display = windowManager.defaultDisplay

                // Metrics
                Log.d(TAG, "Getting Metrics")
                val metrics = DisplayMetrics()
                display.getMetrics(metrics)

                // Size
                Log.d(TAG, "Getting Size")
                val size = Point()
                display.getRealSize(size)
                Log.d(TAG, "Size: ${size.x} x ${size.y}")
                resizeDisplaySizes(size.x, size.y)


                // Density
                Log.d(TAG, "Getting Density")
                density = metrics.densityDpi
                Log.d(TAG, "Density: $density")

                // Create Virtual Display
                createVirtualDisplay()

                return binder
            }



        }else{
            return null
        }

    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "On Unbind")
        binderIntent = null
        binder = null

        this.stopForeground(true)
        this.stopSelf()
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "On Start Command")


        // Do not allow system to restart the service (must be started by user)
        return START_NOT_STICKY
    }

    private fun resizeDisplaySizes(newWidth: Int, newHeight: Int) {
        var multiplication = 1.0

        if (newHeight > MAX_SCREEN_AXIS) {
            multiplication = newHeight.toDouble() / MAX_SCREEN_AXIS
        }

        if (newWidth > MAX_SCREEN_AXIS && newWidth > newHeight) {
            multiplication = newWidth.toDouble() / MAX_SCREEN_AXIS
        }

        width = (newWidth.toDouble() / multiplication).toInt()
        height = (newHeight.toDouble() / multiplication).toInt()

        Log.d(TAG, "Display Size resized to [$width x $height]")
    }

    val mediaProjectionCallback: MediaProjection.Callback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            // Release resources if necessary
            if (mediaProjection != null) {
                mediaProjection!!.unregisterCallback(this)
                mediaProjection = null
            }
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            super.onCapturedContentResize(width, height)
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            super.onCapturedContentVisibilityChanged(isVisible)
        }
    }

    private fun createVirtualDisplay() {
        Log.i(TAG, "Creating Virtual Display [$width x $height]")

        // Create Image Reader
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        mediaProjection!!.registerCallback(mediaProjectionCallback, null);
        // Create Virtual Display
        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            SCREEN_CAPTURE_NAME,
            width,
            height,
            density,
            VIRTUAL_DISPLAY_FLAGS,
            imageReader!!.surface,
            null,
            null
        )

        // Setup Image Available Listener
        try {
            Log.d(TAG, "Setting Image Available Listener")
            val handler = Handler(Looper.getMainLooper())
            imageReader!!.setOnImageAvailableListener(this, handler)
        } catch (ex: Exception) {
            Log.e(TAG, ex.message, ex)
        }
    }

    override fun onImageAvailable(reader: ImageReader?) {
        val image = reader?.acquireLatestImage() ?: return
        image.use {
            // Get Image Buffer
            val imagePlane = image.planes[0]
            val imageBuffer = imagePlane.buffer

            // Compute Width (to avoid image distortion on certain devices)
            val rowStride = imagePlane.rowStride
            val pixelStride = imagePlane.pixelStride
            val width = rowStride / pixelStride

            // Send Image Frame Data
            sendFrame(imageBuffer, width, image.height)
        }
    }

    private fun sendFrame(imageBuffer: ByteBuffer, width: Int, height: Int) {
        binder?.mediaProjectionHandler?.sendFrame(imageBuffer, width, height)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Grypp Projection Service"

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
}