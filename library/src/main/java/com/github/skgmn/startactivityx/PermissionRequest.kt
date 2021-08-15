package com.github.skgmn.startactivityx

import android.content.Context

class PermissionRequest(
        val permissions: Collection<String>,
        val userIntended: Boolean = false,
        val rationaleDialog: suspend (Context, Collection<String>) -> Boolean =
                GlobalPermissionConfig.defaultRationaleDialog,
        val goToSettingsDialog: suspend (Context, Collection<String>) -> Boolean =
                GlobalPermissionConfig.defaultGoToSettingsDialog
)
