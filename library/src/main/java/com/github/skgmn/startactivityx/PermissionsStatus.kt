package com.github.skgmn.startactivityx

import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

enum class PermissionsStatus {
    GRANTED,
    DENIED,
    DO_NOT_ASK_AGAIN;

    val granted: Boolean
        get() = this === GRANTED

    val denied: Boolean
        get() = this === DENIED || this === DO_NOT_ASK_AGAIN
}

internal val globalPermissionResultSignal = MutableSharedFlow<Any>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
)

fun FragmentActivity.permissionsStatus(vararg permissions: String): Flow<PermissionsStatus> {
    return permissionsStatus(listOf(*permissions))
}

fun FragmentActivity.permissionsStatus(permissions: Collection<String>): Flow<PermissionsStatus> {
    return permissionsStatus(
            context = this,
            helperFragment = StartActivityHelperUtils.getHelperFragment(supportFragmentManager),
            permissions = permissions
    )
}

fun Fragment.permissionsStatus(vararg permissions: String): Flow<PermissionsStatus> {
    return permissionsStatus(listOf(*permissions))
}

fun Fragment.permissionsStatus(permissions: Collection<String>): Flow<PermissionsStatus> {
    return permissionsStatus(
            context = requireContext(),
            helperFragment = StartActivityHelperUtils.getHelperFragment(childFragmentManager),
            permissions = permissions
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun permissionsStatus(
        context: Context,
        helperFragment: StartActivityHelperFragment,
        permissions: Collection<String>
): Flow<PermissionsStatus> {
    return helperFragment.isStarted()
            .flatMapLatest { started ->
                if (started) {
                    val resumedAfterPaused = helperFragment.watchLifecycleEvent()
                            .dropWhile { it != Lifecycle.Event.ON_PAUSE }
                            .filter { it == Lifecycle.Event.ON_RESUME }
                    merge(
                            resumedAfterPaused,
                            globalPermissionResultSignal,
                            PermissionStorage.getInstance(context).doNotAskAgainPermissionsChange(),
                            flowOf(Unit)
                    )
                } else {
                    emptyFlow()
                }
            }
            .map { getPermissionsStatus(context, permissions) }
            .distinctUntilChanged()
}

private fun getPermissionsStatus(
        context: Context,
        permissions: Collection<String>
): PermissionsStatus {
    val storage = PermissionStorage.getInstance(context)
    val doNotAskAgainPermissions = storage.doNotAskAgainPermissions
    permissions.forEach {
        val status = when {
            InternalUtils.checkSelfPermission(context, it) ==
                    PermissionChecker.PERMISSION_GRANTED -> {
                PermissionsStatus.GRANTED
            }
            it in doNotAskAgainPermissions -> {
                PermissionsStatus.DO_NOT_ASK_AGAIN
            }
            else -> {
                PermissionsStatus.DENIED
            }
        }
        if (status != PermissionsStatus.GRANTED) {
            return status
        }
    }
    return PermissionsStatus.GRANTED
}
