package com.github.skgmn.rapidstartactivity.sample.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.github.skgmn.rapidstartactivity.sample.camera.databinding.FragmentCameraBinding

class CameraFragment : Fragment() {
    private var binding: FragmentCameraBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentCameraBinding>(
            inflater, R.layout.fragment_camera, container, false
        )

        this.binding = binding

        return binding.root
    }

    override fun onDestroyView() {
        this.binding = null
        super.onDestroyView()
    }
}
