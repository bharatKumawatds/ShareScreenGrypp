package com.app.screensharinggrypp;

import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

public class AppLifecycleObserver implements DefaultLifecycleObserver {

    private final GlobalActionBarService globalService;

    public AppLifecycleObserver(GlobalActionBarService service) {
        this.globalService = service;
    }

    @Override
    public void onStop(LifecycleOwner owner) {
        if (globalService != null) {
            globalService.hideOverlayButton();
        }
    }

    @Override
    public void onStart(LifecycleOwner owner) {
        if (globalService != null) {
            globalService.showOverlayButton();
        }
    }
}