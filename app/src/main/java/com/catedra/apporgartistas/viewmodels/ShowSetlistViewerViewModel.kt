package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.utils.InstrumentoRepository
import com.catedra.apporgartistas.utils.ShowDetailRepository
import com.google.firebase.firestore.ListenerRegistration

class ShowSetlistViewerViewModel : ViewModel() {

    private val showDetailRepository = ShowDetailRepository()
    private val instrumentoRepository = InstrumentoRepository()

    private var showListener: ListenerRegistration? = null
    private var instrumentoListener: ListenerRegistration? = null

    private val _show = MutableLiveData<Show?>()
    val show: LiveData<Show?> = _show

    private val _instrumento = MutableLiveData<Instrumento?>()
    val instrumento: LiveData<Instrumento?> = _instrumento

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun observarDatos(
        agrupacionId: String,
        showId: String,
        instrumentoId: String
    ) {
        limpiarListeners()
        _isLoading.value = true

        showListener = showDetailRepository.observarShow(
            agrupacionId = agrupacionId,
            showId = showId,
            onChange = { show ->
                _show.value = show
                _isLoading.value = false
            },
            onError = {
                _error.value = "Error al escuchar cambios del show"
                _isLoading.value = false
            }
        )

        instrumentoListener = instrumentoRepository.observarInstrumento(
            agrupacionId = agrupacionId,
            showId = showId,
            instrumentoId = instrumentoId,
            onChange = { instrumento ->
                _instrumento.value = instrumento
                _isLoading.value = false
            },
            onError = {
                _error.value = "Error al escuchar cambios del instrumento"
                _isLoading.value = false
            }
        )
    }

    private fun limpiarListeners() {
        showListener?.remove()
        instrumentoListener?.remove()
        showListener = null
        instrumentoListener = null
    }

    override fun onCleared() {
        limpiarListeners()
        super.onCleared()
    }
}
