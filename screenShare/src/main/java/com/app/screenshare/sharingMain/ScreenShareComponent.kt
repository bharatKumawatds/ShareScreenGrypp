package com.app.screenshare.sharingMain


import android.app.ProgressDialog
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
import com.app.screenshare.model.response.CreateSessionResponse
import com.app.screenshare.service.RestApiBuilder
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


class ScreenShareComponent() : MediaProjectionHandler,DefaultLifecycleObserver {
    var session: Session ?= null
    var context:Context ?= null
    var context_actvity:Context ?= null
    var lifecycle:Lifecycle ?= null
    var session_dialog:SessionCodeDialog ?= null

    constructor(context: Context,lifecycle1: Lifecycle) : this() {
        Log.e("Calling","Constructor")
        this.context = context
        lifecycle = lifecycle1
        lifecycle?.addObserver(this)
    }
    companion object {
        const val TAG = "ScreenShareComponent"
        const val RC_VIDEO_APP_PERM = 124
        const val RC_SCREEN_CAPTURE = 125
        var isCurrentAppIsVisible = false
        var matched_session_code = ""

        var API_KEY = "fd81acbc-dfeb-4e74-b14e-167a1c0fdbe0"
        var SESSION_ID = "2_MX5mZDgxYWNiYy1kZmViLTRlNzQtYjE0ZS0xNjdhMWMwZmRiZTB-fjE3NDM1NzE4NTY5Nzd-bFExRkZtdlVmL3pHeE9pRUx5M21CdEFmfn5-"
        var TOKEN = "eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLXVzZTEucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjoxNDc2NjA0NDE0NDk0MTg0MTMyNDI4OTM3NDYwNDk2NTY4MTg5NjEiLCJ0eXAiOiJKV1QiLCJ4NXUiOiJodHRwczovL2FudWJpcy1jZXJ0cy1jMS11c2UxLnByb2QudjEudm9uYWdlbmV0d29ya3MubmV0L3YxL2NlcnRzL2MyMDI1OTA4NTZjNjg5ZDM0ZmIyZmQzODhmMDNhZTM5In0.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXNoaXNoLnRhbndhckBkb3RzcXVhcmVzLmNvbSIsImdpdmVuX25hbWUiOiJBc2hpc2giLCJmYW1pbHlfbmFtZSI6IlRhbndhciIsInBob25lX251bWJlciI6IjkxODA5NDAwMDE3NyIsInBob25lX251bWJlcl9jb3VudHJ5IjoiSU4iLCJvcmdhbml6YXRpb25faWQiOiI5ODE0MTRhOS0yZmQ0LTRkMTgtYjM3Yi00OGUxZDljYTAwN2IiLCJhdXRoZW50aWNhdGlvbk1ldGhvZHMiOlt7ImNvbXBsZXRlZF9hdCI6IjIwMjUtMDQtMDJUMTM6NDk6MzcuODkxNzQwNDI4WiIsIm1ldGhvZCI6ImludGVybmFsIn1dLCJpcFJpc2siOnsicmlza19sZXZlbCI6MH0sInRva2VuVHlwZSI6InZpYW0iLCJhdWQiOiJwb3J0dW51cy5pZHAudm9uYWdlLmNvbSIsImV4cCI6MTc0MzY3NTQ1OSwianRpIjoiN2ZkNTk5ZDAtOWQ0NC00MzI2LWIyYTYtYjhmYjkyNzg5OGQ3IiwiaWF0IjoxNzQzNjc1MTU5LCJpc3MiOiJWSUFNLUlBUCIsIm5iZiI6MTc0MzY3NTE0NCwic3ViIjoiNDk2NmNjZDEtNjBlZS00MDExLWExY2EtZDFhNzU3NDZhNmNhIn19LCJmZWRlcmF0ZWRBc3NlcnRpb25zIjp7InZpZGVvLWFwaSI6W3siYXBpS2V5IjoiNzM2NGE4NzgiLCJhcHBsaWNhdGlvbklkIjoiZmQ4MWFjYmMtZGZlYi00ZTc0LWIxNGUtMTY3YTFjMGZkYmUwIiwiZXh0cmFDb25maWciOnsidmlkZW8tYXBpIjp7ImluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3QiOiIiLCJyb2xlIjoibW9kZXJhdG9yIiwic2NvcGUiOiJzZXNzaW9uLmNvbm5lY3QiLCJzZXNzaW9uX2lkIjoiMl9NWDVtWkRneFlXTmlZeTFrWm1WaUxUUmxOelF0WWpFMFpTMHhOamRoTVdNd1ptUmlaVEItZmpFM05ETTFOekU0TlRZNU56ZC1iRkV4UmtadGRsVm1MM3BIZUU5cFJVeDVNMjFDZEVGbWZuNS0ifX19XX0sImF1ZCI6InBvcnR1bnVzLmlkcC52b25hZ2UuY29tIiwiZXhwIjoxNzQ2MjY3MTcxLCJqdGkiOiJmZWVkM2IzYi0xNDAxLTQxZTQtOWU0Zi01MTZhOWY0OWIzYTAiLCJpYXQiOjE3NDM2NzUxNzEsImlzcyI6IlZJQU0tSUFQIiwibmJmIjoxNzQzNjc1MTU2LCJzdWIiOiI0OTY2Y2NkMS02MGVlLTQwMTEtYTFjYS1kMWE3NTc0NmE2Y2EifQ.HxkIfVe2F1OY5jk6cTacaIe8p_AxGlUgGZPG2YnbOq_uqHF-6CPxHpyZ-EeQ0lD-lBRmxSfDngxqUzTDWZLaM0U8YyefgP0WPxqPbBNxrnHvgjBJQUNnW_fygSJqJiIcU-J7q8eQXsBODEt2DJ4HS3iyzNkqs03x7YADSsdEZKhJH63kyc12RKciyBklGydPiJHDUVfJIk07HYc9aEEff6h5N6aOCCFUKdMiWihi02wdMNCmixLDbapi1muftN3pvVrn04WwaEZtIaMpUGhYrwkOXMUnNmttkZ_C_aYHU6U8NNzkmywALocAx8C40a9C8QQCjNkjuZ9EdgIGHYpn_w"
    }


    private var publisherScreen: Publisher? = null
    private var customVideoCapturer: CustomVideoCapturer? = null
    private var mediaProjectionServiceIsBound: Boolean = false
    private var mediaProjectionBinder: MediaProjectionBinder? = null
    var pd:ProgressDialog?=null

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


    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.d(TAG, "Component: onResume")
        isCurrentAppIsVisible  = true
        resumeSession()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.d(TAG, "Component: onPause")
        isCurrentAppIsVisible  = false
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
    fun attachActivity(context: Context){
        this.context_actvity = context
        pd = ProgressDialog(context_actvity)
    }

    // Public method for Activity to call with permission results
    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, context)
        if (requestCode == RC_VIDEO_APP_PERM && EasyPermissions.hasPermissions(context!!, *permissions)) {
            initializeComponent()
        }
    }

    // Public method for Activity to call with activity results
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SCREEN_CAPTURE) {
            pd?.show()
            Handler().postDelayed({
                Log.e("here Checking", isCurrentAppIsVisible.toString())
                if(isCurrentAppIsVisible){

                    Handler().postDelayed({
                        pd?.hide()
                        val intent = Intent(context, MediaProjectionService::class.java)
                        intent.putExtra("resultCode", resultCode)
                        intent.putExtra("data", data)
                        context?.startService(intent)
                        context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    },4000)

                }

            },100)

        }
    }

    private fun initializeComponent() {


        pd?.setMessage("Please Wait...")
        pd?.show()
        val apiService = RestApiBuilder().service
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = apiService.createSession(" grypp_live_xK2P9M7a1LqVb3Wz6JtD4RfXyE8Nc0Q5")
                if (response.isSuccessful) {

                    val data = response.body()
                    // Update UI on the main thread
                    withContext(Dispatchers.Main) {
                        pd?.hide()
                        session = Session.Builder(context, API_KEY, SESSION_ID).build()
                        session?.setSessionListener(sessionListener)
                        session?.setSignalListener(signalListener)
                        session?.connect(TOKEN)
                        matched_session_code = response.body()?.sessionCode?:""
                        showGlassDialog(response.body()?.sessionCode?:"")
                        Log.e("this@MainActivity", "Data: $data")
                    }
                } else {

                    withContext(Dispatchers.Main) {
                        pd?.hide()
                        Log.e("this@MainActivity", "WErorr: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pd?.hide()
                    Log.e("this@MainActivity", "Error: $e")
                }
            }
        }


    }
    fun startPublishScreen(){
        if (publisherScreen == null) {
                Log.d(TAG, "Initiate Screenshare")
                requestScreenCapture()
            }
    }

    fun startScreenShare(){
        requestPermissions()

    }
    fun stopScreenShare(){
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
            session?.sendSignal("ScreenDetails","{brand: ${android.os.Build.BRAND}, model: ${android.os.Build.MODEL},width: ${Utils.getScreenWidth()},height: ${Utils.getScreenHeight()}}")
        }
    }
    private fun showGlassDialog(sessionCode: String) {
        if(context_actvity != null && sessionCode != ""){
            session_dialog = SessionCodeDialog(
                context = context_actvity!!,
                title = sessionCode,
                message = "Please Share the code to executive",
                cancelText = "Cancel",
                confirmText = "Yes",
                onConfirmClick = {
                    println("Confirmed")
                },
                onCancelClick = {
                    println("Cancelled")
                }
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
    fun pauseSession(){
        session?.onPause()
    }
    fun resumeSession(){
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
    var signalListener = object :Session.SignalListener{
        override fun onSignalReceived(p0: Session?, p1: String?, p2: String?, p3: Connection?) {
            if(p1 == "CodeRequested"){
                if(p2 == matched_session_code){
                    session_dialog?.dismiss()
                    startPublishScreen()
                }

            }
            Log.e("here",p1.toString())
            Log.e("here",p2.toString())
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

    override fun deleteService() {
        onDestroyService()
    }
}