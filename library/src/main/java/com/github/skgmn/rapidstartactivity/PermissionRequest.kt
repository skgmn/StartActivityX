package com.github.skgmn.rapidstartactivity

import android.app.AlertDialog
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class PermissionRequest(
    val permissions: Collection<String>,
    val rationaleDialog: suspend (Context, Collection<String>, Boolean) -> Boolean =
        DefaultRationaleDialogs.general()
)
