package com.github.skgmn.startactivityx.camerasample

import android.app.Application
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.github.skgmn.cameraxx.takePicture
import com.github.skgmn.viewmodelevent.publicEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val imageCaptureUseCaseFlow = MutableStateFlow(newImageCapture())

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    val previewUseCase = Preview.Builder().build()
    val imageCaptureUseCase =
        imageCaptureUseCaseFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val cameraPermissionsGranted = MutableStateFlow(false)

    val requestCameraPermissionsByUserEvent = publicEvent<Any>()
    val requestTakePhotoPermissionsEvent = publicEvent<Any>()
    val showTakenPhotoEvent = publicEvent<Uri>()

    fun requestCameraPermissions() {
        requestCameraPermissionsByUserEvent.post(Unit)
    }

    fun requestTakePhotoPermissions() {
        requestTakePhotoPermissionsEvent.post(Unit)
    }

    fun takePhoto() {
        viewModelScope.launch {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                getApplication<Application>().contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            ).build()

            val result = imageCaptureUseCaseFlow.value.takePicture(outputOptions)
            showTakenPhotoEvent.post(result.savedUri ?: Uri.EMPTY)
        }
    }

    fun replaceImageCapture() {
        imageCaptureUseCaseFlow.value = newImageCapture()
    }

    private fun newImageCapture() = ImageCapture.Builder().build()
}