package com.github.skgmn.startactivityx

import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class StartActivityHelperActivity : ComponentActivity(), PermissionHelper {
    private val permissionRequests = mutableMapOf<Int, Continuation<Unit>>()

    override fun onDestroy() {
        permissionRequests.values.forEach { it.resumeWithException(CancellationException()) }
        permissionRequests.clear()
        super.onDestroy()
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
        if (permissionRequests.isEmpty()) {
            finish()
            overridePendingTransition(0, 0)
        }
    }
}
