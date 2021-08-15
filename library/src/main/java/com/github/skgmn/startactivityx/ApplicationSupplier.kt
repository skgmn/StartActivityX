package com.github.skgmn.startactivityx

import android.app.Application

internal interface ApplicationSupplier {
    fun getApplication(): Application?
}
