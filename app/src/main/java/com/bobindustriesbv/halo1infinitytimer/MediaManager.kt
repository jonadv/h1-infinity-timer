package com.bobindustriesbv.halo1infinitytimer

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import net.mabboud.OneTimeBuzzer

object MediaManager {
    private val tonePlayer by lazy { OneTimeBuzzer() } //only for lower APIs
    private lateinit var soundPool: SoundPool
    private lateinit var mpRandExitSound: MediaPlayer
    
    //media
    private val iCountDownSounds: IntArray = IntArray(10)
    var iBeepShortStart: Int = 11
    private var iBeepShortHalfMinute: Int = 12
    private var iBeepDouble2ndHalfMinute: Int  = 13
    private var iBeepLongEndCount: Int  = 14
    private var iExitSounds = ArrayList<Int>()
    
    fun createMedia(ma: MainActivity){
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
                (SoundPool(6, AudioManager.STREAM_MUSIC, 0))
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
            iCountDownSounds[0] = soundPool.load(ma, R.raw.count_01, 1)
            iCountDownSounds[1] = soundPool.load(ma, R.raw.count_02, 1)
            iCountDownSounds[2] = soundPool.load(ma, R.raw.count_03, 1)
            iCountDownSounds[3] = soundPool.load(ma, R.raw.count_04, 1)
            iCountDownSounds[4] = soundPool.load(ma, R.raw.count_05, 1)
            iCountDownSounds[5] = soundPool.load(ma, R.raw.count_06, 1)
            iCountDownSounds[6] = soundPool.load(ma, R.raw.count_07, 1)
            iCountDownSounds[7] = soundPool.load(ma, R.raw.count_08, 1)
            iCountDownSounds[8] = soundPool.load(ma, R.raw.count_09, 1)
            iCountDownSounds[9] = soundPool.load(ma, R.raw.count_10, 1)
            iBeepShortStart = soundPool.load(ma, R.raw.beep_short_start, 1)
            iBeepShortHalfMinute = soundPool.load(ma, R.raw.beep_short_30secs, 1)
            iBeepDouble2ndHalfMinute = soundPool.load(ma, R.raw.beep_double_90secs, 1)
            iBeepLongEndCount = soundPool.load(ma, R.raw.beep_long_0sec, 1)

            iExitSounds.add(R.raw.exit_cort_halo_its_finished)
            iExitSounds.add(R.raw.exit_cort_halo_its_finished)
            iExitSounds.add(R.raw.exit_cort_sleep_well)
            iExitSounds.add(R.raw.exit_cort_sleep_well)
            iExitSounds.add(R.raw.exit_cort_sleep_well)
            iExitSounds.add(R.raw.exit_mc_doit)
            iExitSounds.add(R.raw.exit_mc_enough)
            iExitSounds.add(R.raw.exit_monitor_ah_pity)
            iExitSounds.add(R.raw.exit_monitor_how_unfortunate)

            mpRandExitSound = MediaPlayer.create(ma, iExitSounds[0])
        }catch (e: Exception){
            e.printStackTrace()
            UserInstructor.showTheToast(ma, ma.getString(R.string.error_audio_creating), true)
        }
    }

    fun releaseMedia(ma: MainActivity){
        try {
            soundPool.release()
            mpRandExitSound.release()
        }catch (e: Exception){
            if (MainActivity.boDbg){
                e.printStackTrace()
                UserInstructor.showTheToast(ma, ma.getString(R.string.error_audio_releasing))
            }
        }
    }
    fun playSound(ma: MainActivity, SoundID: Int, boSwipe: Boolean = false){
        fun playIt(theID: Int, aSwipe: Boolean = false){
            soundPool.play(theID, 1F, 1F, 0, 0, 1.0F)
            if(aSwipe){soundPool.autoPause()}
        }
        try{
            playIt(SoundID, boSwipe)
        }catch (e: Exception){
            try{
                ma.dbg("!! Recreating audio from playSound")
                releaseMedia(ma) // err playing
                createMedia(ma) // err playing
                playIt(SoundID, boSwipe)
            }catch (e: Exception){
                UserInstructor.showTheToast(ma, ma.getString(R.string.error_audio_playing_sound))
            }
        }
    }
    fun playSimpleSound(){
        //iBeepShortStart wont load on older devices. Unclear why
        tonePlayer.play()
    }
    fun playMinuteSounds(ma: MainActivity, boSwipe: Boolean = false){
        when (TimeStates.secsOneMinuteState){ //playMinuteSounds
            in 1L..10L -> playSound(ma, TimeStates.secsOneMinuteState.toInt(), boSwipe) // playMinuteSounds 1-10
            30L -> when {
                (TimeStates.secsSinceStart_down == 90L) && (UserSettings.secsToCountdownFrom == 180L)
                     -> playSound(ma, iBeepDouble2ndHalfMinute)
                else -> playSound(ma, iBeepShortHalfMinute)
            }
            60L -> if (TimeStates.secsSinceStart_total > 59) playSound(ma, iBeepLongEndCount)
        }
    }
    fun playRandomExitSound(ma: MainActivity){
        try{
            val randomInt = (1..iExitSounds.size).random()
            mpRandExitSound.release()
            mpRandExitSound = MediaPlayer.create(ma, iExitSounds[randomInt])
            mpRandExitSound.start()
        }catch (e: Exception){
            e.printStackTrace()
            //no toast cause might always bug on older devices < API 23
        }
    }
}