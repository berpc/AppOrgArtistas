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

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun cargarAgrupaciones() {
        _isLoading.value = true

        agrupacionRepository.listenToAgrupacionesActivasDelDirector(
            onSuccess = { lista ->
                _agrupaciones.value = lista
                _isLoading.value = false
            },
            onFailure = { error ->
                _mensaje.value = "Error al cargar: ${error.message}"
                _isLoading.value = false
            }
        )
    }

    fun crearAgrupacion(nombre: String) {
        _isLoading.value = true

        agrupacionRepository.createAgrupacionDelDirector(
            nombre = nombre,
            onSuccess = {
                _mensaje.value = "Agrupacion creada"
                _isLoading.value = false
            },
            onFailure = {
                _mensaje.value = "Error al crear"
                _isLoading.value = false
            }
        )
    }

    fun borrarAgrupacion(id: String) {
        _isLoading.value = true

        agrupacionRepository.softDeleteAgrupacion(
            id = id,
            onSuccess = {
                _mensaje.value = "Agrupacion enviada a la papelera"
                _isLoading.value = false
            },
            onFailure = {
                _mensaje.value = "Error al eliminar"
                _isLoading.value = false
            }
        )
    }
}
