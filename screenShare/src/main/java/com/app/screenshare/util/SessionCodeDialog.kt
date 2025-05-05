package com.app.screenshare.util


import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.app.screenshare.R

class SessionCodeDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val cancelText: String = "Cancel",
    private val confirmText: String = "Confirm",
    private val onConfirmClick: (() -> Unit)? = null,
    private val onCancelClick: (() -> Unit)? = null,
//    private val customView: Int
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
      /*  if(customView!=null){
            setContentView(customView)
        }else {*/
            setContentView(R.layout.custom_dialog_layout)
//        }

        // Set dialog properties for glass effect
        setCancelable(false)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        window?.attributes?.windowAnimations = R.style.DialogAnimation



        // Initialize views
        val titleTextView = findViewById<TextView>(R.id.dialog_title)
        val messageTextView = findViewById<TextView>(R.id.dialog_message)
        val cancelButton = findViewById<Button>(R.id.cancel_button)
//        val confirmButton = findViewById<Button>(R.id.confirm_button)

        // Set content
        titleTextView.text = title
        messageTextView.text = message
        cancelButton.text = cancelText
//        confirmButton.text = confirmText

        // Set button click listeners
        cancelButton.setOnClickListener {
            onCancelClick?.invoke()
            dismiss()

        }

        /*confirmButton.setOnClickListener {
            onConfirmClick?.invoke()
            dismiss()

        }*/

        // Set dialog dimensions
        val width = (context.resources.displayMetrics.widthPixels * 0.85).toInt()
        window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)

        // Add elevation for depth
//        findViewById<RelativeLayout>(R.id.dialog_container)?.elevation = 20f
    }

    fun setButtonColors(cancelColor: Int, confirmColor: Int) {
        findViewById<Button>(R.id.cancel_button)?.setTextColor(
            ContextCompat.getColor(context, cancelColor)
        )
        findViewById<Button>(R.id.confirm_button)?.setTextColor(
            ContextCompat.getColor(context, confirmColor)
        )
    }
}