package com.catedra.apporgartistas.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _capturedImages = MutableLiveData<List<Uri>>(emptyList())
    val capturedImages: LiveData<List<Uri>> get() = _capturedImages

    private val _pdfResultUri = MutableLiveData<Uri?>()
    val pdfResultUri: LiveData<Uri?> get() = _pdfResultUri

    val error = MutableLiveData<String?>()
    val isProcessing = MutableLiveData(false)

    // ESTO ES CLAVE: Guardamos la URI acá para que sobreviva si Android reinicia la Activity
    var pendingPhotoUri: Uri? = null

    fun addCapturedImage(uri: Uri) {
        val currentList = _capturedImages.value ?: emptyList()
        // Validación anti-duplicados: Solo se agrega si no existe en la lista (Orden FIFO natural)
        if (!currentList.contains(uri)) {
            _capturedImages.value = currentList + uri
        }
    }

    fun generateMultiPagePdf() {
        val uris = _capturedImages.value
        if (uris.isNullOrEmpty()) {
            error.value = "No hay partituras capturadas."
            return
        }

        isProcessing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val archivoPdf = File(context.cacheDir, "partitura_completa_${System.currentTimeMillis()}.pdf")
                val document = android.graphics.pdf.PdfDocument()

                for ((index, uri) in uris.withIndex()) {
                    val originalBitmap = cargarBitmapDesdeUri(uri)

                    if (originalBitmap != null) {
                        val resizedBitmap = reescalarBitmap(originalBitmap, 1200)

                        val stream = java.io.ByteArrayOutputStream()
                        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        val compressedByteArray = stream.toByteArray()

                        val finalCompressedBitmap = android.graphics.BitmapFactory.decodeByteArray(
                            compressedByteArray, 0, compressedByteArray.size
                        )

                        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(
                            finalCompressedBitmap.width,
                            finalCompressedBitmap.height,
                            index + 1
                        ).create()

                        val page = document.startPage(pageInfo)
                        page.canvas.drawBitmap(finalCompressedBitmap, 0f, 0f, null)
                        document.finishPage(page)

                        if (originalBitmap != resizedBitmap) {
                            originalBitmap.recycle()
                        }
                        resizedBitmap.recycle()
                        finalCompressedBitmap.recycle()
                    }
                }

                FileOutputStream(archivoPdf).use { document.writeTo(it) }
                document.close()

                withContext(Dispatchers.Main) {
                    _pdfResultUri.value = Uri.fromFile(archivoPdf)
                    isProcessing.value = false
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    error.value = "Error al generar el PDF: ${e.message}"
                    isProcessing.value = false
                }
            }
        }
    }

    private fun reescalarBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxWidth) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val finalWidth = maxWidth
        val finalHeight = (maxWidth / ratio).toInt()
        return bitmap.scale(finalWidth, finalHeight)
    }

    private fun cargarBitmapDesdeUri(uri: Uri): Bitmap? {
        val context = getApplication<Application>()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}