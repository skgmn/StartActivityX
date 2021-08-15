package com.github.skgmn.startactivityx

import android.app.Application
import android.content.Context

internal class ContextApplicationSupplier(private val context: Context): ApplicationSupplier {
    private var app: Application? = null

    override fun getApplication(): Application? {
        return app ?: tryGetApplication()?.also {
            app = it
        }
    }

    private fun tryGetApplication(): Application? {
        val appContext = context.applicationContext
        return appContext as? Application
            ?: InternalUtils.iterateContext(appContext).firstNotNullOfOrNull { it as? Application }
    }
}
