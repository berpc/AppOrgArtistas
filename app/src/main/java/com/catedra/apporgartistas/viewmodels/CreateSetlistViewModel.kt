package com.catedra.apporgartistas.viewmodels
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.repositories.SetlistRepository
import com.catedra.apporgartistas.services.CloudinaryService
import com.catedra.apporgartistas.services.ArchivoLocalUpload
import com.catedra.apporgartistas.services.AuthService

data class ArchivoLocal(val uri: Uri, val nombre: String)
class CreateSetlistViewModel : ViewModel() {
    private val cloudinaryService = CloudinaryService()
    private val authService = AuthService()

    private val _archivosSeleccionados = MutableLiveData<List<ArchivoLocal>>(emptyList())
    val archivosSeleccionados: LiveData<List<ArchivoLocal>> = _archivosSeleccionados

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _guardadoExitoso = MutableLiveData<Boolean>()
    private val setlistRepository = SetlistRepository()
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    fun agregarPdfLocal(uri: Uri, nombre: String) {
        val listaActual = _archivosSeleccionados.value ?: emptyList()
        _archivosSeleccionados.value = listaActual + ArchivoLocal(uri, nombre)
    }

    fun guardarSetlist(
        titulo: String,
        nombreGrupo: String,
        ubicacion: String,
        usuarioAnonimo: String = "usuario_anonimo"
    ) {
        val archivos = _archivosSeleccionados.value ?: emptyList()
        if (titulo.isBlank() || nombreGrupo.isBlank() || ubicacion.isBlank() || archivos.isEmpty()) {
            _guardadoExitoso.value = false
            return
        }

        val userId = authService.getCurrentUserId() ?: usuarioAnonimo
        _isLoading.value = true

        val archivosUpload = archivos.map { archivoLocal ->
            ArchivoLocalUpload(
                uri = archivoLocal.uri,
                nombre = archivoLocal.nombre
            )
        }
        cloudinaryService.subirPartituras(
            archivos = archivosUpload,
            userId = userId,
            onSuccess = { partiturasSubidas ->
                setlistRepository.crearSetlist(
                    userId = userId,
                    titulo = titulo,
                    nombreGrupo = nombreGrupo,
                    ubicacion = ubicacion,
                    partituras = partiturasSubidas,
                    onSuccess = {
                        _isLoading.postValue(false)
                        _guardadoExitoso.postValue(true)
                    },
                    onError = {
                        _isLoading.postValue(false)
                        _guardadoExitoso.postValue(false)
                    }
                )
            },
            onError = { errorMsg ->
                Log.e("CreateSetlist", "Error Cloudinary: $errorMsg")
                _isLoading.postValue(false)
                _guardadoExitoso.postValue(false)
            }
        )
    }
}
