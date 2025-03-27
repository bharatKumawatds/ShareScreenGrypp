package com.app.screenshare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.opengl.GLSurfaceView
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.app.screenshare.permission.PermissionHandler
import com.app.screenshare.permission.Permissions
import com.app.screenshare.util.Utils


import com.opentok.android.BaseVideoRenderer
import com.opentok.android.Connection
import com.opentok.android.OpentokError
import com.opentok.android.Publisher
import com.opentok.android.PublisherKit
import com.opentok.android.Session
import com.opentok.android.Stream

class ShareScreen(var myActivity: Activity,var context: Context,val contentView: View){

    var session: Session? = null
    var publisher: Publisher? = null

    data class Builder(
        var myActivity: Activity? = null,
        var context: Context? = null,
        var contentView: View? = null,
        var API_KEY: String? = null,
        var SESSION_ID: String? = null,
        var TOKEN: String? = null,

    )

    {
        fun activity(myActivity: Activity) = apply { this.myActivity = myActivity }
        fun context(context: Context) = apply { this.context = context }
        fun contentView(contentView: View) = apply { this.contentView = contentView }


        fun build() = ShareScreen(myActivity!!, context!!, contentView!!)
    }

    private val publisherListener: PublisherKit.PublisherListener = object :
        PublisherKit.PublisherListener {
        override fun onStreamCreated(publisherKit: PublisherKit, stream: Stream) {
            Log.d("TAG", "onStreamCreated: Publisher Stream Created. Own stream ${stream.streamId}")
        }

        override fun onStreamDestroyed(publisherKit: PublisherKit, stream: Stream) {
            Log.d("TAG", "onStreamDestroyed: Publisher Stream Destroyed. Own stream ${stream.streamId}")
        }

        override fun onError(publisherKit: PublisherKit, opentokError: OpentokError) {
            finishWithMessage("PublisherKit onError: ${opentokError.message}")
        }
    }
    private val sessionListener: Session.SessionListener = object : Session.SessionListener {
        override fun onConnected(session: Session) {
            Log.d("TAG", "onConnected: Connected to session: ${session.sessionId}")

            val screenSharingCapturer = ScreenSharingCapturer(contentView)

            publisher = Publisher.Builder(myActivity).capturer(screenSharingCapturer).build()
            publisher?.setPublisherListener(publisherListener)
            publisher?.publisherVideoType = PublisherKit.PublisherKitVideoType.PublisherKitVideoTypeScreen



            publisher?.renderer?.setStyle(BaseVideoRenderer.STYLE_VIDEO_SCALE, BaseVideoRenderer.STYLE_VIDEO_FILL)

            if (publisher?.view is GLSurfaceView) {
                (publisher?.view as GLSurfaceView).setZOrderOnTop(true)
            }
            session.publish(publisher)

        }

        override fun onDisconnected(session: Session) {
            Log.d("TAG", "onDisconnected: Disconnected from session: ${session.sessionId}")
        }

        override fun onStreamReceived(session: Session, stream: Stream) {
            Log.d("TAG", "onStreamReceived: New Stream Received ${stream.streamId} in session: ${session.sessionId}")
        }

        override fun onStreamDropped(session: Session, stream: Stream) {
            Log.d("TAG", "onStreamDropped: Stream Dropped: ${stream.streamId} in session: ${session.sessionId}")
        }

        override fun onError(session: Session, opentokError: OpentokError) {
            finishWithMessage("Session error: ${opentokError.message}")
        }
    }

    private val signalReceviver: Session.SignalListener = object : Session.SignalListener {
        override fun onSignalReceived(p0: Session?, p1: String?, p2: String?, p3: Connection?) {
            Log.e("here", "onSignalReceived: $p1 + $p2")
        }
    }

    fun initializeShareScreen(){
        requestPermissions()
    }
    private fun requestPermissions() {

        val permissions =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        Permissions.check(
            context /*context*/,
            permissions,
            "Please allow permissions for sharing audio and video." /*rationale*/,
            null /*options*/,
            object : PermissionHandler() {
                override fun onGranted() {

                    initializeSession()
                }
            })


    }
    private fun finishWithMessage(message: String) {
        Log.e("TAG", message)
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    }

    private fun initializeSession() {

        session = Session.Builder(context, "fd81acbc-dfeb-4e74-b14e-167a1c0fdbe0", "2_MX5mZDgxYWNiYy1kZmViLTRlNzQtYjE0ZS0xNjdhMWMwZmRiZTB-fjE3NDI5ODA2MTU2MTF-dFdWM1NlN0NvdVZjQVkzajh3a1NYQWtzfn5-").build().also {
            it.setSessionListener(sessionListener)
            it.setSignalListener(signalReceviver)
            it.connect("eyJhbGciOiJSUzI1NiIsImprdSI6Imh0dHBzOi8vYW51YmlzLWNlcnRzLWMxLXVzdzIucHJvZC52MS52b25hZ2VuZXR3b3Jrcy5uZXQvandrcyIsImtpZCI6IkNOPVZvbmFnZSAxdmFwaWd3IEludGVybmFsIENBOjoyNTM3NjAxOTQwODY1MTMyNzYyMjQyNTY0MjU2NjUxMTAzNjIzODIiLCJ0eXAiOiJKV1QiLCJ4NXUiOiJodHRwczovL2FudWJpcy1jZXJ0cy1jMS11c3cyLnByb2QudjEudm9uYWdlbmV0d29ya3MubmV0L3YxL2NlcnRzLzhkMWM3Yzg4YjdiMjBlZGYyODkzYjk3YWVkYzAzNmY3In0.eyJwcmluY2lwYWwiOnsiYWNsIjp7InBhdGhzIjp7Ii8qKiI6e319fSwidmlhbUlkIjp7ImVtYWlsIjoiYXNoaXNoLnRhbndhckBkb3RzcXVhcmVzLmNvbSIsImdpdmVuX25hbWUiOiJBc2hpc2giLCJmYW1pbHlfbmFtZSI6IlRhbndhciIsInBob25lX251bWJlciI6IjkxODA5NDAwMDE3NyIsInBob25lX251bWJlcl9jb3VudHJ5IjoiSU4iLCJvcmdhbml6YXRpb25faWQiOiI5ODE0MTRhOS0yZmQ0LTRkMTgtYjM3Yi00OGUxZDljYTAwN2IiLCJhdXRoZW50aWNhdGlvbk1ldGhvZHMiOlt7ImNvbXBsZXRlZF9hdCI6IjIwMjUtMDMtMjVUMDc6Mjg6MzMuMDY5NzkxNTAxWiIsIm1ldGhvZCI6ImludGVybmFsIn1dLCJpcFJpc2siOnsicmlza19sZXZlbCI6MH0sInRva2VuVHlwZSI6InZpYW0iLCJhdWQiOiJwb3J0dW51cy5pZHAudm9uYWdlLmNvbSIsImV4cCI6MTc0Mjk5MTg0OCwianRpIjoiN2UxZGMwNzEtMGMxNi00ZTkxLTkyNDgtNDhiY2JjMTg2ZDhhIiwiaWF0IjoxNzQyOTkxNTQ4LCJpc3MiOiJWSUFNLUlBUCIsIm5iZiI6MTc0Mjk5MTUzMywic3ViIjoiNDk2NmNjZDEtNjBlZS00MDExLWExY2EtZDFhNzU3NDZhNmNhIn19LCJmZWRlcmF0ZWRBc3NlcnRpb25zIjp7InZpZGVvLWFwaSI6W3siYXBpS2V5IjoiNzM2NGE4NzgiLCJhcHBsaWNhdGlvbklkIjoiZmQ4MWFjYmMtZGZlYi00ZTc0LWIxNGUtMTY3YTFjMGZkYmUwIiwiZXh0cmFDb25maWciOnsidmlkZW8tYXBpIjp7ImluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3QiOiIiLCJyb2xlIjoibW9kZXJhdG9yIiwic2NvcGUiOiJzZXNzaW9uLmNvbm5lY3QiLCJzZXNzaW9uX2lkIjoiMl9NWDVtWkRneFlXTmlZeTFrWm1WaUxUUmxOelF0WWpFMFpTMHhOamRoTVdNd1ptUmlaVEItZmpFM05ESTVPREEyTVRVMk1URi1kRmRXTTFObE4wTnZkVlpqUVZremFqaDNhMU5ZUVd0emZuNS0ifX19XX0sImF1ZCI6InBvcnR1bnVzLmlkcC52b25hZ2UuY29tIiwiZXhwIjoxNzQ1NTgzNTYyLCJqdGkiOiJlYmE4ZGI3ZC1iOWJlLTQ4YmYtOTEwNi0xNzcwODAxYjViODIiLCJpYXQiOjE3NDI5OTE1NjIsImlzcyI6IlZJQU0tSUFQIiwibmJmIjoxNzQyOTkxNTQ3LCJzdWIiOiI0OTY2Y2NkMS02MGVlLTQwMTEtYTFjYS1kMWE3NTc0NmE2Y2EifQ.D7q_PbuFAE1aQnTKx983Jplb7JZ3jDMMAZUsiCWQQ5gRg6YjYj6k9Aa6fMLa9_C8uDX9aCF7eCHrymmVYFON8el_l_NKacXZt1QfJwJMhnfLmjpFovBWkYxqxT1-ROkh_Y80VHth7XGEty9m5rcjTWQq_0iLMKv_jtVxkblCuxIZLSa6P1rcSPWhBj482UKx9hxe9ukNrdvJ2vPpEk-1o4s_HT0qP-9VhdhPzDYMJeVx08nG_hDzEAzZ2r4Els8pRVHY0evjLiG4C0RJdPT3CCOvJDvn6_RVRYNe4Fl5XTpEHpnVbqJ5MgbFkEPpEoaMG7iZ454xA1ZPm1ApIt0tpQ")

        }
        session!!.onResume()


    }

    fun connectSession(){
        if(session != null){
            initializeSession()
        }
    }

     fun disconnectSession() {
        if (session == null) {
            return
        }
        if (publisher != null) {
            session!!.unpublish(publisher)
            publisher = null
        }
        session!!.disconnect()
    }
}