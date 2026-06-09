package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.data.models.Show
import com.google.firebase.firestore.FirebaseFirestore

class InstrumentoSetlistViewerActivity : AppCompatActivity() {

    private lateinit var codigo: String
    private lateinit var agrupacionId: String
    private lateinit var showId: String
    private lateinit var instrumentoId: String

    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var tvTituloShow: TextView
    private lateinit var tvFechaShow: TextView
    private lateinit var tvInstrumento: TextView
    private lateinit var lvPartituras: ListView

    private var setlistMaster: List<SetlistMasterItem> = emptyList()
    private var pdfsPorSetlistItem: Map<String, PartituraCloud> = emptyMap()

    private val titulosVisuales = mutableListOf<String>()
    private val partiturasOrdenadas = mutableListOf<PartituraCloud?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instrumento_setlist_viewer)

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
            Toast.makeText(this, "Error al abrir setlist", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvTituloShow = findViewById(R.id.tvTituloShowViewer)
        tvFechaShow = findViewById(R.id.tvFechaShowViewer)
        tvInstrumento = findViewById(R.id.tvInstrumentoViewer)
        lvPartituras = findViewById(R.id.lvPartiturasInstrumentoViewer)

        cargarDatos()
    }

    private fun cargarDatos() {
        firestore.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .get()
            .addOnSuccessListener { showDoc ->
                val show = showDoc.toObject(Show::class.java)

                if (show == null) {
                    Toast.makeText(this, "No se encontró el show", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                setlistMaster = show.setlistMaster

                tvTituloShow.text = show.nombre.ifBlank { "Show sin nombre" }
                tvFechaShow.text = show.fecha ?: "Sin fecha"

                cargarInstrumento()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al cargar show: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun cargarInstrumento() {
        firestore.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .get()
            .addOnSuccessListener { instrumentoDoc ->
                val instrumento = instrumentoDoc.toObject(Instrumento::class.java)

                if (instrumento == null) {
                    Toast.makeText(this, "No se encontró el instrumento", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                tvInstrumento.text = instrumento.nombre.ifBlank { "Instrumento" }
                pdfsPorSetlistItem = instrumento.pdfsPorSetlistItem

                configurarLista()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error al cargar instrumento: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun configurarLista() {
        titulosVisuales.clear()
        partiturasOrdenadas.clear()

        setlistMaster.forEachIndexed { index, setlistItem ->
            val partitura = pdfsPorSetlistItem[setlistItem.id]

            partiturasOrdenadas.add(partitura)

            val nombreObra = setlistItem.nombre.ifBlank { "Obra sin nombre" }

            val texto = if (partitura != null && partitura.url.isNotBlank()) {
                "${index + 1}. $nombreObra\n${partitura.nombre.ifBlank { "Partitura" }}"
            } else {
                "${index + 1}. $nombreObra\nTacet"
            }

            titulosVisuales.add(texto)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            titulosVisuales
        )

        lvPartituras.adapter = adapter

        lvPartituras.setOnItemClickListener { _, _, position, _ ->
            val partitura = partiturasOrdenadas.getOrNull(position)

            if (partitura == null || partitura.url.isBlank()) {
                Toast.makeText(this, "Tacet: no hay partitura", Toast.LENGTH_SHORT).show()
                return@setOnItemClickListener
            }

            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                putExtra("PDF_URL", partitura.url)
                putExtra("OBRA_TITULO", partitura.nombre.ifBlank { "Partitura" })
            }

            startActivity(intent)
        }
    }
}