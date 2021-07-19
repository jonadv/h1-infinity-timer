package com.bobindustriesbv.halo1infinitytimer

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.content.res.AppCompatResources
import com.bobindustriesbv.halo1infinitytimer.UserInstructor.showTheToast
import com.bobindustriesbv.halo1infinitytimer.databinding.ActivityMainBinding
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence

@SuppressLint("UseCompatLoadingForDrawables")
fun runTutorial(mContext : Context,  bd : ActivityMainBinding) {
    val main = (mContext as MainActivity)
    main.resetSettings() // tutorial start
    main.updateTimeAndUI() //tutorial start
    boTutorialHideToasts = true
    try {
        var tutorialStep = 0
        @SuppressLint("SourceLockedOrientationActivity")
        main.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val introIconHor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
             main.getDrawable(R.drawable.ic_swap_horiz_lined)
        } else {
            AppCompatResources.getDrawable(mContext, R.drawable.ic_swap_horiz_lined)
        }
        val introIconVert = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            main.getDrawable(R.drawable.ic_swap_vertic_lined)
        } else {
            AppCompatResources.getDrawable(mContext, R.drawable.ic_swap_vertic_lined)
        }
        //https://android-arsenal.com/details/1/4338
        TapTargetSequence(mContext)
            .targets(
                //0 one time tutorial, press circles
                TapTarget.forView(bd.btnTopOverlay, main.getString(R.string.tutorial_1_first_press_circle_title), main.getString(R.string.tutorial_1_first_press_circle))
                    .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                    .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(false).cancelable(false)
                    .targetRadius(80),
                //1 minute setting 1, 2, 3
                TapTarget.forView(bd.btnMinuteSetting, main.getString(R.string.tutorial_2_minsetting_title), main.getString(R.string.tutorial_2_minsetting))
                    .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                    .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false),
                //2 add
                TapTarget.forView(bd.btnAdd, main.getString(R.string.tutorial_3_add_title), main.getString(R.string.tutorial_3_add))
                    .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                    .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false),
                //3 start
                TapTarget.forView(bd.btnStartInfinity, main.getString(R.string.tutorial_4_start_title), "")
                    .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                    .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                    .targetRadius(100),
                //4 swipe add/subtract
                TapTarget.forView(bd.btnTopOverlay, main.getString(R.string.tutorial_5_swipe_title), main.getString(R.string.tutorial_5_swipe))
                    .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                    .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                    .icon(introIconHor, false).targetRadius(170),
                //5 swipe up/down
                TapTarget.forView(bd.btnTopOverlay, main.getString(R.string.tutorial_6_switch_title), main.getString(R.string.tutorial_6_switch))
                    .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                    .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                    .icon(introIconVert, false).targetRadius(170),
                //6 close
                TapTarget.forView(bd.btnTopOverlay, main.getString(R.string.tutorial_7_close_title), main.getString(R.string.tutorial_7_close))
                    .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                    .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(false).cancelable(false)
                    .targetRadius( 80)
            )
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                    when (tutorialStep) {
                        1 -> bd.btnMinuteSetting.performClick()
                        2 -> bd.btnAdd.performClick()
                        3 -> bd.btnStartInfinity.performClick()
                        4 -> {
                            boTutorialHideToasts = false
                            main.smallTimeAdjustment(1, true) //tutorial
                            boTutorialHideToasts = true
                        }
                        5 -> main.switchBigSmallTimerTexts()
                        else -> {
                        }
                    }
                    tutorialStep += 1
                }

                override fun onSequenceCanceled(lastTarget: TapTarget?) {}
                override fun onSequenceFinish() {
                    boTutorialFinished = true
                    boTutorialHideToasts = false
                    main.resetSettings() //tutorial finish
                    UserPrefsManager.saveUserPrefs(main)
                    main.updateTimeAndUI() //tutorial finish
                    main.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }).start()
    }catch (e: Exception){
        showTheToast(mContext, main.getString(R.string.error_tutor_tutorial))
        boTutorialFinished = true
    }
}