package com.bobindustriesbv.halo1infinitytimer.helpers

import android.app.Application
import android.content.res.Resources

//call app context from anywhere
//https://stackoverflow.com/a/58627769/6544310
class App : Application() {
    companion object {
        lateinit var instance: App private set
        lateinit var res: Resources private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        res = resources
    }
}