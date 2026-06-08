package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.utils.AgrupacionRepository
import com.google.firebase.firestore.ListenerRegistration

class DirectorViewModel : ViewModel() {

    private val repository = AgrupacionRepository()
    private var snapshotListener: ListenerRegistration? = null

    private val _agrupaciones = MutableLiveData<List<Agrupacion>>()
    val agrupaciones: LiveData<List<Agrupacion>> get() = _agrupaciones

    private val _operacionExitosa = MutableLiveData<Boolean>()
    val operacionExitosa: LiveData<Boolean> get() = _operacionExitosa

    init {
        cargarAgrupaciones()
    }

    private fun cargarAgrupaciones() {
        snapshotListener = repository.listenToAgrupaciones(
            onSuccess = { lista ->
                _agrupaciones.value = lista
            },
            onFailure = {
                // Aquí se puede manejar el error (por ejemplo, registrarlo en logs)
            }
        )
    }

    fun agregarAgrupacion(nombre: String) {
        if (nombre.isNotBlank()) {
            repository.createAgrupacion(nombre) { exito ->
                _operacionExitosa.value = exito
            }
        }
    }

    fun editarAgrupacion(id: String, nuevoNombre: String) {
        if (nuevoNombre.isNotBlank()) {
            repository.updateAgrupacionNombre(id, nuevoNombre) { exito ->
                _operacionExitosa.value = exito
            }
        }
    }

    fun eliminarAgrupacion(id: String) {
        repository.deleteAgrupacion(id) { exito ->
            _operacionExitosa.value = exito
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Es fundamental remover el listener cuando el ViewModel se destruye para evitar fugas de memoria
        snapshotListener?.remove()
    }
}