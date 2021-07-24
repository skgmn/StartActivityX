package com.github.skgmn.rapidstartactivity

import android.app.Application

internal interface ApplicationSupplier {
    fun getApplication(): Application?
}
