package com.app.screensharinggrypp;

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