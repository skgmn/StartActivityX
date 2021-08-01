package com.github.skgmn.rapidstartactivity.sample.camera

import android.app.Application
import com.github.skgmn.rapidstartactivity.DefaultPermissionDialogs
import com.github.skgmn.rapidstartactivity.GlobalPermissionConfig

class SampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GlobalPermissionConfig.defaultRationaleDialog =
                DefaultPermissionDialogs.detailedRationale(R.raw.permission_rationales)
    }
}
