package com.github.skgmn.rapidstartactivity

import android.content.Context
import android.content.ContextWrapper

internal object ContextUtils {
    fun iterateContext(context: Context): Sequence<Context> {
        return generateSequence(context) {
            (it as? ContextWrapper)?.baseContext
        }
    }
}
