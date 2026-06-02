package com.catedra.apporgartistas.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.activities.CameraActivity
import com.catedra.apporgartistas.viewmodels.CreateSetlistViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth

class CreateSetlistActivity : AppCompatActivity() {

    private val viewModel: CreateSetlistViewModel by viewModels()

    private val abrirCamara =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uriString = result.data?.getStringExtra("PDF_CAPTURADO")
                if (uriString != null) {
                    pedirNombrePartitura(uriString.toUri(), "Foto Partitura")
                }
            }
        }

    private val selectorDePdf =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                val nombreOriginal = obtenerNombreDelArchivo(uri)
                pedirNombrePartitura(uri, nombreOriginal)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_setlist)

        supportActionBar?.title = "Nuevo Setlist"

        val btnSubirLocal = findViewById<Button>(R.id.btnSubirLocal)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarSetlist)
        val btnCamara = findViewById<Button>(R.id.btnCamara)
        val tvArchivos = findViewById<TextView>(R.id.tvArchivosSeleccionados)
        val etTitulo = findViewById<EditText>(R.id.etSetlistTitulo)
        val etNombreGrupo = findViewById<EditText>(R.id.etNombreGrupo)
        val etUbicacion = findViewById<EditText>(R.id.etUbicacion)
        val tvCoordenadas = findViewById<TextView>(R.id.tvCoordenadas)
        val btnUbicacion = findViewById<Button>(R.id.btnUbicacion)

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        btnSubirLocal.setOnClickListener {
            selectorDePdf.launch("application/pdf")
        }

        viewModel.archivosSeleccionados.observe(this) { lista ->
            if (lista.isNotEmpty()) {
                tvArchivos.text = "${lista.size} archivo(s) listo(s) para subir"
            }
        }

        viewModel.guardadoExitoso.observe(this) { exito ->
            if (exito == true) {
                Toast.makeText(this, "¡Setlist creado con éxito!", Toast.LENGTH_SHORT).show()
                finish()
            } else if (exito == false) {
                Toast.makeText(this, "Hubo un error al guardar.", Toast.LENGTH_LONG).show()
            }
        }

        btnUbicacion.setOnClickListener {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    100
                )
                return@setOnClickListener
            }

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    tvCoordenadas.text =
                        "Lat: ${location.latitude} | Lng: ${location.longitude}"
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo obtener ubicación",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnCamara.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            abrirCamara.launch(intent)
        }

        btnGuardar.setOnClickListener {

            val titulo = etTitulo.text.toString()
            val nombreGrupo = etNombreGrupo.text.toString()
            val ubicacion = etUbicacion.text.toString()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"

            if (titulo.isBlank()) {
                Toast.makeText(this, "Ingresá un título", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nombreGrupo.isBlank()) {
                Toast.makeText(this, "Ingresá el nombre del grupo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ubicacion.isBlank()) {
                Toast.makeText(this, "Ingresá una ubicación", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (viewModel.archivosSeleccionados.value.isNullOrEmpty()) {
                Toast.makeText(this, "Subí al menos un PDF o foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Subiendo archivos...", Toast.LENGTH_SHORT).show()

            viewModel.guardarSetlist(titulo, nombreGrupo, ubicacion, userId)
        }
    }

    // 🔹 OBTENER NOMBRE DEL ARCHIVO
    @SuppressLint("Range")
    private fun obtenerNombreDelArchivo(uri: Uri): String {
        var result: String? = null

        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    )
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

    // 🔹 PEDIR NOMBRE DE PARTITURA (LO QUE SE PERDIÓ EN EL MERGE)
    private fun pedirNombrePartitura(uri: Uri, nombreSugerido: String) {

        val input = EditText(this)
        input.setText(nombreSugerido)
        input.setSelection(input.text.length)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nombre de la Partitura")
            .setMessage("Verificá o editá el nombre:")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Agregar") { _, _ ->

                val nombreFinal = input.text.toString().ifBlank { nombreSugerido }

                viewModel.agregarPdfLocal(uri, nombreFinal)

                Toast.makeText(this, "Agregada: $nombreFinal", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}