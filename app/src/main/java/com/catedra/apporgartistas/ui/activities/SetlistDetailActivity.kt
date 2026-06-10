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
                pedirNombrePartitura(Uri.parse(uriString),
                    getString(R.string.default_detail_foto_partitura))
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
        } ?: run { supportActionBar?.title =
            getString(R.string.message_detail_error_al_cargar_setlist) }
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
            Toast.makeText(this,
                getString(R.string.message_detail_setlist_actualizado), Toast.LENGTH_SHORT).show()
        }

        viewModel.partiturasEnNube.observe(this) { listaPartituras ->
            if (listaPartituras.isEmpty()) {
                Toast.makeText(this,
                    getString(R.string.message_detail_no_tenes_partituras_en_la_nube), Toast.LENGTH_SHORT).show()
                return@observe
            }

            val nombresAmostrar = listaPartituras.map { it.nombre.ifBlank { "Partitura sin nombre" } }.toTypedArray()

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_detail_tu_repertorio_en_la_nube))
                .setItems(nombresAmostrar) { _, which ->
                    Toast.makeText(this,
                        getString(R.string.message_detail_vinculando), Toast.LENGTH_SHORT).show()
                    // YA NO PASAMOS EL USER_ID
                    viewModel.agregarPartituraExistente(setlistActual!!, listaPartituras[which])
                }
                .setNegativeButton(getString(R.string.btn_detail_cancelar), null)
                .show()
        }
    }

    private fun configurarListaDePartituras(setlist: Setlist) {
        titulosVisuales = setlist.partituras.map { it.nombre }.toMutableList()
        adapter = ArrayAdapter(this, R.layout.item_partitura, titulosVisuales)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                putExtra(PdfViewerActivity.EXTRA_PDF_URL, setlist.partituras[position].url)
                putExtra(PdfViewerActivity.EXTRA_OBRA_TITULO, titulosVisuales[position])
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
                mode?.title =
                    getString(R.string.default_detail_seleccionados, listView.checkedItemCount)
            }
        })
    }

    private fun actualizarListaVisual() {
        titulosVisuales.clear()
        setlistActual?.partituras?.forEach { partitura ->
            titulosVisuales.add(partitura.nombre.ifBlank { getString(R.string.default_detail_partitura_sin_nombre) })
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
        val opciones = arrayOf(getString(R.string.option_detail_subir_pdf_desde_el_celular),
            getString(
                R.string.option_detail_tomar_foto_con_la_camara
            ), getString(R.string.default_detail_elegir_partitura_de_la_nube))
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_detail_agregar_partitura))
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> selectorDePdf.launch("application/pdf")
                    1 -> abrirCamara.launch(Intent(this, CameraActivity::class.java))
                    2 -> {
                        Toast.makeText(this,
                            getString(R.string.message_detail_buscando), Toast.LENGTH_SHORT).show()
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
            .setTitle(getString(R.string.title_detail_nombre_de_la_partitura))
            .setView(input)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_detail_subir)) { _, _ ->
                val nombreFinal = input.text.toString().ifBlank { nombreSugerido }
                Toast.makeText(this, getString(R.string.message_detail_subiendo), Toast.LENGTH_SHORT).show()
                // YA NO PASAMOS EL USER_ID
                viewModel.agregarNuevaPartitura(uri, nombreFinal, setlistActual!!)
            }
            .setNegativeButton(getString(R.string.btn_detail_cancelar), null)
            .show()
    }
}
