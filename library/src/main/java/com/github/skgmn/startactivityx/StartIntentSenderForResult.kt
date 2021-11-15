package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val KEY_PREFIX = "com.github.skgmn.startactivityx.StartIntentSenderForResult_"

suspend fun Context.startIntentSenderForResult(
    intent: IntentSender,
    fillInIntent: Intent? = null,
    flagsMask: Int = 0,
    flagsValues: Int = 0,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return when (this) {
        is ComponentActivity -> {
            startIntentSenderForResult(
                intent,
                fillInIntent,
                flagsMask,
                flagsValues,
                activityOptions
            )
        }
        is Activity -> {
            StartActivityHelperUtils.launchHelperActivity(this).startIntentSenderForResult(
                intent,
                fillInIntent,
                flagsMask,
                flagsValues,
                activityOptions
            )
        }
        else -> {
            StartActivityHelperUtils.launchHelperActivity(this).startIntentSenderForResult(
                intent,
                fillInIntent,
                flagsMask,
                flagsValues,
                activityOptions
            )
        }
    }
}

suspend fun ComponentActivity.startIntentSenderForResult(
    intent: IntentSender,
    fillInIntent: Intent? = null,
    flagsMask: Int = 0,
    flagsValues: Int = 0,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ||
        this is StartActivityHelperActivity
    ) {
        activityResultRegistry.startIntentSenderForResult(
            intent,
            fillInIntent,
            flagsMask,
            flagsValues,
            activityOptions
        )
    } else {
        // Before Lollipop, startActivityForResult fails when it is called from the activity
        // of which launch mode is either singleTask or singleInstance.
        // So use dummy, invisible activity to avoid the problem.
        StartActivityHelperUtils.launchHelperActivity(this).startIntentSenderForResult(
            intent,
            fillInIntent,
            flagsMask,
            flagsValues,
            activityOptions
        )
    }
}

suspend fun Fragment.startIntentSenderForResult(
    intent: IntentSender,
    fillInIntent: Intent? = null,
    flagsMask: Int = 0,
    flagsValues: Int = 0,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val registryOwner = requireHost() as? ActivityResultRegistryOwner ?: requireActivity()
        registryOwner.activityResultRegistry.startIntentSenderForResult(
            intent,
            fillInIntent,
            flagsMask,
            flagsValues,
            activityOptions
        )
    } else {
        // Before Lollipop, startActivityForResult fails when it is called from the activity
        // of which launch mode is either singleTask or singleInstance.
        // So use dummy, invisible activity to avoid the problem.
        StartActivityHelperUtils.launchHelperActivity(this).startIntentSenderForResult(
            intent,
            fillInIntent,
            flagsMask,
            flagsValues,
            activityOptions
        )
    }
}

private suspend fun ActivityResultRegistry.startIntentSenderForResult(
    intent: IntentSender,
    fillInIntent: Intent? = null,
    flagsMask: Int = 0,
    flagsValues: Int = 0,
    activityOptions: ActivityOptionsCompat? = null
): ActivityResult {
    return suspendCoroutine { cont ->
        @Suppress("JoinDeclarationAndAssignment")
        lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>
        launcher = register(
            KEY_PREFIX + UUID.randomUUID().toString(),
            ActivityResultContracts.StartIntentSenderForResult()
        ) {
            launcher.unregister()
            cont.resume(it)
        }
        val request = IntentSenderRequest.Builder(intent)
            .setFillInIntent(fillInIntent)
            .setFlags(flagsValues, flagsMask)
            .build()
        launcher.launch(request, activityOptions)
    }
}