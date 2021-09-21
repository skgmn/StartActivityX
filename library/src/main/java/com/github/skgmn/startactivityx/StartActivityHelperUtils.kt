package com.github.skgmn.startactivityx

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment

internal object StartActivityHelperUtils {
    suspend fun launchHelperActivity(context: Context): StartActivityHelperActivity {
        val intent = ExplicitIntent(context, StartActivityHelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return context.startActivityForInstance(intent)
    }

    suspend fun launchHelperActivity(activity: Activity): StartActivityHelperActivity {
        val intent = ExplicitIntent(activity, StartActivityHelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return activity.startActivityForInstance(
            intent,
            ActivityOptionsCompat.makeCustomAnimation(activity, 0, 0)
        )
    }

    suspend fun launchHelperActivity(fragment: Fragment): StartActivityHelperActivity {
        val context = fragment.requireContext()
        val intent = ExplicitIntent(context, StartActivityHelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return fragment.startActivityForInstance(
            intent,
            ActivityOptionsCompat.makeCustomAnimation(context, 0, 0)
        )
    }
}
