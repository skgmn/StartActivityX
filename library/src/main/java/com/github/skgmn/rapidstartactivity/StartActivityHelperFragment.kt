package com.github.skgmn.rapidstartactivity

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperFragment : Fragment(), PermissionHelper {
    private val permissionReloadSignal = MutableSharedFlow<Any>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var needToReloadPermission = true
    private val permissionRequests = mutableMapOf<Int, Continuation<Unit>>()
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

    override fun onStart() {
        super.onStart()
        emitPermissionReloadSignal()
    }

    override fun onResume() {
        super.onResume()
        emitPermissionReloadSignal()
    }

    override fun onPause() {
        super.onPause()
        needToReloadPermission = true
    }

    override fun onStop() {
        super.onStop()
        needToReloadPermission = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        activityLaunches.remove(requestCode)?.resume(ActivityResult(resultCode, data))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionReloadSignal.tryEmit(Unit)
        permissionRequests.remove(requestCode)?.resume(Unit)
    }

    private fun emitPermissionReloadSignal() {
        if (needToReloadPermission) {
            needToReloadPermission = false
            permissionReloadSignal.tryEmit(Unit)
        }
    }

    override fun getPermissionReloadSignal(): Flow<*> {
        return permissionReloadSignal
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
    suspend fun requestPermissions(permissions: Collection<String>) {
        withContext(Dispatchers.Main.immediate) {
            val requestCode = StartActivityHelperUtils.allocateRequestCode(permissionRequests.keys)
            suspendCoroutine<Unit> { cont ->
                permissionRequests[requestCode] = cont
                requestPermissions(permissions.toTypedArray(), requestCode)
            }
        }
    }
}
