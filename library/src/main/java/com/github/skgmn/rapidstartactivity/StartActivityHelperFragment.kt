package com.github.skgmn.rapidstartactivity

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperFragment : Fragment(), RequestPermissionsHelper {

    private val permissionsReloadable = MutableStateFlow(false)
    private val permissionRequests = mutableMapOf<Int, Continuation<Unit>>()
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

    override fun onStart() {
        super.onStart()
        permissionsReloadable.tryEmit(true)
    }

    override fun onResume() {
        super.onResume()
        permissionsReloadable.tryEmit(true)
    }

    override fun onPause() {
        super.onPause()
        permissionsReloadable.tryEmit(false)
    }

    override fun onStop() {
        super.onStop()
        permissionsReloadable.tryEmit(false)
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

    fun arePermissionsReloadable(): Flow<Boolean> {
        return permissionsReloadable
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
