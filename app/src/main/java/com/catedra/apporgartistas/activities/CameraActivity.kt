package com.catedra.apporgartistas.activities

import android.Manifest // <-- IMPORTANTE: Agregar este import
import android.content.Intent
import android.content.pm.PackageManager // <-- IMPORTANTE: Agregar este import
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat // <-- IMPORTANTE: Agregar este import
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.viewmodels.CameraViewModel
import java.io.File
import java.io.FileOutputStream
class CameraActivity : AppCompatActivity() {

    private val viewModel: CameraViewModel by viewModels()
    private lateinit var ivPreview: ImageView
    private var archivoPdf: File? = null

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            ivPreview.setImageBitmap(bitmap)
            archivoPdf = convertirBitmapAPdf(bitmap)
        }
    }
    private val solicitarPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            tomarFoto.launch(null)
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara para capturar la partitura", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        supportActionBar?.title = "Capturar Partitura"

        ivPreview = findViewById(R.id.ivPreview)
        val btnCapturar = findViewById<Button>(R.id.btnCapturar)
        val btnSubir = findViewById<Button>(R.id.btnSubir)

        btnCapturar.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // Ya tenemos el permiso, lanzamos la cámara directo
                tomarFoto.launch(null)
            } else {
                // No tenemos el permiso, lanzamos el cuadro de diálogo para pedirlo
                solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
            }
        }

        btnSubir.setOnClickListener {
            val pdf = archivoPdf
            if (pdf == null) {
                Toast.makeText(this, "Primero tomá una foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val uri = Uri.fromFile(pdf)
            // Devolvemos el URI a CreateSetlistActivity
            val result = Intent().apply {
                putExtra("PDF_CAPTURADO", uri.toString())
            }
            setResult(RESULT_OK, result)
            finish()
        }

        viewModel.error.observe(this) { mensaje ->
            if (mensaje != null) Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        }
    }

    private fun convertirBitmapAPdf(bitmap: Bitmap): File {
        val archivo = File(cacheDir, "partitura_${System.currentTimeMillis()}.pdf")
        val document = android.graphics.pdf.PdfDocument()
        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)
        FileOutputStream(archivo).use { document.writeTo(it) }
        document.close()
        return archivo
    }
}