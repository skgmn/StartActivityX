package com.github.skgmn.startactivityx

import android.app.Activity
import android.app.Application

internal class ActivityApplicationSupplier(private val activity: Activity) : ApplicationSupplier {
    override fun getApplication(): Application? {
        return activity.application
    }
}
