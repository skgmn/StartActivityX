package com.github.skgmn.rapidstartactivity

import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal val globalPermissionResultSignal = MutableSharedFlow<Any>(
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)

@ExperimentalCoroutinesApi
fun FragmentActivity.hasPermissions(permissions: Collection<String>): Flow<Boolean> {
    return hasPermissions(
        context = this,
        helperFragment = StartActivityHelperUtils.getHelperFragment(supportFragmentManager),
        permissions = permissions
    )
}

@ExperimentalCoroutinesApi
fun Fragment.hasPermissions(permissions: Collection<String>): Flow<Boolean> {
    return hasPermissions(
        context = requireContext(),
        helperFragment = StartActivityHelperUtils.getHelperFragment(childFragmentManager),
        permissions = permissions
    )
}

@ExperimentalCoroutinesApi
private fun hasPermissions(
    context: Context,
    helperFragment: StartActivityHelperFragment,
    permissions: Collection<String>
): Flow<Boolean> {
    return helperFragment.arePermissionsReloadable()
        .flatMapLatest { reloadable ->
            if (reloadable) {
                globalPermissionResultSignal.onStart { emit(Unit) }
            } else {
                emptyFlow()
            }
        }
        .map { checkPermissionsGranted(context, permissions) }
        .distinctUntilChanged()
}

internal fun checkPermissionsGranted(context: Context, permissions: Collection<String>): Boolean {
    return permissions.all {
        PermissionChecker.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
    }
}
