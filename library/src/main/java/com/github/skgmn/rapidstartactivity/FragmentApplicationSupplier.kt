package com.github.skgmn.rapidstartactivity

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.Fragment

internal class FragmentApplicationSupplier(private val fragment: Fragment): ApplicationSupplier {
    private var app: Application? = null

    override fun getApplication(): Application? {
        return app ?: tryGetApplication()?.also {
            app = it
        }
    }

    private fun tryGetApplication(): Application? {
        return fragment.activity?.application
            ?: fragment.context?.applicationContext?.let { ctx ->
                ctx as? Application
                    ?: ContextUtils.iterateContext(ctx).firstNotNullOfOrNull { it as? Application }
            }
    }
}
