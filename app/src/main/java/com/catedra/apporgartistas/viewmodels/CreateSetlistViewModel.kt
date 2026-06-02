package com.catedra.apporgartistas.viewmodels
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.utils.CloudinaryManager
import com.google.firebase.firestore.FirebaseFirestore

data class ArchivoLocal(val uri: Uri, val nombre: String)
class CreateSetlistViewModel : ViewModel() {

    private val _archivosSeleccionados = MutableLiveData<List<ArchivoLocal>>(emptyList())
    val archivosSeleccionados: LiveData<List<ArchivoLocal>> = _archivosSeleccionados

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _guardadoExitoso = MutableLiveData<Boolean>()
    val guardadoExitoso: LiveData<Boolean> = _guardadoExitoso

    private val cloudinaryManager = CloudinaryManager(uploadPreset = "upload_from_local")
    private val firestore = FirebaseFirestore.getInstance()

    private fun generarCodigoAleatorio(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { caracteres.random() }.joinToString("")
    }

    fun agregarPdfLocal(uri: Uri, nombre: String) {
        val listaActual = _archivosSeleccionados.value ?: emptyList()
        _archivosSeleccionados.value = listaActual + ArchivoLocal(uri, nombre)
    }

    fun guardarSetlist(titulo: String, nombreGrupo: String, ubicacion: String, userId: String){
        val archivos = _archivosSeleccionados.value ?: emptyList()
        if (titulo.isBlank() || archivos.isEmpty()) return

        _isLoading.value = true

        val partiturasSubidas = mutableListOf<PartituraCloud>()
        var subidasCompletadas = 0
        var huboError = false

        archivos.forEach { archivoLocal ->
            cloudinaryManager.subirPartitura(
                fileUri = archivoLocal.uri,
                userId = userId,
                onSuccess = { urlSegura, publicId ->
                    if (!huboError) {
                        partiturasSubidas.add(PartituraCloud(archivoLocal.nombre, urlSegura, publicId))
                        subidasCompletadas++

                        if (subidasCompletadas == archivos.size) {
                            crearDocumentoEnFirestore(userId, titulo, nombreGrupo, ubicacion, partiturasSubidas)
                        }
                    }
                },
                onError = { errorMsg ->
                    Log.e("CreateSetlist", "Error Cloudinary: $errorMsg")
                    huboError = true
                    _isLoading.postValue(false)
                    _guardadoExitoso.postValue(false)
                }
            )
        }
    }

    private fun crearDocumentoEnFirestore(userId: String, titulo: String, nombreGrupo: String,
                                          ubicacion: String, partituras: List<PartituraCloud>){

        val codigo = generarCodigoAleatorio()

        val datosSetlist = hashMapOf(
            "titulo" to titulo,
            "nombreGrupo" to nombreGrupo,
            "ubicacion" to ubicacion,
            "fechaCreacion" to System.currentTimeMillis(),
            "partituras" to partituras,
            "isActive" to true,
            // Agregamos los nuevos campos a la base de datos
            "ownerId" to userId,
            "codigoCompartir" to codigo,
            "suscriptores" to emptyList<String>()
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