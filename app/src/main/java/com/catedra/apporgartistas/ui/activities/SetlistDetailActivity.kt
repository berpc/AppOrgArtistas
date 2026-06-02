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
import com.catedra.apporgartistas.utils.FileUtils // IMPORTANTE
import com.catedra.apporgartistas.viewmodels.SetlistDetailViewModel

class SetlistDetailActivity : AppCompatActivity() {

    private val viewModel: SetlistDetailViewModel by viewModels()
    private var setlistActual: Setlist? = null

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var titulosVisuales: MutableList<String>
    private var progressBar: ProgressBar? = null

    private val selectorDePdf = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && setlistActual != null) {
            // USAMOS EL UTILS ACÁ
            val nombreOriginal = FileUtils.obtenerNombreDelArchivo(this, uri)
            pedirNombrePartitura(uri, nombreOriginal)
        }
    }

    private val abrirCamara = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uriString = result.data?.getStringExtra("PDF_CAPTURADO")
            if (uriString != null && setlistActual != null) {
                pedirNombrePartitura(Uri.parse(uriString), "Foto Partitura")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_detail)

        listView = findViewById(R.id.lvPartituras)
        progressBar = findViewById(R.id.progressBarDetail)
        findViewById<View>(R.id.fabAgregarPartitura).setOnClickListener { mostrarOpcionesDeAgregado() }

        setlistActual = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SETLIST_COMPLETO", Setlist::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SETLIST_COMPLETO") as? Setlist
        }

        setlistActual?.let {
            supportActionBar?.title = it.titulo
            configurarListaDePartituras(it)
            observarViewModel()
        } ?: run { supportActionBar?.title = "Error al cargar Setlist" }
    }

    private fun observarViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { errorMsg ->
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }

        viewModel.setlistActualizado.observe(this) { setlist ->
            setlistActual = setlist
            actualizarListaVisual()
            Toast.makeText(this, "Setlist actualizado", Toast.LENGTH_SHORT).show()
        }

        viewModel.partiturasEnNube.observe(this) { listaPartituras ->
            if (listaPartituras.isEmpty()) {
                Toast.makeText(this, "No tenés partituras en la nube.", Toast.LENGTH_SHORT).show()
                return@observe
            }

            val nombresAmostrar = listaPartituras.map { it.nombre.ifBlank { "Partitura sin nombre" } }.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tu Repertorio en la Nube")
                .setItems(nombresAmostrar) { _, which ->
                    Toast.makeText(this, "Vinculando...", Toast.LENGTH_SHORT).show()
                    // YA NO PASAMOS EL USER_ID
                    viewModel.agregarPartituraExistente(setlistActual!!, listaPartituras[which])
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun configurarListaDePartituras(setlist: Setlist) {
        titulosVisuales = setlist.partituras.map { it.nombre }.toMutableList()
       // adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titulosVisuales)
        val adapter = ArrayAdapter(this, R.layout.item_partitura, titulosVisuales)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                putExtra("PDF_URL", setlist.partituras[position].url)
                putExtra("OBRA_TITULO", titulosVisuales[position])
            }
            startActivity(intent)
        }

        configurarBorradoMultiple()
    }

    private fun configurarBorradoMultiple() {
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
        listView.setMultiChoiceModeListener(object : AbsListView.MultiChoiceModeListener {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.menu_contextual_delete, menu)
                return true
            }
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (item?.itemId == R.id.action_delete) {
                    borrarItemsSeleccionados()
                    mode?.finish()
                    return true
                }
                return false
            }
            override fun onDestroyActionMode(mode: ActionMode?) {}
            override fun onItemCheckedStateChanged(mode: ActionMode?, position: Int, id: Long, checked: Boolean) {
                mode?.title = "${listView.checkedItemCount} seleccionados"
            }
        })
    }

    private fun actualizarListaVisual() {
        titulosVisuales.clear()
        setlistActual?.partituras?.forEach { partitura ->
            titulosVisuales.add(partitura.nombre.ifBlank { "Partitura sin nombre" })
        }
        adapter.notifyDataSetChanged()
    }

    private fun borrarItemsSeleccionados() {
        val posiciones = listView.checkedItemPositions
        val nuevasPartituras = setlistActual!!.partituras.toMutableList()

        for (i in listView.count - 1 downTo 0) {
            if (posiciones.get(i)) nuevasPartituras.removeAt(i)
        }
        // YA NO PASAMOS EL USER_ID
        viewModel.borrarPartituras(setlistActual!!, nuevasPartituras)
    }

    private fun mostrarOpcionesDeAgregado() {
        val opciones = arrayOf("Subir PDF desde el celular", "Tomar foto con la cámara", "Elegir partitura de la nube")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Agregar Partitura")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> selectorDePdf.launch("application/pdf")
                    1 -> abrirCamara.launch(Intent(this, CameraActivity::class.java))
                    2 -> {
                        Toast.makeText(this, "Buscando...", Toast.LENGTH_SHORT).show()
                        // YA NO PASAMOS EL USER_ID
                        viewModel.obtenerTodasLasPartiturasDeLaNube()
                    }
                }
            }.show()
    }

    private fun pedirNombrePartitura(uri: Uri, nombreSugerido: String) {
        val input = android.widget.EditText(this).apply {
            setText(nombreSugerido)
            setSelection(text.length)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Nombre de la Partitura")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Subir") { _, _ ->
                val nombreFinal = input.text.toString().ifBlank { nombreSugerido }
                Toast.makeText(this, "Subiendo...", Toast.LENGTH_SHORT).show()
                // YA NO PASAMOS EL USER_ID
                viewModel.agregarNuevaPartitura(uri, nombreFinal, setlistActual!!)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}