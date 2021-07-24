package com.github.skgmn.rapidstartactivity

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlin.random.Random

internal object StartActivityHelperUtils {
    fun getHelperFragment(fragmentManager: FragmentManager): StartActivityHelperFragment {
        return fragmentManager.fragments
            .firstNotNullOfOrNull { it as? StartActivityHelperFragment }
            ?: StartActivityHelperFragment().also {
                fragmentManager.beginTransaction()
                    .add(it, null)
                    .commitAllowingStateLoss()
            }
    }

    suspend fun launchHelperActivity(context: Context): StartActivityHelperActivity {
        val intent = ExplicitIntent(context, StartActivityHelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return context.launchActivity(intent)
    }

    suspend fun launchHelperActivity(activity: Activity): StartActivityHelperActivity {
        val intent = ExplicitIntent(activity, StartActivityHelperActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return activity.launchActivity(intent, OverridePendingTransition(0, 0))
    }

    suspend fun launchHelperActivity(fragment: Fragment): StartActivityHelperActivity {
        val intent = ExplicitIntent(
            fragment.requireContext(),
            StartActivityHelperActivity::class.java
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        return fragment.launchActivity(intent, OverridePendingTransition(0, 0))
    }

    fun allocateRequestCode(keys: Set<Int>): Int {
        return generateSequence { Random.nextInt(0, 65536) }
            .filter { it !in keys }
            .first()
    }
}
