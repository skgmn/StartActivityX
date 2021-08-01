package com.github.skgmn.rapidstartactivity.sample.camera

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.github.skgmn.rapidstartactivity.*
import com.github.skgmn.rapidstartactivity.sample.camera.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    private var startingCamera = false

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.requestPermissionButton.setOnClickListener {
            lifecycleScope.launch {
                startCamera()
            }
        }

        lifecycleScope.launch {
            startCamera()
            permissionStatus(REQUIRED_PERMISSIONS).collect {
                binding.permissionsStatus = it
            }
        }
    }

    private fun createPermissionRequest() = PermissionRequest(
            permissions = REQUIRED_PERMISSIONS,
            rationaleDialog = DefaultRationaleDialogs.detailed(R.raw.rationales)
    )

    private suspend fun startCamera() {
        if (startingCamera) {
            return
        }
        startingCamera = true

        try {
            if (!acquirePermissions(createPermissionRequest())) {
                startingCamera = false
                return
            }

            val cameraProvider = getCameraProvider()

            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this@MainActivity, cameraSelector, preview)
            } catch (e: Exception) {
                Log.e(TAG, "Use case binding failed", e)
            }
        } finally {
            startingCamera = false
        }
    }

    private suspend fun getCameraProvider(): ProcessCameraProvider {
        return suspendCoroutine { cont ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)
            cameraProviderFuture.addListener({
                cont.resume(cameraProviderFuture.get())
            }, ContextCompat.getMainExecutor(this@MainActivity))
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        private val REQUIRED_PERMISSIONS = listOf(
                android.Manifest.permission.CAMERA
        )
    }
}
