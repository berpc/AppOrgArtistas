package com.catedra.apporgartistas.utils

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class CloudinaryManager(private val uploadPreset: String) {

    fun subirPartitura(
        fileUri: Uri,
        userId: String,
        onSuccess: (url: String, publicId: String) -> Unit,
        onError: (mensaje: String) -> Unit
    ) {
        MediaManager.get().upload(fileUri)
            .unsigned(uploadPreset)
            .option("resource_type", "raw")
            .option("folder", "usuarios/$userId/partituras")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val urlSegura = resultData["secure_url"] as String
                    val publicId = resultData["public_id"] as String
                    onSuccess(urlSegura, publicId)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError(error.description ?: "Error desconocido")
                }
            })
            .dispatch()
    }
}