package com.github.skgmn.rapidstartactivity

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.core.content.PermissionChecker

internal object InternalUtils {
    fun iterateContext(context: Context): Sequence<Context> {
        return generateSequence(context) {
            (it as? ContextWrapper)?.baseContext
        }
    }

    @PermissionChecker.PermissionResult
    fun checkSelfPermission(context: Context, permission: String): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {

            return PermissionChecker.PERMISSION_GRANTED
        }
        return PermissionChecker.checkSelfPermission(context, permission)
    }
}
