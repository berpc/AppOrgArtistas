package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun cargarAgrupacionesDirector() {
        _isLoading.value = true
        agrupacionesListener?.remove()

        val listener = agrupacionRepository.listenToAgrupacionesActivasDelDirector(
            onSuccess = { lista ->
                _agrupaciones.value = lista
                _isLoading.value = false
            },
            onFailure = { exception ->
                _error.value = "Error al cargar agrupaciones: ${exception.message}"
                _isLoading.value = false
            }
        )

        agrupacionesListener = listener

        if (listener == null) {
            _agrupaciones.value = emptyList()
            _error.value = "Usuario no autenticado"
            _isLoading.value = false
        }
    }

    fun cerrarSesion() {
        authService.logout()
    }

    override fun onCleared() {
        agrupacionesListener?.remove()
        super.onCleared()
    }
}
