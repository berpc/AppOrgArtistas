package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Show
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
class ShowsDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _shows = MutableLiveData<List<Show>>()
    val shows: LiveData<List<Show>> get() = _shows

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> get() = _mensaje

    // Referencia dinámica a la subcolección
    private fun getShowsRef(agrupacionId: String) =
        db.collection("agrupaciones").document(agrupacionId).collection("shows")

    fun cargarShows(agrupacionId: String) {
        Log.d("SHOWS_DEBUG", "Cargando shows de agrupacionId=$agrupacionId")

        getShowsRef(agrupacionId)
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SHOWS_DEBUG", "Error al cargar shows", error)
                    _mensaje.value = "Error al cargar shows: ${error.message}"
                    return@addSnapshotListener
                }

                Log.d("SHOWS_DEBUG", "Snapshot existe: ${snapshot != null}")
                Log.d("SHOWS_DEBUG", "Cantidad docs: ${snapshot?.documents?.size}")

                snapshot?.documents?.forEach { doc ->
                    Log.d("SHOWS_DEBUG", "Doc id=${doc.id}, data=${doc.data}")
                }

                val lista = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(Show::class.java)
                } ?: emptyList()

                Log.d("SHOWS_DEBUG", "Lista convertida size=${lista.size}")

                _shows.value = lista
            }
    }
    fun crearShow(agrupacionId: String, nombre: String, fecha: String?) {
        val docRef = getShowsRef(agrupacionId).document()
        val nuevoShow = Show(id = docRef.id, nombre = nombre, fecha = fecha)

        docRef.set(nuevoShow)
            .addOnSuccessListener { _mensaje.value = "Show creado" }
            .addOnFailureListener { _mensaje.value = "Error al crear show" }
    }

    fun editarShow(agrupacionId: String, showId: String, nuevoNombre: String, nuevaFecha: String?) {
        getShowsRef(agrupacionId).document(showId)
            .update(mapOf("nombre" to nuevoNombre, "fecha" to nuevaFecha))
            .addOnSuccessListener { _mensaje.value = "Show actualizado" }
            .addOnFailureListener { _mensaje.value = "Error al actualizar" }
    }

    fun borrarShow(agrupacionId: String, showId: String) {
        getShowsRef(agrupacionId).document(showId)
            .update("active", false)
            .addOnSuccessListener {
                _mensaje.value = "show enviado a la papelera"
            }
            .addOnFailureListener {
                _mensaje.value = "Error al eliminar"
            }
    }
}