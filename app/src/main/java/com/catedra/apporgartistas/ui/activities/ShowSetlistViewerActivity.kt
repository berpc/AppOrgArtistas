package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.ui.adapters.ShowSetlistViewerAdapter
import com.catedra.apporgartistas.viewmodels.ShowSetlistViewerViewModel

class ShowSetlistViewerActivity : AppCompatActivity() {

    private val viewModel: ShowSetlistViewerViewModel by viewModels()

    private lateinit var codigo: String
    private lateinit var agrupacionId: String
    private lateinit var showId: String
    private lateinit var instrumentoId: String

    private lateinit var tvTituloShow: TextView
    private lateinit var tvFechaShow: TextView
    private lateinit var tvInstrumento: TextView
    private lateinit var rvShowSetlistViewer: RecyclerView
    private lateinit var adapter: ShowSetlistViewerAdapter

    private var setlistMaster: List<SetlistMasterItem> = emptyList()
    private var pdfsPorSetlistItem: Map<String, PartituraCloud> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_setlist_viewer)

        codigo = intent.getStringExtra("CODIGO") ?: ""
        agrupacionId = intent.getStringExtra("AGRUPACION_ID") ?: ""
        showId = intent.getStringExtra("SHOW_ID") ?: ""
        instrumentoId = intent.getStringExtra("INSTRUMENTO_ID") ?: ""

        if (
            codigo.isBlank() ||
            agrupacionId.isBlank() ||
            showId.isBlank() ||
            instrumentoId.isBlank()
        ) {
            Toast.makeText(
                this,
                "Error al abrir Show Setlist",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        configurarViews()
        configurarRecycler()
        observarViewModel()

        viewModel.observarDatos(
            agrupacionId = agrupacionId,
            showId = showId,
            instrumentoId = instrumentoId
        )
    }

    private fun configurarViews() {
        findViewById<TextView>(R.id.tvBackShowSetlistViewer).setOnClickListener {
            finish()
        }

        tvTituloShow = findViewById(R.id.tvTituloShowSetlistViewer)
        tvFechaShow = findViewById(R.id.tvFechaShowSetlistViewer)
        tvInstrumento = findViewById(R.id.tvInstrumentoShowSetlistViewer)
        rvShowSetlistViewer = findViewById(R.id.rvShowSetlistViewer)
    }

    private fun configurarRecycler() {
        rvShowSetlistViewer.layoutManager = LinearLayoutManager(this)

        adapter = ShowSetlistViewerAdapter(
            setlistMaster = emptyList(),
            pdfsPorSetlistItem = emptyMap(),
            onObraClick = { setlistItem, partitura ->
                abrirPartituraSiExiste(setlistItem, partitura)
            }
        )

        rvShowSetlistViewer.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.show.observe(this) { show ->
            if (show == null) {
                Toast.makeText(
                    this,
                    "Show no encontrado",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            tvTituloShow.text = show.nombre.ifBlank { "Show sin nombre" }
            tvFechaShow.text = show.fecha ?: "Sin fecha"

            setlistMaster = show.setlistMaster

            actualizarLista()
        }

        viewModel.instrumento.observe(this) { instrumento ->
            if (instrumento == null) {
                Toast.makeText(
                    this,
                    "Instrumento no encontrado",
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }

            tvInstrumento.text = instrumento.nombre.ifBlank { "Instrumento" }
            pdfsPorSetlistItem = instrumento.pdfsPorSetlistItem

            actualizarLista()
        }

        viewModel.error.observe(this) { mensaje ->
            Toast.makeText(
                this,
                mensaje,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun actualizarLista() {
        adapter.actualizarDatos(
            nuevoSetlistMaster = setlistMaster,
            nuevosPdfsPorSetlistItem = pdfsPorSetlistItem
        )
    }

    private fun abrirPartituraSiExiste(
        setlistItem: SetlistMasterItem,
        partitura: PartituraCloud?
    ) {
        if (partitura == null || partitura.url.isBlank()) {
            Toast.makeText(
                this,
                "Esta obra no tiene partitura asignada",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra(PdfViewerActivity.EXTRA_PDF_URL, partitura.url)
            putExtra(PdfViewerActivity.EXTRA_OBRA_TITULO, setlistItem.nombre.ifBlank { "Partitura" })
        }

        startActivity(intent)
    }
}
