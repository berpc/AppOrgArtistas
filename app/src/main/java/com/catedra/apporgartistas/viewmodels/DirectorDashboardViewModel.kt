package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.utils.AgrupacionRepository

class DirectorDashboardViewModel : ViewModel() {

    private val agrupacionRepository = AgrupacionRepository()

    private val _agrupaciones = MutableLiveData<List<Agrupacion>>()
    val agrupaciones: LiveData<List<Agrupacion>> get() = _agrupaciones

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> get() = _mensaje

    fun cargarAgrupaciones() {
        agrupacionRepository.listenToAgrupacionesActivasDelDirector(
            onSuccess = { lista ->
                _agrupaciones.value = lista
            },
            onFailure = { error ->
                _mensaje.value = "Error al cargar: ${error.message}"
            }
        )
    }

    fun crearAgrupacion(nombre: String) {
        agrupacionRepository.createAgrupacionDelDirector(
            nombre = nombre,
            onSuccess = { _mensaje.value = "Agrupacion creada" },
            onFailure = { _mensaje.value = "Error al crear" }
        )
    }

    fun borrarAgrupacion(id: String) {
        agrupacionRepository.softDeleteAgrupacion(
            id = id,
            onSuccess = { _mensaje.value = "Agrupacion enviada a la papelera" },
            onFailure = { _mensaje.value = "Error al eliminar" }
        )
    }
}
