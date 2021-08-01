package com.github.skgmn.rapidstartactivity

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.net.Uri
import android.provider.Settings
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
                requestPermissionsHelperSupplier = {
                    StartActivityHelperUtils.launchHelperActivity(this)
                },
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
    storage.removeDoNotAskAgainPermissions(permissionsGranted)

    if (request.permissions.all { it in permissionsGranted }) {
        return@withContext true
    }

    val permissionsShouldShowRationale = request.permissions.asSequence()
            .filter { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            .toCollection(LinkedHashSet())
    val doNotAskAgainPermissions = storage.removeDoNotAskAgainPermissions(
            permissionsShouldShowRationale
    )

    if (request.userIntended && doNotAskAgainPermissions.isNotEmpty()) {
        val permissions = permissionsShouldShowRationale + doNotAskAgainPermissions
        if (request.goToSettingsDialog(activity, permissions)) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivityForResult(intent)

            return@withContext permissions.all {
                PermissionChecker.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
        }
        return@withContext false
    }

    if (!request.userIntended &&
            permissionsShouldShowRationale.isNotEmpty() &&
            !request.rationaleDialog(activity, permissionsShouldShowRationale)) {

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
    storage.addDoNotAskAgainPermissions(request.permissions.filter {
        permissionMap[it] == false &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) &&
                isDeniable(pm, it)
    })

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
