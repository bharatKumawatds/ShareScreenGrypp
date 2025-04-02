package com.app.screenshare.sharingMain

import java.nio.ByteBuffer

interface MediaProjectionHandler {
    fun sendFrame(imageBuffer: ByteBuffer, width: Int, height: Int)
    fun deleteService()
}