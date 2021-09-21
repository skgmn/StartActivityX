package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.LinkedHashSet
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val KEY_PREFIX = "com.github.skgmn.startactivityx.RequestMultiplePermissions_"

suspend fun Activity.requestPermissions(vararg permissions: String): GrantResult {
    return requestPermissions(listOf(*permissions))
}

suspend fun Activity.requestPermissions(permissions: Collection<String>): GrantResult {
    return requestPermissions(PermissionRequest(permissions))
}

suspend fun Activity.requestPermissions(request: PermissionRequest): GrantResult {
    return if (this is ComponentActivity) {
        requestPermissions(request)
    } else {
        requestPermissions(
            activity = this,
            activityResultRegistrySupplier = {
                StartActivityHelperUtils.launchHelperActivity(this).activityResultRegistry
            },
            request = request
        )
    }
}

suspend fun ComponentActivity.requestPermissions(
    vararg permissions: String
): GrantResult {
    return requestPermissions(listOf(*permissions))
}

suspend fun ComponentActivity.requestPermissions(
    permissions: Collection<String>
): GrantResult {
    return requestPermissions(PermissionRequest(permissions))
}

suspend fun ComponentActivity.requestPermissions(
    request: PermissionRequest
): GrantResult {
    return requestPermissions(
        activity = this,
        activityResultRegistrySupplier = { activityResultRegistry },
        request = request
    )
}

suspend fun Fragment.requestPermissions(vararg permissions: String): GrantResult {
    return requestPermissions(listOf(*permissions))
}

suspend fun Fragment.requestPermissions(permissions: Collection<String>): GrantResult {
    return requestPermissions(PermissionRequest(permissions))
}

suspend fun Fragment.requestPermissions(request: PermissionRequest): GrantResult {
    val activity = requireActivity()
    return requestPermissions(
        activity = activity,
        activityResultRegistrySupplier = { activity.activityResultRegistry },
        request = request
    )
}

private suspend fun ActivityResultRegistry.requestPermissionsImpl(
    permissions: Collection<String>
): Map<String, Boolean> {
    return suspendCoroutine { cont ->
        @Suppress("JoinDeclarationAndAssignment")
        lateinit var launcher: ActivityResultLauncher<Array<String>>
        launcher = register(
            KEY_PREFIX + UUID.randomUUID().toString(),
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            launcher.unregister()
            globalPermissionResultSignal.tryEmit(Unit)
            cont.resume(it)
        }
        launcher.launch(permissions.toTypedArray())
    }
}

private suspend fun requestPermissions(
    activity: Activity,
    activityResultRegistrySupplier: suspend () -> ActivityResultRegistry,
    request: PermissionRequest
): GrantResult = withContext(Dispatchers.Main.immediate) {
    val storage = PermissionStorage.getInstance(activity)
    val permissionsGranted = request.permissions.asSequence()
        .filter {
            PermissionHelper.checkSelfPermission(activity, it) ==
                    PermissionChecker.PERMISSION_GRANTED
        }
        .toSet()
    storage.removeDoNotAskAgainPermissions(permissionsGranted)

    if (request.permissions.all { it in permissionsGranted }) {
        return@withContext GrantResult.ALREADY_GRANTED
    }

    val permissionsShouldShowRationale = request.permissions.asSequence()
        .filter { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
        .toCollection(LinkedHashSet())
    val permissionsShouldNotAskAgain = storage.removeDoNotAskAgainPermissions(
        permissionsShouldShowRationale
    )

    if (permissionsShouldNotAskAgain.isNotEmpty()) {
        val permissions = permissionsShouldShowRationale + permissionsShouldNotAskAgain
        if (request.userIntended && request.goToSettingsDialog(activity, permissions)) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.data = uri
            activity.startActivityForResult(intent)

            val allGranted = permissions.all {
                PermissionHelper.checkSelfPermission(activity, it) ==
                        PermissionChecker.PERMISSION_GRANTED
            }
            if (allGranted) {
                return@withContext GrantResult.JUST_GRANTED
            }
        }
        return@withContext GrantResult.DO_NOT_ASK_AGAIN
    }

    if (!request.userIntended &&
        permissionsShouldShowRationale.isNotEmpty() &&
        !request.rationaleDialog(activity, permissionsShouldShowRationale)
    ) {
        return@withContext GrantResult.DENIED
    }

    val permissionMapCandidate =
        activityResultRegistrySupplier().requestPermissionsImpl(request.permissions)
    val permissionMap =
        if (permissionMapCandidate.any { PermissionHelper.regardGrantedImplicitly(it.key) }) {
            permissionMapCandidate.mapValues { (key, value) ->
                if (PermissionHelper.regardGrantedImplicitly(key)) {
                    true
                } else {
                    value
                }
            }
        } else {
            permissionMapCandidate
        }
    if (permissionMap.all { it.value }) {
        return@withContext GrantResult.JUST_GRANTED
    }

    val pm = activity.packageManager
    val permissionsDoNotAskAgain = request.permissions.filter {
        permissionMap[it] == false &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) &&
                PermissionHelper.isDeniable(pm, it)
    }
    storage.addDoNotAskAgainPermissions(permissionsDoNotAskAgain)

    return@withContext if (permissionsDoNotAskAgain.isEmpty()) {
        GrantResult.DENIED
    } else {
        GrantResult.DO_NOT_ASK_AGAIN
    }
}