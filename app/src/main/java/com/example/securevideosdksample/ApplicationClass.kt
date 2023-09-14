package com.example.securevideosdksample

import android.content.Context
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication


class ApplicationClass : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        instance = this

    }

    companion object {

        lateinit var instance: ApplicationClass
            private set
    }

    override fun attachBaseContext(base: Context?) {
        MultiDex.install(this)
        super.attachBaseContext(base)
    }


    override fun onTerminate() {
        super.onTerminate()
    }


}