package com.github.skgmn.rapidstartactivity

import android.content.Context

internal interface RequestPermissionsHelper {
    suspend fun requestPermissions(permissions: Collection<String>)
}
