package com.catedra.apporgartistas.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class AuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUserIdOrAnonymous(): String {
        return auth.currentUser?.uid ?: "usuario_anonimo"
    }

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error desconocido al iniciar sesión")
            }
    }

    fun register(
        email: String,
        password: String,
        nombre: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    val userProfile = hashMapOf(
                        "nombre" to nombre,
                        "email" to email
                    )
                    firestore.collection("usuarios")
                        .document(userId)
                        .set(userProfile, SetOptions.merge())
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onError(e.message ?: "Error al crear perfil de usuario") }
                }
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al registrar usuario")
            }
    }

    fun logout() {
        auth.signOut()
    }

    fun guardarTokenFcm(
        token: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val userId = getCurrentUserId()

        if (userId == null) {
            onError("No hay usuario logueado")
            return
        }

        val datosActualizados = hashMapOf(
            "fcmToken" to token
        )

        firestore.collection("usuarios")
            .document(userId)
            .set(datosActualizados, SetOptions.merge())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al guardar token FCM")
            }
    }

    fun obtenerYGuardarTokenFcm(
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onError(task.exception?.message ?: "Error al obtener token FCM")
                    onComplete()
                    return@addOnCompleteListener
                }

                guardarTokenFcm(
                    token = task.result,
                    onError = onError
                )
                onComplete()
            }
    }

    fun obtenerTokenFcmDeUsuario(
        userId: String,
        onSuccess: (String?) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("usuarios")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val token = document.getString("fcmToken")
                onSuccess(token)
            }
            .addOnFailureListener { exception ->
                onError(exception.message ?: "Error al obtener token FCM")
            }
    }
}
