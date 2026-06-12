package com.catedra.apporgartistas.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.R
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
    private var cargasPendientes = 0

    private val _setlists = MutableLiveData<List<Setlist>>()
    private val _setlistsInstrumentoSuscriptos = MutableLiveData<List<SetlistInstrumentoSuscripto>>()
    val setlistsInstrumentoSuscriptos: LiveData<List<SetlistInstrumentoSuscripto>> =
        _setlistsInstrumentoSuscriptos
    val setlists: LiveData<List<Setlist>> = _setlists

    private val _suscripcionExitosa = MutableLiveData<Boolean>()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    val suscripcionExitosa: LiveData<Boolean> = _suscripcionExitosa

    private val _error = MutableLiveData<Int?>()
    val error: LiveData<Int?> = _error

    fun unirseASetlistConCodigo(codigo: String) {
        val userId = getCurrentUserId()
        val codigoNormalizado = codigo.trim().uppercase()

        if (codigoNormalizado.isBlank()) {
            mostrarError(R.string.message_dashboard_ingresa_un_codigo)
            return
        }

        if (userId.isBlank()) {
            mostrarError(R.string.message_dashboard_usuario_no_autenticado)
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
        cargasPendientes = 2
        _isLoading.value = true

        cargarSetlistsViejos(userId)
        cargarSetlistsInstrumentoSuscriptos(userId)
    }

    fun ocultarSetlist(setlistId: String) {
        ocultarSetlists(listOf(setlistId))
    }

    fun ocultarSetlists(setlistIds: List<String>) {
        val userId = getCurrentUserId()
        _isLoading.value = true

        setlistSubscriptionRepository.ocultarSetlists(
            userId = userId,
            setlistIds = setlistIds,
            onSuccess = { cargarSetlists() },
            onError = {
                mostrarError(R.string.message_dashboard_error_borrar_setlist)
                _isLoading.postValue(false)
            }
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
                finalizarCargaDashboard()
            },
            onError = {
                mostrarError(R.string.message_dashboard_error_cargar_repertorio)
                finalizarCargaDashboard()
            }
        )
    }

    private fun cargarSetlistsInstrumentoSuscriptos(userId: String) {
        setlistSubscriptionRepository.cargarSetlistsInstrumentoSuscriptos(
            userId = userId,
            onSuccess = { lista ->
                _setlistsInstrumentoSuscriptos.postValue(lista)
                finalizarCargaDashboard()
            },
            onError = {
                _setlistsInstrumentoSuscriptos.postValue(emptyList())
                finalizarCargaDashboard()
            }
        )
    }

    private fun finalizarCargaDashboard() {
        cargasPendientes = (cargasPendientes - 1).coerceAtLeast(0)
        if (cargasPendientes == 0) {
            _isLoading.postValue(false)
        }
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
                mostrarError(R.string.message_dashboard_setlist_propio)
            }

            ResultadoSuscripcionSetlist.YaSuscripto -> {
                mostrarError(R.string.message_dashboard_ya_suscripto)
            }

            ResultadoSuscripcionSetlist.CodigoNoEncontrado -> {
                mostrarError(R.string.message_dashboard_codigo_no_encontrado)
            }

            ResultadoSuscripcionSetlist.CodigoInactivo -> {
                mostrarError(R.string.message_dashboard_codigo_inactivo)
            }

            ResultadoSuscripcionSetlist.CodigoIncompleto -> {
                mostrarError(R.string.message_dashboard_codigo_incompleto)
            }

            ResultadoSuscripcionSetlist.ErrorBusqueda -> {
                mostrarError(R.string.message_dashboard_error_buscar_codigo)
            }

            ResultadoSuscripcionSetlist.ErrorSuscripcion -> {
                mostrarError(R.string.message_dashboard_error_suscripcion)
            }
        }

        _isLoading.postValue(false)
    }

    fun limpiarError() {
        _error.value = null
    }

    private fun mostrarError(@StringRes mensajeResId: Int) {
        _error.postValue(mensajeResId)
    }
}
