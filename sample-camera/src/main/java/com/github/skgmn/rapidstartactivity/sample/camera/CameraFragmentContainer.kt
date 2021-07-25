package com.github.skgmn.rapidstartactivity.sample.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

class CameraFragmentContainer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val fragmentStates = mutableMapOf<Class<out Fragment>, Fragment.SavedState?>()
    private var permissionsGranted: Boolean? = null

    init {
        if (id == View.NO_ID) {
            id = View.generateViewId()
        }
    }

    companion object {
        val REQUIRED_PERMISSIONS = listOf(
            android.Manifest.permission.CAMERA
        )

        @JvmStatic
        @BindingAdapter("fragmentManager", "permissionsGranted")
        fun setPermissionGranted(
            view: CameraFragmentContainer,
            fragmentManager: FragmentManager,
            permissionsGranted: Boolean?
        ) {
            if (view.permissionsGranted != permissionsGranted) {
                view.permissionsGranted = permissionsGranted
                when (permissionsGranted) {
                    true -> {
                        replaceFragment(view, fragmentManager, CameraFragment())
                    }
                    false -> {
                        replaceFragment(view, fragmentManager, CameraPermissionOverlayFragment())
                    }
                    else -> {
                        replaceFragment(view, fragmentManager, null)
                    }
                }
            }
        }

        private fun replaceFragment(
            view: CameraFragmentContainer,
            fragmentManager: FragmentManager,
            newFragment: Fragment?
        ) {
            var transaction = fragmentManager.beginTransaction()
            val oldFragment = fragmentManager.fragments.firstOrNull()
            oldFragment?.let {
                view.fragmentStates[it.javaClass] = fragmentManager.saveFragmentInstanceState(it)
                transaction = transaction.remove(it)
            }
            newFragment?.let {
                it.setInitialSavedState(view.fragmentStates.remove(newFragment.javaClass))
                transaction = transaction.add(view.id, it)
            }
            transaction.commitAllowingStateLoss()
        }
    }
}
