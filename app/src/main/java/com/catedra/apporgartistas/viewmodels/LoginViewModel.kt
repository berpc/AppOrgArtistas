package com.catedra.apporgartistas.viewmodels


import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.services.AuthService

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
class LoginViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> get() = _loginState

    fun loginUser(emailRaw: String, passwordRaw: String) {
        val email = emailRaw.trim()
        val password = passwordRaw.trim()

        if (email.isEmpty() || password.isEmpty()) {
            _loginState.value = LoginState.Error("Complete todos los campos")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.value = LoginState.Error("Ingrese un email válido")
            return
        }

        _loginState.value = LoginState.Loading

        authService.login(
            email = email,
            password = password,
            onSuccess = {
                _loginState.value = LoginState.Success
            },
            onError = { mensaje ->
                _loginState.value = LoginState.Error(mensaje)
            }
        )
    }

    fun logout() {
        authService.logout()
        _loginState.value = LoginState.Idle
    }

    fun obtenerYGuardarTokenFcm(
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        authService.obtenerYGuardarTokenFcm(
            onComplete = onComplete,
            onError = onError
        )
    }
}



