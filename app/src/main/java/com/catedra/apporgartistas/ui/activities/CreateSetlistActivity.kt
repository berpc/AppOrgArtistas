package com.catedra.apporgartistas.ui.activities
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.viewmodels.CreateSetlistViewModel
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import com.catedra.apporgartistas.activities.CameraActivity
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import androidx.core.net.toUri


class CreateSetlistActivity : AppCompatActivity() {
    private val viewModel: CreateSetlistViewModel by viewModels()


    private val abrirCamara = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra("PDF_CAPTURADO")
            if (uriString != null) {
                // Al sacar foto, le damos un nombre genérico para que el usuario lo cambie
                pedirNombrePartitura(uriString.toUri(), "Foto Partitura")
            }
        }
    }



    private val selectorDePdf = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if (uri != null) {
            // Obtenemos el nombre real del archivo PDF
            val nombreOriginal = obtenerNombreDelArchivo(uri)
            pedirNombrePartitura(uri, nombreOriginal)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?){
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
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        btnSubirLocal.setOnClickListener {
            selectorDePdf.launch("application/pdf")
        }

        //Observamos el ViewModel para actualizar la pantalla
        viewModel.archivosSeleccionados.observe(this) { listaArchivos ->
            if (listaArchivos.isNotEmpty()) {
                tvArchivos.text = "${listaArchivos.size} archivo(s) listo(s) para subir"
            }
        }
        viewModel.guardadoExitoso.observe(this) { exito ->
            if (exito == true) {
                Toast.makeText(this, "¡Setlist creado con éxito!", Toast.LENGTH_SHORT).show()
                finish() // Cierra esta pantalla y vuelve al Dashboard
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

                    val lat = location.latitude
                    val lng = location.longitude

                    tvCoordenadas.text = "Lat: $lat | Lng: $lng"

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
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"
            val nombreGrupo = etNombreGrupo.text.toString()
            val ubicacion = etUbicacion.text.toString()

            // VALIDACIONES
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

            // ACÁ ESTÁ EL CAMBIO
            if (viewModel.archivosSeleccionados.value.isNullOrEmpty()) {
                Toast.makeText(this, "Subí al menos un PDF o foto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Subiendo archivos, por favor esperá...", Toast.LENGTH_SHORT).show()
            viewModel.guardarSetlist(titulo, nombreGrupo, ubicacion, userId)
        }
    }
    @android.annotation.SuppressLint("Range")
    private fun obtenerNombreDelArchivo(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        // Le sacamos la extensión .pdf si la tiene para que quede más limpio
        return result?.substringBeforeLast(".") ?: "Nueva Partitura"
    }

    // 2. Mostrar el cuadro de diálogo para editar el nombre
    private fun pedirNombrePartitura(uri: Uri, nombreSugerido: String) {
        val input = EditText(this)
        input.setText(nombreSugerido)
        input.setSelection(input.text.length) // Pone el cursor al final

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nombre de la Partitura")
            .setMessage("Verificá o editá el nombre de la partitura:")
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