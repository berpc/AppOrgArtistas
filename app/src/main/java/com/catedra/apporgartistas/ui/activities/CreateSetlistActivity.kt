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
import kotlinx.coroutines.selects.SelectInstance


class CreateSetlistActivity : AppCompatActivity() {
    private val viewModel: CreateSetlistViewModel by viewModels()

    private val selectorDePdf = registerForActivityResult(ActivityResultContracts.GetContent()){ uri: Uri? ->
        if (uri != null) {
            viewModel.agregarPdfLocal(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_setlist)

        supportActionBar?.title = "Nuevo Setlist"

        val btnSubirLocal = findViewById<Button>(R.id.btnSubirLocal)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarSetlist)
        val tvArchivos = findViewById<TextView>(R.id.tvArchivosSeleccionados)
        val etTitulo = findViewById<EditText>(R.id.etSetlistTitulo)

        btnSubirLocal.setOnClickListener {
            selectorDePdf.launch("application/pdf")
        }

        //Observamos el ViewModel para actualizar la pantalla
        viewModel.pdfsSeleccionados.observe(this) { listaUris ->
            if (listaUris.isNotEmpty()) {
                tvArchivos.text = "${listaUris.size} archivo(s) listo(s) para subir"
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

        btnGuardar.setOnClickListener {
            val titulo = etTitulo.text.toString()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"

            Toast.makeText(this, "Subiendo archivos, por favor esperá...", Toast.LENGTH_SHORT).show()
            viewModel.guardarSetlist(titulo, userId)
        }
    }
}