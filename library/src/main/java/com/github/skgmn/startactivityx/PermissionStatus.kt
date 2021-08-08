package com.github.skgmn.startactivityx

import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

enum class PermissionStatus {
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

fun FragmentActivity.permissionStatus(vararg permissions: String): Flow<PermissionStatus> {
    return permissionStatus(listOf(*permissions))
}

fun FragmentActivity.permissionStatus(permissions: Collection<String>): Flow<PermissionStatus> {
    return permissionStatus(
            context = this,
            helperFragment = StartActivityHelperUtils.getHelperFragment(supportFragmentManager),
            permissions = permissions
    )
}

fun Fragment.permissionStatus(vararg permissions: String): Flow<PermissionStatus> {
    return permissionStatus(listOf(*permissions))
}

fun Fragment.permissionStatus(permissions: Collection<String>): Flow<PermissionStatus> {
    return permissionStatus(
            context = requireContext(),
            helperFragment = StartActivityHelperUtils.getHelperFragment(childFragmentManager),
            permissions = permissions
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun permissionStatus(
        context: Context,
        helperFragment: StartActivityHelperFragment,
        permissions: Collection<String>
): Flow<PermissionStatus> {
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
            .map { getPermissionStatus(context, permissions) }
            .distinctUntilChanged()
}

private fun getPermissionStatus(
        context: Context,
        permissions: Collection<String>
): PermissionStatus {
    val storage = PermissionStorage.getInstance(context)
    val doNotAskAgainPermissions = storage.doNotAskAgainPermissions
    permissions.forEach {
        val status = when {
            InternalUtils.checkSelfPermission(context, it) ==
                    PermissionChecker.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            it in doNotAskAgainPermissions -> {
                PermissionStatus.DO_NOT_ASK_AGAIN
            }
            else -> {
                PermissionStatus.DENIED
            }
        }
        if (status != PermissionStatus.GRANTED) {
            return status
        }
    }
    return PermissionStatus.GRANTED
}
