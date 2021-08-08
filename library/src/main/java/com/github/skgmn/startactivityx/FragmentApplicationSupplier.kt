package com.github.skgmn.startactivityx

import android.app.Application
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
                    ?: InternalUtils.iterateContext(ctx).firstNotNullOfOrNull { it as? Application }
            }
    }
}
