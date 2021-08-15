package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment

fun Activity.getPermissionsStatus(vararg permissions: String): PermissionsStatus {
    return getPermissionsStatus(listOf(*permissions))
}

fun Activity.getPermissionsStatus(permissions: Collection<String>): PermissionsStatus {
    return getPermissionsStatus(this, permissions)
}

fun Fragment.getPermissionsStatus(vararg permissions: String): PermissionsStatus {
    return getPermissionsStatus(*permissions)
}

fun Fragment.getPermissionsStatus(permissions: Collection<String>): PermissionsStatus {
    return getPermissionsStatus(requireContext(), permissions)
}

internal fun getPermissionsStatus(
    context: Context,
    permissions: Collection<String>
): PermissionsStatus {
    val storage = PermissionStorage.getInstance(context)
    val doNotAskAgainPermissions = storage.doNotAskAgainPermissions
    permissions.forEach {
        val status = when {
            InternalUtils.checkSelfPermission(context, it) ==
                    PermissionChecker.PERMISSION_GRANTED -> {
                PermissionsStatus.GRANTED
            }
            it in doNotAskAgainPermissions -> {
                PermissionsStatus.DO_NOT_ASK_AGAIN
            }
            else -> {
                PermissionsStatus.DENIED
            }
        }
        if (status != PermissionsStatus.GRANTED) {
            return status
        }
    }
    return PermissionsStatus.GRANTED
}
