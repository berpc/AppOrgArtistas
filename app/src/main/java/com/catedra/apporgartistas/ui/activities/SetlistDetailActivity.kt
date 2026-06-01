package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.activities.CameraActivity
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.viewmodels.SetlistDetailViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlin.jvm.java

class SetlistDetailActivity : AppCompatActivity() {

    private val viewModel: SetlistDetailViewModel by viewModels()
    private var setlistActual: Setlist? = null

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var titulosVisuales: MutableList<String>

    // Necesitás agregar un ProgressBar en tu activity_setlist_detail.xml con este ID
    // para mostrar cuando está subiendo/borrando
    private var progressBar: ProgressBar? = null

    private val selectorDePdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && setlistActual != null) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"
            Toast.makeText(this, "Subiendo partitura...", Toast.LENGTH_SHORT).show()
            viewModel.agregarNuevaPartitura(uri, userId, setlistActual!!)
        }
    }
    private val abrirCamara = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra("PDF_CAPTURADO")
            if (uriString != null && setlistActual != null) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"
                Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show()
                viewModel.agregarNuevaPartitura(Uri.parse(uriString), userId, setlistActual!!)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_detail)

        listView = findViewById(R.id.lvPartituras)
        progressBar = findViewById(R.id.progressBarDetail)
        val fabAgregar = findViewById<View>(R.id.fabAgregarPartitura)
        fabAgregar.setOnClickListener {
            selectorDePdf.launch("application/pdf")
        }

        setlistActual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SETLIST_COMPLETO", Setlist::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SETLIST_COMPLETO") as? Setlist
        }

        if (setlistActual != null) {
            supportActionBar?.title = setlistActual!!.titulo
            configurarListaDePartituras(setlistActual!!)
            observarViewModel()
        } else {
            supportActionBar?.title = "Error al cargar Setlist"
        }
    }
    private fun observarViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }

        viewModel.setlistActualizado.observe(this) { setlist ->
            // Cuando Firebase confirma el guardado, actualizamos la UI
            setlistActual = setlist
            actualizarListaVisual()
            Toast.makeText(this, "Setlist actualizado con éxito", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarListaDePartituras(setlist: Setlist) {
        listView = findViewById(R.id.lvPartituras)

        titulosVisuales = setlist.partituras.map { it.nombre }.toMutableList()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titulosVisuales)
        listView.adapter = adapter

        // Evento de click normal: Abrir partitura
        listView.setOnItemClickListener { _, _, position, _ ->
            val partituraSeleccionada = setlist.partituras[position]
            val tituloDinamico = titulosVisuales[position]

            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                putExtra("PDF_URL", partituraSeleccionada.url)
                putExtra("OBRA_TITULO", tituloDinamico)
            }
            startActivity(intent)
        }

        // EVENTO LONG-PRESS: SELECCIÓN MÚLTIPLE
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView.setMultiChoiceModeListener(object : AbsListView.MultiChoiceModeListener {

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                // Inflamos el menú con el tacho de basura
                mode?.menuInflater?.inflate(R.menu.menu_contextual_delete, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.action_delete -> {
                        borrarItemsSeleccionados()
                        mode?.finish() // Cierra el menú contextual
                        true
                    }
                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                // Se ejecuta al salir del modo selección
            }

            override fun onItemCheckedStateChanged(mode: ActionMode?, position: Int, id: Long, checked: Boolean) {
                val count = listView.checkedItemCount
                mode?.title = "$count seleccionados"
            }
        })
    }
    private fun actualizarListaVisual() {
        titulosVisuales.clear()

        setlistActual?.partituras?.forEach { partitura ->
            val nombreAMostrar = if (partitura.nombre.isNotBlank()) partitura.nombre else "Partitura sin nombre"
            titulosVisuales.add(nombreAMostrar)
        }

        adapter.notifyDataSetChanged()
    }

    private fun borrarItemsSeleccionados() {
        val posicionesSeleccionadas = listView.checkedItemPositions
        val nuevasPartituras = setlistActual!!.partituras.toMutableList()

        for (i in listView.count - 1 downTo 0) {
            if (posicionesSeleccionadas.get(i)) {
                nuevasPartituras.removeAt(i)
            }
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"
        // Llamamos al ViewModel para que guarde esta nueva lista en Firestore
        viewModel.borrarPartituras(userId, setlistActual!!, nuevasPartituras)
    }
    private fun mostrarOpcionesDeAgregado() {
        val opciones = arrayOf(
            "Subir PDF desde el celular",
            "Tomar foto con la cámara",
            "Elegir partitura de la nube"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Agregar Partitura")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> {
                        // Opción 1: Archivo local
                        selectorDePdf.launch("application/pdf")
                    }
                    1 -> {
                        // Opción 2: Cámara
                        val intent = Intent(this, CameraActivity::class.java)
                        abrirCamara.launch(intent)
                    }
                    2 -> {
                        // Opción 3: Base de datos / Nube
          //              abrirSelectorDeNube()
                    }
                }
            }
            .show()
    }

}