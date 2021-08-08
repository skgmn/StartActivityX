package com.github.skgmn.startactivityx

internal interface RequestPermissionsHelper {
    suspend fun requestPermissions(permissions: Collection<String>)
}
