package com.catedra.apporgartistas.utils

import android.util.Log
import com.catedra.apporgartistas.data.models.Show
import com.google.firebase.firestore.FirebaseFirestore

class ShowRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun getShowsRef(agrupacionId: String) =
        db.collection("agrupaciones").document(agrupacionId).collection("shows")

    fun escucharShowsActivos(
        agrupacionId: String,
        onSuccess: (List<Show>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d("SHOWS_DEBUG", "Cargando shows de agrupacionId=$agrupacionId")

        getShowsRef(agrupacionId)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SHOWS_DEBUG", "Error al cargar shows", error)
                    onFailure(error)
                    return@addSnapshotListener
                }

                Log.d("SHOWS_DEBUG", "Snapshot existe: ${snapshot != null}")
                Log.d("SHOWS_DEBUG", "Cantidad docs: ${snapshot?.documents?.size}")

                snapshot?.documents?.forEach { document ->
                    Log.d("SHOWS_DEBUG", "Doc id=${document.id}, data=${document.data}")
                }

                val lista = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Show::class.java)
                } ?: emptyList()

                Log.d("SHOWS_DEBUG", "Lista convertida size=${lista.size}")

                onSuccess(lista)
            }
    }

    fun crearShow(
        agrupacionId: String,
        nombre: String,
        fecha: String?,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val docRef = getShowsRef(agrupacionId).document()
        val nuevoShow = Show(id = docRef.id, nombre = nombre, fecha = fecha)

        docRef.set(nuevoShow)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }

    fun editarShow(
        agrupacionId: String,
        showId: String,
        nuevoNombre: String,
        nuevaFecha: String?,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        getShowsRef(agrupacionId).document(showId)
            .update(mapOf("nombre" to nuevoNombre, "fecha" to nuevaFecha))
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }

    fun borrarShow(
        agrupacionId: String,
        showId: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        getShowsRef(agrupacionId).document(showId)
            .update("active", false)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
}
