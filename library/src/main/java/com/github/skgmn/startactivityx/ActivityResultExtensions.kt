package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult

val ActivityResult.isOk
    inline get() = resultCode == Activity.RESULT_OK

val ActivityResult.isCanceled
    inline get() = resultCode == Activity.RESULT_CANCELED

fun ActivityResult.getDataIfOk(): Intent? {
    return data?.takeIf { resultCode == Activity.RESULT_OK }
}