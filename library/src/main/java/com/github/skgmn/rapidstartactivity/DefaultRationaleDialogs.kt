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
    fun general(): suspend (Context, Collection<String>) -> Boolean =
        { context, permissions ->
            val pm = context.packageManager
            val permissionGroupNames = withContext(Dispatchers.IO) {
                getGroupNames(pm, permissions).joinToString(", ")
                    .takeIf { it.isNotEmpty() }
                    ?: getPermissionNames(pm, permissions)
                        .map { "- $it" }
                        .joinToString("\n")
            }
            val title = context.getString(R.string.default_rationale_general_title)
            val messageHtml = context.getString(
                R.string.default_rationale_general_message,
                permissionGroupNames
            )
            val message = Html.fromHtml(messageHtml)

            suspendCancellableCoroutine { cont ->
                val dialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> cont.resume(true) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> cont.resume(false) }
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

    private fun getGroupNames(
        pm: PackageManager,
        permissions: Collection<String>
    ): Sequence<String> {
        return permissions.asSequence()
            .distinct()
            .mapNotNull { getGroupName(pm, it) }
            .distinct()
            .mapNotNull {
                try {
                    pm.getPermissionGroupInfo(it, 0).loadLabel(pm).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
    }

    private fun getPermissionNames(
        pm: PackageManager,
        permissions: Collection<String>
    ): Sequence<String> {
        return permissions.asSequence()
            .distinct()
            .mapNotNull {
                try {
                    pm.getPermissionInfo(it, 0).loadLabel(pm)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .map { "- $it" }
    }

    private fun getGroupName(pm: PackageManager, permission: String): String? {
        return try {
            pm.getPermissionInfo(permission, 0).group
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
            ?.takeIf { it != "android.permission-group.UNDEFINED" }
            ?: getHeuristicGroupName(permission)
    }

    @Suppress("DEPRECATION")
    private fun getHeuristicGroupName(permission: String): String? {
        return when (permission) {
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_WAP_PUSH,
            android.Manifest.permission.RECEIVE_MMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.SEND_SMS ->
                "android.permission-group.SMS"
            android.Manifest.permission.READ_CALENDAR,
            android.Manifest.permission.WRITE_CALENDAR ->
                "android.permission-group.CALENDAR"
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG ->
                "android.permission-group.CALL_LOG"
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.ACCESS_MEDIA_LOCATION ->
                "android.permission-group.LOCATION"
            android.Manifest.permission.ANSWER_PHONE_CALLS,
            android.Manifest.permission.READ_PHONE_NUMBERS,
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.CALL_PHONE,
            android.Manifest.permission.ACCEPT_HANDOVER,
            android.Manifest.permission.USE_SIP,
            android.Manifest.permission.PROCESS_OUTGOING_CALLS ->
                "android.permission-group.PHONE"
            android.Manifest.permission.BODY_SENSORS ->
                "android.permission-group.SENSORS"
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                "android.permission-group.STORAGE"
            android.Manifest.permission.WRITE_CONTACTS,
            android.Manifest.permission.READ_CONTACTS ->
                "android.permission-group.CONTACTS"
            android.Manifest.permission.CAMERA ->
                "android.permission-group.CAMERA"
            android.Manifest.permission.GET_ACCOUNTS ->
                null
            android.Manifest.permission.ACTIVITY_RECOGNITION ->
                "android.permission-group.ACTIVITY_RECOGNITION"
            android.Manifest.permission.RECORD_AUDIO ->
                "android.permission-group.MICROPHONE"
            else -> null
        }
    }
}
