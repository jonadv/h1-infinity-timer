package com.bobindustriesbv.halo1infinitytimer

import android.content.res.Configuration
import android.text.TextUtils

object TimeStates {
    //the big start time. is stored in settings. core of all times
    var millisTimeStamp_StartToInfinity: Long = UserPrefsManager.millisTimeStamp_StartToInfinity_default

    //not saved
    var secsSinceStart_total: Long = 0
    var secsSinceStart_down: Long = UserSettings.secsToCountdownFrom
    var secsOneMinuteState = 60L

    const val millisOneSecond: Long = 1000
    const val secsMIN: Double = 60.0
    const val secsHOUR: Double = secsMIN * 60
    const val secsDAY: Double = secsHOUR * 24
    const val secsYEAR : Double = secsDAY * 365.25
    //Julian astronomical year https://www.rapidtables.com/calc/time/seconds-in-year 365.25
//https://www.volkskrant.nl/wetenschap/afgesproken-jaar-duurt-31-556-925-445-seconden~ba74b7f2/ 31556925.445
    const val secsMONTH: Double = secsYEAR


    fun updateTimes(mainActivity: MainActivity, boSkipOffset    : Boolean = false){
        try{
            if (TimerManager.boTimerRunning) {
                TimeCalculator.recalcSecondStatesFromInfinity()
            }else{
                TimeCalculator.recalcPausedSecondStates(boSkipOffset)
            }
        }catch (e: Exception){
            UserInstructor.showTheToast(mainActivity, mainActivity.getString(R.string.error_updating_times))
        }
    }

    fun secondsToText(ma: MainActivity, aTimeInSeconds: Long, strTopOrBottom: String, strTotalOrDown: String): String{
        fun secsOfUnit(secsIn: Double, secsOfUnit: Double): Int{
            return (secsIn / secsOfUnit).toInt()
        }
        var  secsRemaining : Double = aTimeInSeconds.toDouble()
        var minutes = secsOfUnit(secsRemaining, secsMIN)
        secsRemaining -= minutes * secsMIN //subSecs(secsRemaining, secsMIN)
        var seconds =  secsRemaining.toInt()
        var strMinutes: String = if(strTotalOrDown == "total" && minutes < 10 ) "0$minutes" else "$minutes"
        var strSeconds: String  = if(seconds < 10) "0$seconds" else "$seconds"

        if(aTimeInSeconds < secsHOUR) return "$strMinutes:$strSeconds"

        secsRemaining = aTimeInSeconds.toDouble()
        val years = secsOfUnit(secsRemaining, secsYEAR)
        secsRemaining -= years * secsYEAR
        val months = secsOfUnit(secsRemaining, secsMONTH)
        secsRemaining -= months * secsMONTH
        val days = secsOfUnit(secsRemaining, secsDAY)
        secsRemaining -= days * secsDAY
        val hours = secsOfUnit(secsRemaining, secsHOUR)
        secsRemaining -= hours * secsHOUR
        minutes = secsOfUnit(secsRemaining, secsMIN)
        secsRemaining -= minutes * secsMIN
        seconds =  secsRemaining.toInt()

        val strTimeHas : String = if(strTotalOrDown == "total") TextUtils.concat(
            if (years > 0) "y" else "",
            if (months > 0) "m" else "",
            if (days > 0) "d" else "",
            if (hours > 0) "h" else ""
        ) as String else{""}

        val strWayToWrite = if(ma.resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT &&
            strTimeHas.length.toLong() < 3L &&
            strTopOrBottom =="bottom"){
            "long"
        }else{
            "short"
        }

        fun strTime(iTime: Int, timeType: String, strWayToWrite: String, strTimeHas: String): String{
            if(iTime==0) return ""

            val hasFollowup: Boolean = strTimeHas.takeLast(1) != timeType
            val addSemiCol: String = if(hasFollowup && strWayToWrite == "short") ":" else ""
            val spaceAfter: String = if(!hasFollowup) " " else "" //auto return if text to long
            if (strWayToWrite == "short") return "$iTime" + timeType + addSemiCol + spaceAfter

            val isFirst: Boolean = strTimeHas.take(1) == timeType
            val addS: String = if(iTime > 1) "s" else ""
            val spaceBefore: String = if(!isFirst && strWayToWrite == "long") " " else ""
            return when(timeType){
                //ToDO vertalen strTimes small TimeText
                "y" -> spaceBefore + "$iTime" + "year" + addS + addSemiCol + spaceAfter
                "m" -> spaceBefore + "$iTime" + "month" + addS + addSemiCol + spaceAfter
                "d" -> spaceBefore + "$iTime" + "day" + addS + addSemiCol + spaceAfter
                "h" -> spaceBefore + "$iTime" + "hour" + addS + addSemiCol + spaceAfter
                else -> ""
            }
        }

        val strYears: String = strTime(years, "y", strWayToWrite, strTimeHas)
        val strMonths: String = strTime(months, "m", strWayToWrite, strTimeHas)
        val strDays: String = strTime(days, "d", strWayToWrite, strTimeHas)
        val strHours: String = strTime(hours, "h", strWayToWrite, strTimeHas)

        strMinutes = if(strTotalOrDown == "total" && minutes < 10 ) "0$minutes" else "$minutes"
        strSeconds = if(seconds < 10) "0$seconds" else "$seconds"

        return "$strYears$strMonths$strDays$strHours$strMinutes:$strSeconds"
    }
}