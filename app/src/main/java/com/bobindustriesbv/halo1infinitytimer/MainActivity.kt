package com.bobindustriesbv.halo1infinitytimer

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.media.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils.concat
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bobindustriesbv.halo1infinitytimer.UserInstructor.showTheToast
import com.bobindustriesbv.halo1infinitytimer.UserPrefsManager.saveUserPrefs
import com.bobindustriesbv.halo1infinitytimer.databinding.ActivityMainBinding
import net.mabboud.OneTimeBuzzer
import java.sql.Timestamp
import kotlin.Long.Companion.MAX_VALUE
import kotlin.math.max
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity (){
    companion object {
        //media
        val iCountDownSounds: IntArray = IntArray(10)
        var iBeepShortStart = 11
        var iBeepShortHalfMinute = 12
        var iBeepDouble2ndHalfMinute = 13
        var iBeepLongEndCount = 14
        var iExitSounds = ArrayList<Int>()

        //volume check
        const val iVolumeWarningOff = 1
        const val iVolumeWarningLow = 35 //  6/15 = 0.40
        var boVolumeCheckOnCreateDone = false
        var boVolumeCheckDoneRecently = false
        var secsVolumeCheckLoop = 2L
    }
//    private var theToast: Toast? = null
    private val tonePlayer by lazy { OneTimeBuzzer() } //only for lower APIs
    private lateinit var soundPool: SoundPool
    private lateinit var mpRandExitSound: MediaPlayer

    private var theTimer: PreciseCountdown = object : PreciseCountdown(MAX_VALUE, millisOneSecond) {
        override fun onTick(timeLeft: Long) {
            try{
                if (boFirstActivation){
                    updateTimes() //theTimer
                    this@MainActivity.runOnUiThread { updateTimeTexts() }
                    this@MainActivity.runOnUiThread { playMinuteSounds() }
                }
                boFirstActivation = true
            }catch (e: Exception){
                showTheToast(this@MainActivity,getString(R.string.error_timer_ticking), true)
            }
        }
        override fun onFinished() {
            onTick(0) // when the timer finishes onTick isn't called
            showTheToast(this@MainActivity,getString(R.string.timer_reached_infinity), true)
        }
    }

    private lateinit var bd : ActivityMainBinding //Jetpack view binding
    @SuppressLint("ClickableViewAccessibility") //tbv btnTopOverlay
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bd = ActivityMainBinding.inflate(layoutInflater)
        this.setContentView(bd.root)

        UserPrefsManager.getUserPrefs(this)
        updateTimeAndUI(true) //onCreate
        if(!boTutorialFinished) runTutorial(this, bd)
        if(!boVolumeCheckOnCreateDone) {
            Handler().postDelayed({
                checkVolume(true) //onCreate
            },  1000)
            boVolumeCheckOnCreateDone = true
        }

        bd.btnMinuteSetting.setOnClickListener{
            adjustTimeSetting()
            updateTimeAndUI() //btnMinuteSetting
            when(secsToCountdownFrom) {
                60L -> showTheToast(this,resources.getQuantityString(R.plurals.counting_down_from_minute,1,1) + ":\n" + getString(R.string.maps_minute_one))
                120L -> showTheToast(this,resources.getQuantityString(R.plurals.counting_down_from_minute,2,2) + ":\n" + getString(R.string.maps_minute_two))
                180L -> showTheToast(this,resources.getQuantityString(R.plurals.counting_down_from_minute,3,3) + ":\n" + getString(R.string.maps_minute_three))
            }
            saveUserPrefs(this)
        }
        bd.btnStartInfinity.setOnClickListener {
            checkVolume() //btnStart
            startOrRestartTheTimer()  //start
            playMinuteSounds()
            updateTimeAndUI() //btnStart (only ActionButtons?)
            saveUserPrefs(this)
        }
        bd.btnRestart.setOnClickListener {
            if(!boTimerRunning){
                startOrRestartTheTimer() //restart from stop - never happens/for safety when returning from onPause/other activity
            }else{
                stopTheTimer() //before restart
                startOrRestartTheTimer() //restart from running
            }
            updateTimeAndUI() //btnRestart
            saveUserPrefs(this)
        }
        bd.btnAdd.setOnClickListener {
            smallTimeAdjustment(1L) //btnAdd
        }
        bd.btnAdd.setOnLongClickListener{
            resetSettings() //btnAdd long
            updateTimeAndUI() //btnAdd long
            showTheToast(this,getString(R.string.settings_back_to_default))
            saveUserPrefs(this)
            return@setOnLongClickListener true
        }
        bd.btnClear.setOnClickListener {
            stopTheTimer() //btnClear
            updateTimeAndUI() //btnClear
            saveUserPrefs(this)
        }
        bd.btnTopOverlay.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeTop() {switchBigSmallTimerTexts()}
            override fun onSwipeBottom() {switchBigSmallTimerTexts()}
            override fun onSwipeLeft()  {smallTimeAdjustment(-1L, true)} //SwipeLeft
            override fun onSwipeRight() {smallTimeAdjustment(1L, true)} //SwipeRight
        })
        bd.btnMidOverlayDbg.setOnClickListener {
            //only if boDbg is true
/*            showTheToast(this,"dbg")
            checkVolume() //dbg*/

            showTheToast(this,"dbg: ")
           //startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS))


/*            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            when(am.isMusicActive){
                true -> showTheToast(this,"test music is active: TRUE")
                else -> showTheToast(this, "test music is active: FALSE")}
 */
        }
    }
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
    override fun onBackPressed() {
        try{
            if (boBackbuttonPressedOnceRecently) {super.onBackPressed()
                return
            }
            showTheToast(this,getString(R.string.exit_press_again))
            playRandomExitSound()
            boBackbuttonPressedOnceRecently = true
            Handler().postDelayed({
                boBackbuttonPressedOnceRecently = false
            }, 2000)
        }catch (e: Exception){
            e.printStackTrace()
            return
        }
    }
    fun switchBigSmallTimerTexts(){
        boBigTimeIsTotalTime = !boBigTimeIsTotalTime
        updateTimeTexts() //switch Big Small
        updateTextColors()
        showTheToast(this,
            if (boBigTimeIsTotalTime) getString(R.string.countdown_on_bottom) else getString(
                R.string.countdown_on_top
            )
        )
        saveUserPrefs(this)
    }
    fun smallTimeAdjustment(secAdjustment: Long, boSwipe: Boolean = false){
        if(boTimerRunning) {
            if (secAdjustment > 0) {
                millisTimeStamp_StartToInfinity -= 1000
                showTheToast(this,"+ 1 " + getString(R.string.one_second))
            } else{
                if(System.currentTimeMillis() - millisTimeStamp_StartToInfinity  > 1000){
                    millisTimeStamp_StartToInfinity += 1000
                    showTheToast(this,"- 1 " + getString(R.string.one_second))
                }else{
                    showTheToast(this,getString(R.string._00_00))
                }
            }
        }else{ //!boTimerRunning -> bd.btnAdd or SwipeRight
            secsAddedToCountdownStart = if(secAdjustment > 0){ //btnAdd or SwipeRight
                when (secsAddedToCountdownStart){
                    in 0 until secsMaxAddedToCount_Reset -> secsAddedToCountdownStart + 1
                    secsMaxAddedToCount_Reset -> secsAddedToCountdownStart + (secsAdding_steps - secsMaxAddedToCount_Reset)
                    in secsAdding_steps..(secsToCountdownFrom - (2 * secsAdding_steps)) -> secsAddedToCountdownStart + secsAdding_steps
                    else -> 0
                }
            }else{ //SwipeLeft
                when (secsAddedToCountdownStart){
                    in 1..secsMaxAddedToCount_Reset -> secsAddedToCountdownStart - 1
                    secsAdding_steps -> secsAddedToCountdownStart - (secsAdding_steps - secsMaxAddedToCount_Reset)
                    else -> max(secsAddedToCountdownStart - secsAdding_steps, 0)
                }
            }
            showTheToast(this,
                when (secsAddedToCountdownStart) {
                    0L                              -> getString(R.string.starts_and_restarts_from) + "$secsAddedToCountdownStart"
                    in 1..secsMaxAddedToCount_Reset -> getString(R.string.starts_and_restarts_from) + "$secsAddedToCountdownStart" +
                            if (!boSwipe) {
                                "\n" + getString(R.string.long_press_to_reset)
                            } else {
                                ""
                            }
                    else                            -> resources.getQuantityString(R.plurals.starts_from_many, secsAddedToCountdownStart.toInt(), secsAddedToCountdownStart.toInt()
                    ) + "\n" + getString(R.string.resets_from_zero) +
                            if (!boSwipe) {
                                "\n" + getString(R.string.long_press_to_reset)
                            } else {
                                ""
                            }
                }
            )
        }
        updateTimeAndUI(true)
        if(boTimerRunning) playMinuteSounds(true)
        saveUserPrefs(this)
    }
 
    fun resetSettings(){
        boSettingsAvailable = boSettingsAvailable_default
        boFirstActivation = boFirstActivation_default
        boTimerRunning = boTimerRunning_default
        boBigTimeIsTotalTime = boBigTimeIsTotalTime_default
        millisTimeStamp_StartToInfinity = millisTimeStamp_StartToInfinity_default
        secsToCountdownFrom = secsToCountdownFrom_default
        secsAddedToCountdownStart = secsAddedToCountdownStart_default
    }
    fun startOrRestartTheTimer(){
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
    fun stopTheTimer(){
        boTimerRunning = false
        stopTheTimerItself()
        secsAddedToCountdownStart = if (secsAddedToCountdownStart > secsMaxAddedToCount_Reset) 0 else secsAddedToCountdownStart
/*        releaseMedia() //instant stops countdown
        createMedia()*/
        boFirstActivation = false
    }
    fun startOrRestartTheTimerItself(){
        try{
            theTimer.restart()
        }catch (e: Exception){
            try{
                theTimer.stop()
                theTimer.restart()
            }catch (e: Exception){
                //versie 2 was 1 try{theTimer.restart()}catch{toast}. Toch nog af en toe error_timer_start melding gezien (na app lang open hebben met timer op pauze bijv)
                //versie 3.0 dubbele try catch met bij tweede .stop() ervoor.
                showTheToast(this,getString(R.string.error_timer_start), true)
            }
        }
    }
    fun stopTheTimerItself(){
        try{
            theTimer.stop()
        }catch (e: Exception){
            try{
                startOrRestartTheTimerItself()
                theTimer.stop()
            }catch (e: Exception){
                //should only happen if timer thread is killed (by overuse of sources and running in background or by calling .dispose -> ("Timer already cancelled."))
                showTheToast(this,getString(R.string.error_timer_stop), true)
            }
        }
    }
    fun updateTimes(boSkipAddSecs: Boolean = false){
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
        }catch (e: Exception){
            showTheToast(this,getString(R.string.error_updating_times))
        }
    }
    fun getSecondState(secsInput: Long, secsBig: Long): Long{
        val t1 = secsBig.toDouble() / secsInput.toDouble()
        //dbg("getSecondState --> secsInput: $secsInput  secsBig: $secsBig result: " + (secsInput - (t1 - t1.toInt()) * secsInput).toDouble())
        return (secsInput - (t1 - t1.toInt()) * secsInput).roundToLong()
    }
    fun adjustTimeSetting(){
        when(secsToCountdownFrom) {
            60L -> secsToCountdownFrom = 120L
            120L -> secsToCountdownFrom = 180L
            180L -> secsToCountdownFrom = 60L
        }
        secsSinceStart_down = getSecondState(secsToCountdownFrom, secsSinceStart_total)
        secsAddedToCountdownStart = if( secsAddedToCountdownStart > secsMaxAddedToCount_Reset) 0 else secsAddedToCountdownStart
    }
    fun createMedia(){
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
            }else { // < API21
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
            iCountDownSounds[0] = soundPool.load(this, R.raw.count_01, 1)
            iCountDownSounds[1] = soundPool.load(this, R.raw.count_02, 1)
            iCountDownSounds[2] = soundPool.load(this, R.raw.count_03, 1)
            iCountDownSounds[3] = soundPool.load(this, R.raw.count_04, 1)
            iCountDownSounds[4] = soundPool.load(this, R.raw.count_05, 1)
            iCountDownSounds[5] = soundPool.load(this, R.raw.count_06, 1)
            iCountDownSounds[6] = soundPool.load(this, R.raw.count_07, 1)
            iCountDownSounds[7] = soundPool.load(this, R.raw.count_08, 1)
            iCountDownSounds[8] = soundPool.load(this, R.raw.count_09, 1)
            iCountDownSounds[9] = soundPool.load(this, R.raw.count_10, 1)
            iBeepShortStart = soundPool.load(this, R.raw.beep_short_start, 1)
            iBeepShortHalfMinute = soundPool.load(this, R.raw.beep_short_30secs, 1)
            iBeepDouble2ndHalfMinute = soundPool.load(this, R.raw.beep_double_90secs, 1)
            iBeepLongEndCount = soundPool.load(this, R.raw.beep_long_0sec, 1)

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
        }catch (e: Exception){
            e.printStackTrace()
            showTheToast(this,getString(R.string.error_audio_creating), true)
        }
    }

    fun checkVolume(boSkipTimer: Boolean = false){
        try {
            if(!boSkipTimer) {
                if (boVolumeCheckDoneRecently) return
                boVolumeCheckDoneRecently = true
                Handler().postDelayed({
                    boVolumeCheckDoneRecently = false
                }, secsVolumeCheckLoop * 1000)
            }

            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            val volNow : Double = am.getStreamVolume(AudioManager.STREAM_MUSIC).toDouble()
            val volMax : Double = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toDouble()
            val volPerc : Int = (volNow / volMax * 100).toInt()

            when {
                volPerc < iVolumeWarningOff -> {
                    showTheToast(this,getString(R.string.volume_off) + " $volPerc%")
                }
                volPerc < iVolumeWarningLow -> {
                //  turned off for consistent user experience
                //    if(iVolumeWarningCounterLow < iVolumeWarningCounterMax) {
                        showTheToast(this,getString(R.string.volume_low) + " $volPerc%")
                //        iVolumeWarningCounterLow += 1
                //    }
                }
                else -> {
                //    if(iVolumeWarningCounterOK < iVolumeWarningCounterMax) {
                        showTheToast(this,getString(R.string.volume_ok) + " $volPerc%")
                //        iVolumeWarningCounterOK += 1
                //    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
            showTheToast(this,getString(R.string.error_volume_check))
        }
    }
    fun releaseMedia(){
        try {
            soundPool.release()
            mpRandExitSound.release()
        }catch (e: Exception){
            if (boDbg){
                e.printStackTrace()
                showTheToast(this,getString(R.string.error_audio_releasing))
            }
        }
    }
    fun playSound(SoundID: Int, boSwipe: Boolean = false){
        fun playIt(theID: Int, aSwipe: Boolean = false){
            soundPool.play(theID, 1F, 1F, 0, 0, 1.0F)
            if(aSwipe){soundPool.autoPause()}
        }
        try{
            playIt(SoundID, boSwipe)
        }catch (e: Exception){
            try{
                dbg("!! Recreating audio from playSound")
                releaseMedia() // err playing
                createMedia() // err playing
                playIt(SoundID, boSwipe)
            }catch (e: Exception){
                showTheToast(this,getString(R.string.error_audio_playing_sound))
            }
        }
    }
    fun playMinuteSounds(boSwipe: Boolean = false){
        when (secsOneMinuteState){
            in 1L..10L -> playSound(secsOneMinuteState.toInt(), boSwipe) // iCounts
            30L -> when {
                (secsSinceStart_down == 90L) && (secsToCountdownFrom == 180L)
                -> playSound(iBeepDouble2ndHalfMinute)
                else -> playSound(iBeepShortHalfMinute)
            }
            60L -> if (secsSinceStart_total > 59) playSound(iBeepLongEndCount)
        }
    }
    fun playRandomExitSound(){
        try{
            val randomInt = (1..iExitSounds.size).random()
            mpRandExitSound.release()
            mpRandExitSound = MediaPlayer.create(this, iExitSounds[randomInt])
            mpRandExitSound.start()
        }catch (e: Exception){
            e.printStackTrace()
            //no toast cause might always bug on older devices < API 23
        }
    }
    fun updateTimeAndUI(boSkipAddSecs: Boolean = false){
        updateTimes(boSkipAddSecs) //updateTimeAndUI
        updateTimeTexts() //updateTimeAndUI
        updateTextColors()
        updateActionButtons() //updateTimeAndUI
    }
    fun updateActionButtons(){
        try{
            bd.imgBackgroundTimer.visibility = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.imgBackgroundTimerOff.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.imgInfinity.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.btnStartInfinity.visibility  =  if(!boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.imgRestart.visibility  = if(boTimerRunning){View.VISIBLE}else{View.INVISIBLE}
            bd.btnRestart.visibility = if(boTimerRunning){
                when (secsAddedToCountdownStart){
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
            if (!boDbg) bd.btnMidOverlayDbg.visibility = View.GONE
            when(secsToCountdownFrom) {
                60L -> bd.imgMinuteSetting.setImageResource(R.drawable.ic_nr_i)
                120L -> bd.imgMinuteSetting.setImageResource(R.drawable.ic_nr_ii)
                180L -> bd.imgMinuteSetting.setImageResource(R.drawable.ic_nr_iii)
            }
        }catch (e: Exception){
            if(boDbg) showTheToast(this,getString(R.string.error_showing_buttons))
        }
    }
    fun updateTextColors(){
        //imgClear, imgRestart en imgInfinity zijn vaste kleur ivm zichtbaarheid
        if (!boTimerRunning) {
            bd.imgMinuteSetting.setColorFilter(ContextCompat.getColor(this, R.color.WhiteGrey))
            if (boBigTimeIsTotalTime) {
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(this, R.color.WhiteGrey))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(this, R.color.WhiteGreyLight))
            }else{
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(this, R.color.WhiteGreyLight))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(this, R.color.WhiteGrey))
            }
        } else {
            bd.imgMinuteSetting.setColorFilter(ContextCompat.getColor(this, R.color.WhiteGreyDark))
            if (boBigTimeIsTotalTime) {
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(this, R.color.WhiteGreyDark))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(this, R.color.WhiteGreyLight))
            }else{
                bd.edtTimeSmall.setTextColor(ContextCompat.getColor(this, R.color.WhiteGreyLight))
                bd.edtTimeBig.setTextColor(ContextCompat.getColor(this, R.color.WhiteGreyDark))
            }
        }
    }
    fun updateTimeTexts(){
        if (boBigTimeIsTotalTime) {
            bd.edtTimeBig.text = longToTimeText(secsSinceStart_total, "top", "total")
            bd.edtTimeSmall.text = longToTimeText(secsSinceStart_down, "bottom", "down")
        } else {
            bd.edtTimeBig.text = longToTimeText(secsSinceStart_down, "top", "down")
            bd.edtTimeSmall.text = longToTimeText(secsSinceStart_total, "bottom", "total")
        }
        bd.edtTimeBig.maxLines = 1
        if(resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            if(secsSinceStart_total > secsMONTH) bd.edtTimeBig.maxLines = 2 else bd.edtTimeBig.maxLines = 1
        }
    }
    fun longToTimeText(aTimeInSeconds: Long, strTopOrBottom: String, strTotalOrDown: String): String{
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

        val strTimeHas : String = if(strTotalOrDown == "total") concat(
            if (years > 0) "y" else "",
            if (months > 0) "m" else "",
            if (days > 0) "d" else "",
            if (hours > 0) "h" else ""
        ) as String else{""}

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

    private fun dbg(str: String, boPrintAlways: Boolean = false){
        if(boDbg or boDbgTxt or boPrintAlways)
            println("\n !!! " + Timestamp(System.currentTimeMillis()).toString() + "  " + str)
    }
}


