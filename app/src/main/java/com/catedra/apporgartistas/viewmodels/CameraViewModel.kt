package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CameraViewModel : ViewModel() {
    val error = MutableLiveData<String?>()
}