package com.github.skgmn.rapidstartactivity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.withContext

suspend fun Activity.acquirePermissions(permissions: Collection<String>): Boolean {
    return acquirePermissions(PermissionRequest(permissions))
}

suspend fun Activity.acquirePermissions(request: PermissionRequest): Boolean {
    return if (this is FragmentActivity) {
        acquirePermissions(request)
    } else {
        acquirePermissions(
            activity = this,
            requestPermissionsHelperSupplier = { StartActivityHelperUtils.launchHelperActivity(this) },
            request = request
        )
    }
}

suspend fun FragmentActivity.acquirePermissions(permissions: Collection<String>): Boolean {
    return acquirePermissions(PermissionRequest(permissions))
}

suspend fun FragmentActivity.acquirePermissions(request: PermissionRequest): Boolean {
    return acquirePermissions(
        activity = this,
        requestPermissionsHelperSupplier = {
            StartActivityHelperUtils.getHelperFragment(supportFragmentManager)
        },
        request = request
    )
}

suspend fun Fragment.acquirePermissions(permissions: Collection<String>): Boolean {
    return acquirePermissions(PermissionRequest(permissions))
}

suspend fun Fragment.acquirePermissions(request: PermissionRequest): Boolean {
    return acquirePermissions(
        activity = requireActivity(),
        requestPermissionsHelperSupplier = {
            StartActivityHelperUtils.getHelperFragment(childFragmentManager)
        },
        request = request
    )
}

private suspend fun acquirePermissions(
    activity: Activity,
    requestPermissionsHelperSupplier: suspend () -> RequestPermissionsHelper,
    request: PermissionRequest
): Boolean = withContext(Dispatchers.Main.immediate) {
    if (checkPermissionsGranted(activity, request.permissions)) {
        return@withContext true
    }

    val permissionsShouldShowRationale = request.permissions.asSequence()
        .filter { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        .toCollection(LinkedHashSet())
    if (permissionsShouldShowRationale.isNotEmpty() &&
        !request.rationaleDialog(activity, permissionsShouldShowRationale)
    ) {
        return@withContext false
    }

    requestPermissionsHelperSupplier().requestPermissions(request.permissions)
    val permissionMap = request.permissions.associateBy(
        keySelector = { it },
        valueTransform = {
            PermissionChecker.checkSelfPermission(activity, it) ==
                    PermissionChecker.PERMISSION_GRANTED
        }
    )
    if (permissionMap.all { it.value }) {
        return@withContext true
    }

    val dontAskAgainPermissions = request.permissions.asSequence()
        .filter {
            it !in permissionsShouldShowRationale &&
                    permissionMap[it] == false &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
        .toCollection(LinkedHashSet())
    if (dontAskAgainPermissions.isEmpty()) {
        return@withContext false
    }
    if (!request.goToSettingsDialog(activity, dontAskAgainPermissions)) {
        return@withContext false
    }

    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", activity.packageName, null)
    intent.data = uri
    activity.startActivityForResult(intent)
    return@withContext checkPermissionsGranted(activity, request.permissions)
}
