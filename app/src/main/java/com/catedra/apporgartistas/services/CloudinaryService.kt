package com.catedra.apporgartistas.services

import android.net.Uri
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.utils.CloudinaryManager

class CloudinaryService(
    private val cloudinaryManager: CloudinaryManager = CloudinaryManager(
        uploadPreset = "upload_from_local"
    )
) {

    fun subirPartitura(
        uri: Uri,
        nombre: String,
        userId: String,
        onSuccess: (PartituraCloud) -> Unit,
        onError: (String) -> Unit
    ) {
        cloudinaryManager.subirPartitura(
            fileUri = uri,
            userId = userId,
            onSuccess = { urlSegura, publicId ->
                val partitura = PartituraCloud(
                    nombre = nombre,
                    url = urlSegura,
                    publicId = publicId
                )

                onSuccess(partitura)
            },
            onError = { mensajeError ->
                onError(mensajeError)
            }
        )
    }

    fun subirPartituras(
        archivos: List<ArchivoLocalUpload>,
        userId: String,
        onSuccess: (List<PartituraCloud>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (archivos.isEmpty()) {
            onSuccess(emptyList())
            return
        }

        val partiturasSubidas = mutableListOf<PartituraCloud>()
        var subidasCompletadas = 0
        var huboError = false

        archivos.forEach { archivo ->
            subirPartitura(
                uri = archivo.uri,
                nombre = archivo.nombre,
                userId = userId,
                onSuccess = { partitura ->
                    if (huboError) return@subirPartitura

                    partiturasSubidas.add(partitura)
                    subidasCompletadas++

                    if (subidasCompletadas == archivos.size) {
                        onSuccess(partiturasSubidas)
                    }
                },
                onError = { error ->
                    if (huboError) return@subirPartitura

                    huboError = true
                    onError(error)
                }
            )
        }
    }
}

data class ArchivoLocalUpload(
    val uri: Uri,
    val nombre: String
)