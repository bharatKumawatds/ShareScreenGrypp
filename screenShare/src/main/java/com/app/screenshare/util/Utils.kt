package com.app.screenshare.util

import android.app.*
import android.content.*
import android.content.res.Resources
import android.graphics.*
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.text.TextUtils
import android.util.*
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created by : Umesh Jangid
 * Date : 31 March 2020
 *
 */
object Utils {

    private var toast: Toast? = null
    private var snackbar: Snackbar? = null

    fun getOSName(): String {
        var osName = ""
        try {
            val fields = Build.VERSION_CODES::class.java.fields
            var codeName = "UNKNOWN"
            fields.filter { it.getInt(Build.VERSION_CODES::class) == Build.VERSION.SDK_INT }
                .forEach { codeName = it.name }
            osName = codeName
        } catch (e: Exception) {

        }
        return osName
    }

    fun getScreenWidth(): Int {
        return Resources.getSystem().getDisplayMetrics().widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().getDisplayMetrics().heightPixels
    }

    /*Get Device Name */
    fun getDeviceName(): String? {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer)) {
            capitalize(model)
        } else {
            capitalize(manufacturer).toString() + " " + model
        }
    }

    private fun capitalize(s: String?): String? {
        if (s == null || s.isEmpty()) {
            return ""
        }
        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first).toString() + s.substring(1)
        }
    }

    /**
     * This method return date in this format :  2015/01/02 23:14:05
     * */
    fun getCurrentTimeInFormat(): String {
        val c = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val strDate = dateFormat.format(c.time)
        return strDate
    }

    fun getCurrentTimeInFormatAlt(): String {
        val c = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        val strDate = dateFormat.format(c.time)
        return strDate
    }

    fun getTimeInTheFormat(timemills : Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = timemills
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val strDate = dateFormat.format(c.time)
        return strDate
    }
    fun getCurrentDate(): String {
        val c = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val strDate = dateFormat.format(c.time)
        return strDate
    }

    fun isNetworkOnline1(context: Context?): Boolean {
        val connectivityManager = context?.getSystemService(
            Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)


    }

    @JvmStatic
    fun showToast(context: Context, message: String) {
        toast(context, message, false)// false mean DURATION_SHORT
    }

    fun toast(context: Context, message: String, durationShort: Boolean) {
        if (toast != null) {
            toast!!.cancel()
        }
        toast = Toast.makeText(
            context,
            message,
            if (durationShort) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
        )
        toast!!.show()
    }

    fun showSnackBar(
        view: View,
        message: String,
        actionName1: String?,
        listener1: View.OnClickListener?,
        duration: Int,
    ) {
        showSnackBar(view, message, actionName1, listener1, null, null, duration)
    }

    fun showSnackBar(
        view: View,
        message: String,
        actionName1: String?,
        listener1: View.OnClickListener?,
        actionName2: String?,
        listener2: View.OnClickListener?,
        duration: Int,
    ) {
        if (snackbar != null) {
            snackbar!!.dismiss()
        }
        snackbar = Snackbar.make(view, message, duration)
        val snackbarView = snackbar!!.view
        snackbarView.setBackgroundColor(Color.parseColor("#ec3338"))
        //  snackbar.(ContextCompat.getColor(getActivity(), R.color.colorPrimary))
        if (actionName1 != null && listener1 != null) {
            snackbar!!.setAction(actionName1, listener1)
        }
        if (actionName2 != null && listener2 != null) {
            snackbar!!.setAction(actionName2, listener2)
        }
        snackbar!!.show()
    }


    fun commaSeparatedStringToArrayList(commaSeparated: String?): ArrayList<String>? {
        var list: ArrayList<String>? = null
        if (commaSeparated != null && commaSeparated.isNotEmpty()) {
            list =
                ArrayList(Arrays.asList(*commaSeparated.split(",".toRegex()).dropLastWhile { it.isNotEmpty() }.toTypedArray()))
        }
        return list
    }

    fun arrayListToCommaSeparatedString(list: ArrayList<String>): String? {
        var s: String? = null
        if (list.size > 0) {
            s = TextUtils.join(",", list.toTypedArray())
        }
        return s
    }


    fun isValidEmail(email: String): Boolean {
        return if (TextUtils.isEmpty(email)) false else Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Close key board.
     *
     * @param context the context
     */
    fun closeKeyBoard(context: Context) {
        if (context is Activity) {
            val view = context.currentFocus
            if (view != null) {
                val imm =
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }





}
