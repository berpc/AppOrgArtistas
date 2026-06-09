package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.ui.adapters.ShowSetlistViewerAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ShowSetlistViewerActivity : AppCompatActivity() {

    private lateinit var codigo: String
    private lateinit var agrupacionId: String
    private lateinit var showId: String
    private lateinit var instrumentoId: String

    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var tvTituloShow: TextView
    private lateinit var tvFechaShow: TextView
    private lateinit var tvInstrumento: TextView
    private lateinit var rvShowSetlistViewer: RecyclerView
    private lateinit var adapter: ShowSetlistViewerAdapter

    private var showListener: ListenerRegistration? = null
    private var instrumentoListener: ListenerRegistration? = null

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
        observarShowEnTiempoReal()
        observarInstrumentoEnTiempoReal()
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

    private fun observarShowEnTiempoReal() {
        val showRef = firestore.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)

        showListener = showRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(
                    this,
                    "Error al escuchar cambios del show",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            val show = snapshot?.toObject(Show::class.java)

            if (show == null) {
                Toast.makeText(
                    this,
                    "Show no encontrado",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            tvTituloShow.text = show.nombre.ifBlank { "Show sin nombre" }
            tvFechaShow.text = show.fecha ?: "Sin fecha"

            setlistMaster = show.setlistMaster

            actualizarLista()
        }
    }

    private fun observarInstrumentoEnTiempoReal() {
        val instrumentoRef = firestore.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)

        instrumentoListener = instrumentoRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(
                    this,
                    "Error al escuchar cambios del instrumento",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            val instrumento = snapshot?.toObject(Instrumento::class.java)

            if (instrumento == null) {
                Toast.makeText(
                    this,
                    "Instrumento no encontrado",
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }

            tvInstrumento.text = instrumento.nombre.ifBlank { "Instrumento" }

            pdfsPorSetlistItem = instrumento.pdfsPorSetlistItem

            actualizarLista()
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
            putExtra("PDF_URL", partitura.url)
            putExtra("OBRA_TITULO", setlistItem.nombre.ifBlank { "Partitura" })
        }

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()

        showListener?.remove()
        instrumentoListener?.remove()
    }
}