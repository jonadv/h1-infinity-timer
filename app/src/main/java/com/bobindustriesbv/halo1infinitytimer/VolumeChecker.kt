package com.bobindustriesbv.halo1infinitytimer

import android.media.AudioManager
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

object VolumeChecker {
    //settings
    private const val iVolumeWarningOff = 1
    private const val iVolumeWarningLow = 35 //  6/15 = 0.40
    private var boVolumeCheckDoneRecently = false
    private var secsVolumeCheckLoop = 2L

    fun checkVolume(mainActivity: MainActivity, boSkipTimer: Boolean = false){
        try {
            if(!boSkipTimer) {
                if (boVolumeCheckDoneRecently) return
                boVolumeCheckDoneRecently = true
                Handler().postDelayed({
                                          boVolumeCheckDoneRecently = false
                                      }, secsVolumeCheckLoop * 1000)
            }

            val am = mainActivity.getSystemService(AppCompatActivity.AUDIO_SERVICE) as AudioManager
            val volNow : Double = am.getStreamVolume(AudioManager.STREAM_MUSIC).toDouble()
            val volMax : Double = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toDouble()
            val volPerc : Int = (volNow / volMax * 100).toInt()

            when {
                volPerc < iVolumeWarningOff -> {
                    UserInstructor.showTheToast(mainActivity, mainActivity.getString(R.string.volume_off) + " $volPerc%")
                }
                volPerc < iVolumeWarningLow -> {
                    //  turned off for consistent user experience
                    //    if(iVolumeWarningCounterLow < iVolumeWarningCounterMax) {
                    UserInstructor.showTheToast(mainActivity, mainActivity.getString(R.string.volume_low) + " $volPerc%")
                    //        iVolumeWarningCounterLow += 1
                    //    }
                }
                else                                     -> {
                    //    if(iVolumeWarningCounterOK < iVolumeWarningCounterMax) {
                    UserInstructor.showTheToast(mainActivity, mainActivity.getString(R.string.volume_ok) + " $volPerc%")
                    //        iVolumeWarningCounterOK += 1
                    //    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
            UserInstructor.showTheToast(mainActivity, mainActivity.getString(R.string.error_volume_check))
        }
    }
}