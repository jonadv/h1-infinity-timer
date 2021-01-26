package com.bobindustriesbv.halo1infinitytimer

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils.concat
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import kotlinx.android.synthetic.main.activity_main.*
import net.mabboud.OneTimeBuzzer
import java.sql.Timestamp
import kotlin.Long.Companion.MAX_VALUE
import kotlin.math.max
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity (){
    private companion object {
        var boDbg = true
        var boDbgTxt = false
        const val iDbg_AddedAtStart_Seconds: Long = 0//45
        const val iDbg_AddedAtStart_Minutes: Long = 0//13
        const val iDbg_AddedAtStart_Hours: Long = 0
        const val iDbg_AddedAtStart_Days: Long = 0//247
        const val iDbg_AddedAtStart_Years: Long = 0//532
        //2552, sept 19. "John-117 is awoken from cryo sleep and tasked with preventing Cortana's capture by the Covenant." https://halo.fandom.com/wiki/2552

        //defaults for saving in sharedPrefFile
        const val sharedPrefFile = "h1_infinity_timer_settings"
        const val boSettingsAvailable_default = false
        const val boFirstActivation_default = false
        const val boTimerRunning_default = false
        const val boBigTimeIsTotalTime_default = false
        const val millisTimeStamp_StartToInfinity_default: Long = 0
        const val secsToCountdownFrom_default: Long = 120 //was timSetting
        const val secsAddedToCountdownStart_default: Long = 0
        var boTutorialFinished = false
        var boTutorialHideToasts = false
        var boSettingsAvailable = boSettingsAvailable_default
        var boFirstActivation = boFirstActivation_default
        var boTimerRunning = boTimerRunning_default
        var boBigTimeIsTotalTime = boBigTimeIsTotalTime_default
        var millisTimeStamp_StartToInfinity: Long = millisTimeStamp_StartToInfinity_default
        var secsToCountdownFrom: Long = secsToCountdownFrom_default
        var secsAddedToCountdownStart: Long = secsAddedToCountdownStart_default
        var doubleBackToExitPressedOnce = false

        //not saved
        var secsSinceStart_total: Long = 0
        var secsSinceStart_down: Long = secsToCountdownFrom
        var millisOneSecond: Long = 1000
        var secsOneMinuteState = 60L
        var secsAdding_steps = 15L
        var secsMaxAddedToCount_Reset: Long = 3L

        const val secsMIN: Double = 60.0
        const val secsHOUR: Double = secsMIN * 60
        const val secsDAY: Double = secsHOUR * 24
        const val secsYEAR : Double = secsDAY * 365.25
        //Julian astronomical year https://www.rapidtables.com/calc/time/seconds-in-year 365.25
        //https://www.volkskrant.nl/wetenschap/afgesproken-jaar-duurt-31-556-925-445-seconden~ba74b7f2/ 31556925.445
        const val secsMONTH: Double = secsYEAR / 12

        //media
        val iCountDownSounds: IntArray = IntArray(10)
        var iBeepShortStart = 11
        var iBeepShortHalfMinute = 12
        var iBeepDouble2ndHalfMinute = 13
        var iBeepLongEndCount = 14
        var iExitSounds = ArrayList<Int>()
    }
    private var theToast: Toast? = null
    private val tonePlayer by lazy { OneTimeBuzzer() } //only for lower APIs
    private lateinit var soundPool: SoundPool
    private lateinit var mpRandExitSound: MediaPlayer

    override fun onResume() {
        super.onResume()
        createMedia()
        if (boTimerRunning) {
            startOrRestartTheTimerItself() //onResume
        }
    }
    override fun onPause() {
        super.onPause()
        releaseMedia()
        stopTheTimerItself() //onPause
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_main)

        getUserPrefs()
        updateTimeAndUI(true) //onCreate
        if(!boTutorialFinished) runTutorial()

        btnMinuteSetting.setOnClickListener{
            adjustTimeSetting()
            updateTimeAndUI() //btnMinuteSetting
            when(secsToCountdownFrom) {
                60L -> showTheToast(resources.getQuantityString(R.plurals.counting_down_from_minute, 1, 1) + ":\n" + getString(R.string.maps_minute_one))
                120L -> showTheToast(resources.getQuantityString(R.plurals.counting_down_from_minute, 2, 2) + ":\n" + getString(R.string.maps_minute_two))
                180L -> showTheToast(resources.getQuantityString(R.plurals.counting_down_from_minute, 3, 3)+ ":\n" + getString(R.string.maps_minute_three))
            }
            saveUserPrefs()
        }
        btnStartInfinity.setOnClickListener {
            startOrRestartTheTimer()  //start
            playMinuteSounds()
            updateTimeAndUI() //btnStart (only ActionButtons?)
            saveUserPrefs()
        }
        btnRestart.setOnClickListener{
            if(!boTimerRunning){
                startOrRestartTheTimer() //restart from stop - never happens/for safety when returning from onPause/other activity
            }else{
                stopTheTimer() //before restart
                startOrRestartTheTimer() //restart from running
            }
            updateTimeAndUI() //btnRestart
            saveUserPrefs()
        }
        btnAdd.setOnClickListener {
            smallTimeAdjustment(1L) //btnAdd
        }
        btnAdd.setOnLongClickListener{
            resetSettings() //btnAdd long
            updateTimeAndUI() //btnAdd long
            showTheToast(getString(R.string.settings_back_to_default))
            saveUserPrefs()
            return@setOnLongClickListener true
        }
        btnClear.setOnClickListener {
            stopTheTimer() //btnClear
            updateTimeAndUI() //btnClear
            saveUserPrefs()
        }
        btnTopOverlay.setOnTouchListener( object: OnSwipeTouchListener(this) {
            override fun onSwipeTop() {switchBigSmallTimerTexts()}
            override fun onSwipeBottom() {switchBigSmallTimerTexts()}
            override fun onSwipeLeft()  {smallTimeAdjustment(-1L, true)} //SwipeLeft
            override fun onSwipeRight() {smallTimeAdjustment(1L, true)} //SwipeRight
        })
        btnMidOverlay_dbg.setOnClickListener{
            //only if boDbg is true
            //showTheToast("err")
        }
    }
    override fun onBackPressed() {
        try{
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed()
                return
            }
            doubleBackToExitPressedOnce = true
            showTheToast(getString(R.string.exit_press_again))
            playRandomExitSound()
            Handler().postDelayed({
                doubleBackToExitPressedOnce = false
            }, 2000)
        }catch(e : Exception){
            e.printStackTrace()
            return
        }
    }
    private fun switchBigSmallTimerTexts(){
        boBigTimeIsTotalTime = !boBigTimeIsTotalTime
        updateTimeTexts() //switch Big Small
        updateTextColors()
        showTheToast(if (boBigTimeIsTotalTime) getString(R.string.countdown_on_bottom) else getString(R.string.countdown_on_top))
        saveUserPrefs()
    }
    private fun smallTimeAdjustment(secAdjustment: Long, boSwipe: Boolean = false){
        if(boTimerRunning) {
            if (secAdjustment > 0) {
                millisTimeStamp_StartToInfinity -= 1000
                showTheToast("+ 1 " + getString(R.string.one_second))
            } else{
                if(System.currentTimeMillis() - millisTimeStamp_StartToInfinity  > 1000){
                    millisTimeStamp_StartToInfinity += 1000
                    showTheToast("- 1 " + getString(R.string.one_second))
                }else{
                    showTheToast(getString(R.string._00_00))
                }
            }
        }else{ //!boTimerRunning -> btnAdd or SwipeRight
            secsAddedToCountdownStart = if(secAdjustment > 0){ //btnAdd or SwipeRight
                when (secsAddedToCountdownStart){
                    in 0 until secsMaxAddedToCount_Reset ->  secsAddedToCountdownStart+1
                    secsMaxAddedToCount_Reset -> secsAddedToCountdownStart+(secsAdding_steps-secsMaxAddedToCount_Reset)
                    in secsAdding_steps..(secsToCountdownFrom-(2*secsAdding_steps)) -> secsAddedToCountdownStart+secsAdding_steps
                    else -> 0
                }
            }else{ //SwipeLeft
                when (secsAddedToCountdownStart){
                    in 1..secsMaxAddedToCount_Reset -> secsAddedToCountdownStart-1
                    secsAdding_steps -> secsAddedToCountdownStart - (secsAdding_steps - secsMaxAddedToCount_Reset)
                    else -> max(secsAddedToCountdownStart - secsAdding_steps, 0)
                }
            }
            showTheToast(when(secsAddedToCountdownStart){
                0L ->  getString(R.string.starts_and_restarts_from) + "$secsAddedToCountdownStart"
                in 1..secsMaxAddedToCount_Reset -> getString(R.string.starts_and_restarts_from) + "$secsAddedToCountdownStart" +
                        if(!boSwipe){"\n" + getString(R.string.long_press_to_reset)}else{""}
                else -> resources.getQuantityString(R.plurals.starts_from_many,secsAddedToCountdownStart.toInt(),secsAddedToCountdownStart.toInt()) +
                        "\n" + getString(R.string.resets_from_zero) +
                        if(!boSwipe){"\n" + getString(R.string.long_press_to_reset)}else{""}
            })
        }
        updateTimeAndUI(true)
        if(boTimerRunning) playMinuteSounds(true)
        saveUserPrefs()
    }
    private var theTimer: PreciseCountdown = object : PreciseCountdown(MAX_VALUE, millisOneSecond) {
        override fun onTick(timeLeft: Long) {
            try{
                if (boFirstActivation){
                    updateTimes() //theTimer
                    this@MainActivity.runOnUiThread { updateTimeTexts() }
                    this@MainActivity.runOnUiThread { playMinuteSounds() }
                }
                boFirstActivation = true
            }catch(e: Exception){
                showTheToast(getString(R.string.error_timer_ticking), true)
            }
        }
        override fun onFinished() {
            onTick(0) // when the timer finishes onTick isn't called
            showTheToast(getString(R.string.timer_reached_infinity), true)
        }
    }
    private fun saveUserPrefs() {
        try {
            val sharedPref: SharedPreferences = this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
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
        }catch(e: Exception){
            showTheToast(getString(R.string.error_saving_preferences))
        }
    }
    private fun getUserPrefs(){
        try{
            val sharedPref: SharedPreferences = this.getSharedPreferences(sharedPrefFile, Context.MODE_PRIVATE)
            boSettingsAvailable = sharedPref.getBoolean("boSettingsAvailable", boSettingsAvailable)
            boTutorialFinished =  sharedPref.getBoolean("boTutorialFinished", boTutorialFinished)
            if(boSettingsAvailable){
                boFirstActivation = sharedPref.getBoolean("boFirstActivation", boFirstActivation)
                boTimerRunning = sharedPref.getBoolean("boTimeRunning", boTimerRunning)
                boBigTimeIsTotalTime = sharedPref.getBoolean("boBigTimeIsTotalTime", boBigTimeIsTotalTime)
                millisTimeStamp_StartToInfinity = sharedPref.getLong("millisTimeStamp_StartToInfinity", millisTimeStamp_StartToInfinity)
                secsToCountdownFrom = sharedPref.getLong("secsToCountdownFrom", secsToCountdownFrom)
                secsAddedToCountdownStart = sharedPref.getLong("secsAddedToCountdownStart", secsAddedToCountdownStart)
            }
        }catch(e: Exception){
            showTheToast(getString(R.string.error_getting_preferences))
        }
    }
    private fun resetSettings(){
        boSettingsAvailable = boSettingsAvailable_default
        boFirstActivation = boFirstActivation_default
        boTimerRunning = boTimerRunning_default
        boBigTimeIsTotalTime = boBigTimeIsTotalTime_default
        millisTimeStamp_StartToInfinity = millisTimeStamp_StartToInfinity_default
        secsToCountdownFrom = secsToCountdownFrom_default
        secsAddedToCountdownStart = secsAddedToCountdownStart_default
    }
    private fun startOrRestartTheTimer(){
        boTimerRunning = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            //iBeepShortStart wont load on older devices. Unclear why
            tonePlayer.play()
        }else {
            playSound(iBeepShortStart)
        }
        millisTimeStamp_StartToInfinity = System.currentTimeMillis()
        millisTimeStamp_StartToInfinity -= (secsAddedToCountdownStart * 1000)
        if (boDbg){ //add debug time
            millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Seconds * 1000)
            millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Minutes * 1000 * secsMIN.toLong())
            millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Hours * 1000 * secsHOUR.toLong())
            millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Days * 1000 * secsDAY.toLong())
            millisTimeStamp_StartToInfinity -= (iDbg_AddedAtStart_Years * 1000 * secsYEAR.toLong())
        }
        //millisTimeStamp_StartToInfinity = maxOf(millisTimeStamp_StartToInfinity, 0) //for <1970 (EPOCH), deactivated cause solved with double minus
        startOrRestartTheTimerItself()
    }
    private fun stopTheTimer(){
        boTimerRunning = false
        stopTheTimerItself()
        secsAddedToCountdownStart = if (secsAddedToCountdownStart > secsMaxAddedToCount_Reset) 0 else secsAddedToCountdownStart
/*        releaseMedia() //instant stops countdown
        createMedia()*/
        boFirstActivation = false
    }
    private fun startOrRestartTheTimerItself(){
        try{
            theTimer.restart()
        }catch(e: Exception){
            showTheToast(getString(R.string.error_timer_start), true)
        }
    }
    private fun stopTheTimerItself(){
        try{
            theTimer.stop()
        }catch(e: Exception){
            try{
                startOrRestartTheTimerItself()
                theTimer.stop()
            }catch(e: Exception){
                //should only happen if timer thread is killed (by overuse of sources and running in background or by calling .dispose -> ("Timer already cancelled."))
                showTheToast(getString(R.string.error_timer_stop), true)
            }
        }
    }
    private fun updateTimes(boSkipAddSecs: Boolean = false){
        try{
            if (boTimerRunning) {
                secsSinceStart_total = ((System.currentTimeMillis() - millisTimeStamp_StartToInfinity ) / 1000)
                secsSinceStart_down = getSecondState(secsToCountdownFrom, secsSinceStart_total)
                secsOneMinuteState = getSecondState(60L, secsSinceStart_total)
            }else{
                if(!boSkipAddSecs) {secsAddedToCountdownStart = if (secsAddedToCountdownStart > secsMaxAddedToCount_Reset) 0 else secsAddedToCountdownStart}
                secsSinceStart_total = secsAddedToCountdownStart
                secsSinceStart_down = secsToCountdownFrom - secsAddedToCountdownStart
                secsOneMinuteState = 60L - secsAddedToCountdownStart
            }
        }catch(e: Exception){
            showTheToast(getString(R.string.error_updating_times))
        }
    }
    private fun getSecondState(secsInput: Long, secsBig: Long): Long{
        val t1 = secsBig.toDouble() / secsInput.toDouble()
        //dbg("getSecondState --> secsInput: $secsInput  secsBig: $secsBig result: " + (secsInput - (t1 - t1.toInt()) * secsInput).toDouble())
        return (secsInput - (t1 - t1.toInt()) * secsInput).roundToLong()
    }
    private fun adjustTimeSetting(){
        when(secsToCountdownFrom) {
            60L -> secsToCountdownFrom = 120L
            120L -> secsToCountdownFrom = 180L
            180L -> secsToCountdownFrom = 60L
        }
        secsSinceStart_down = getSecondState(secsToCountdownFrom, secsSinceStart_total)
        secsAddedToCountdownStart = if( secsAddedToCountdownStart > secsMaxAddedToCount_Reset) 0 else secsAddedToCountdownStart
    }
    private fun createMedia(){
        try {
            soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //https://codinginflow.com/tutorials/android/soundpool
                //https://stackoverflow.com/questions/28210921/set-audio-attributes-in-soundpool-builder-class-for-api-21
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(audioAttributes)
                    .build()
            }else {
                @Suppress("DEPRECATION")
                SoundPool(6, AudioManager.STREAM_MUSIC, 0)
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                //iBeepShortStart wont load on older devices. Unclear why
                tonePlayer.duration = 0.15
                //minimum to play on emulator is 0.07
                //minimum to play on Huawei TAG-L21 (android 5.1): 10 ms
                //minimum to play on Galaxy S10e TAG-L21 (android 10): 13 ms
                tonePlayer.toneFreqInHz = 1900.0
            }

            //loading these 14 files in SoundPool takes about 2 full seconds.
            //secsOneMinuteState.toInt() is used instead of iCountDownSounds.
            iCountDownSounds[0] = soundPool.load(this, R.raw.count_01,1)
            iCountDownSounds[1] = soundPool.load(this, R.raw.count_02,1)
            iCountDownSounds[2] = soundPool.load(this, R.raw.count_03,1)
            iCountDownSounds[3] = soundPool.load(this, R.raw.count_04,1)
            iCountDownSounds[4] = soundPool.load(this, R.raw.count_05,1)
            iCountDownSounds[5] = soundPool.load(this, R.raw.count_06,1)
            iCountDownSounds[6] = soundPool.load(this, R.raw.count_07,1)
            iCountDownSounds[7] = soundPool.load(this, R.raw.count_08,1)
            iCountDownSounds[8] = soundPool.load(this, R.raw.count_09,1)
            iCountDownSounds[9] = soundPool.load(this, R.raw.count_10,1)
            iBeepShortStart = soundPool.load(this, R.raw.beep_short_start,1)
            iBeepShortHalfMinute = soundPool.load(this, R.raw.beep_short_30secs,1)
            iBeepDouble2ndHalfMinute = soundPool.load(this, R.raw.beep_double_90secs,1)
            iBeepLongEndCount = soundPool.load(this, R.raw.beep_long_0sec,1)

            iExitSounds.add(R.raw.exit_cort_halo_its_finished)
            iExitSounds.add(R.raw.exit_cort_halo_its_finished)
            iExitSounds.add(R.raw.exit_cort_sleep_well)
            iExitSounds.add(R.raw.exit_cort_sleep_well)
            iExitSounds.add(R.raw.exit_cort_sleep_well)
            iExitSounds.add(R.raw.exit_mc_doit)
            iExitSounds.add(R.raw.exit_mc_enough)
            iExitSounds.add(R.raw.exit_monitor_ah_pity)
            iExitSounds.add(R.raw.exit_monitor_how_unfortunate)

            mpRandExitSound = MediaPlayer.create(this, iExitSounds[0])
        }catch(e : Exception){
            e.printStackTrace()
            showTheToast(getString(R.string.error_audio_creating), true)
        }
    }
    private fun releaseMedia(){
        try {
            soundPool.release()
            mpRandExitSound.release()
        }catch(e : Exception){
            if (boDbg){
                e.printStackTrace()
                showTheToast(getString(R.string.error_audio_releasing))
            }
        }
    }
    private fun playSound(SoundID: Int,  boSwipe: Boolean = false){
        fun playIt(theID: Int, aSwipe: Boolean = false){
            soundPool.play(theID, 1F, 1F, 0, 0, 1.0F)
            if(aSwipe){soundPool.autoPause()}
        }
        try{
            playIt(SoundID, boSwipe)
        }catch(e: Exception){
            try{
                dbg("!! Recreating audio from playSound")
                releaseMedia() // err playing
                createMedia() // err playing
                playIt(SoundID, boSwipe)
            }catch(e: Exception){
                showTheToast(getString(R.string.error_audio_playing_sound))
            }
        }
    }
    private fun playMinuteSounds(boSwipe: Boolean = false){
        when (secsOneMinuteState){
            in 1L..10L -> playSound(secsOneMinuteState.toInt(), boSwipe) // iCounts
            30L -> when {(secsSinceStart_down == 90L) && (secsToCountdownFrom == 180L)
            -> playSound(iBeepDouble2ndHalfMinute)
                else -> playSound(iBeepShortHalfMinute)
            }
            60L -> if(secsSinceStart_total > 59) playSound(iBeepLongEndCount)
        }
    }
    private fun playRandomExitSound(){
        try{
            val randomInt = (1..iExitSounds.size).random()
            mpRandExitSound.release()
            mpRandExitSound = MediaPlayer.create(this, iExitSounds[randomInt])
            mpRandExitSound.start()
        }catch(e : Exception){
            e.printStackTrace()
            //no toast cause might always bug on older devices < API 23
        }
    }
    private fun updateTimeAndUI(boSkipAddSecs: Boolean = false){
        updateTimes(boSkipAddSecs) //updateTimeAndUI
        updateTimeTexts() //updateTimeAndUI
        updateTextColors()
        updateActionButtons() //updateTimeAndUI
    }
    private fun updateActionButtons(){
        try{
            imgBackground_timer.visibility = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            imgBackground_timer_off.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            imgInfinity.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            btnStartInfinity.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            imgRestart.visibility  = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            btnRestart.visibility = if(boTimerRunning){
                when (secsAddedToCountdownStart){
                    1L ->  imgRestart.setImageResource(R.drawable.ic_restart_from_1)
                    2L ->  imgRestart.setImageResource(R.drawable.ic_restart_from_2)
                    3L ->  imgRestart.setImageResource(R.drawable.ic_restart_from_3)
                    else -> imgRestart.setImageResource(R.drawable.ic_restart_from_0)
                }
                View.VISIBLE}else{View.INVISIBLE}
            imgClear.visibility  = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            btnClear.visibility  = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            imgAdd.visibility = if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            btnAdd.visibility  = if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            if (!boDbg) btnMidOverlay_dbg.visibility = View.GONE
            when(secsToCountdownFrom) {
                60L -> imgMinuteSetting.setImageResource(R.drawable.ic_nr_i)
                120L -> imgMinuteSetting.setImageResource(R.drawable.ic_nr_ii)
                180L -> imgMinuteSetting.setImageResource(R.drawable.ic_nr_iii)
            }
        }catch(e: Exception){
            if(boDbg) showTheToast(getString(R.string.error_showing_buttons))
        }
    }
    private fun updateTextColors(){
        //imgClear, imgRestart en imgInfinity zijn vaste kleur ivm zichtbaarheid
        if (!boTimerRunning) {
            imgMinuteSetting.setColorFilter(resources.getColor(R.color.WhiteGrey))
            if (boBigTimeIsTotalTime) {
                edtTimeSmall.setTextColor(resources.getColor(R.color.WhiteGrey))
                edtTimeBig.setTextColor(resources.getColor(R.color.WhiteGreyLight))
            }else{
                edtTimeSmall.setTextColor(resources.getColor(R.color.WhiteGreyLight))
                edtTimeBig.setTextColor(resources.getColor(R.color.WhiteGrey))
            }
        } else {
            imgMinuteSetting.setColorFilter (resources.getColor(R.color.WhiteGreyDark))
            if (boBigTimeIsTotalTime) {
                edtTimeSmall.setTextColor(resources.getColor(R.color.WhiteGreyDark))
                edtTimeBig.setTextColor(resources.getColor(R.color.WhiteGreyLight))
            }else{
                edtTimeSmall.setTextColor(resources.getColor(R.color.WhiteGreyLight))
                edtTimeBig.setTextColor(resources.getColor(R.color.WhiteGreyDark))
            }
        }
    }
    private fun updateTimeTexts(){
        if (boBigTimeIsTotalTime) {
            edtTimeBig.text = longToTimeText(secsSinceStart_total, "top", "total")
            edtTimeSmall.text = longToTimeText(secsSinceStart_down, "bottom", "down")
        } else {
            edtTimeBig.text = longToTimeText(secsSinceStart_down, "top", "down")
            edtTimeSmall.text = longToTimeText(secsSinceStart_total, "bottom", "total")
        }
        edtTimeBig.maxLines = 1
        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            if(secsSinceStart_total > secsMONTH) edtTimeBig.maxLines = 2 else edtTimeBig.maxLines = 1
        }
    }
    private fun longToTimeText(aTimeInSeconds: Long, strTopOrBottom: String, strTotalOrDown: String): String{
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

        val strTimeHas : String = if(strTotalOrDown == "total") concat(if(years > 0) "y" else "",
            if(months > 0) "m" else "",
            if(days > 0) "d" else "" ,
            if(hours > 0) "h" else "") as String else{""}

        val strWayToWrite = if(resources.configuration.orientation != Configuration.ORIENTATION_PORTRAIT &&
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
                "y" -> spaceBefore+"$iTime" +"year" + addS + addSemiCol + spaceAfter
                "m" -> spaceBefore+"$iTime" +"month" + addS + addSemiCol + spaceAfter
                "d" -> spaceBefore+"$iTime" +"day" + addS + addSemiCol + spaceAfter
                "h" -> spaceBefore+"$iTime" +"hour" + addS + addSemiCol + spaceAfter
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
    private fun runTutorial() {
        resetSettings() // tutorial start
        updateTimeAndUI() //tutorial start
        boTutorialHideToasts = true
        try {
            var tutorialStep = 0
            @SuppressLint("SourceLockedOrientationActivity")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            val introIconHor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                resources.getDrawable(R.drawable.ic_swap_horiz_lined, theme)
            } else {
                AppCompatResources.getDrawable(this, R.drawable.ic_swap_horiz_lined)
            }
            val introIconVert = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                resources.getDrawable(R.drawable.ic_swap_vertic_lined, theme)
            } else {
                AppCompatResources.getDrawable(this, R.drawable.ic_swap_vertic_lined)
            }
            //https://android-arsenal.com/details/1/4338
            TapTargetSequence(this)
                .targets(
                    //0 one time tutorial, press circles
                    TapTarget.forView(btnTopOverlay,getString(R.string.tutorial_1_first_press_circle_title), getString(R.string.tutorial_1_first_press_circle))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(false).cancelable(false)
                        .targetRadius(80),
                    //1 minute setting 1, 2, 3
                    TapTarget.forView(btnMinuteSetting,getString(R.string.tutorial_2_minsetting_title),getString(R.string.tutorial_2_minsetting))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false),
                    //2 add
                    TapTarget.forView(btnAdd,getString(R.string.tutorial_3_add_title),getString(R.string.tutorial_3_add))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false),
                    //3 start
                    TapTarget.forView(btnStartInfinity,getString(R.string.tutorial_4_start_title), "")
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                        .targetRadius(100),
                    //4 swipe add/subtract
                    TapTarget.forView(btnTopOverlay,getString(R.string.tutorial_5_swipe_title),getString(R.string.tutorial_5_swipe))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                        .icon(introIconHor, false).targetRadius(170),
                    //5 swipe up/down
                    TapTarget.forView(btnTopOverlay, getString(R.string.tutorial_6_switch_title),getString(R.string.tutorial_6_switch))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(true).cancelable(false)
                        .icon(introIconVert, false).targetRadius(170),
                    //6 close
                    TapTarget.forView(btnTopOverlay,getString(R.string.tutorial_7_close_title),getString(R.string.tutorial_7_close))
                        .outerCircleColor(R.color.GreyDark).targetCircleColor(R.color.WhiteGrey).textColor(R.color.WhiteGrey).textTypeface(Typeface.DEFAULT_BOLD)
                        .dimColor(R.color.Black).drawShadow(true).tintTarget(true).transparentTarget(false).cancelable(false)
                        .targetRadius( 80)
                )
                .listener(object : TapTargetSequence.Listener {
                    override fun onSequenceStep(lastTarget: TapTarget?, targetClicked: Boolean) {
                        when (tutorialStep) {
                            1 -> btnMinuteSetting.performClick()
                            2 -> btnAdd.performClick()
                            3 -> btnStartInfinity.performClick()
                            4 -> {boTutorialHideToasts = false
                                smallTimeAdjustment(1, true) //tutorial
                                boTutorialHideToasts = true}
                            5 -> switchBigSmallTimerTexts()
                            else -> {}
                        }
                        tutorialStep += 1
                    }
                    override fun onSequenceCanceled(lastTarget: TapTarget?) {}
                    override fun onSequenceFinish() {
                        boTutorialFinished = true
                        boTutorialHideToasts = false
                        resetSettings() //tutorial finish
                        saveUserPrefs()
                        updateTimeAndUI() //tutorial finish
                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    }
                }).start()
        }catch (e: Exception){
            showTheToast(getString(R.string.error_tutor_tutorial))
            boTutorialFinished = true
        }
    }
    private fun showTheToast(strToast: String, boLong: Boolean = false){
        // https://stackoverflow.com/questions/2755277/android-hide-all-shown-toast-messages  user "olearyj234"
        if (boTutorialHideToasts) return
        try{
            dbg("ShowToast input: $strToast")
            theToast?.cancel()
            theToast = Toast.makeText(this@MainActivity, strToast,
                if(boLong or boTutorialHideToasts) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).also {
                it?.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.CENTER  , 0, 0)
                it?.show()
            }
        } catch(e: Exception){
            e.printStackTrace()
            Toast.makeText(this@MainActivity, getString(R.string.error_toast), Toast.LENGTH_SHORT).also{
                it.show()
            }
        }
    }
    private fun dbg(str: String, boPrintAlways: Boolean = false){
        if(boDbg or boDbgTxt or boPrintAlways)
            println("\n !!! "+ Timestamp(System.currentTimeMillis()).toString() + "  " + str)
    }
}


