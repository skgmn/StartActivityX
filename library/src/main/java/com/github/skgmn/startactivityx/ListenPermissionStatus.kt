package com.github.skgmn.startactivityx

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal val globalPermissionResultSignal = MutableSharedFlow<Any>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
)

fun FragmentActivity.listenPermissionStatus(vararg permissions: String): Flow<PermissionStatus> {
    return listenPermissionStatus(listOf(*permissions))
}

fun FragmentActivity.listenPermissionStatus(permissions: Collection<String>): Flow<PermissionStatus> {
    return listenPermissionStatus(
            context = this,
            helperFragment = StartActivityHelperUtils.getHelperFragment(supportFragmentManager),
            permissions = permissions
    )
}

fun Fragment.listenPermissionStatus(vararg permissions: String): Flow<PermissionStatus> {
    return listenPermissionStatus(listOf(*permissions))
}

fun Fragment.listenPermissionStatus(permissions: Collection<String>): Flow<PermissionStatus> {
    return listenPermissionStatus(
            context = requireContext(),
            helperFragment = StartActivityHelperUtils.getHelperFragment(childFragmentManager),
            permissions = permissions
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun listenPermissionStatus(
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