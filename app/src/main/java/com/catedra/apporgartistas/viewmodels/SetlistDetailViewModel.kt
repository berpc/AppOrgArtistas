package com.catedra.apporgartistas.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.data.repositories.SetlistRepository
import com.catedra.apporgartistas.services.AuthService
import com.catedra.apporgartistas.services.CloudinaryService
class SetlistDetailViewModel : ViewModel(){
    private val setlistRepository = SetlistRepository()
    private val cloudinaryService = CloudinaryService()
    private val authService = AuthService()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _setlistActualizado = MutableLiveData<Setlist>()
    val setlistActualizado: LiveData<Setlist> = _setlistActualizado

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _partiturasEnNube = MutableLiveData<List<PartituraCloud>>()
    val partiturasEnNube: LiveData<List<PartituraCloud>> = _partiturasEnNube

    // Función interna para obtener el ID limpio
    private fun getCurrentUserId(): String {
        return authService.getCurrentUserIdOrAnonymous()
    }

    fun obtenerTodasLasPartiturasDeLaNube() {
        val userId = authService.getCurrentUserIdOrAnonymous()

        _isLoading.value = true

        setlistRepository.obtenerTodasLasPartiturasDeLaNube(
            userId = userId,
            onSuccess = {partiturasUnicas ->
                _partiturasEnNube.postValue(partiturasUnicas)
                _isLoading.postValue(false)
            },
            onError = {mensaje ->
                _error.postValue(mensaje)
                _isLoading.postValue(false)
            }
        )
    }

    fun agregarPartituraExistente(setlist: Setlist, partitura: PartituraCloud) {
        _isLoading.value = true
        val nuevasPartituras = setlist.partituras.toMutableList()
        nuevasPartituras.add(partitura)

        actualizarPartiturasEnFirestore(getCurrentUserId(), setlist, nuevasPartituras)
    }

    fun agregarNuevaPartitura(uri: Uri, nombre: String, setlist: Setlist) {
        val userId = getCurrentUserId()
        _isLoading.value = true

        cloudinaryService.subirPartitura(
            uri = uri,
            nombre = nombre,
            userId = userId,
            onSuccess = { nuevaPartitura ->
                val nuevasPartituras = setlist.partituras.toMutableList()
                nuevasPartituras.add(nuevaPartitura)

                actualizarPartiturasEnFirestore(userId, setlist, nuevasPartituras)
            },
            onError = { errorMsg ->
                Log.e("SetlistDetail", "Error Cloudinary: $errorMsg")
                _error.postValue("Error al subir partitura a Cloudinary")
                _isLoading.postValue(false)
            }
        )
    }

    fun borrarPartituras(setlist: Setlist, nuevasPartituras: List<PartituraCloud>) {
        _isLoading.value = true
        actualizarPartiturasEnFirestore(getCurrentUserId(), setlist, nuevasPartituras)
    }

    private fun actualizarPartiturasEnFirestore(
        userId: String,
        setlist: Setlist,
        nuevasPartituras: List<PartituraCloud>
    ) {
        _isLoading.postValue(true)

        setlistRepository.actualizarPartituras(
            userId = userId,
            setlistId = setlist.id,
            nuevasPartituras = nuevasPartituras,
            onSuccess = {
                setlist.partituras = nuevasPartituras
                _setlistActualizado.postValue(setlist)
                _isLoading.postValue(false)
            },
            onError = { mensaje ->
                _error.postValue(mensaje)
                _isLoading.postValue(false)
            }
        )
    }
}