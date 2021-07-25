package com.github.skgmn.rapidstartactivity.sample.camera

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraPermissionOverlayViewModel : ViewModel() {
    private val _acquirePermissionsSignal = MutableLiveData<Any>()
    val acquirePermissionSignal: LiveData<Any>
        get() = _acquirePermissionsSignal

    fun acquirePermissions() {
        _acquirePermissionsSignal.value = Unit
    }
}