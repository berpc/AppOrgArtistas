package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.utils.InstrumentoRepository
import com.catedra.apporgartistas.utils.ShowDetailRepository
import kotlinx.coroutines.launch
import java.util.UUID

class ShowDetailViewModel : ViewModel() {

    private val showDetailRepository = ShowDetailRepository()
    private val instrumentoRepository = InstrumentoRepository()

    private var agrupacionId: String = ""
    private var showId: String = ""

    private val _showActual = MutableLiveData<Show?>()
    val showActual: LiveData<Show?> = _showActual

    private val _canciones = MutableLiveData<List<SetlistMasterItem>>(emptyList())
    val canciones: LiveData<List<SetlistMasterItem>> = _canciones

    private val _instrumentos = MutableLiveData<List<Instrumento>>(emptyList())
    val instrumentos: LiveData<List<Instrumento>> = _instrumentos

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _mensaje = MutableLiveData<String>()
    val mensaje: LiveData<String> = _mensaje

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun cargarDatos(agrupacionId: String, showId: String) {
        this.agrupacionId = agrupacionId
        this.showId = showId

        viewModelScope.launch {
            _isLoading.value = true

            try {
                cargarShowInterno()
                cargarInstrumentosInterno()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun crearCancionEnSetlist(nombre: String) {
        val nuevoItem = SetlistMasterItem(
            id = UUID.randomUUID().toString(),
            nombre = nombre
        )

        val nuevaLista = (_canciones.value ?: emptyList()) + nuevoItem
        guardarSetlistMaster(nuevaLista)
    }

    fun editarCancionEnSetlist(
        setlistItem: SetlistMasterItem,
        nuevoNombre: String
    ) {
        val nuevaLista = (_canciones.value ?: emptyList()).map { item ->
            if (item.id == setlistItem.id) {
                item.copy(nombre = nuevoNombre)
            } else {
                item
            }
        }

        guardarSetlistMaster(nuevaLista)
    }

    fun borrarCancionDeSetlist(setlistItem: SetlistMasterItem) {
        val nuevaLista = (_canciones.value ?: emptyList()).filter { item ->
            item.id != setlistItem.id
        }

        guardarSetlistMaster(nuevaLista)
    }

    fun guardarNuevoOrdenSetlist(nuevoOrden: List<SetlistMasterItem>) {
        val ordenAnterior = _canciones.value ?: emptyList()

        viewModelScope.launch {
            _isLoading.value = true

            try {
                showDetailRepository.actualizarSetlistMaster(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    nuevoSetlistMaster = nuevoOrden
                )

                actualizarCanciones(nuevoOrden)
                _mensaje.value = "Orden actualizado"
            } catch (e: Exception) {
                _error.value = "Error al actualizar orden: ${e.message}"
                _canciones.value = ordenAnterior
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun agregarInstrumento(nombre: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val instrumento = Instrumento(
                    nombre = nombre,
                    activo = true
                )

                instrumentoRepository.agregarInstrumento(
                    showId = showId,
                    agrupacionId = agrupacionId,
                    instrumento = instrumento
                )

                _mensaje.value = "Instrumento agregado"
                cargarInstrumentosInterno()
            } catch (e: Exception) {
                _error.value = "Error al agregar instrumento: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun cargarShowInterno() {
        try {
            val show = showDetailRepository.obtenerShow(
                agrupacionId = agrupacionId,
                showId = showId
            )

            _showActual.value = show
            actualizarCanciones(show?.setlistMaster ?: emptyList())
        } catch (e: Exception) {
            _error.value = "Error al cargar show"
        }
    }

    private suspend fun cargarInstrumentosInterno() {
        try {
            val instrumentos = instrumentoRepository.obtenerInstrumentos(
                showId = showId,
                agrupacionId = agrupacionId
            )

            _instrumentos.value = instrumentos
        } catch (e: Exception) {
            _error.value = "Error al cargar instrumentos: ${e.message}"
        }
    }

    private fun guardarSetlistMaster(nuevaLista: List<SetlistMasterItem>) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                showDetailRepository.actualizarSetlistMaster(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    nuevoSetlistMaster = nuevaLista
                )

                actualizarCanciones(nuevaLista)
            } catch (e: Exception) {
                _error.value = "Error al guardar setlist: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun actualizarCanciones(nuevaLista: List<SetlistMasterItem>) {
        _canciones.value = nuevaLista
        _showActual.value = _showActual.value?.copy(setlistMaster = nuevaLista)
    }
}
