package com.github.skgmn.startactivityx

import android.content.Context

object GlobalPermissionConfig {
    var defaultRationaleDialog: suspend (Context, Collection<String>) -> Boolean =
            DefaultPermissionDialogs.generalRationale()
    var defaultGoToSettingsDialog: suspend (Context, Collection<String>) -> Boolean =
            DefaultPermissionDialogs.goToSettings()
}
