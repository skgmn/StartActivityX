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

suspend fun Activity.acquirePermissions(vararg permissions: String): AcquirePermissionsResult {
    return acquirePermissions(listOf(*permissions))
}

suspend fun Activity.acquirePermissions(permissions: Collection<String>): AcquirePermissionsResult {
    return acquirePermissions(PermissionRequest(permissions))
}

suspend fun Activity.acquirePermissions(request: PermissionRequest): AcquirePermissionsResult {
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

suspend fun FragmentActivity.acquirePermissions(
        vararg permissions: String
): AcquirePermissionsResult {
    return acquirePermissions(listOf(*permissions))
}

suspend fun FragmentActivity.acquirePermissions(
        permissions: Collection<String>
): AcquirePermissionsResult {
    return acquirePermissions(PermissionRequest(permissions))
}

suspend fun FragmentActivity.acquirePermissions(
        request: PermissionRequest
): AcquirePermissionsResult {
    return acquirePermissions(
            activity = this,
            requestPermissionsHelperSupplier = {
                StartActivityHelperUtils.getHelperFragment(supportFragmentManager)
            },
            request = request
    )
}

suspend fun Fragment.acquirePermissions(vararg permissions: String): AcquirePermissionsResult {
    return acquirePermissions(listOf(*permissions))
}

suspend fun Fragment.acquirePermissions(permissions: Collection<String>): AcquirePermissionsResult {
    return acquirePermissions(PermissionRequest(permissions))
}

suspend fun Fragment.acquirePermissions(request: PermissionRequest): AcquirePermissionsResult {
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
): AcquirePermissionsResult = withContext(Dispatchers.Main.immediate) {
    val storage = PermissionStorage.getInstance(activity)
    val permissionsGranted = request.permissions.asSequence()
            .filter {
                InternalUtils.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
            .toSet()
    storage.removeDoNotAskAgainPermissions(permissionsGranted)

    if (request.permissions.all { it in permissionsGranted }) {
        return@withContext AcquirePermissionsResult.ALREADY_GRANTED
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

            val allGranted = permissions.all {
                InternalUtils.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
            return@withContext if (allGranted) {
                AcquirePermissionsResult.JUST_GRANTED
            } else {
                AcquirePermissionsResult.DENIED
            }
        }
        return@withContext AcquirePermissionsResult.DENIED
    }

    if (!request.userIntended &&
            permissionsShouldShowRationale.isNotEmpty() &&
            !request.rationaleDialog(activity, permissionsShouldShowRationale)) {

        return@withContext AcquirePermissionsResult.DENIED
    }

    requestPermissionsHelperSupplier().requestPermissions(request.permissions)
    val permissionMap = request.permissions.associateBy(
            keySelector = { it },
            valueTransform = {
                InternalUtils.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
    )
    if (permissionMap.all { it.value }) {
        return@withContext AcquirePermissionsResult.JUST_GRANTED
    }

    val pm = activity.packageManager
    storage.addDoNotAskAgainPermissions(request.permissions.filter {
        permissionMap[it] == false &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) &&
                isDeniable(pm, it)
    })

    return@withContext AcquirePermissionsResult.DENIED
}

private fun isDeniable(pm: PackageManager, permission: String): Boolean {
    return try {
        PermissionInfoCompat.getProtection(pm.getPermissionInfo(permission, 0)) !=
                PermissionInfo.PROTECTION_NORMAL
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
