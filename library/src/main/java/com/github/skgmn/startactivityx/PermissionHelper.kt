package com.github.skgmn.startactivityx

internal interface PermissionHelper {
    suspend fun requestPermissions(permissions: Collection<String>)
}
