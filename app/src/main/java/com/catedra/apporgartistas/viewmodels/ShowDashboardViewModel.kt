package com.catedra.apporgartistas.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.utils.ShowRepository
import com.google.firebase.firestore.ListenerRegistration

class ShowsDashboardViewModel : ViewModel() {

    private val showRepository = ShowRepository()
    private var showsListener: ListenerRegistration? = null

    private val _shows = MutableLiveData<List<Show>>()
    val shows: LiveData<List<Show>> get() = _shows

    private val _mensaje = MutableLiveData<Int?>()
    val mensaje: LiveData<Int?> get() = _mensaje

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun cargarShows(agrupacionId: String) {
        _isLoading.value = true
        showsListener?.remove()

        showsListener = showRepository.escucharShowsActivos(
            agrupacionId = agrupacionId,
            onSuccess = { lista ->
                _shows.value = lista
                _isLoading.value = false
            },
            onFailure = {
                mostrarMensaje(R.string.message_show_error_cargar)
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
                mostrarMensaje(R.string.message_show_creado)
                _isLoading.value = false
            },
            onFailure = {
                mostrarMensaje(R.string.message_show_error_crear)
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
                mostrarMensaje(R.string.message_show_actualizado)
                _isLoading.value = false
            },
            onFailure = {
                mostrarMensaje(R.string.message_show_error_actualizar)
                _isLoading.value = false
            }
        )
    }

    fun borrarShow(agrupacionId: String, showId: String) {
        borrarShows(agrupacionId, listOf(showId))
    }

    fun borrarShows(agrupacionId: String, showIds: List<String>) {
        _isLoading.value = true

        showRepository.borrarShows(
            agrupacionId = agrupacionId,
            showIds = showIds,
            onSuccess = {
                mostrarMensaje(R.string.message_show_papelera)
                _isLoading.value = false
            },
            onFailure = {
                mostrarMensaje(R.string.message_show_error_eliminar)
                _isLoading.value = false
            }
        )
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    private fun mostrarMensaje(@StringRes mensajeResId: Int) {
        _mensaje.value = mensajeResId
    }

    override fun onCleared() {
        showsListener?.remove()
        super.onCleared()
    }
}
