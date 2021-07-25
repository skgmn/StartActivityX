package com.github.skgmn.rapidstartactivity

import android.content.Context
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

fun FragmentActivity.hasPermissions(vararg permissions: String): Flow<Boolean> {
    return hasPermissions(listOf(*permissions))
}

fun FragmentActivity.hasPermissions(permissions: Collection<String>): Flow<Boolean> {
    return permissionStatus(permissions).map { it == PermissionStatus.GRANTED }
}

fun Fragment.hasPermissions(vararg permissions: String): Flow<Boolean> {
    return hasPermissions(listOf(*permissions))
}

fun Fragment.hasPermissions(permissions: Collection<String>): Flow<Boolean> {
    return permissionStatus(permissions).map { it == PermissionStatus.GRANTED }
}
