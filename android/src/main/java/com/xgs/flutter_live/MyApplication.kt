package com.xgs.flutter_live

import android.app.Application
import com.baijiayun.BJYPlayerSDK

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        //配置sdk
        BJYPlayerSDK.Builder(this)
                .setDevelopMode(BuildConfig.DEBUG)
                .build()
    }
}