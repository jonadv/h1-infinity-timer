package com.bobindustriesbv.halo1infinitytimer

import android.content.res.Configuration
import android.view.View
import androidx.core.content.ContextCompat
import com.bobindustriesbv.halo1infinitytimer.TimerManager.boTimerRunning

object UIUpdater {
    fun updateTimeAndUI(ma: MainActivity, boSkipOffset: Boolean = false){
        TimeStates.updateTimes(ma, boSkipOffset) //updateTimeAndUI
        updateTimeTexts(ma) //updateTimeAndUI
        updateTextColors(ma) //updateTimeAndUI
        updateActionButtons(ma) //updateTimeAndUI
    }
    private fun updateActionButtons(ma: MainActivity){
        try{
            val bd = ma.bd
            bd.imgBackgroundTimer.visibility = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.imgBackgroundTimerOff.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.imgInfinity.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.btnStartInfinity.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.imgRestart.visibility  = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.btnRestart.visibility = if(boTimerRunning){
                when (UserSettings.secsOffsetFromStart){
                    1L -> bd.imgRestart.setImageResource(R.drawable.ic_restart_from_1)
                    2L -> bd.imgRestart.setImageResource(R.drawable.ic_restart_from_2)
                    3L -> bd.imgRestart.setImageResource(R.drawable.ic_restart_from_3)
                    else -> bd.imgRestart.setImageResource(R.drawable.ic_restart_from_0)
                }
                View.VISIBLE}else{View.INVISIBLE}
            bd.imgClear.visibility  = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.btnClear.visibility  = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.imgAdd.visibility = if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.btnAdd.visibility  = if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            if (!MainActivity.boDbg) bd.btnMidOverlayDbg.visibility = View.GONE
            when(UserSettings.secsToCountdownFrom) {
                60L -> bd.imgMinuteSetting.setImageResource(R.drawable.ic_nr_i)
                120L -> bd.imgMinuteSetting.setImageResource(R.drawable.ic_nr_ii)
                180L -> bd.imgMinuteSetting.setImageResource(R.drawable.ic_nr_iii)
            }
        }catch (e: Exception){
            if(MainActivity.boDbg) UserInstructor.showTheToast(ma,ma.getString(R.string.error_showing_buttons))
        }
    }
    fun updateTextColors(ma: MainActivity){
        //imgClear, imgRestart en imgInfinity zijn vaste kleur ivm zichtbaarheid
        val bd = ma.bd
        if (!boTimerRunning) {
            bd.imgMinuteSetting.setColorFilter(ContextCompat.getColor(ma, R.color.WhiteGrey))
            if (UserSettings.boBigTimeIsTotalTime) {
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGrey))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGreyLight))
            }else{
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGreyLight))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGrey))
            }
        } else {
            bd.imgMinuteSetting.setColorFilter(ContextCompat.getColor(ma, R.color.WhiteGreyDark))
            if (UserSettings.boBigTimeIsTotalTime) {
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGreyDark))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGreyLight))
            }else{
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGreyLight))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(ma, R.color.WhiteGreyDark))
            }
        }
    }
    fun updateTimeTexts(ma: MainActivity){
        if (UserSettings.boBigTimeIsTotalTime) {
            ma.bd.edtTimeBig.text = TimeStates.secondsToText(ma, TimeStates.secsSinceStart_total, "top", "total")
            ma.bd.edtTimeSmall.text = TimeStates.secondsToText(ma, TimeStates.secsSinceStart_down, "bottom", "down")
        } else {
            ma.bd.edtTimeBig.text = TimeStates.secondsToText(ma, TimeStates.secsSinceStart_down, "top", "down")
            ma.bd.edtTimeSmall.text = TimeStates.secondsToText(ma, TimeStates.secsSinceStart_total, "bottom", "total")
        }
        ma.bd.edtTimeBig.maxLines = 1
        if(ma.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            if(TimeStates.secsSinceStart_total > TimeStates.secsMONTH) ma.bd.edtTimeBig.maxLines = 2 else ma.bd.edtTimeBig.maxLines = 1
        }
    }
}