package com.catedra.apporgartistas.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.services.AuthService
import com.catedra.apporgartistas.utils.AgrupacionRepository
import com.google.firebase.firestore.ListenerRegistration

class DashboardHostViewModel(
    private val agrupacionRepository: AgrupacionRepository = AgrupacionRepository(),
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private var agrupacionesListener: ListenerRegistration? = null

    private val _agrupaciones = MutableLiveData<List<Agrupacion>>(emptyList())
    val agrupaciones: LiveData<List<Agrupacion>> get() = _agrupaciones

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> get() = _error

    fun cargarAgrupacionesDirector() {
        _isLoading.value = true
        agrupacionesListener?.remove()

        val listener = agrupacionRepository.listenToAgrupacionesActivasDelDirector(
            onSuccess = { lista ->
                _agrupaciones.value = lista
                _isLoading.value = false
            },
            onFailure = {
                mostrarError(R.string.message_director_error_cargar)
                _isLoading.value = false
            }
        )

        agrupacionesListener = listener

        if (listener == null) {
            _agrupaciones.value = emptyList()
            mostrarError(R.string.message_director_usuario_no_autenticado)
            _isLoading.value = false
        }
    }

    fun cerrarSesion() {
        authService.logout()
    }

    fun limpiarError() {
        _error.value = null
    }

    private fun mostrarError(@StringRes mensajeResId: Int) {
        _error.value = mensajeResId
    }

    override fun onCleared() {
        agrupacionesListener?.remove()
        super.onCleared()
    }
}
