package com.github.skgmn.rapidstartactivity

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow

internal class PermissionStorage(context: Context) {
    private val prefs by lazy {
        context.getSharedPreferences(
                "com.github.skgmn.rapidstartactivity.ADDITIONAL_PERMISSION_DATA",
                Context.MODE_PRIVATE
        )
    }
    private val doNotAskAgainPermissionsLock = Any()

    var doNotAskAgainPermissions: Set<String>
        get() = prefs.getStringSet(KEY_DO_NOT_ASK_AGAIN_PERMISSIONS, emptySet()) ?: emptySet()
        private set(value) {
            prefs.edit().putStringSet(KEY_DO_NOT_ASK_AGAIN_PERMISSIONS, value).apply()
        }

    fun addDoNotAskAgainPermissions(permissions: Collection<String>): Set<String> {
        return synchronized(doNotAskAgainPermissionsLock) {
            (doNotAskAgainPermissions + permissions).also {
                doNotAskAgainPermissions = it
            }
        }
    }

    fun removeDoNotAskAgainPermissions(permissions: Collection<String>): Set<String> {
        return synchronized(doNotAskAgainPermissionsLock) {
            (doNotAskAgainPermissions - permissions).also {
                doNotAskAgainPermissions = it
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun doNotAskAgainPermissionsChange(): Flow<*> {
        return callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_DO_NOT_ASK_AGAIN_PERMISSIONS) {
                    trySend(Unit)
                }
            }
            prefs.registerOnSharedPreferenceChangeListener(listener)
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }
                .buffer(Channel.CONFLATED)
    }

    companion object {
        private const val KEY_DO_NOT_ASK_AGAIN_PERMISSIONS = "doNotAskAgainPermissions"

        @Volatile
        private var instance: PermissionStorage? = null

        fun getInstance(context: Context): PermissionStorage {
            return instance ?: synchronized(this) {
                instance ?: PermissionStorage(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
