package com.github.skgmn.startactivityx.camerasample

import android.Manifest
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.github.skgmn.startactivityx.AcquirePermissionsResult
import com.github.skgmn.startactivityx.PermissionRequest
import com.github.skgmn.startactivityx.acquirePermissions
import com.github.skgmn.startactivityx.camerasample.databinding.ActivityMainBinding
import com.github.skgmn.startactivityx.permissionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MainActivity : AppCompatActivity() {
    private var startingCamera = false

    private var imageCapture: ImageCapture? = null

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.requestPermissionButton.setOnClickListener {
            lifecycleScope.launch {
                startCamera(true)
            }
        }

        binding.takePhotoButton.setOnClickListener {
            lifecycleScope.launch {
                takePhoto()
            }
        }

        lifecycleScope.launch {
            startCamera(false)
            permissionStatus(Manifest.permission.CAMERA).collect {
                binding.permissionsStatus = it
            }
        }
    }

    private suspend fun startCamera(fromUser: Boolean) {
        if (startingCamera) {
            return
        }
        startingCamera = true

        try {
            val permissionRequest = PermissionRequest(listOf(Manifest.permission.CAMERA), fromUser)
            if (acquirePermissions(permissionRequest).denied) {
                return
            }

            val cameraProvider = getCameraProvider()

            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build().also {
                imageCapture = it
            }

            preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                        this@MainActivity, cameraSelector, preview, imageCapture)
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

    private suspend fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val permissionResult = acquirePermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionResult.denied) {
            Toast.makeText(this, R.string.no_permissions, Toast.LENGTH_SHORT).show()
            return
        }

        if (permissionResult == AcquirePermissionsResult.JUST_GRANTED) {
            // I don't know why but calling ImageCapture.takePicture() right after permissions are
            // granted causes a crash.
            // So just delay some times to avoid it.
            delay(500)
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            ContentValues()
        )
                .build()

        imageCapture.takePicture(outputOptions)
        Toast.makeText(this, R.string.photo_saved, Toast.LENGTH_SHORT).show()
    }

    private suspend fun ImageCapture.takePicture(
            outputOptions: ImageCapture.OutputFileOptions
    ): ImageCapture.OutputFileResults {
        return suspendCoroutine { cont ->
            val callback = object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    cont.resume(outputFileResults)
                }

                override fun onError(exception: ImageCaptureException) {
                    cont.resumeWithException(exception)
                }
            }
            takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this@MainActivity),
                callback
            )
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}