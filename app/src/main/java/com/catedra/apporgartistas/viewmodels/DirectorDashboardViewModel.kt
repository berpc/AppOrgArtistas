package com.catedra.apporgartistas.viewmodels


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Agrupacion
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DirectorDashboardViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val agrupacionesRef = db.collection("agrupaciones")

    private val _agrupaciones = MutableLiveData<List<Agrupacion>>()
    val agrupaciones: LiveData<List<Agrupacion>> get() = _agrupaciones

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> get() = _mensaje

    fun cargarAgrupaciones() {
        val currentUserId = auth.currentUser?.uid ?: return

        agrupacionesRef
            .whereEqualTo("directorId", currentUserId)
            .whereEqualTo("active", true) // Filtramos las agrupaciones borradas
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _mensaje.value = "Error al cargar: ${error.message}"
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { it.toObject(Agrupacion::class.java) } ?: emptyList()
                _agrupaciones.value = lista
            }
    }

    fun crearAgrupacion(nombre: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val docRef = agrupacionesRef.document()
        val nuevaAgrupacion = Agrupacion(id = docRef.id, nombre = nombre, directorId = currentUserId)

        docRef.set(nuevaAgrupacion)
            .addOnSuccessListener { _mensaje.value = "Agrupación creada" }
            .addOnFailureListener { _mensaje.value = "Error al crear" }
    }

    fun borrarAgrupacion(id: String) {
        // Reemplazamos el delete() por un update()
        agrupacionesRef.document(id)
            .update("active", false)
            .addOnSuccessListener {
                _mensaje.value = "Agrupación enviada a la papelera"
            }
            .addOnFailureListener {
                _mensaje.value = "Error al eliminar"
            }
    }
}