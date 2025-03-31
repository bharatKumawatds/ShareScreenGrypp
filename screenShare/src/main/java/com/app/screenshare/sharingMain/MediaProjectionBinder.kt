package com.app.screenshare.sharingMain

import android.os.Binder


class MediaProjectionBinder : Binder() {
    public var mediaProjectionHandler: MediaProjectionHandler? = null
}