package com.catedra.apporgartistas.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.utils.CloudinaryManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SetlistDetailViewModel : ViewModel(){
    private val firestore = FirebaseFirestore.getInstance()
    private val cloudinaryManager = CloudinaryManager(uploadPreset = "upload_from_local")

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
        return FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"
    }

    fun obtenerTodasLasPartiturasDeLaNube() {
        val userId = getCurrentUserId()
        _isLoading.value = true
        firestore.collection("usuarios").document(userId).collection("setlists")
            .get()
            .addOnSuccessListener { result ->
                val todasLasPartituras = mutableListOf<PartituraCloud>()

                for (document in result) {
                    val setlist = document.toObject(Setlist::class.java)
                    todasLasPartituras.addAll(setlist.partituras)
                }

                // Filtramos por URL para que la lista no tenga duplicados
                val partiturasUnicas = todasLasPartituras.distinctBy { it.url }

                _partiturasEnNube.postValue(partiturasUnicas)
                _isLoading.postValue(false)
            }
            .addOnFailureListener { e ->
                Log.e("SetlistDetail", "Error al obtener partituras", e)
                _error.postValue("Error al cargar partituras de la nube")
                _isLoading.postValue(false)
            }
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

        cloudinaryManager.subirPartitura(
            fileUri = uri,
            userId = userId,
            onSuccess = { urlSegura, publicId ->
                val nuevaPartitura = PartituraCloud(
                    nombre = nombre,
                    url = urlSegura,
                    publicId = publicId
                )

                val nuevasPartituras = setlist.partituras.toMutableList()
                nuevasPartituras.add(nuevaPartitura)

                actualizarPartiturasEnFirestore(userId, setlist, nuevasPartituras)
            },
            onError = { errorMsg ->
                Log.e("SetlistDetail", "Error Cloudinary: $errorMsg")
                _error.postValue("Error al subir imagen a Cloudinary")
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
        if (setlist.id.isBlank()) {
            _error.postValue("Error: El Setlist no tiene un ID válido.")
            _isLoading.postValue(false)
            return
        }

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