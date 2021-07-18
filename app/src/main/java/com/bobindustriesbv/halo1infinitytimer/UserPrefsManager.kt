package com.bobindustriesbv.halo1infinitytimer

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.bobindustriesbv.halo1infinitytimer.UserInstructor.showTheToast
import com.bobindustriesbv.halo1infinitytimer.helpers.Strings

object UserPrefsManager {
    fun getUserPrefs(apa : AppCompatActivity){
        try{
            val sharedPref: SharedPreferences = apa.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
            boSettingsAvailable = sharedPref.getBoolean("boSettingsAvailable", boSettingsAvailable)
            boTutorialFinished =  sharedPref.getBoolean("boTutorialFinished", boTutorialFinished)
            if(boSettingsAvailable){
                boFirstActivation = sharedPref.getBoolean("boFirstActivation", boFirstActivation)
                boTimerRunning = sharedPref.getBoolean("boTimeRunning", boTimerRunning)
                boBigTimeIsTotalTime = sharedPref.getBoolean("boBigTimeIsTotalTime",boBigTimeIsTotalTime)
                millisTimeStamp_StartToInfinity = sharedPref.getLong("millisTimeStamp_StartToInfinity",millisTimeStamp_StartToInfinity)
                secsToCountdownFrom = sharedPref.getLong("secsToCountdownFrom", secsToCountdownFrom)
                secsAddedToCountdownStart = sharedPref.getLong("secsAddedToCountdownStart",secsAddedToCountdownStart)
            }
        }catch (e: Exception){
            showTheToast(apa, Strings.get(R.string.error_getting_preferences))
        }
    }
    fun saveUserPrefs(apa : AppCompatActivity) {
        try {
            val sharedPref: SharedPreferences = apa.getSharedPreferences(sharedPrefFile,Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = sharedPref.edit()
            editor.putBoolean("boSettingsAvailable", true)
            editor.putBoolean("boTutorialFinished", boTutorialFinished)
            editor.putBoolean("boFirstActivation", boFirstActivation)
            editor.putBoolean("boTimeRunning", boTimerRunning)
            editor.putBoolean("boBigTimeIsTotalTime", boBigTimeIsTotalTime)
            editor.putLong("millisTimeStamp_StartToInfinity", millisTimeStamp_StartToInfinity)
            editor.putLong("secsToCountdownFrom", secsToCountdownFrom)
            editor.putLong("secsAddedToCountdownStart", secsAddedToCountdownStart)
            editor.apply()
            editor.commit()
        }catch (e: Exception){
            showTheToast(apa, Strings.get(R.string.error_saving_preferences))
        }
    }
}