package com.app.screenshare.sharingMain

import android.graphics.Bitmap

object BlurUtils {
    fun gaussianBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val blurredBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        val blurredPixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Box blur approximation (two-pass algorithm)
        val kernelSize = radius * 2 + 1
        val kernel = IntArray(kernelSize) { 1 } // Simple box kernel

        // First pass: Blur horizontally
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                var count = 0

                for (k in -radius..radius) {
                    val px = x + k
                    if (px in 0 until width) {
                        val pixel = pixels[y * width + px]
                        a += (pixel shr 24) and 0xFF
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                }

                blurredPixels[y * width + x] = (
                    (a / count shl 24) or
                    (r / count shl 16) or
                    (g / count shl 8) or
                    (b / count)
                )
            }
        }

        // Second pass: Blur vertically
        val tempPixels = blurredPixels.copyOf()
        for (x in 0 until width) {
            for (y in 0 until height) {
                var r = 0
                var g = 0
                var b = 0
                var a = 0
                var count = 0

                for (k in -radius..radius) {
                    val py = y + k
                    if (py in 0 until height) {
                        val pixel = tempPixels[py * width + x]
                        a += (pixel shr 24) and 0xFF
                        r += (pixel shr 16) and 0xFF
                        g += (pixel shr 8) and 0xFF
                        b += pixel and 0xFF
                        count++
                    }
                }

                blurredPixels[y * width + x] = (
                    (a / count shl 24) or
                    (r / count shl 16) or
                    (g / count shl 8) or
                    (b / count)
                )
            }
        }

        blurredBitmap.setPixels(blurredPixels, 0, width, 0, 0, width, height)
        return blurredBitmap
    }
}