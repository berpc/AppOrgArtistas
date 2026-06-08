package com.catedra.apporgartistas.utils

import com.catedra.apporgartistas.data.models.Agrupacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AgrupacionRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val agrupacionesRef = db.collection("agrupaciones")

    // Obtener el ID del director actual
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    // Escuchar las agrupaciones del director en tiempo real
    fun listenToAgrupaciones(onSuccess: (List<Agrupacion>) -> Unit, onFailure: (Exception) -> Unit): ListenerRegistration? {
        val userId = currentUserId ?: return null

        // Filtramos para que el director solo vea las agrupaciones que él creó
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

    // Crear una nueva agrupación
    fun createAgrupacion(nombre: String, onComplete: (Boolean) -> Unit) {
        val userId = currentUserId ?: return onComplete(false)
        val docRef = agrupacionesRef.document() // Genera un ID automático
        val nuevaAgrupacion = Agrupacion(id = docRef.id, nombre = nombre, directorId = userId)

        docRef.set(nuevaAgrupacion)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // Editar el nombre de una agrupación
    fun updateAgrupacionNombre(id: String, nuevoNombre: String, onComplete: (Boolean) -> Unit) {
        agrupacionesRef.document(id)
            .update("nombre", nuevoNombre)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    // Borrar una agrupación
    fun deleteAgrupacion(id: String, onComplete: (Boolean) -> Unit) {
        agrupacionesRef.document(id)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}