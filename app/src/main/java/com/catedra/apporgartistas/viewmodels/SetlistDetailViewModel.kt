package com.catedra.apporgartistas.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.utils.CloudinaryManager
import com.google.firebase.firestore.FirebaseFirestore

class SetlistDetailViewModel : ViewModel(){
    private val firestore = FirebaseFirestore.getInstance()
    private val cloudinaryManager = CloudinaryManager(uploadPreset = "upload_from_local")

    // LiveData para manejar el estado de carga y avisarle a la Activity
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // LiveData para devolver el Setlist actualizado a la vista
    private val _setlistActualizado = MutableLiveData<Setlist>()
    val setlistActualizado: LiveData<Setlist> = _setlistActualizado

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun agregarNuevaPartitura(uri: Uri, userId: String, setlist: Setlist) {
        _isLoading.value = true

        cloudinaryManager.subirPartitura(
            fileUri = uri,
            userId = userId,
            onSuccess = { urlSegura, publicId ->
                val nuevaPartitura = PartituraCloud(urlSegura, publicId)

                // Creamos una nueva lista copiando la anterior y sumando la nueva
                val nuevasPartituras = setlist.partituras.toMutableList()
                nuevasPartituras.add(nuevaPartitura)

                // Actualizamos Firestore
                actualizarPartiturasEnFirestore(userId, setlist, nuevasPartituras)
            },
            onError = { errorMsg ->
                Log.e("SetlistDetail", "Error Cloudinary: $errorMsg")
                _error.postValue("Error al subir imagen a Cloudinary")
                _isLoading.postValue(false)
            }
        )
    }
    fun borrarPartituras(userId: String, setlist: Setlist, nuevasPartituras: List<PartituraCloud>) {
        _isLoading.value = true
        actualizarPartiturasEnFirestore(userId, setlist, nuevasPartituras)
    }
    private fun actualizarPartiturasEnFirestore(
        userId: String,
        setlist: Setlist,
        nuevasPartituras: List<PartituraCloud>
    ) {
        if (setlist.id.isBlank()) {
            _error.postValue("Error: El Setlist no tiene un ID válido.")
            _isLoading.postValue(false)
            return
        }

        // Actualizamos solamente el campo "partituras" del documento
        firestore.collection("usuarios").document(userId)
            .collection("setlists").document(setlist.id)
            .update("partituras", nuevasPartituras)
            .addOnSuccessListener {
                // Si sale bien, actualizamos nuestro objeto local y avisamos a la vista
                setlist.partituras = nuevasPartituras
                _setlistActualizado.postValue(setlist)
                _isLoading.postValue(false)
            }
            .addOnFailureListener { e ->
                Log.e("SetlistDetail", "Error al actualizar Firestore", e)
                _error.postValue("Error al guardar en la base de datos")
                _isLoading.postValue(false)
            }
    }
}