package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperActivity : Activity(), PermissionHelper {
    private val permissionRequests = mutableMapOf<Int, Continuation<Unit>>()
    private val activityLaunches = mutableMapOf<Int, Continuation<ActivityResult>>()

    override fun onDestroy() {
        permissionRequests.values.forEach { it.resumeWithException(CancellationException()) }
        permissionRequests.clear()
        activityLaunches.values.forEach { it.resumeWithException(CancellationException()) }
        activityLaunches.clear()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        activityLaunches.remove(requestCode)?.resume(ActivityResult(resultCode, data))
        finishIfPossible()
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        globalPermissionResultSignal.tryEmit(Unit)
        permissionRequests.remove(requestCode)?.resume(Unit)
        finishIfPossible()
    }

    // registerForActivityResult is rather more difficult to match sender and receiver,
    // so keep using deprecated startActivityForResult
    @Suppress("DEPRECATION")
    suspend fun startActivityForResult(intent: Intent): ActivityResult {
        return withContext(Dispatchers.Main.immediate) {
            suspendCoroutine<ActivityResult> { cont ->
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
        withContext(Dispatchers.Main.immediate) {
            suspendCoroutine<Unit> { cont ->
                val requestCode =
                    StartActivityHelperUtils.allocateRequestCode(permissionRequests.keys)
                permissionRequests[requestCode] = cont
                ActivityCompat.requestPermissions(
                    this@StartActivityHelperActivity,
                    permissions.toTypedArray(),
                    requestCode
                )
            }
        }
    }

    private fun finishIfPossible() {
        if (activityLaunches.isEmpty() && permissionRequests.isEmpty()) {
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
