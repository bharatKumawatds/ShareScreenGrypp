package com.app.screensharinggrypp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.app.screenshare.sharingMain.ScreenShareComponent

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
        screenShareComponent = ScreenShareComponent(this, ProcessLifecycleOwner.get().lifecycle,"grypp_live_xK2P9M7a1LqVb3Wz6JtD4RfXyE8Nc0Q5",sensitiveTags)
    }
}