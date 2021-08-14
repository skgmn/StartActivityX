package com.github.skgmn.startactivityx

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperFragment : Fragment(), PermissionHelper {
    private val permissionRequests = mutableMapOf<Int, Continuation<Unit>>()
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

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

    // registerForActivityResult is rather more difficult to match sender and receiver,
    // so keep using deprecated requestPermissions
    @Suppress("DEPRECATION")
    override suspend fun requestPermissions(permissions: Collection<String>) {
        withContext(Dispatchers.Main.immediate) {
            ensureCreated()
            val requestCode = StartActivityHelperUtils.allocateRequestCode(permissionRequests.keys)
            suspendCoroutine<Unit> { cont ->
                permissionRequests[requestCode] = cont
                requestPermissions(permissions.toTypedArray(), requestCode)
            }
        }
    }
}
