package com.app.screenshare.sharingMain

import android.graphics.Rect
import android.util.Log
import android.view.View

class RedactionManager {
    private val sensitiveViews = mutableMapOf<View, Rect>()
    private val redactionRegions = mutableListOf<Rect>()

    companion object {
        const val TAG = "RedactionManager"
    }

    fun redact(view: View) {
        if (!sensitiveViews.containsKey(view)) {
            sensitiveViews[view] = Rect()
            updateRedactionRegions()
            Log.d(TAG, "Redacting view: $view")
        }
    }

    fun removeRedaction(view: View) {
        if (sensitiveViews.remove(view) != null) {
            updateRedactionRegions()
            Log.d(TAG, "Removed redaction for view: $view")
        }
    }

    fun redactRect(rect: Rect) {
        redactionRegions.add(Rect(rect))
        Log.d(TAG, "Added redaction region for rectangle: $rect")
    }

    fun clear() {
        sensitiveViews.clear()
        redactionRegions.clear()
        Log.d(TAG, "Cleared all redactions")
    }

    fun updateAllPositions() {
        updateRedactionRegions()
    }

    fun getRedactionRegions(): List<Rect> {
        return redactionRegions.toList()
    }

//    private fun updateRedactionRegions() {
//        redactionRegions.clear()
//        sensitiveViews.forEach { (view, rect) ->
//            if (view.width > 0 && view.height > 0 && view.isShown) {
//                val location = IntArray(2)
//                view.getLocationOnScreen(location)
//                rect.set(
//                    location[0],
//                    location[1],
//                    location[0] + view.width,
//                    location[1] + view.height
//                )
//                if (rect.width() > 0 && rect.height() > 0) {
//                    redactionRegions.add(Rect(rect))
//                    Log.d(TAG, "Updated redaction region for view at (${rect.left}, ${rect.top}) with size ${rect.width()}x${rect.height()}")
//                } else {
//                    Log.w(TAG, "Skipping redaction for view with invalid size: ${view.width}x${view.height}")
//                }
//            } else {
//                Log.w(TAG, "Skipping redaction for invisible or zero-sized view: $view")
//            }
//        }
//    }

    private fun updateRedactionRegions() {
        redactionRegions.clear()
        sensitiveViews.forEach { (view, rect) ->
            if (view.isShown && view.width > 0 && view.height > 0) {
                val location = IntArray(2)
                view.getLocationOnScreen(location)
                rect.set(
                    location[0],
                    location[1],
                    location[0] + view.width,
                    location[1] + view.height
                )
                if (rect.width() > 0 && rect.height() > 0) {
                    redactionRegions.add(Rect(rect))
                    Log.d(TAG, "Updated redaction region for view at (${rect.left}, ${rect.top}) with size ${rect.width()}x${rect.height()}")
                } else {
                    Log.w(TAG, "Skipping redaction for view with invalid size: ${view.width}x${view.height}")
                }
            } else {
                Log.w(TAG, "Skipping redaction for invisible or zero-sized view: $view")
            }
        }
    }
}