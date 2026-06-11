package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.utils.ShowRepository

class ShowsDashboardViewModel : ViewModel() {

    private val showRepository = ShowRepository()

    private val _shows = MutableLiveData<List<Show>>()
    val shows: LiveData<List<Show>> get() = _shows

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> get() = _mensaje

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun cargarShows(agrupacionId: String) {
        _isLoading.value = true

        showRepository.escucharShowsActivos(
            agrupacionId = agrupacionId,
            onSuccess = { lista ->
                _shows.value = lista
                _isLoading.value = false
            },
            onFailure = { error ->
                _mensaje.value = "Error al cargar shows: ${error.message}"
                _isLoading.value = false
            }
        )
    }

    fun crearShow(agrupacionId: String, nombre: String, fecha: String?) {
        _isLoading.value = true

        showRepository.crearShow(
            agrupacionId = agrupacionId,
            nombre = nombre,
            fecha = fecha,
            onSuccess = {
                _mensaje.value = "Show creado"
                _isLoading.value = false
            },
            onFailure = {
                _mensaje.value = "Error al crear show"
                _isLoading.value = false
            }
        )
    }

    fun editarShow(
        agrupacionId: String,
        showId: String,
        nuevoNombre: String,
        nuevaFecha: String?
    ) {
        _isLoading.value = true

        showRepository.editarShow(
            agrupacionId = agrupacionId,
            showId = showId,
            nuevoNombre = nuevoNombre,
            nuevaFecha = nuevaFecha,
            onSuccess = {
                _mensaje.value = "Show actualizado"
                _isLoading.value = false
            },
            onFailure = {
                _mensaje.value = "Error al actualizar"
                _isLoading.value = false
            }
        )
    }

    fun borrarShow(agrupacionId: String, showId: String) {
        _isLoading.value = true

        showRepository.borrarShow(
            agrupacionId = agrupacionId,
            showId = showId,
            onSuccess = {
                _mensaje.value = "show enviado a la papelera"
                _isLoading.value = false
            },
            onFailure = {
                _mensaje.value = "Error al eliminar"
                _isLoading.value = false
            }
        )
    }
}
