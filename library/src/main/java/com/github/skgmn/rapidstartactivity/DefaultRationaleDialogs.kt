package com.github.skgmn.rapidstartactivity

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object DefaultRationaleDialogs {
    @Suppress("DEPRECATION")
    fun general(): suspend (Context, Collection<String>, Boolean) -> Boolean =
        { context, permissions, doNotAskAgain ->
            val pm = context.packageManager
            val permissionGroupNames = withContext(Dispatchers.IO) {
                permissions.asSequence()
                    .distinct()
                    .mapNotNull { getGroupName(pm, it) }
                    .distinct()
                    .mapNotNull {
                        try {
                            pm.getPermissionGroupInfo(it, 0).loadLabel(pm)
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                    }
                    .joinToString(", ")
            }
            val title = context.getString(R.string.default_rationale_general_title)
            val messageHtml = if (doNotAskAgain) {
                context.getString(
                    R.string.default_rationale_general_message_do_not_ask_again,
                    permissionGroupNames
                )
            } else {
                context.getString(R.string.default_rationale_general_message, permissionGroupNames)
            }
            val message = Html.fromHtml(messageHtml)
            val positiveButtonText = if (doNotAskAgain) {
                R.string.default_rationale_button_open_settings
            } else {
                android.R.string.ok
            }
            val negativeButtonText = if (doNotAskAgain) {
                R.string.default_rationale_button_not_now
            } else {
                android.R.string.cancel
            }

            suspendCancellableCoroutine { cont ->
                val dialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(positiveButtonText) { _, _ -> cont.resume(true) }
                    .setNegativeButton(negativeButtonText) { _, _ -> cont.resume(false) }
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

    private fun getGroupName(pm: PackageManager, permission: String): String? {
        return try {
            pm.getPermissionInfo(permission, 0).group
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
            ?.takeIf { it != "android.permission-group.UNDEFINED" }
            ?: getAdjustedPermissionGroup(permission)
    }

    private fun getAdjustedPermissionGroup(permission: String): String? {
        return when (permission) {
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.WRITE_CALENDAR -> "android.permission-group.CALENDAR"
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG -> "android.permission-group.CALL_LOG"
            android.Manifest.permission.CAMERA -> "android.permission-group.CAMERA"
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.WRITE_CONTACTS -> "android.permission-group.CONTACTS"
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION -> "android.permission-group.LOCATION"
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.READ_PHONE_NUMBERS,
            android.Manifest.permission.READ_PHONE_STATE -> "android.permission-group.PHONE"
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.RECEIVE_SMS -> "android.permission-group.SMS"
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE -> "android.permission-group.STORAGE"
            else -> null
        }
    }
}
