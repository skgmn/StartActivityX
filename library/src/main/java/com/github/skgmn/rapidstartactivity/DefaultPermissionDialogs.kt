package com.github.skgmn.rapidstartactivity

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.text.Html
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume

object DefaultPermissionDialogs {
    fun generalRationale(): suspend (Context, Collection<String>) -> Boolean =
            { context, permissions ->
                val pm = context.packageManager
                val permissionListString = withContext(Dispatchers.IO) {
                    makePermissionListString(pm, permissions)
                }
                val title = context.getString(R.string.default_rationale_general_title)
                val messageHtml = context.getString(
                        R.string.default_rationale_general_message,
                        permissionListString
                )

                @Suppress("DEPRECATION")
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

    fun detailedRationale(
            @RawRes rationaleId: Int
    ): suspend (Context, Collection<String>) -> Boolean = { context, permissions ->
        val jsonString = context.resources.openRawResource(rationaleId).reader().readText()
        val json = JSONObject(jsonString)
        val rationales = json.keys().asSequence()
                .associateBy(
                        keySelector = { it },
                        valueTransform = { json.getString(it) }
                )
        detailedRationale(rationales)(context, permissions)
    }

    fun detailedRationale(
            rationales: Map<String, String>
    ): suspend (Context, Collection<String>) -> Boolean = { context, permissions ->
        val pm = context.packageManager
        val title = context.getString(R.string.default_rationale_general_title)
        val message = permissions.asSequence()
                .distinct()
                .groupBy(
                        keySelector = { getGroupName(pm, it) ?: "" },
                        valueTransform = { rationales[it] ?: "" }
                )
                .filter { it.key.isNotEmpty() }
                .mapNotNull { entry ->
                    val groupTitle = pm.getPermissionGroupInfo(entry.key, 0).loadLabel(pm)
                    val rationaleList = entry.value
                            .filter { it.isNotEmpty() }
                            .joinToString("<br>") { "- $it" }
                    if (rationaleList.isNotEmpty()) {
                        "<b>$groupTitle</b><br><small>$rationaleList</small>"
                    } else {
                        null
                    }
                }
                .joinToString("<br><br>")

        suspendCancellableCoroutine { cont ->
            val dialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(@Suppress("DEPRECATION") Html.fromHtml(message))
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

    fun goToSettings(): suspend (Context, Collection<String>) -> Boolean = { context, permissions ->
        val pm = context.packageManager
        val permissionListString = withContext(Dispatchers.IO) {
            makePermissionListString(pm, permissions)
        }
        val title = context.getString(R.string.default_rationale_general_title)
        val messageHtml = context.getString(
                R.string.default_go_to_settings_message,
                permissionListString
        )

        @Suppress("DEPRECATION")
        val message = Html.fromHtml(messageHtml)

        suspendCancellableCoroutine { cont ->
            val dialog = AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton(R.string.default_go_to_settings_button_open_settings) { _, _ ->
                        cont.resume(true)
                    }
                    .setNegativeButton(R.string.default_go_to_settings_button_not_now) { _, _ ->
                        cont.resume(false)
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

    private fun makePermissionListString(
            pm: PackageManager,
            permissions: Collection<String>
    ): String {
        val groupNamesString = permissions
                .asSequence()
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
        if (groupNamesString.isNotEmpty()) {
            return groupNamesString
        }

        return permissions.asSequence()
                .distinct()
                .mapNotNull {
                    try {
                        val label = pm.getPermissionInfo(it, 0).loadLabel(pm)
                        "- $label"
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                }
                .joinToString("<br>")
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
