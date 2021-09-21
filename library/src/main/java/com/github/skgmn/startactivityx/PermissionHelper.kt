package com.github.skgmn.startactivityx

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.core.content.PermissionChecker
import androidx.core.content.pm.PermissionInfoCompat

internal object PermissionHelper {
    @PermissionChecker.PermissionResult
    internal fun checkSelfPermission(context: Context, permission: String): Int {
        if (regardGrantedImplicitly(permission)) {
            return PermissionChecker.PERMISSION_GRANTED
        }
        return PermissionChecker.checkSelfPermission(context, permission)
    }

    internal fun regardGrantedImplicitly(permission: String): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                permission == Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    internal fun isDeniable(pm: PackageManager, permission: String): Boolean {
        return try {
            PermissionInfoCompat.getProtection(pm.getPermissionInfo(permission, 0)) !=
                    PermissionInfo.PROTECTION_NORMAL
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}