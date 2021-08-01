package com.github.skgmn.rapidstartactivity

import android.content.Context

class PermissionRequest(
        val permissions: Collection<String>,
        val userIntended: Boolean = false,
        val rationaleDialog: suspend (Context, Collection<String>) -> Boolean =
                DefaultRationaleDialogs.general()
)
