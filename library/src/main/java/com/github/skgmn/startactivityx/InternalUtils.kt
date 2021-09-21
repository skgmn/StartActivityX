package com.github.skgmn.startactivityx

import android.content.Context
import android.content.ContextWrapper

internal object InternalUtils {
    fun iterateContext(context: Context): Sequence<Context> {
        return generateSequence(context) {
            (it as? ContextWrapper)?.baseContext
        }
    }
}
