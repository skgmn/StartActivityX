package com.github.skgmn.rapidstartactivity

import android.app.AlertDialog
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class PermissionRequest(
        val permissions: Collection<String>,
        val userIntended: Boolean = false,
        val rationaleDialog: suspend (Context, Collection<String>) -> Boolean =
                DefaultPermissionDialogs.generalRationale(),
        val goToSettingsDialog: suspend (Context, Collection<String>) -> Boolean =
                DefaultPermissionDialogs.goToSettings()
)
