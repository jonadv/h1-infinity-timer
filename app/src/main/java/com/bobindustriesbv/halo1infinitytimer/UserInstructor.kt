package com.bobindustriesbv.halo1infinitytimer

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence

object UserInstructor {
    private var theToast: Toast? = null
    var boTutorialFinished = false
    private var boTutorialHideToasts = false

    // https://developer.android.com/guide/topics/ui/notifiers/toasts
    // If your app targets Android 12 (API level 31) or higher, its toast is limited to two lines of text and shows the application icon next to the text.
    fun showTheToast(mainActivity: MainActivity, strToast: String, boLong: Boolean = false){
        if (boTutorialHideToasts) return

        // https://stackoverflow.com/questions/2755277/android-hide-all-shown-toast-messages  user "olearyj234"
        try{
            mainActivity.dbg("showTheToast input: $strToast")
            theToast?.cancel()
            theToast = Toast.makeText( mainActivity //must be main activity? https://stackoverflow.com/a/35189629/6544310
                , strToast,
                if (boLong or boTutorialHideToasts) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
            ).also {
                it?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER, 0, 0)
                it?.show()
            }
        } catch (e: Exception){
            e.printStackTrace()
            Toast.makeText(mainActivity, mainActivity.getString(R.string.error_toast), Toast.LENGTH_SHORT).show() //ehm..
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun runTutorial(ma : MainActivity) {

        UserPrefsManager.resetSettings() // tutorial start
        UIUpdater.updateTimeAndUI(ma) //tutorial start
        boTutorialHideToasts = true
        try {
            var tutorialStep = 0
            @SuppressLint("SourceLockedOrientationActivity")
            ma.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val introIconHor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ma.getDrawable(R.drawable.ic_swap_horiz_lined)
            } else {
                AppCompatResources.getDrawable(ma, R.drawable.ic_swap_horiz_lined)
            }
            val introIconVert = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ma.getDrawable(R.drawable.ic_swap_vertic_lined)
            } else {
                AppCompatResources.getDrawable(ma, R.drawable.ic_swap_vertic_lined)
            }
            //https://android-arsenal.com/details/1/4338
            TapTargetSequence(ma)
                .targets(
                    //0 one time tutorial, press circles
                    TapTarget.forView(ma.bd.btnTopOverlay, ma.getString(R.string.tutorial_1_first_press_circle_title), ma.getString(R.string.tutorial_1_first_press_circle))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(false).cancelable(false)
                        .targetRadius(80),
                    //1 minute setting 1, 2, 3
                    TapTarget.forView(ma.bd.btnMinuteSetting, ma.getString(R.string.tutorial_2_minsetting_title), ma.getString(R.string.tutorial_2_minsetting))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false),
                    //2 add
                    TapTarget.forView(ma.bd.btnAdd, ma.getString(R.string.tutorial_3_add_title), ma.getString(R.string.tutorial_3_add))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false),
                    //3 start
                    TapTarget.forView(ma.bd.btnStartInfinity, ma.getString(R.string.tutorial_4_start_title), "")
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                        .targetRadius(100),
                    //4 swipe add/subtract
                    TapTarget.forView(ma.bd.btnTopOverlay, ma.getString(R.string.tutorial_5_swipe_title), ma.getString(R.string.tutorial_5_swipe))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                        .icon(introIconHor, false).targetRadius(170),
                    //5 swipe up/down
                    TapTarget.forView(ma.bd.btnTopOverlay, ma.getString(R.string.tutorial_6_switch_title), ma.getString(R.string.tutorial_6_switch))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                        .icon(introIconVert, false).targetRadius(170),
                    //6 close
                    TapTarget.forView(ma.bd.btnTopOverlay, ma.getString(R.string.tutorial_7_close_title), ma.getString(R.string.tutorial_7_close))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(false).cancelable(false)
                        .targetRadius( 80)
                )
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                        when (tutorialStep) {
                            1 -> ma.bd.btnMinuteSetting.performClick()
                            2 -> ma.bd.btnAdd.performClick()
                            3 -> ma.bd.btnStartInfinity.performClick()
                            4 -> {
                                boTutorialHideToasts = false
                                UserSettings.adjustTimeItself(ma, 1, true) //tutorial
                                boTutorialHideToasts = true
                            }
                            5 -> UserSettings.switchBigSmallTimerTexts(ma)
                            else -> {
                            }
                        }
                        tutorialStep += 1
                    }

                    override fun onSequenceCanceled(lastTarget: TapTarget?) {}
                    override fun onSequenceFinish() {
                        boTutorialFinished = true
                        boTutorialHideToasts = false
                        UserPrefsManager.resetSettings() //tutorial finish
                        UserPrefsManager.saveUserPrefs(ma)
                        UIUpdater.updateTimeAndUI(ma) //tutorial finish
                        ma.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }).start()
        }catch (e: Exception){
            showTheToast(ma, ma.getString(R.string.error_tutor_tutorial))
            boTutorialFinished = true
        }
    }
}