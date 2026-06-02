package com.catedra.apporgartistas.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

object FileUtils {
    @android.annotation.SuppressLint("Range")
    fun obtenerNombreDelArchivo(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return result?.substringBeforeLast(".") ?: "Nueva Partitura"
    }
}