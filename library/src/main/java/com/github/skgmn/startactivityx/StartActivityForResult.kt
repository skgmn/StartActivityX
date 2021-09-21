package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val KEY_PREFIX = "com.github.skgmn.startactivityx.StartActivityForResult_"

suspend fun Context.startActivityForResult(
    intent: Intent,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return when (this) {
        is ComponentActivity -> {
            startActivityForResult(intent, activityOptions)
        }
        is Activity -> {
            StartActivityHelperUtils.launchHelperActivity(this)
                .startActivityForResult(intent, activityOptions)
        }
        else -> {
            StartActivityHelperUtils.launchHelperActivity(this)
                .startActivityForResult(intent, activityOptions)
        }
    }
}

suspend fun ComponentActivity.startActivityForResult(
    intent: Intent,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ||
        this is StartActivityHelperActivity
    ) {
        activityResultRegistry.startActivityForResult(intent, activityOptions)
    } else {
        // Before Lollipop, startActivityForResult fails when it is called from the activity
        // of which launch mode is either singleTask or singleInstance.
        // So use dummy, invisible activity to avoid the problem.
        StartActivityHelperUtils.launchHelperActivity(this)
            .startActivityForResult(intent, activityOptions)
    }
}

suspend fun Fragment.startActivityForResult(
    intent: Intent,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        (activity as? ActivityResultRegistryOwner)
            ?.activityResultRegistry
            ?.startActivityForResult(intent, activityOptions)
            ?: StartActivityHelperUtils.getHelperFragment(childFragmentManager)
                .startActivityForResultImpl(intent, activityOptions)
    } else {
        // Before Lollipop, startActivityForResult fails when it is called from the activity
        // of which launch mode is either singleTask or singleInstance.
        // So use dummy, invisible activity to avoid the problem.
        StartActivityHelperUtils.launchHelperActivity(this)
            .startActivityForResult(intent)
    }
}

private suspend fun ActivityResultRegistry.startActivityForResult(
    intent: Intent,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return suspendCoroutine { cont ->
        @Suppress("JoinDeclarationAndAssignment")
        lateinit var launcher: ActivityResultLauncher<Intent>
        launcher = register(
            KEY_PREFIX + UUID.randomUUID().toString(),
            ActivityResultContracts.StartActivityForResult()
        ) {
            launcher.unregister()
            cont.resume(it)
        }
        launcher.launch(intent, activityOptions)
    }
}