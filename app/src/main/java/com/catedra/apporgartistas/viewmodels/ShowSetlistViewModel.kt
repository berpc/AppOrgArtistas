package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.ShowSetlistSuscripto
import com.catedra.apporgartistas.data.repositories.SetlistSubscriptionRepository
import com.catedra.apporgartistas.services.AuthService

class ShowSetlistViewModel : ViewModel() {

    private val authService = AuthService()
    private val setlistSubscriptionRepository = SetlistSubscriptionRepository()

    private val _showSetlists = MutableLiveData<List<ShowSetlistSuscripto>>()
    val showSetlists: LiveData<List<ShowSetlistSuscripto>> = _showSetlists

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun cargarShowSetlists() {
        val userId = authService.getCurrentUserId().orEmpty()

        if (userId.isBlank()) {
            _error.value = "Usuario no autenticado"
            return
        }

        _isLoading.value = true

        setlistSubscriptionRepository.cargarShowSetlistsSuscriptos(
            userId = userId,
            onSuccess = { lista ->
                _showSetlists.postValue(lista)
                _isLoading.postValue(false)
            },
            onError = {
                _error.postValue("Error al cargar Show Setlists")
                _isLoading.postValue(false)
            }
        )
    }
}
