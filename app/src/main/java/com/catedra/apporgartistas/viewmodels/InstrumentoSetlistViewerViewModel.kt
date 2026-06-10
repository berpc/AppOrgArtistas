package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.utils.InstrumentoRepository
import com.catedra.apporgartistas.utils.ShowDetailRepository
import kotlinx.coroutines.launch

class InstrumentoSetlistViewerViewModel : ViewModel() {

    private val showDetailRepository = ShowDetailRepository()
    private val instrumentoRepository = InstrumentoRepository()

    private val _show = MutableLiveData<Show?>()
    val show: LiveData<Show?> = _show

    private val _instrumento = MutableLiveData<Instrumento?>()
    val instrumento: LiveData<Instrumento?> = _instrumento

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun cargarDatos(
        agrupacionId: String,
        showId: String,
        instrumentoId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val showActual = showDetailRepository.obtenerShow(
                    agrupacionId = agrupacionId,
                    showId = showId
                )

                _show.value = showActual

                if (showActual == null) {
                    _isLoading.value = false
                    return@launch
                }

                val instrumentoActual = instrumentoRepository.obtenerInstrumento(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    instrumentoId = instrumentoId
                )

                _instrumento.value = instrumentoActual
            } catch (e: Exception) {
                _error.value = "Error al cargar show: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
