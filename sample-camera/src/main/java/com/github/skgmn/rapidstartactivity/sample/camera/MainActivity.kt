package com.github.skgmn.rapidstartactivity.sample.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.github.skgmn.rapidstartactivity.DefaultRationaleDialogs
import com.github.skgmn.rapidstartactivity.PermissionRequest
import com.github.skgmn.rapidstartactivity.acquirePermissions
import com.github.skgmn.rapidstartactivity.hasPermissions
import com.github.skgmn.rapidstartactivity.sample.camera.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy(LazyThreadSafetyMode.NONE) {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.fragmentManager = supportFragmentManager

        lifecycleScope.launch {
            val permissionRequest = PermissionRequest(
                permissions = CameraFragmentContainer.REQUIRED_PERMISSIONS,
                rationaleDialog = DefaultRationaleDialogs.detailed(R.raw.rationales)
            )
            acquirePermissions(permissionRequest)
            hasPermissions(CameraFragmentContainer.REQUIRED_PERMISSIONS).collect {
                binding.permissionsGranted = it
            }
        }
    }
}
