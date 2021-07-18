package com.bobindustriesbv.halo1infinitytimer

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import com.bobindustriesbv.halo1infinitytimer.helpers.App

object UserInstructor {
    private var theToast: Toast? = null

    fun showTheToast(mContext: Context, strToast: String, boLong: Boolean = false){
        // https://stackoverflow.com/questions/2755277/android-hide-all-shown-toast-messages  user "olearyj234"
        if (boTutorialHideToasts) return
        try{
//            dbg("showTheToast input: $strToast")
            theToast?.cancel()
            theToast = Toast.makeText( mContext
                , strToast,
                if (boLong or boTutorialHideToasts) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).also {
                it?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER, 0, 0)
                it?.show()
            }
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(mContext, "Error showing toasts, like this one.. Restart app?", Toast.LENGTH_SHORT).show()
        }
    }
}