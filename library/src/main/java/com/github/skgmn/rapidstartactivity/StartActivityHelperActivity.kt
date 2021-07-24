package com.github.skgmn.rapidstartactivity

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperActivity : Activity() {
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityLaunches.remove(requestCode)?.resume(ActivityResult(resultCode, data))
        finishIfPossible()
    }

    // registerForActivityResult is rather more difficult to match sender and receiver,
    // so keep using deprecated startActivityForResult
    @Suppress("DEPRECATION")
    suspend fun startActivityForResult(intent: Intent) =
        withContext(Dispatchers.Main.immediate) {
            val requestCode = StartActivityHelperUtils.allocateRequestCode(activityLaunches.keys)
            suspendCoroutine<ActivityResult> { cont ->
                activityLaunches[requestCode] = cont
                startActivityForResult(intent, requestCode)
            }
        }

    private fun finishIfPossible() {
        if (activityLaunches.isEmpty()) {
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
