package com.catedra.apporgartistas.utils

import android.util.Log
import com.catedra.apporgartistas.data.models.Show
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ShowRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun getShowsRef(agrupacionId: String) =
        db.collection("agrupaciones").document(agrupacionId).collection("shows")

    fun escucharShowsActivos(
        agrupacionId: String,
        onSuccess: (List<Show>) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        Log.d("SHOWS_DEBUG", "Cargando shows de agrupacionId=$agrupacionId")

        return getShowsRef(agrupacionId)
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

    fun escucharCantidadShowsActivos(
        agrupacionId: String,
        onSuccess: (Int) -> Unit,
        onFailure: (Exception) -> Unit
    ): ListenerRegistration {
        return getShowsRef(agrupacionId)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onFailure(error)
                    return@addSnapshotListener
                }

                onSuccess(snapshot?.size() ?: 0)
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
        borrarShows(
            agrupacionId = agrupacionId,
            showIds = listOf(showId),
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    fun borrarShows(
        agrupacionId: String,
        showIds: List<String>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val idsValidos = showIds.distinct().filter { it.isNotBlank() }

        if (idsValidos.isEmpty()) {
            onSuccess()
            return
        }

        val tareas = idsValidos.map { showId ->
            getShowsRef(agrupacionId).document(showId)
                .update("active", false)
        }

        Tasks.whenAll(tareas)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure() }
    }
}
