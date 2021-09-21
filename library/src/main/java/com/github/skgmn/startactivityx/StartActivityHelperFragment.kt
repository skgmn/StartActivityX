package com.github.skgmn.startactivityx

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.whenCreated
import kotlinx.coroutines.CancellationException
import kotlin.collections.set
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperFragment : Fragment() {
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

    override fun onDestroy() {
        activityLaunches.values.forEach { it.resumeWithException(CancellationException()) }
        activityLaunches.clear()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityLaunches.remove(requestCode)?.resume(ActivityResult(resultCode, data))
    }

    @Suppress("DEPRECATION")
    suspend fun startActivityForResultImpl(
        intent: Intent,
        activityOptions: ActivityOptionsCompat? = null
    ): ActivityResult {
        return whenCreated {
            suspendCoroutine { cont ->
                val requestCode =
                    StartActivityHelperUtils.allocateRequestCode(activityLaunches.keys)
                activityLaunches[requestCode] = cont
                startActivityForResult(intent, requestCode, activityOptions?.toBundle())
            }
        }
    }
}