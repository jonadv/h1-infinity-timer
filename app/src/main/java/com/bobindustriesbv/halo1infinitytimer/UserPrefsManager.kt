package com.bobindustriesbv.halo1infinitytimer

import android.content.Context
import android.content.SharedPreferences

object UserPrefsManager{

    //defaults for saving in sharedPrefFile
    private const val sharedPrefFile = "h1_infinity_timer_settings"
    private const val boSettingsAvailable_default = false
    const val boFirstActivation_default = false
    const val boTimerRunning_default = false
    const val boBigTimeIsTotalTime_default = true
    const val secsToCountdownFrom_default: Long = 120 //was timSetting
    const val secsAddedToCountdownStart_default: Long = 1
    const val millisTimeStamp_StartToInfinity_default: Long = 0

    var boSettingsAvailable = boSettingsAvailable_default

    fun getUserPrefs(ma : MainActivity ){
        try{
            val sharedPref: SharedPreferences = ma.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
            boSettingsAvailable = sharedPref.getBoolean("boSettingsAvailable", boSettingsAvailable)
            UserInstructor.boTutorialFinished =  sharedPref.getBoolean("boTutorialFinished", UserInstructor.boTutorialFinished)
            if(boSettingsAvailable){
                MainActivity.boFirstActivation = sharedPref.getBoolean("boFirstActivation", MainActivity.boFirstActivation)
                TimerManager.boTimerRunning = sharedPref.getBoolean("boTimeRunning", TimerManager.boTimerRunning)
                UserSettings.boBigTimeIsTotalTime = sharedPref.getBoolean("boBigTimeIsTotalTime", UserSettings.boBigTimeIsTotalTime)
                UserSettings.secsToCountdownFrom = sharedPref.getLong("secsToCountdownFrom", UserSettings.secsToCountdownFrom)
                UserSettings.secsOffsetFromStart = sharedPref.getLong("secsAddedToCountdownStart", UserSettings.secsOffsetFromStart)
                TimeStates.millisTimeStamp_StartToInfinity = sharedPref.getLong("millisTimeStamp_StartToInfinity",TimeStates.millisTimeStamp_StartToInfinity)
            }
        }catch (e: Exception){
            UserInstructor.showTheToast(ma, ma.getString(R.string.error_getting_preferences))
        }
    }
    fun saveUserPrefs(ma : MainActivity) {
        try {
            val sharedPref: SharedPreferences = ma.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putBoolean("boSettingsAvailable", true)
            editor.putBoolean("boTutorialFinished", UserInstructor.boTutorialFinished)
            editor.putBoolean("boFirstActivation", MainActivity.boFirstActivation)
            editor.putBoolean("boTimeRunning", TimerManager.boTimerRunning)
            editor.putBoolean("boBigTimeIsTotalTime", UserSettings.boBigTimeIsTotalTime)
            editor.putLong("secsToCountdownFrom", UserSettings.secsToCountdownFrom)
            editor.putLong("secsAddedToCountdownStart", UserSettings.secsOffsetFromStart)
            editor.putLong("millisTimeStamp_StartToInfinity", TimeStates.millisTimeStamp_StartToInfinity)
            editor.apply()
            editor.commit()
        }catch (e: Exception){
            UserInstructor.showTheToast(ma, ma.getString(R.string.error_saving_preferences))
        }
    }
    fun resetSettings(){
        boSettingsAvailable = boSettingsAvailable_default
        MainActivity.boFirstActivation = boFirstActivation_default
        TimerManager.boTimerRunning = boTimerRunning_default
        UserSettings.boBigTimeIsTotalTime = boBigTimeIsTotalTime_default
        UserSettings.secsToCountdownFrom = secsToCountdownFrom_default
        UserSettings.secsOffsetFromStart = secsAddedToCountdownStart_default
        TimeStates.millisTimeStamp_StartToInfinity = millisTimeStamp_StartToInfinity_default
    }
}