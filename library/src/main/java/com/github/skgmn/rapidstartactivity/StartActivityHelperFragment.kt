package com.github.skgmn.rapidstartactivity

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperFragment : Fragment() {
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityLaunches.remove(requestCode)?.resume(ActivityResult(resultCode, data))
    }

    private suspend fun ensureCreated() {
        return suspendCancellableCoroutine { cont ->
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                cont.resume(Unit)
                return@suspendCancellableCoroutine
            }

            val observer = object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
                fun onCreate() {
                    lifecycle.removeObserver(this)
                    cont.resume(Unit)
                }
            }
            lifecycle.addObserver(observer)
            cont.invokeOnCancellation {
                lifecycle.removeObserver(observer)
            }
        }
    }

    // registerForActivityResult is rather more difficult to match sender and receiver,
    // so keep using deprecated startActivityForResult
    @Suppress("DEPRECATION")
    suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return withContext(Dispatchers.Main.immediate) {
            ensureCreated()
            val requestCode = StartActivityHelperUtils.allocateRequestCode(activityLaunches.keys)
            suspendCoroutine { cont ->
                activityLaunches[requestCode] = cont
                startActivityForResult(intent, requestCode)
            }
        }
    }
}