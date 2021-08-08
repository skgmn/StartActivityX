package com.github.skgmn.startactivityx

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

suspend fun Context.startActivityForResult(intent: Intent): ActivityResult {
    return if (this is FragmentActivity) {
        startActivityForResult(intent)
    } else {
        StartActivityHelperUtils.launchHelperActivity(this)
            .startActivityForResult(intent)
    }
}

suspend fun FragmentActivity.startActivityForResult(intent: Intent): ActivityResult {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        StartActivityHelperUtils.getHelperFragment(supportFragmentManager)
            .startActivityForResult(intent)
    } else {
        // Before Lollipop, startActivityForResult fails when it is called from the activity
        // of which launch mode is either singleTask or singleInstance.
        // So use dummy, invisible activity to avoid the problem.
        StartActivityHelperUtils.launchHelperActivity(this)
            .startActivityForResult(intent)
    }
}

suspend fun Fragment.startActivityForResult(intent: Intent): ActivityResult {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        StartActivityHelperUtils.getHelperFragment(childFragmentManager)
            .startActivityForResult(intent)
    } else {
        // Before Lollipop, startActivityForResult fails when it is called from the activity
        // of which launch mode is either singleTask or singleInstance.
        // So use dummy, invisible activity to avoid the problem.
        StartActivityHelperUtils.launchHelperActivity(this)
            .startActivityForResult(intent)
    }
}
