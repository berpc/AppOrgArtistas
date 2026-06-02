package com.catedra.apporgartistas.viewmodels


import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}
class LoginViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _loginState.value = LoginState.Success
                } else {
                    val errorMsg = task.exception?.message ?: "Error desconocido al iniciar sesión"
                    _loginState.value = LoginState.Error(errorMsg)
                }
            }
    }
        fun logout() {
            auth.signOut()
            _loginState.value = LoginState.Idle
        }
    fun guardarTokenFcm(token: String) {
        val userId = auth.currentUser?.uid

        if (userId != null) {
            val datosActualizados = hashMapOf(
                "fcmToken" to token
            )

            // SetOptions.merge() actualiza el token sin borrar otros datos del usuario
            firestore.collection("usuarios").document(userId)
                .set(datosActualizados, SetOptions.merge())
                .addOnSuccessListener {
                    // Token guardado perfecto
                }
                .addOnFailureListener { e ->
                    // Manejo de error si falla la escritura
                }
        }
    }

    }



