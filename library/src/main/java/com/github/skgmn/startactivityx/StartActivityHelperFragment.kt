package com.github.skgmn.startactivityx

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperFragment : Fragment(), PermissionHelper {
    private val permissionRequests = mutableMapOf<Int, Continuation<Unit>>()
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

    override fun onDestroy() {
        permissionRequests.values.forEach { it.resumeWithException(CancellationException()) }
        permissionRequests.clear()
        activityLaunches.values.forEach { it.resumeWithException(CancellationException()) }
        activityLaunches.clear()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityLaunches.remove(requestCode)?.resume(ActivityResult(resultCode, data))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        globalPermissionResultSignal.tryEmit(Unit)
        permissionRequests.remove(requestCode)?.resume(Unit)
    }

    @ExperimentalCoroutinesApi
    fun isStarted(): Flow<Boolean> {
        return callbackFlow {
            send(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
            val observer = LifecycleEventObserver { _, _ ->
                trySend(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
            }
            lifecycle.addObserver(observer)
            awaitClose {
                lifecycle.removeObserver(observer)
            }
        }
            .buffer(Channel.Factory.CONFLATED)
            .flowOn(Dispatchers.Main.immediate)
            .distinctUntilChanged()
    }

    @ExperimentalCoroutinesApi
    fun watchLifecycleEvent(): Flow<Lifecycle.Event> {
        return callbackFlow {
            val observer = LifecycleEventObserver { _, event ->
                trySend(event)
            }
            lifecycle.addObserver(observer)
            awaitClose {
                lifecycle.removeObserver(observer)
            }
        }
            .buffer(Channel.Factory.UNLIMITED)
            .flowOn(Dispatchers.Main.immediate)
    }

    // registerForActivityResult is rather more difficult to match sender and receiver,
    // so keep using deprecated startActivityForResult
    @Suppress("DEPRECATION")
    suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return whenCreated {
            suspendCoroutine { cont ->
                val requestCode =
                    StartActivityHelperUtils.allocateRequestCode(activityLaunches.keys)
                activityLaunches[requestCode] = cont
                startActivityForResult(intent, requestCode)
            }
        }
    }

    // registerForActivityResult is rather more difficult to match sender and receiver,
    // so keep using deprecated requestPermissions
    @Suppress("DEPRECATION")
    override suspend fun requestPermissions(permissions: Collection<String>) {
        whenCreated {
            suspendCoroutine<Unit> { cont ->
                val requestCode =
                    StartActivityHelperUtils.allocateRequestCode(permissionRequests.keys)
                permissionRequests[requestCode] = cont
                requestPermissions(permissions.toTypedArray(), requestCode)
            }
        }
    }
}
