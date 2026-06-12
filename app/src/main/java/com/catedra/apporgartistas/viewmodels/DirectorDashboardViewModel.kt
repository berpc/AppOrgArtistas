package com.catedra.apporgartistas.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.ui.models.AgrupacionDashboardItem
import com.catedra.apporgartistas.utils.AgrupacionRepository
import com.catedra.apporgartistas.utils.ShowRepository
import com.google.firebase.firestore.ListenerRegistration

class DirectorDashboardViewModel : ViewModel() {

    private val agrupacionRepository = AgrupacionRepository()
    private val showRepository = ShowRepository()
    private var agrupacionesListener: ListenerRegistration? = null
    private val showsListeners = mutableMapOf<String, ListenerRegistration>()
    private val cantidadesShows = mutableMapOf<String, Int>()
    private var agrupacionesActuales: List<Agrupacion> = emptyList()

    private val _agrupaciones = MutableLiveData<List<AgrupacionDashboardItem>>(emptyList())
    val agrupaciones: LiveData<List<AgrupacionDashboardItem>> get() = _agrupaciones

    private val _mensaje = MutableLiveData<Int?>()
    val mensaje: LiveData<Int?> get() = _mensaje

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    fun cargarAgrupaciones() {
        _isLoading.value = true
        agrupacionesListener?.remove()

        agrupacionesListener = agrupacionRepository.listenToAgrupacionesActivasDelDirector(
            onSuccess = { lista ->
                agrupacionesActuales = lista
                sincronizarListenersDeShows(lista)
                publicarAgrupaciones()
                _isLoading.value = false
            },
            onFailure = {
                mostrarMensaje(R.string.message_director_error_cargar)
                _isLoading.value = false
            }
        )

        if (agrupacionesListener == null) {
            agrupacionesActuales = emptyList()
            limpiarListenersDeShows()
            publicarAgrupaciones()
            mostrarMensaje(R.string.message_director_usuario_no_autenticado)
            _isLoading.value = false
        }
    }

    fun crearAgrupacion(nombre: String) {
        _isLoading.value = true

        agrupacionRepository.createAgrupacionDelDirector(
            nombre = nombre,
            onSuccess = {
                mostrarMensaje(R.string.message_director_agrupacion_creada)
                _isLoading.value = false
            },
            onFailure = {
                mostrarMensaje(R.string.message_director_error_crear)
                _isLoading.value = false
            }
        )
    }

    fun borrarAgrupacion(id: String) {
        borrarAgrupaciones(listOf(id))
    }

    fun borrarAgrupaciones(ids: List<String>) {
        _isLoading.value = true

        agrupacionRepository.softDeleteAgrupaciones(
            ids = ids,
            onSuccess = {
                mostrarMensaje(R.string.message_director_agrupaciones_papelera)
                _isLoading.value = false
            },
            onFailure = {
                mostrarMensaje(R.string.message_director_error_eliminar)
                _isLoading.value = false
            }
        )
    }

    fun editarAgrupacion(id: String, nuevoNombre: String) {
        if (id.isBlank() || nuevoNombre.isBlank()) {
            mostrarMensaje(R.string.message_director_nombre_vacio)
            return
        }

        _isLoading.value = true

        agrupacionRepository.updateAgrupacionNombre(id, nuevoNombre) { exito ->
            mostrarMensaje(if (exito) {
                R.string.message_director_agrupacion_actualizada
            } else {
                R.string.message_director_error_actualizar
            })
            _isLoading.value = false
        }
    }

    fun limpiarMensaje() {
        _mensaje.value = null
    }

    private fun mostrarMensaje(@StringRes mensajeResId: Int) {
        _mensaje.value = mensajeResId
    }

    private fun sincronizarListenersDeShows(agrupaciones: List<Agrupacion>) {
        val idsActuales = agrupaciones.map { it.id }.toSet()
        val idsEliminados = showsListeners.keys - idsActuales

        idsEliminados.forEach { agrupacionId ->
            showsListeners.remove(agrupacionId)?.remove()
            cantidadesShows.remove(agrupacionId)
        }

        agrupaciones.forEach { agrupacion ->
            if (agrupacion.id.isBlank() || showsListeners.containsKey(agrupacion.id)) {
                return@forEach
            }

            cantidadesShows[agrupacion.id] = cantidadesShows[agrupacion.id] ?: 0
            showsListeners[agrupacion.id] = showRepository.escucharCantidadShowsActivos(
                agrupacionId = agrupacion.id,
                onSuccess = { cantidad ->
                    cantidadesShows[agrupacion.id] = cantidad
                    publicarAgrupaciones()
                },
                onFailure = {
                    mostrarMensaje(R.string.message_director_error_cargar_shows)
                }
            )
        }
    }

    private fun publicarAgrupaciones() {
        _agrupaciones.value = agrupacionesActuales.map { agrupacion ->
            AgrupacionDashboardItem(
                agrupacion = agrupacion,
                cantidadShows = cantidadesShows[agrupacion.id] ?: 0
            )
        }
    }

    private fun limpiarListenersDeShows() {
        showsListeners.values.forEach { listener ->
            listener.remove()
        }
        showsListeners.clear()
        cantidadesShows.clear()
    }

    override fun onCleared() {
        agrupacionesListener?.remove()
        limpiarListenersDeShows()
        super.onCleared()
    }
}
