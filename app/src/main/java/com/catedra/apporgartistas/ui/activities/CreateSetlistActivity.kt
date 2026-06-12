package com.catedra.apporgartistas.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.activities.CameraActivity
import com.catedra.apporgartistas.ui.adapters.ArchivoLocalAdapter
import com.catedra.apporgartistas.viewmodels.CreateSetlistViewModel
import com.google.android.gms.location.LocationServices

class CreateSetlistActivity : AppCompatActivity() {

    private val viewModel: CreateSetlistViewModel by viewModels()

    private companion object {
        const val EXTRA_PDF_CAPTURADO = "PDF_CAPTURADO"
    }


    private val abrirCamara = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra(EXTRA_PDF_CAPTURADO)
            if (uriString != null) {
                // Al sacar foto, le damos un nombre genérico para que el usuario lo cambie
                pedirNombrePartitura(uriString.toUri(),
                    getString(R.string.default_create_foto_partitura))
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

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_setlist)

        supportActionBar?.title = getString(R.string.title_activity_create_nuevo_setlist)

        val btnSubirLocal = findViewById<Button>(R.id.btnSubirLocal)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarSetlist)
        val btnCamara = findViewById<Button>(R.id.btnCamara)
        val tvArchivos = findViewById<TextView>(R.id.tvArchivosSeleccionados)
        val etTitulo = findViewById<EditText>(R.id.etSetlistTitulo)
        val etNombreGrupo = findViewById<EditText>(R.id.etNombreGrupo)
        val etUbicacion = findViewById<EditText>(R.id.etUbicacion)
        val tvCoordenadas = findViewById<TextView>(R.id.tvCoordenadas)
        val btnUbicacion = findViewById<Button>(R.id.btnUbicacion)
        val progressBar = findViewById<ProgressBar>(R.id.progressBarCreateSetlist)
        val rvArchivos = findViewById<RecyclerView>(R.id.rvArchivosSeleccionados)
        val archivosAdapter = ArchivoLocalAdapter()

        rvArchivos.layoutManager = LinearLayoutManager(this)
        rvArchivos.adapter = archivosAdapter

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        btnSubirLocal.setOnClickListener {
            selectorDePdf.launch("application/pdf")
        }

        //Observamos el ViewModel para actualizar la pantalla
        viewModel.archivosSeleccionados.observe(this) { listaArchivos ->
            archivosAdapter.actualizarLista(listaArchivos)
            tvArchivos.visibility = if (listaArchivos.isEmpty()) View.VISIBLE else View.GONE
            rvArchivos.visibility = if (listaArchivos.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.guardadoExitoso.observe(this) { exito ->
            if (exito == true) {
                Toast.makeText(this,
                    getString(R.string.message_create_setlist_creado_con_xito), Toast.LENGTH_SHORT).show()
                finish() // Cierra esta pantalla y vuelve al Dashboard
            } else {
                Toast.makeText(this,
                    getString(R.string.message_create_hubo_un_error_al_guardar), Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            mostrarLoading(loading, progressBar)
            btnGuardar.isEnabled = !loading
            btnSubirLocal.isEnabled = !loading
            btnCamara.isEnabled = !loading
            btnUbicacion.isEnabled = !loading
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
                    tvCoordenadas.text = getString(
                        R.string.coordenadas,
                        location.latitude.toString(),
                        location.longitude.toString()
                    )
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.message_create_no_se_pudo_obtener_ubicaci_n),
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
            if (titulo.isBlank()) {
                Toast.makeText(this,
                    getString(R.string.message_create_ingresa_un_titulo), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nombreGrupo.isBlank()) {
                Toast.makeText(this,
                    getString(R.string.message_create_ingresa_el_nombre_del_grupo), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ubicacion.isBlank()) {
                Toast.makeText(this,
                    getString(R.string.message_create_ingresa_una_ubicacion), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (viewModel.archivosSeleccionados.value.isNullOrEmpty()) {
                Toast.makeText(this,
                    getString(R.string.message_create_subi_al_menos_un_pdf_o_foto), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this,
                getString(R.string.message_create_subiendo_archivos_por_favor_espera), Toast.LENGTH_SHORT).show()
            viewModel.guardarSetlist(
                titulo,
                nombreGrupo,
                ubicacion,
                getString(R.string.default_create_usuario_anonimo)
            )
        }
    }

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
        return result?.substringBeforeLast(".") ?: getString(R.string.new_partitura)
    }

    // 2. Mostrar el cuadro de diálogo para editar el nombre
    private fun pedirNombrePartitura(uri: Uri, nombreSugerido: String) {

        val input = EditText(this)
        input.setText(nombreSugerido)
        input.setSelection(input.text.length)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.default_create_nombre_de_la_partitura))
            .setMessage(getString(R.string.message_create_verifica_o_edita_el_nombre_de_la_partitura))
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_create_agregar)) { _, _ ->
                val nombreFinal = input.text.toString().ifBlank { nombreSugerido }
                viewModel.agregarPdfLocal(uri, nombreFinal)
                Toast.makeText(this,
                    getString(R.string.message_create_agregada, nombreFinal), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.btn_create_cancelar), null)
            .show()
    }

    private fun mostrarLoading(isLoading: Boolean, progressBar: ProgressBar) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
