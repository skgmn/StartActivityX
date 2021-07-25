package com.github.skgmn.rapidstartactivity

import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.pm.PermissionInfoCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun Activity.acquirePermissions(vararg permissions: String): Boolean {
    return acquirePermissions(listOf(*permissions))
}

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

suspend fun FragmentActivity.acquirePermissions(vararg permissions: String): Boolean {
    return acquirePermissions(listOf(*permissions))
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

suspend fun Fragment.acquirePermissions(vararg permissions: String): Boolean {
    return acquirePermissions(listOf(*permissions))
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
    val storage = PermissionStorage.getInstance(activity)
    val permissionsGranted = request.permissions.asSequence()
        .filter {
            PermissionChecker.checkSelfPermission(activity, it) ==
                    PermissionChecker.PERMISSION_GRANTED
        }
        .toSet()
    storage.doNotAskAgainPermissions -= permissionsGranted

    if (request.permissions.all { it in permissionsGranted }) {
        return@withContext true
    }

    val permissionsShouldShowRationale = request.permissions.asSequence()
        .filter { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        .toCollection(LinkedHashSet())
    storage.doNotAskAgainPermissions -= permissionsShouldShowRationale

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

    val pm = activity.packageManager
    storage.doNotAskAgainPermissions += request.permissions.filter {
        it !in permissionsShouldShowRationale &&
                permissionMap[it] == false &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) &&
                isDeniable(pm, it)
    }

    return@withContext false
}

private fun isDeniable(pm: PackageManager, permission: String): Boolean {
    return try {
        PermissionInfoCompat.getProtection(pm.getPermissionInfo(permission, 0)) !=
                PermissionInfo.PROTECTION_NORMAL
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
