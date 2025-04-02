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
        screenShareComponent = ScreenShareComponent(this, ProcessLifecycleOwner.get().lifecycle)

    }
}