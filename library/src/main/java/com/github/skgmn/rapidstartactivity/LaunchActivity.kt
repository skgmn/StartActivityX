package com.github.skgmn.rapidstartactivity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.random.Random

suspend fun <T : Activity> Context.launchActivity(intent: ExplicitIntent<T>): T {
    return launchActivity(
        applicationSupplier = ContextApplicationSupplier(this),
        activityStarter = { startActivity(it) },
        intent = intent
    )
}

suspend fun <T : Activity> Activity.launchActivity(
    intent: ExplicitIntent<T>,
    overridePendingTransition: OverridePendingTransition? = null
): T {
    return launchActivity(
        applicationSupplier = ActivityApplicationSupplier(this),
        activityStarter = { intentToStart ->
            startActivity(intentToStart)
            overridePendingTransition?.let {
                overridePendingTransition(it.enterAnim, it.exitAnim)
            }
        },
        intent = intent
    )
}

suspend fun <T : Activity> Fragment.launchActivity(
    intent: ExplicitIntent<T>,
    overridePendingTransition: OverridePendingTransition? = null
): T {
    return launchActivity(
        applicationSupplier = FragmentApplicationSupplier(this),
        activityStarter = { intentToStart ->
            startActivity(intentToStart)
            overridePendingTransition?.let {
                activity?.overridePendingTransition(it.enterAnim, it.exitAnim)
            }
        },
        intent = intent
    )
}

private suspend fun <T : Activity> launchActivity(
    applicationSupplier: ApplicationSupplier,
    activityStarter: (Intent) -> Unit,
    intent: ExplicitIntent<T>
): T = withContext(Dispatchers.Main.immediate) {
    suspendCancellableCoroutine { cont ->
        val randomKey = UUID.randomUUID().toString()
        val randomValue = Random.nextLong()

        val actualIntent = Intent(intent)
        actualIntent.putExtra(randomKey, randomValue)

        val app = applicationSupplier.getApplication()
            ?: throw IllegalStateException("Cannot get application instance")

        val callback = object : Application.ActivityLifecycleCallbacks {
            @Suppress("UNCHECKED_CAST")
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                val isExpectedActivity =
                    activity.intent?.getLongExtra(randomKey, 0) == randomValue
                if (isExpectedActivity) {
                    app.unregisterActivityLifecycleCallbacks(this)
                    cont.resume(activity as T)
                }
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }

        }
        app.registerActivityLifecycleCallbacks(callback)

        try {
            activityStarter(actualIntent)
        } catch (e: Throwable) {
            app.unregisterActivityLifecycleCallbacks(callback)
            throw e
        }

        cont.invokeOnCancellation {
            app.unregisterActivityLifecycleCallbacks(callback)
        }
    }
}
