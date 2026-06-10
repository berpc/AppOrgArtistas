package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.services.AuthService

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    object Success : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Idle)
    val registerState: LiveData<RegisterState> = _registerState

    fun register(nombre: String, email: String, password: String) {
        _registerState.value = RegisterState.Loading

        authService.register(
            email = email,
            password = password,
            nombre = nombre,
            onSuccess = {
                _registerState.value = RegisterState.Success
            },
            onError = { message ->
                _registerState.value = RegisterState.Error(message)
            }
        )
    }
}
