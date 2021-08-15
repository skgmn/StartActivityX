package com.github.skgmn.startactivityx

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

suspend fun Activity.requestPermissions(vararg permissions: String): GrantResult {
    return requestPermissions(listOf(*permissions))
}

suspend fun Activity.requestPermissions(permissions: Collection<String>): GrantResult {
    return requestPermissions(PermissionRequest(permissions))
}

suspend fun Activity.requestPermissions(request: PermissionRequest): GrantResult {
    return if (this is FragmentActivity) {
        requestPermissions(request)
    } else {
        requestPermissions(
            activity = this,
            permissionHelperSupplier = {
                StartActivityHelperUtils.launchHelperActivity(this)
            },
            request = request
        )
    }
}

suspend fun FragmentActivity.requestPermissions(
        vararg permissions: String
): GrantResult {
    return requestPermissions(listOf(*permissions))
}

suspend fun FragmentActivity.requestPermissions(
        permissions: Collection<String>
): GrantResult {
    return requestPermissions(PermissionRequest(permissions))
}

suspend fun FragmentActivity.requestPermissions(
        request: PermissionRequest
): GrantResult {
    return requestPermissions(
        activity = this,
        permissionHelperSupplier = {
            StartActivityHelperUtils.getHelperFragment(supportFragmentManager)
        },
        request = request
    )
}

suspend fun Fragment.requestPermissions(vararg permissions: String): GrantResult {
    return requestPermissions(listOf(*permissions))
}

suspend fun Fragment.requestPermissions(permissions: Collection<String>): GrantResult {
    return requestPermissions(PermissionRequest(permissions))
}

suspend fun Fragment.requestPermissions(request: PermissionRequest): GrantResult {
    return requestPermissions(
        activity = requireActivity(),
        permissionHelperSupplier = {
            StartActivityHelperUtils.getHelperFragment(childFragmentManager)
        },
        request = request
    )
}

private suspend fun requestPermissions(
    activity: Activity,
    permissionHelperSupplier: suspend () -> PermissionHelper,
    request: PermissionRequest
): GrantResult = withContext(Dispatchers.Main.immediate) {
    val storage = PermissionStorage.getInstance(activity)
    val permissionsGranted = request.permissions.asSequence()
            .filter {
                InternalUtils.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
            .toSet()
    storage.removeDoNotAskAgainPermissions(permissionsGranted)

    if (request.permissions.all { it in permissionsGranted }) {
        return@withContext GrantResult.ALREADY_GRANTED
    }

    val permissionsShouldShowRationale = request.permissions.asSequence()
            .filter { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            .toCollection(LinkedHashSet())
    val permissionsShouldNotAskAgain = storage.removeDoNotAskAgainPermissions(
            permissionsShouldShowRationale
    )

    if (permissionsShouldNotAskAgain.isNotEmpty()) {
        val permissions = permissionsShouldShowRationale + permissionsShouldNotAskAgain
        if (request.userIntended && request.goToSettingsDialog(activity, permissions)) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivityForResult(intent)

            val allGranted = permissions.all {
                InternalUtils.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
            if (allGranted) {
                return@withContext GrantResult.JUST_GRANTED
            }
        }
        return@withContext GrantResult.DO_NOT_ASK_AGAIN
    }

    if (!request.userIntended &&
            permissionsShouldShowRationale.isNotEmpty() &&
            !request.rationaleDialog(activity, permissionsShouldShowRationale)) {

        return@withContext GrantResult.DENIED
    }

    permissionHelperSupplier().requestPermissions(request.permissions)
    val permissionMap = request.permissions.associateBy(
            keySelector = { it },
            valueTransform = {
                InternalUtils.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
    )
    if (permissionMap.all { it.value }) {
        return@withContext GrantResult.JUST_GRANTED
    }

    val pm = activity.packageManager
    val permissionsDoNotAskAgain = request.permissions.filter {
        permissionMap[it] == false &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) &&
                isDeniable(pm, it)
    }
    storage.addDoNotAskAgainPermissions(permissionsDoNotAskAgain)

    return@withContext if (permissionsDoNotAskAgain.isEmpty()) {
        GrantResult.DENIED
    } else {
        GrantResult.DO_NOT_ASK_AGAIN
    }
}

private fun isDeniable(pm: PackageManager, permission: String): Boolean {
    return try {
        PermissionInfoCompat.getProtection(pm.getPermissionInfo(permission, 0)) !=
                PermissionInfo.PROTECTION_NORMAL
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
