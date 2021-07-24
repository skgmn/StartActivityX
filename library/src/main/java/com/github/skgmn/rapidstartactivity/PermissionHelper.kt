package com.github.skgmn.rapidstartactivity

import kotlinx.coroutines.flow.Flow

internal interface PermissionHelper {
    fun getPermissionReloadSignal(): Flow<*>
}
