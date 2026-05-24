package com.kmpsdk

import android.content.Context

object KmpSdkAndroid {
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun requireContext(): Context =
        if (::appContext.isInitialized) {
            appContext
        } else {
            error("Call KmpSdkAndroid.init(context) before KmpSdk.init")
        }
}
