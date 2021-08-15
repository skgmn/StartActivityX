package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment

fun Activity.getPermissionStatus(vararg permissions: String): PermissionStatus {
    return getPermissionStatus(listOf(*permissions))
}

fun Activity.getPermissionStatus(permissions: Collection<String>): PermissionStatus {
    return getPermissionStatus(this, permissions)
}

fun Fragment.getPermissionStatus(vararg permissions: String): PermissionStatus {
    return getPermissionStatus(*permissions)
}

fun Fragment.getPermissionStatus(permissions: Collection<String>): PermissionStatus {
    return getPermissionStatus(requireContext(), permissions)
}

internal fun getPermissionStatus(
    context: Context,
    permissions: Collection<String>
): PermissionStatus {
    val storage = PermissionStorage.getInstance(context)
    val doNotAskAgainPermissions = storage.doNotAskAgainPermissions
    permissions.forEach {
        val status = when {
            InternalUtils.checkSelfPermission(context, it) ==
                    PermissionChecker.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            it in doNotAskAgainPermissions -> {
                PermissionStatus.DO_NOT_ASK_AGAIN
            }
            else -> {
                PermissionStatus.DENIED
            }
        }
        if (status != PermissionStatus.GRANTED) {
            return status
        }
    }
    return PermissionStatus.GRANTED
}
