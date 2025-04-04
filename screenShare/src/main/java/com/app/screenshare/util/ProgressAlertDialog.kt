package com.app.screenshare.util


import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.TextView
import com.app.screenshare.R


class ProgressAlertDialog(context: Context) {

    private val dialog: AlertDialog
    private val messageTextView: TextView

    init {
        // Inflate the custom layout
        val view = LayoutInflater.from(context).inflate(R.layout.layout_progress_alert_dialog, null)

        // Find the message TextView
        messageTextView = view.findViewById(R.id.progressMessage)

        // Build the AlertDialog
        dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(false)
            .create()

        // Transparent background
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)


    }

    fun show() {
        if (!dialog.isShowing) {
            dialog.show()
        }
    }

    fun hide() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    fun isShowing(): Boolean = dialog.isShowing

    fun setMessage(message: String) {
        messageTextView.text = message
        messageTextView.visibility = if (message.isNotEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    companion object {
        fun show(context: Context, message: String = "Please Wait..."): ProgressAlertDialog {
            val progressDialog = ProgressAlertDialog(context)
            progressDialog.setMessage(message)
            progressDialog.show()
            return progressDialog
        }
    }
}