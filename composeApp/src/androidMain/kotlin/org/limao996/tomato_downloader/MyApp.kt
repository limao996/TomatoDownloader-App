package org.limao996.tomato_downloader

import android.app.Application
import android.content.Context

lateinit var application: Context

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        application = applicationContext
    }
}