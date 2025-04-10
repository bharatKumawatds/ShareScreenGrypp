package com.app.screenshare.util


import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.app.screenshare.R

class ProgressAlertDialog(context: Context) {

    private val dialog: Dialog
    private val messageTextView: TextView

    init {
        // Inflate the custom layout
        val view = LayoutInflater.from(context).inflate(R.layout.layout_progress_alert_dialog, null)

        // Find the message TextView
        messageTextView = view.findViewById(R.id.progressMessage)

        // Create the Dialog
        dialog = Dialog(context).apply {
            setContentView(view)
            setCancelable(false)

            // Optional: Set window parameters
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
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