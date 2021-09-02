package com.github.skgmn.startactivityx

import android.content.Context
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal val globalPermissionResultSignal = MutableSharedFlow<Any>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

fun ComponentActivity.listenPermissionStatus(vararg permissions: String): Flow<PermissionStatus> {
    return listenPermissionStatus(listOf(*permissions))
}

fun ComponentActivity.listenPermissionStatus(permissions: Collection<String>): Flow<PermissionStatus> {
    return listenPermissionStatus(
        context = this,
        lifecycleOwner = this,
        permissions = permissions
    )
}

fun Fragment.listenPermissionStatus(vararg permissions: String): Flow<PermissionStatus> {
    return listenPermissionStatus(listOf(*permissions))
}

fun Fragment.listenPermissionStatus(permissions: Collection<String>): Flow<PermissionStatus> {
    return listenPermissionStatus(
        context = requireContext(),
        lifecycleOwner = this,
        permissions = permissions
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun listenPermissionStatus(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    permissions: Collection<String>
): Flow<PermissionStatus> {
    return lifecycleOwner.isStarted()
        .flatMapLatest { started ->
            if (started) {
                val resumedAfterPaused = lifecycleOwner.watchLifecycleEvent()
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