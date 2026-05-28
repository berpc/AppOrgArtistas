package com.catedra.apporgartistas.viewmodels
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.utils.CloudinaryManager
import com.google.firebase.firestore.FirebaseFirestore


class CreateSetlistViewModel : ViewModel() {
    private val _pdfsSeleccionados = MutableLiveData<List<Uri>>(emptyList())
    val pdfsSeleccionados: LiveData<List<Uri>> = _pdfsSeleccionados

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _guardadoExitoso = MutableLiveData<Boolean>()
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    private val cloudinaryManager = CloudinaryManager(
        uploadPreset = "upload_from_local"
    )

    private val firestore = FirebaseFirestore.getInstance()

    fun agregarPdfLocal(uri: Uri) {
        val listaActual = _pdfsSeleccionados.value ?: emptyList()
        _pdfsSeleccionados.value = listaActual + uri
    }

    fun guardarSetlist(titulo: String, userId: String){
        val archivos = _pdfsSeleccionados.value ?: emptyList()
        if (titulo.isBlank() || archivos.isEmpty()) return

        _isLoading.value = true

        val partiturasSubidas = mutableListOf<PartituraCloud>()
        var subidasCompletadas = 0
        var huboError = false

        archivos.forEach { uri ->
            cloudinaryManager.subirPartitura(
                fileUri = uri,
                userId = userId,
                onSuccess = {urlSegura, publicId ->
                    if (!huboError) {
                        // Guardamos el resultado en nuestra lista temporal
                        partiturasSubidas.add(PartituraCloud(urlSegura, publicId))
                        subidasCompletadas++

                        // 3. Chequeamos si ya terminamos de subir el último PDF
                        if (subidasCompletadas == archivos.size) {
                            crearDocumentoEnFirestore(userId, titulo, partiturasSubidas)
                        }
                    }
                },
                onError = { errorMsg ->
                    Log.e("CreateSetlist", "Error al subir a Cloudinary: $errorMsg")
                    huboError = true
                    _isLoading.postValue(false)
                    _guardadoExitoso.postValue(false)
                }
            )
        }
    }

    private fun crearDocumentoEnFirestore(userId: String, titulo: String, partituras: List<PartituraCloud>){
        val datosSetlist = hashMapOf(
            "titulo" to titulo,
            "fechaCreacion" to System.currentTimeMillis(),
            "partituras" to partituras
        )

        firestore.collection("usuarios").document(userId)
            .collection("setlists")
            .add(datosSetlist)
            .addOnSuccessListener { documentReference ->
                Log.d("CreateSetlist", "Setlist guardado con ID: ${documentReference.id}")
                _isLoading.postValue(false)
                _guardadoExitoso.postValue(true)
            }
            .addOnFailureListener { e ->
                Log.e("CreateSetlist", "Error al guardar en Firestore", e)
                _isLoading.postValue(false)
                _guardadoExitoso.postValue(false)
            }
    }
}