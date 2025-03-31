package com.app.screenshare.sharingMain

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle

import androidx.lifecycle.LifecycleOwner
import com.opentok.android.*



import pub.devrel.easypermissions.EasyPermissions
import java.nio.ByteBuffer


class ScreenShareComponent(
    private val context: Context,
    val lifecycle: Lifecycle
) : MediaProjectionHandler,DefaultLifecycleObserver {

    companion object {
        const val TAG = "ScreenShareComponent"
        const val RC_VIDEO_APP_PERM = 124
        const val RC_SCREEN_CAPTURE = 125
        var isCurrentAppIsVisible = false

        var API_KEY = "fd81acbc-dfeb-4e74-b14e-167a1c0fdbe0"
        var SESSION_ID = "1_MX5mZDgxYWNiYy1kZmViLTRlNzQtYjE0ZS0xNjdhMWMwZmRiZTB-fjE3NDMxNjkwMTY1Mzh-MVpZNFJ2akZ3QnNtN0Fwd3RXaUJSb3NWfn5-"
        var TOKEN = "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLXVzZTEucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjoxNDc2NjA0NDE0NDk0MTg0MTMyNDI4OTM3NDYwNDk2NTY4MTg5NjEiLCJ0eXAiOiJKV1QiLCJ4NXUiOiJodHRwczovL2FudWJpcy1jZXJ0cy1jMS11c2UxLnByb2QudjEudm9uYWdlbmV0d29ya3MubmV0L3YxL2NlcnRzL2MyMDI1OTA4NTZjNjg5ZDM0ZmIyZmQzODhmMDNhZTM5In0.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXNoaXNoLnRhbndhckBkb3RzcXVhcmVzLmNvbSIsImdpdmVuX25hbWUiOiJBc2hpc2giLCJmYW1pbHlfbmFtZSI6IlRhbndhciIsInBob25lX251bWJlciI6IjkxODA5NDAwMDE3NyIsInBob25lX251bWJlcl9jb3VudHJ5IjoiSU4iLCJvcmdhbml6YXRpb25faWQiOiI5ODE0MTRhOS0yZmQ0LTRkMTgtYjM3Yi00OGUxZDljYTAwN2IiLCJhdXRoZW50aWNhdGlvbk1ldGhvZHMiOlt7ImNvbXBsZXRlZF9hdCI6IjIwMjUtMDMtMzFUMDY6MjQ6NTQuOTc3NTAwMjgxWiIsIm1ldGhvZCI6ImludGVybmFsIn1dLCJpcFJpc2siOnsicmlza19sZXZlbCI6MH0sInRva2VuVHlwZSI6InZpYW0iLCJhdWQiOiJwb3J0dW51cy5pZHAudm9uYWdlLmNvbSIsImV4cCI6MTc0MzQxNDI5MSwianRpIjoiYzFmZTMzNjctZWUxZS00MTkxLTg2ODYtY2ZjZDc2NWY1ZTk4IiwiaWF0IjoxNzQzNDEzOTkxLCJpc3MiOiJWSUFNLUlBUCIsIm5iZiI6MTc0MzQxMzk3Niwic3ViIjoiNDk2NmNjZDEtNjBlZS00MDExLWExY2EtZDFhNzU3NDZhNmNhIn19LCJmZWRlcmF0ZWRBc3NlcnRpb25zIjp7InZpZGVvLWFwaSI6W3siYXBpS2V5IjoiNzM2NGE4NzgiLCJhcHBsaWNhdGlvbklkIjoiZmQ4MWFjYmMtZGZlYi00ZTc0LWIxNGUtMTY3YTFjMGZkYmUwIiwiZXh0cmFDb25maWciOnsidmlkZW8tYXBpIjp7ImluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3QiOiIiLCJyb2xlIjoibW9kZXJhdG9yIiwic2NvcGUiOiJzZXNzaW9uLmNvbm5lY3QiLCJzZXNzaW9uX2lkIjoiMV9NWDVtWkRneFlXTmlZeTFrWm1WaUxUUmxOelF0WWpFMFpTMHhOamRoTVdNd1ptUmlaVEItZmpFM05ETXhOamt3TVRZMU16aC1NVnBaTkZKMmFrWjNRbk50TjBGd2QzUlhhVUpTYjNOV2ZuNS0ifX19XX0sImF1ZCI6InBvcnR1bnVzLmlkcC52b25hZ2UuY29tIiwiZXhwIjoxNzQ2MDA1OTkyLCJqdGkiOiJmNTVlZTY2OS0yNWQ5LTQzMjUtYWJjNS0yZGM1YjUzNzVkMzIiLCJpYXQiOjE3NDM0MTM5OTIsImlzcyI6IlZJQU0tSUFQIiwibmJmIjoxNzQzNDEzOTc3LCJzdWIiOiI0OTY2Y2NkMS02MGVlLTQwMTEtYTFjYS1kMWE3NTc0NmE2Y2EifQ.u97f6dJ7YWkr4a40CVpKSwm2HHUns-YEkPI0rmHIlVuyD-4WXr9TyTptYVT-1lb0fU2r8pBWhxjXs6GfOLoZHdlY1hSOuLuTvK7MiQ1HSHWIg11KcDTZL0cb4AG8vNL-Sijh5fGPp5eMdXCnhqbBjXpR33c2kWflsiZjnIPgPbc6H5LwHUVUMKLoEp_KwFEO2eaV9MFct4SAAJgbgTbFxa_2z8JRkYsXeJPnOywkYbu3kyYUG7s4TOjX1szMxm-nozPCmMJ7d2dl55KtF5tGDl962a80AUNPPYO9o6LYmBaQGcNwoyr6FdkRTC0BIZ8t6UgpvIGU0HxtcAK4xBN4xA"
    }

    private val session: Session = Session.Builder(context, API_KEY, SESSION_ID).build()
    private var publisherScreen: Publisher? = null
    private var customVideoCapturer: CustomVideoCapturer? = null
    private var mediaProjectionServiceIsBound: Boolean = false
    private var mediaProjectionBinder: MediaProjectionBinder? = null

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

    init {
        lifecycle.addObserver(this)
        requestPermissions()

    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(TAG, "Component: onResume")
        isCurrentAppIsVisible  = true

    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(TAG, "Component: onPause")
        isCurrentAppIsVisible  = false
        cleanup()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.d(TAG, "Component: onDestroy")
        cleanup()
        lifecycle.removeObserver(this)
    }

    private fun requestPermissions() {
        val perms = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.FOREGROUND_SERVICE
        )

        if (EasyPermissions.hasPermissions(context, *perms)) {
            initializeComponent()
        } else {
            EasyPermissions.requestPermissions(
                context as AppCompatActivity,
                "This app needs access to your camera and mic to make video calls",
                RC_VIDEO_APP_PERM,
                *perms
            )
        }
    }

    // Public method for Activity to call with permission results
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, context)
        if (requestCode == RC_VIDEO_APP_PERM && EasyPermissions.hasPermissions(context, *permissions)) {
            initializeComponent()
        }
    }

    // Public method for Activity to call with activity results
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SCREEN_CAPTURE) {
            Handler().postDelayed({
                Log.e("here Checking", isCurrentAppIsVisible.toString())
                if(isCurrentAppIsVisible){
                    session.setSessionListener(sessionListener)
                    session.connect(TOKEN)

                    Handler().postDelayed({
                        val intent = Intent(context, MediaProjectionService::class.java)
                        intent.putExtra("resultCode", resultCode)
                        intent.putExtra("data", data)
                        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    },4000)

                }

            },100)

        }
    }

    private fun initializeComponent() {
        session.setSessionListener(sessionListener)
        session.connect(TOKEN)
    }

    fun startScreenShare(){
        if (publisherScreen == null) {
            Log.d(TAG, "Initiate Screenshare")
            requestScreenCapture()

        }
    }
    fun stopScreenShare(){
        Log.d(TAG, "Ending Screenshare")
        cleanup()
    }



    private fun requestScreenCapture() {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = projectionManager.createScreenCaptureIntent()
        (context as AppCompatActivity).startActivityForResult(intent, RC_SCREEN_CAPTURE)
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
            session.publish(publisherScreen)
        }
    }

    private fun unpublishScreen() {
        if (publisherScreen != null) {
            Log.d(TAG, "Unpublishing Screen")
            session.unpublish(publisherScreen)
            publisherScreen?.capturer?.stopCapture()
            publisherScreen = null
        }
    }

    fun disconnect() {
        unpublishScreen()
        session.disconnect()
    }

    fun cleanup() {
        disconnect()
        if (mediaProjectionServiceIsBound) {
            context.unbindService(connection)
            mediaProjectionServiceIsBound = false
        }
    }
    var sessionListener = object :Session.SessionListener{
        // Session.SessionListener implementations
        override fun onConnected(p0: Session?) {
            Log.d(TAG, "Session Connected")
        }

        override fun onDisconnected(p0: Session?) {
            Log.d(TAG, "Session Disconnected")
        }

        override fun onStreamReceived(p0: Session?, p1: Stream?) {
            Log.d(TAG, "Stream Received")
        }

        override fun onStreamDropped(p0: Session?, p1: Stream?) {
            Log.d(TAG, "Stream Dropped")
        }

        override fun onError(p0: Session?, p1: OpentokError?) {
            Log.e(TAG, "Session Error: ${p1?.message ?: "null error message"}")
        }
    }
    var publisherListener = object:PublisherKit.PublisherListener {
        // PublisherKit.PublisherListener implementations
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
}