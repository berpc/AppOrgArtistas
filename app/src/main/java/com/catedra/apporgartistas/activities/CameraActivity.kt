package com.catedra.apporgartistas.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView // <-- Importante
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.ui.adapters.FotosAdapter // <-- Importante
import com.catedra.apporgartistas.viewmodels.CameraViewModel
import java.io.File

class CameraActivity : AppCompatActivity() {

    private val viewModel: CameraViewModel by viewModels()
    private lateinit var rvPreviewFotos: RecyclerView
    private var tvPageCount: TextView? = null
    private lateinit var adapterFotos: FotosAdapter

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            val uri = viewModel.pendingPhotoUri
            if (uri != null) {
                viewModel.addCapturedImage(uri)
                mostrarDialogoContinuar()
            }
        } else {
            Toast.makeText(this, "Captura cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private val solicitarPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            lanzarCamara()
        } else {
            Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        tvPageCount = findViewById(R.id.tvPageCount)

        supportActionBar?.title = "Capturar Partituras"

        rvPreviewFotos = findViewById(R.id.rvPreviewFotos)
        adapterFotos = FotosAdapter()
        rvPreviewFotos.layoutManager = LinearLayoutManager(this)
        rvPreviewFotos.adapter = adapterFotos



        val btnCapturar = findViewById<Button>(R.id.btnCapturar)
        val btnSubir = findViewById<Button>(R.id.btnSubir)

        observarViewModel()

        btnCapturar.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                lanzarCamara()
            } else {
                solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
            }
        }

        btnSubir.setOnClickListener {
            viewModel.generateMultiPagePdf()
        }
    }

    private fun lanzarCamara() {
        val archivoFoto = File(cacheDir, "temp_partitura_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            archivoFoto
        )
        // Guardamos en el ViewModel ANTES de abrir la cámara
        viewModel.pendingPhotoUri = uri
        tomarFoto.launch(uri)
    }

    private fun mostrarDialogoContinuar() {
        val pageCount = viewModel.capturedImages.value?.size ?: 1
        AlertDialog.Builder(this)
            .setTitle("Página $pageCount capturada")
            .setMessage("¿Querés capturar la siguiente página de esta partitura?")
            .setPositiveButton("Sí, sacar otra") { _, _ ->
                lanzarCamara() // Reinicia el ciclo inmediatamente
            }
            .setNegativeButton("No, terminar") { _, _ ->
                // Se cierra el diálogo y el usuario puede darle al botón "Usar esta Foto"
                rvPreviewFotos.scrollToPosition(pageCount - 1)
            }
            .setCancelable(false)
            .show()
    }

    private fun observarViewModel() {
        viewModel.capturedImages.observe(this) { images ->
            tvPageCount?.text = "Páginas en cola: ${images.size}"
            adapterFotos.actualizarFotos(images)
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            val btnSubir = findViewById<Button>(R.id.btnSubir)
            val btnCapturar = findViewById<Button>(R.id.btnCapturar)
            btnSubir.isEnabled = !isProcessing
            btnCapturar.isEnabled = !isProcessing

            if (isProcessing) {
                Toast.makeText(this, "Ensamblando PDF...", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.pdfResultUri.observe(this) { uri ->
            if (uri != null) {
                val result = Intent().apply {
                    putExtra("PDF_CAPTURADO", uri.toString())
                }
                setResult(RESULT_OK, result)
                finish()
            }
        }

        viewModel.error.observe(this) { mensaje ->
            if (mensaje != null) Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        }
    }
}