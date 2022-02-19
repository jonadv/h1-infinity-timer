package com.bobindustriesbv.halo1infinitytimer

import kotlin.math.roundToLong

object TimeCalculator {
    fun recalcSecondStatesFromInfinity(){
        TimeStates.secsSinceStart_total = ((System.currentTimeMillis() - TimeStates.millisTimeStamp_StartToInfinity) / TimeStates.millisOneSecond) //recalcSecondStatesFromInfinity
        TimeStates.secsSinceStart_down = calcSecondState(UserSettings.secsToCountdownFrom, TimeStates.secsSinceStart_total) //recalcSecondStatesFromInfinity
        TimeStates.secsOneMinuteState = calcSecondState(60L, TimeStates.secsSinceStart_total) //recalcSecondStatesFromInfinity
    }
    fun recalcPausedSecondStates(boSkipOffset: Boolean){
        if(!boSkipOffset) recalcOffsetFromStart()
        TimeStates.secsSinceStart_total = UserSettings.secsOffsetFromStart
        TimeStates.secsSinceStart_down = UserSettings.secsToCountdownFrom - UserSettings.secsOffsetFromStart //updateTimes !boTimerRunning
        TimeStates.secsOneMinuteState = 60L - UserSettings.secsOffsetFromStart
    }
    fun recalcOffsetFromStart(){
        UserSettings.secsOffsetFromStart = if (UserSettings.secsOffsetFromStart > UserSettings.secsMaxOffsetFromStart) 0 else UserSettings.secsOffsetFromStart

    }
    private fun calcSecondState(secsInput: Long, secsBig: Long): Long{
        val t1 = secsBig.toDouble() / secsInput.toDouble()
        //dbg("getSecondState --> secsInput: $secsInput  secsBig: $secsBig result: " + (secsInput - (t1 - t1.toInt()) * secsInput).toDouble())
        return (secsInput - (t1 - t1.toInt()) * secsInput).roundToLong()
    }
}