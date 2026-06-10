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

    fun cargarShows(agrupacionId: String) {
        showRepository.escucharShowsActivos(
            agrupacionId = agrupacionId,
            onSuccess = { lista ->
                _shows.value = lista
            },
            onFailure = { error ->
                _mensaje.value = "Error al cargar shows: ${error.message}"
            }
        )
    }

    fun crearShow(agrupacionId: String, nombre: String, fecha: String?) {
        showRepository.crearShow(
            agrupacionId = agrupacionId,
            nombre = nombre,
            fecha = fecha,
            onSuccess = { _mensaje.value = "Show creado" },
            onFailure = { _mensaje.value = "Error al crear show" }
        )
    }

    fun editarShow(
        agrupacionId: String,
        showId: String,
        nuevoNombre: String,
        nuevaFecha: String?
    ) {
        showRepository.editarShow(
            agrupacionId = agrupacionId,
            showId = showId,
            nuevoNombre = nuevoNombre,
            nuevaFecha = nuevaFecha,
            onSuccess = { _mensaje.value = "Show actualizado" },
            onFailure = { _mensaje.value = "Error al actualizar" }
        )
    }

    fun borrarShow(agrupacionId: String, showId: String) {
        showRepository.borrarShow(
            agrupacionId = agrupacionId,
            showId = showId,
            onSuccess = { _mensaje.value = "show enviado a la papelera" },
            onFailure = { _mensaje.value = "Error al eliminar" }
        )
    }
}
