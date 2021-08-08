package com.github.skgmn.startactivityx.camerasample

import android.app.Application
import com.github.skgmn.startactivityx.DefaultPermissionDialogs
import com.github.skgmn.startactivityx.GlobalPermissionConfig

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalPermissionConfig.defaultRationaleDialog =
            DefaultPermissionDialogs.detailedRationale(R.raw.permission_rationales)
    }
}