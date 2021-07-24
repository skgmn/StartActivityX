package com.github.skgmn.rapidstartactivity

import android.app.AlertDialog
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class PermissionRequest(
    val permissions: Collection<String>,
    val rationaleDialog: suspend (Context, Collection<String>) -> Boolean = { context, _ ->
        suspendCancellableCoroutine { cont ->
            val dialog = AlertDialog.Builder(context)
                .setMessage(R.string.permission_dialog_rationale_message_default)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    cont.resume(true)
                }
                .setCancelable(false)
                .show()
            cont.invokeOnCancellation {
                dialog.dismiss()
            }
        }
    },
    val goToSettingsDialog: suspend (Context, Collection<String>) -> Boolean =
        { context, permissionSet ->
            val pm = context.packageManager
            val permissionNames = withContext(Dispatchers.Default) {
                permissionSet
                    .mapNotNull { pm.getPermissionInfo(it, 0)?.loadLabel(pm) }
                    .joinToString(", ")
            }
            suspendCancellableCoroutine { cont ->
                val dialog = AlertDialog.Builder(context)
                    .setMessage(
                        context.getString(
                            R.string.permission_dialog_settings_message_default,
                            permissionNames
                        )
                    )
                    .setPositiveButton(R.string.permission_dialog_settings_button_default) { _, _ ->
                        cont.resume(true)
                    }
                    .setOnCancelListener {
                        cont.resume(false)
                    }
                    .setCancelable(true)
                    .show()
                cont.invokeOnCancellation {
                    dialog.dismiss()
                }
            }
        }
)
