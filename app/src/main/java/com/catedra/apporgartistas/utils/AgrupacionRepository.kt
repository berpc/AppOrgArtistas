package com.catedra.apporgartistas.utils

import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.services.AuthService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AgrupacionRepository(
    private val authService: AuthService = AuthService()
) {

    private val db = FirebaseFirestore.getInstance()
    private val agrupacionesRef = db.collection("agrupaciones")

    private val currentUserId: String?
        get() = authService.getCurrentUserId()

    fun listenToAgrupaciones(
        onSuccess: (List<Agrupacion>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration? {
        val userId = currentUserId ?: return null

        return agrupacionesRef
            .whereEqualTo("directorId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Agrupacion::class.java)
                } ?: emptyList()

                onSuccess(lista)
            }
    }

    fun listenToAgrupacionesActivasDelDirector(
        onSuccess: (List<Agrupacion>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = currentUserId ?: return

        agrupacionesRef
            .whereEqualTo("directorId", userId)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Agrupacion::class.java)
                } ?: emptyList()

                onSuccess(lista)
            }
    }

    fun createAgrupacion(nombre: String, onComplete: (Boolean) -> Unit) {
        val userId = currentUserId ?: return onComplete(false)
        val docRef = agrupacionesRef.document()
        val nuevaAgrupacion = Agrupacion(id = docRef.id, nombre = nombre, directorId = userId)

        docRef.set(nuevaAgrupacion)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun createAgrupacionDelDirector(
        nombre: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val userId = currentUserId ?: return
        val docRef = agrupacionesRef.document()
        val nuevaAgrupacion = Agrupacion(id = docRef.id, nombre = nombre, directorId = userId)

        docRef.set(nuevaAgrupacion)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }

    fun updateAgrupacionNombre(id: String, nuevoNombre: String, onComplete: (Boolean) -> Unit) {
        agrupacionesRef.document(id)
            .update("nombre", nuevoNombre)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteAgrupacion(id: String, onComplete: (Boolean) -> Unit) {
        agrupacionesRef.document(id)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun softDeleteAgrupacion(
        id: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        agrupacionesRef.document(id)
            .update("active", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
}
