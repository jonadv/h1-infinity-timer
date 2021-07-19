package com.bobindustriesbv.halo1infinitytimer

import android.annotation.SuppressLint
import android.media.*
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.bobindustriesbv.halo1infinitytimer.databinding.ActivityMainBinding
import java.sql.Timestamp
import kotlin.Long.Companion.MAX_VALUE

class MainActivity : AppCompatActivity (){
    companion object {
        var boBackbuttonPressedOnceRecently = false
        var boVolumeCheckOnCreateDone = false
        var boFirstActivation = UserPrefsManager.boFirstActivation_default

        var boDbg = false
        var boDbgTxt = false
    }
    lateinit var bd : ActivityMainBinding //Jetpack view binding

    var theTimer: PreciseCountdown = object : PreciseCountdown(MAX_VALUE, TimeStates.millisOneSecond) {
        override fun onTick(timeLeft: Long) {
            val ma = this@MainActivity
            try{
                if (boFirstActivation){
                    TimeStates.updateTimes(ma) //theTimer
                    ma.runOnUiThread { UIUpdater.updateTimeTexts(ma)  }
                    ma.runOnUiThread { MediaManager.playMinuteSounds(ma) }
                }
                boFirstActivation = true
            }catch (e: Exception){
                UserInstructor.showTheToast(ma ,getString(R.string.error_timer_ticking), true)
            }
        }
        override fun onFinished() {
            onTick(0) // when the timer finishes onTick isn't called
            UserInstructor.showTheToast(this@MainActivity,getString(R.string.timer_reached_infinity), true)
        }
    }
    @SuppressLint("ClickableViewAccessibility") //tbv btnTopOverlay
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bd = ActivityMainBinding.inflate(layoutInflater)
        this.setContentView(bd.root)

        UserPrefsManager.getUserPrefs(this)
        UIUpdater.updateTimeAndUI(this, true) //onCreate

        if(!UserInstructor.boTutorialFinished){
            UserInstructor.runTutorial(this)//onCreate
        }
        if(!boVolumeCheckOnCreateDone) {
            Handler().postDelayed({
                    VolumeChecker.checkVolume(this,true) //onCreate
            },  3000)
            boVolumeCheckOnCreateDone = true
        }

        bd.btnMinuteSetting.setOnClickListener{
            UserSettings.adjustMinuteSetting()
            UIUpdater.updateTimeAndUI(this, !TimerManager.boTimerRunning) //btnMinuteSetting
            when(UserSettings.secsToCountdownFrom) {
                60L -> UserInstructor.showTheToast(this,resources.getQuantityString(R.plurals.counting_down_from_minute,1,1) + ":\n" + getString(R.string.maps_minute_one))
                120L -> UserInstructor.showTheToast(this,resources.getQuantityString(R.plurals.counting_down_from_minute,2,2) + ":\n" + getString(R.string.maps_minute_two))
                180L -> UserInstructor.showTheToast(this,resources.getQuantityString(R.plurals.counting_down_from_minute,3,3) + ":\n" + getString(R.string.maps_minute_three))
            }
            UserPrefsManager.saveUserPrefs(this)
        }
        bd.btnStartInfinity.setOnClickListener {
            VolumeChecker.checkVolume(this) //btnStart
            TimerManager.startOrRestartTheTimer(this)  //start
            MediaManager.playMinuteSounds(this)
            UIUpdater.updateTimeAndUI(this) //btnStart (only ActionButtons?)
            UserPrefsManager.saveUserPrefs(this)
        }
        bd.btnRestart.setOnClickListener {
            if(!TimerManager.boTimerRunning){
                TimerManager.startOrRestartTheTimer(this) //restart from stop - never happens/for safety when returning from onPause/other activity
            }else{
                TimerManager.stopTheTimer(this) //before restart
                TimeCalculator.recalcOffsetFromStart()
                TimerManager.startOrRestartTheTimer(this) //restart from running
            }
            UIUpdater.updateTimeAndUI(this) //btnRestart
            UserPrefsManager.saveUserPrefs(this)
        }
        bd.btnAdd.setOnClickListener {
            UserSettings.adjustTimeItself(this, 1L) //btnAdd
        }
        bd.btnAdd.setOnLongClickListener{
            UserPrefsManager.resetSettings() //btnAdd long
            UIUpdater.updateTimeAndUI(this) //btnAdd long
            UserInstructor.showTheToast(this,getString(R.string.settings_back_to_default))
            UserPrefsManager.saveUserPrefs(this)
            return@setOnLongClickListener true
        }
        bd.btnClear.setOnClickListener {
            TimerManager.stopTheTimer(this) //btnClear
            UIUpdater.updateTimeAndUI(this) //btnClear
            UserPrefsManager.saveUserPrefs(this)
        }
        bd.btnTopOverlay.setOnTouchListener(object : OnSwipeTouchListener(this) {
            val ma = this@MainActivity
            override fun onSwipeTop() {UserSettings.switchBigSmallTimerTexts(ma)}
            override fun onSwipeBottom() {UserSettings.switchBigSmallTimerTexts(ma)}
            override fun onSwipeLeft()  {UserSettings.adjustTimeItself(ma, -1L, true)} //SwipeLeft
            override fun onSwipeRight() {UserSettings.adjustTimeItself(ma, 1L, true)} //SwipeRight
        })
        bd.btnMidOverlayDbg.setOnClickListener {
            //only if boDbg is true

            UserInstructor.showTheToast(this,"dbg: ")
           //startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS))

        }
    }
    override fun onResume() {
        super.onResume()
        MediaManager.createMedia(this)
        if (TimerManager.boTimerRunning) {
            TimerManager.startOrRestartTheTimerItself(this) //onResume
        }
    }
    override fun onPause() {
        super.onPause()
        MediaManager.releaseMedia(this)
        TimerManager.stopTheTimerItself(this) //onPause
    }
    override fun onBackPressed() {
        try{
            if (boBackbuttonPressedOnceRecently) {
                super.onBackPressed()
                return
            }
            UserInstructor.showTheToast(this, getString(R.string.exit_press_again))
            MediaManager.playRandomExitSound(this)
            boBackbuttonPressedOnceRecently = true
            Handler().postDelayed({
                boBackbuttonPressedOnceRecently = false
            }, 2000)
        }catch (e: Exception){
            e.printStackTrace()
            return
        }
    }

    fun dbg(str: String){
        if(boDbg)
            println("\n !!! " + Timestamp(System.currentTimeMillis()).toString() + "  " + str)
    }
}


