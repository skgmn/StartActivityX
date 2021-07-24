package com.github.skgmn.rapidstartactivity

import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@FlowPreview
fun FragmentActivity.hasPermissions(permissions: Collection<String>): Flow<Boolean> {
    return hasPermissions(
        context = this,
        permissionHelper = StartActivityHelperUtils.getHelperFragment(supportFragmentManager),
        permissions = permissions
    )
}

@FlowPreview
fun Fragment.hasPermissions(permissions: Collection<String>): Flow<Boolean> {
    return hasPermissions(
        context = requireContext(),
        permissionHelper = StartActivityHelperUtils.getHelperFragment(childFragmentManager),
        permissions = permissions
    )
}

@FlowPreview
private fun hasPermissions(
    context: Context,
    permissionHelper: PermissionHelper,
    permissions: Collection<String>
): Flow<Boolean> {
    return permissionHelper.getPermissionReloadSignal()
        .onStart { emit(Unit) }
        .map { checkPermissionsGranted(context, permissions) }
        .distinctUntilChanged()
}

private fun checkPermissionsGranted(context: Context, permissions: Collection<String>): Boolean {
    return permissions.all {
        PermissionChecker.checkSelfPermission(context, it) == PermissionChecker.PERMISSION_GRANTED
    }
}
