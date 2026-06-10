package com.catedra.apporgartistas.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.data.models.SetlistInstrumentoSuscripto
import com.catedra.apporgartistas.data.repositories.ResultadoSuscripcionSetlist
import com.catedra.apporgartistas.data.repositories.SetlistSubscriptionRepository
import com.catedra.apporgartistas.services.AuthService
import com.catedra.apporgartistas.services.NotificationService

class SetlistDashboardViewModel : ViewModel() {

    private val authService = AuthService()
    private val notificationService = NotificationService()
    private val setlistSubscriptionRepository = SetlistSubscriptionRepository()

    private val _setlists = MutableLiveData<List<Setlist>>()
    private val _setlistsInstrumentoSuscriptos = MutableLiveData<List<SetlistInstrumentoSuscripto>>()
    val setlistsInstrumentoSuscriptos: LiveData<List<SetlistInstrumentoSuscripto>> =
        _setlistsInstrumentoSuscriptos
    val setlists: LiveData<List<Setlist>> = _setlists

    private val _suscripcionExitosa = MutableLiveData<Boolean>()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    val suscripcionExitosa: LiveData<Boolean> = _suscripcionExitosa

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun unirseASetlistConCodigo(codigo: String) {
        val userId = getCurrentUserId()
        val codigoNormalizado = codigo.trim().uppercase()

        if (codigoNormalizado.isBlank()) {
            _error.postValue("Ingres\u00e1 un c\u00f3digo.")
            return
        }

        if (userId.isBlank()) {
            _error.postValue("Usuario no autenticado.")
            return
        }

        _isLoading.value = true

        setlistSubscriptionRepository.unirseASetlistConCodigo(
            codigo = codigoNormalizado,
            userId = userId,
            onResult = ::manejarResultadoSuscripcion
        )
    }

    fun cargarSetlists() {
        val userId = getCurrentUserId()
        _isLoading.value = true

        cargarSetlistsViejos(userId)
        cargarSetlistsInstrumentoSuscriptos(userId)
    }

    fun ocultarSetlist(setlistId: String) {
        val userId = getCurrentUserId()

        setlistSubscriptionRepository.ocultarSetlist(
            userId = userId,
            setlistId = setlistId,
            onSuccess = { cargarSetlists() },
            onError = {}
        )
    }

    private fun getCurrentUserId(): String {
        return authService.getCurrentUserIdOrAnonymous()
    }

    private fun cargarSetlistsViejos(userId: String) {
        setlistSubscriptionRepository.cargarSetlistsViejos(
            userId = userId,
            onSuccess = { listaCombinada ->
                _setlists.postValue(listaCombinada)
                _isLoading.postValue(false)
            },
            onError = {
                _error.postValue("Error al cargar tu repertorio.")
                _isLoading.postValue(false)
            }
        )
    }

    private fun cargarSetlistsInstrumentoSuscriptos(userId: String) {
        setlistSubscriptionRepository.cargarSetlistsInstrumentoSuscriptos(
            userId = userId,
            onSuccess = { lista ->
                _setlistsInstrumentoSuscriptos.postValue(lista)
            },
            onError = {
                _setlistsInstrumentoSuscriptos.postValue(emptyList())
            }
        )
    }

    private fun manejarResultadoSuscripcion(resultado: ResultadoSuscripcionSetlist) {
        when (resultado) {
            is ResultadoSuscripcionSetlist.Exitosa -> {
                resultado.notificacionDirector?.let { notificacion ->
                    notificationService.notificarNuevoMusicoEnSetlist(
                        tokenDirector = notificacion.tokenDirector,
                        nombreInvitado = "Un compa\u00f1ero",
                        nombreSetlist = notificacion.nombreSetlist
                    )
                }

                _suscripcionExitosa.postValue(true)
            }

            ResultadoSuscripcionSetlist.SetlistPropio -> {
                _error.postValue("Este setlist ya es tuyo, no necesitas unirte.")
            }

            ResultadoSuscripcionSetlist.YaSuscripto -> {
                _error.postValue("Ya est\u00e1s suscrito a este setlist.")
            }

            ResultadoSuscripcionSetlist.CodigoNoEncontrado -> {
                _error.postValue("C\u00f3digo inv\u00e1lido o setlist no encontrado.")
            }

            ResultadoSuscripcionSetlist.CodigoInactivo -> {
                _error.postValue("Este c\u00f3digo ya no est\u00e1 activo.")
            }

            ResultadoSuscripcionSetlist.CodigoIncompleto -> {
                _error.postValue("El c\u00f3digo est\u00e1 incompleto.")
            }

            ResultadoSuscripcionSetlist.ErrorBusqueda -> {
                _error.postValue("Error al buscar el c\u00f3digo.")
            }

            ResultadoSuscripcionSetlist.ErrorSuscripcion -> {
                _error.postValue("Error al suscribirse al setlist.")
            }
        }

        _isLoading.postValue(false)
    }
}
