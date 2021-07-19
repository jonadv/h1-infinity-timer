package com.bobindustriesbv.halo1infinitytimer

import android.os.Build

object TimerManager {
    var boTimerRunning = UserPrefsManager.boTimerRunning_default

    private const val iDbg_AddedAtStart_Seconds: Long = 0//45
    private const val iDbg_AddedAtStart_Minutes: Long = 4535//13
    private const val iDbg_AddedAtStart_Hours: Long = 0
    private const val iDbg_AddedAtStart_Days: Long =8285//247
    private const val iDbg_AddedAtStart_Years: Long = 0//532
    //2552, sept 19. "John-117 is awoken from cryo sleep and tasked with preventing Cortana's capture by the Covenant." https://halo.fandom.com/wiki/2552

    fun startOrRestartTheTimer(ma: MainActivity){
        boTimerRunning = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //iBeepShortStart wont load on older devices. Unclear why
            MediaManager.playSimpleSound()
        }else {
            MediaManager.playSound(ma, MediaManager.iBeepShortStart)
        }
        TimeStates.millisTimeStamp_StartToInfinity = System.currentTimeMillis()
        TimeStates.millisTimeStamp_StartToInfinity -= (UserSettings.secsOffsetFromStart * TimeStates.millisOneSecond)
        if (MainActivity.boDbg){ //add debug time
            TimeStates.millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Seconds * TimeStates.millisOneSecond)
            TimeStates.millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Minutes * TimeStates.millisOneSecond * TimeStates.secsMIN.toLong())
            TimeStates.millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Hours * TimeStates.millisOneSecond * TimeStates.secsHOUR.toLong())
            TimeStates.millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Days * TimeStates.millisOneSecond * TimeStates.secsDAY.toLong())
            TimeStates.millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Years * TimeStates.millisOneSecond * TimeStates.secsYEAR.toLong())
        }
        //millisTimeStamp_StartToInfinity = maxOf(millisTimeStamp_StartToInfinity, 0) //for <1970 (EPOCH), deactivated cause solved with double minus
        startOrRestartTheTimerItself(ma)
    }
    fun stopTheTimer(ma: MainActivity){
        boTimerRunning = false
        stopTheTimerItself(ma)
        MainActivity.boFirstActivation = false //s3csAddedToCountdownStart  s3csMaxAddedToCount_Reset
    }
    fun startOrRestartTheTimerItself(ma: MainActivity){
        try{
            ma.theTimer.restart()
        }catch (e: Exception){
            try{
                ma.theTimer.stop()
                ma.theTimer.restart()
            }catch (e: Exception){
                //versie 2 was 1 try{theTimer.restart()}catch{toast}. Toch nog af en toe error_timer_start melding gezien (na app lang open hebben met timer op pauze bijv)
                //versie 3.0 dubbele try catch met bij tweede .stop() ervoor.
                UserInstructor.showTheToast(ma, ma.getString(R.string.error_timer_start), true)
            }
        }
    }
    fun stopTheTimerItself(ma: MainActivity){
        try{
            ma.theTimer.stop()
        }catch (e: Exception){
            try{
                startOrRestartTheTimerItself(ma)
                ma.theTimer.stop()
            }catch (e: Exception){
                //should only happen if timer thread is killed (by overuse of sources and running in background or by calling .dispose -> ("Timer already cancelled."))
                UserInstructor.showTheToast(ma, ma.getString(R.string.error_timer_stop), true)
            }
        }
    }
}