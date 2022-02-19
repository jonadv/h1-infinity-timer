package com.bobindustriesbv.halo1infinitytimer

import kotlin.math.max

object UserSettings {
    var boBigTimeIsTotalTime = UserPrefsManager.boBigTimeIsTotalTime_default
    var secsToCountdownFrom: Long = UserPrefsManager.secsToCountdownFrom_default
    var secsOffsetFromStart: Long = UserPrefsManager.secsAddedToCountdownStart_default

    var secsMaxOffsetFromStart: Long = 3L

    fun switchBigSmallTimerTexts(ma: MainActivity){
        boBigTimeIsTotalTime = !boBigTimeIsTotalTime
        UIUpdater.updateTimeTexts(ma) //switch Big Small
        UIUpdater.updateTextColors(ma) //switch Big Small
        UserInstructor.showTheToast(ma,
                                    if (boBigTimeIsTotalTime) {
                                        ma.getString(R.string.countdown_on_bottom)
                                    } else {
                                        ma.getString(R.string.countdown_on_top)
                                    }
        )
        UserPrefsManager.saveUserPrefs(ma)
    }
    fun adjustMinuteSetting(){
        secsToCountdownFrom = when(secsToCountdownFrom) {
            60L -> 120L
            120L -> 180L
            180L -> 60L
            else -> UserPrefsManager.secsToCountdownFrom_default
        }
    }
    fun adjustTimeItself(ma: MainActivity, secAdjustment: Long, boSwipe: Boolean = false){
        if(TimerManager.boTimerRunning) {
            adjustRunningTime(ma, secAdjustment)
        }else { //!boTimerRunning -> bd.btnAdd or SwipeRight/SwipeLeft
            adjustOffsetFromStart(ma, secAdjustment, boSwipe)
        }
        UIUpdater.updateTimeAndUI(ma, true) //adjustTimeItself
        if(TimerManager.boTimerRunning) MediaManager.playMinuteSounds(ma)
        UserPrefsManager.saveUserPrefs(ma)
    }
    private fun adjustRunningTime(ma: MainActivity, secAdjustment: Long){
        if (secAdjustment > 0) { //add
            TimeStates.millisTimeStamp_StartToInfinity -= TimeStates.millisOneSecond
            UserInstructor.showTheToast(ma, "+ 1 " + ma.getString(R.string.one_second))
        } else{ //substract
            if(System.currentTimeMillis() - TimeStates.millisTimeStamp_StartToInfinity > TimeStates.millisOneSecond){
                TimeStates.millisTimeStamp_StartToInfinity += TimeStates.millisOneSecond
                UserInstructor.showTheToast(ma, "- 1 " + ma.getString(R.string.one_second))
            }else{ //substract below 0
                UserInstructor.showTheToast(ma, ma.getString(R.string._00_00))
            }
        }
    }
    private fun adjustOffsetFromStart(ma: MainActivity, secAdjustment: Long, boSwipe: Boolean = false){
        val secsAddingSteps = 30L
        secsOffsetFromStart = if(secAdjustment > 0){ //btnAdd or SwipeRight
            when (secsOffsetFromStart){
                in 0 until secsMaxOffsetFromStart                                 -> secsOffsetFromStart + 1
                secsMaxOffsetFromStart                                            -> secsOffsetFromStart + (secsAddingSteps - secsMaxOffsetFromStart)
                in secsAddingSteps..(secsToCountdownFrom - (2 * secsAddingSteps)) -> secsOffsetFromStart + secsAddingSteps
                else                                                              -> 0
            }
        }else{ //SwipeLeft
            when (secsOffsetFromStart){
                in 1..secsMaxOffsetFromStart -> secsOffsetFromStart - 1
                secsAddingSteps              -> secsOffsetFromStart - (secsAddingSteps - secsMaxOffsetFromStart)
                else                         -> max(secsOffsetFromStart - secsAddingSteps, 0)
            }
        }
        UserInstructor.showTheToast(ma,
                                    when (secsOffsetFromStart) {
                                        0L                           -> ma.getString(R.string.starts_and_restarts_from) + "$secsOffsetFromStart"
                                        in 1..secsMaxOffsetFromStart -> ma.getString(R.string.starts_and_restarts_from) + "$secsOffsetFromStart" +
                                                if (!boSwipe) {
                                                    "\n" + ma.getString(R.string.long_press_to_reset)
                                                } else {
                                                    ""
                                                }
                                        else                         -> ma.resources.getQuantityString(R.plurals.starts_from_many, secsOffsetFromStart.toInt(), secsOffsetFromStart.toInt()
                                        ) + "\n" + ma.getString(R.string.resets_from_zero) +
                                                if (!boSwipe) {
                                                    "\n" + ma.getString(R.string.long_press_to_reset)
                                                } else {
                                                    ""
                                                }
                                    }
        )
    }

}