package com.github.skgmn.rapidstartactivity.sample.camera

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.skgmn.rapidstartactivity.DefaultRationaleDialogs
import com.github.skgmn.rapidstartactivity.PermissionRequest
import com.github.skgmn.rapidstartactivity.acquirePermissions
import com.github.skgmn.rapidstartactivity.permissionStatus
import com.github.skgmn.rapidstartactivity.sample.camera.databinding.FragmentCameraPermissionOverlayBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CameraPermissionOverlayFragment : Fragment() {
    private var binding: FragmentCameraPermissionOverlayBinding? = null

    private val viewModel: CameraPermissionOverlayViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.acquirePermissionSignal.observe(this) {
            lifecycleScope.launch {
                val request = PermissionRequest(
                    CameraFragmentContainer.REQUIRED_PERMISSIONS,
                    DefaultRationaleDialogs.detailed(R.raw.rationales)
                )
                acquirePermissions(request)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = DataBindingUtil.inflate<FragmentCameraPermissionOverlayBinding>(
            inflater, R.layout.fragment_camera_permission_overlay, container, false
        )

        binding.viewModel = viewModel
        this.binding = binding

        viewLifecycleOwner.lifecycleScope.launch {
            permissionStatus(CameraFragmentContainer.REQUIRED_PERMISSIONS).collect {
                binding.permissionsStatus = it
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
