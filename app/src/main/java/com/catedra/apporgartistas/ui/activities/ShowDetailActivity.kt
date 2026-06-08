package com.catedra.apporgartistas.ui.activities

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.ui.adapters.InstrumentoAdapter
import com.catedra.apporgartistas.utils.InstrumentoRepository
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.ui.adapters.SetlistMatrixAdapter
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.utils.CloudinaryManager
import com.catedra.apporgartistas.utils.ShowDetailRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.UUID


class ShowDetailActivity : AppCompatActivity() {

    private lateinit var showId: String
    private lateinit var agrupacionId: String

    private lateinit var rvInstrumentos: RecyclerView
    private lateinit var layoutIndicadores: LinearLayout
    private lateinit var instrumentoAdapter: InstrumentoAdapter
    private val db = FirebaseFirestore.getInstance()

    private var showActual: Show? = null


    private val instrumentoRepository = InstrumentoRepository()
    private var instrumentoSeleccionadoParaPdf: Instrumento? = null
    private var setlistItemSeleccionadoParaPdf: SetlistMasterItem? = null
    // pegado
    private val showDetailRepository = ShowDetailRepository()

    private var cancionesSetlist: List<SetlistMasterItem> = emptyList()

    private lateinit var layoutHeaderMatriz: LinearLayout
    private lateinit var rvMatrizSetlist: RecyclerView
    private lateinit var matrizAdapter: SetlistMatrixAdapter

    private var instrumentosActuales: List<Instrumento> = emptyList()

    private val cloudinaryManager = CloudinaryManager(uploadPreset = "upload_from_local")
    private val seleccionarPdfLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri == null) return@registerForActivityResult

            val instrumento = instrumentoSeleccionadoParaPdf ?: return@registerForActivityResult
            val setlistItem = setlistItemSeleccionadoParaPdf ?: return@registerForActivityResult

            subirPdfACloudinary(
                instrumento = instrumento,
                setlistItem = setlistItem,
                uri = uri
            )
        }
    private fun seleccionarPdfParaInstrumento(
        instrumento: Instrumento,
        setlistItem: SetlistMasterItem
    ) {
        instrumentoSeleccionadoParaPdf = instrumento
        setlistItemSeleccionadoParaPdf = setlistItem

        seleccionarPdfLauncher.launch("application/pdf")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_detail)

        showId = intent.getStringExtra("SHOW_ID") ?: ""
        agrupacionId = intent.getStringExtra("AGRUPACION_ID") ?: ""

        if (showId.isBlank() || agrupacionId.isBlank()) {
            Toast.makeText(this, "Error al abrir el show", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        configurarCabecera()
        configurarCarruselInstrumentos()
        cargarInstrumentos()
        configurarMatrizSetlist()
        cargarShow()
    }
    private fun configurarMatrizSetlist() {
        layoutHeaderMatriz = findViewById(R.id.layoutHeaderMatriz)
        rvMatrizSetlist = findViewById(R.id.rvMatrizSetlist)

        rvMatrizSetlist.layoutManager = LinearLayoutManager(this)

        matrizAdapter = SetlistMatrixAdapter(
            canciones = mutableListOf(),
            instrumentos = emptyList(),

            onCrearCancionConfirmada = { nombre ->
                crearCancionEnSetlist(nombre)
            },

            onEditarCancionConfirmada = { setlistItem, nuevoNombre ->
                editarCancionEnSetlist(setlistItem, nuevoNombre)
            },

            onBorrarCancionConfirmada = { setlistItem ->
                borrarCancionDeSetlist(setlistItem)
            },

            onCeldaClick = { instrumento, setlistItem ->
                seleccionarPdfParaInstrumento(
                    instrumento = instrumento,
                    setlistItem = setlistItem
                )
            }
        )

        rvMatrizSetlist.adapter = matrizAdapter
    }

    private fun guardarSetlistMaster(
        nuevaLista: List<SetlistMasterItem>
    ) {
        lifecycleScope.launch {
            try {
                showDetailRepository.actualizarSetlistMaster(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    nuevoSetlistMaster = nuevaLista
                )

                cancionesSetlist = nuevaLista
                actualizarMatriz()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ShowDetailActivity,
                    "Error al guardar setlist: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun editarCancionEnSetlist(
        setlistItem: SetlistMasterItem,
        nuevoNombre: String
    ) {
        val nuevaLista = cancionesSetlist.map { item ->
            if (item.id == setlistItem.id) {
                item.copy(nombre = nuevoNombre)
            } else {
                item
            }
        }

        guardarSetlistMaster(nuevaLista)
    }
    private fun borrarCancionDeSetlist(
        setlistItem: SetlistMasterItem
    ) {
        val nuevaLista = cancionesSetlist.filter { item ->
            item.id != setlistItem.id
        }

        guardarSetlistMaster(nuevaLista)
    }
    private fun guardarPdfEnInstrumento(
        instrumento: Instrumento,
        setlistItem: SetlistMasterItem,
        urlPdf: String
    ) {
        lifecycleScope.launch {
            try {
                instrumentoRepository.actualizarPdfDeInstrumento(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    instrumentoId = instrumento.id,
                    setlistItemId = setlistItem.id,
                    urlPdf = urlPdf
                )

                Toast.makeText(
                    this@ShowDetailActivity,
                    "PDF cargado correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                cargarInstrumentos()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ShowDetailActivity,
                    "Error al guardar PDF: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun subirPdfACloudinary(
        instrumento: Instrumento,
        setlistItem: SetlistMasterItem,
        uri: Uri
    ) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Subiendo PDF...", Toast.LENGTH_SHORT).show()

        cloudinaryManager.subirPartitura(
            fileUri = uri,
            userId = userId,
            onSuccess = { urlSegura, _ ->
                guardarPdfEnInstrumento(
                    instrumento = instrumento,
                    setlistItem = setlistItem,
                    urlPdf = urlSegura
                )
            },
            onError = { mensaje ->
                Toast.makeText(
                    this,
                    "Error al subir PDF: $mensaje",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }


    private fun configurarCabecera() {
        findViewById<TextView>(R.id.tvBackShow).setOnClickListener {
            finish()
        }
    }
    private fun cargarShow() {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .get()
            .addOnSuccessListener { document ->
                showActual = document.toObject(Show::class.java)

                cancionesSetlist = showActual?.setlistMaster ?: emptyList()

                actualizarMatriz()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar show", Toast.LENGTH_SHORT).show()
            }
    }
    private fun actualizarHeaderMatriz() {
        layoutHeaderMatriz.removeAllViews()

        layoutHeaderMatriz.addView(
            crearCeldaHeader(
                texto = "Obra",
                ancho = 260,
                gravity = android.view.Gravity.CENTER_VERTICAL
            )
        )

        instrumentosActuales.forEach { instrumento ->
            layoutHeaderMatriz.addView(
                crearCeldaHeader(
                    texto = instrumento.nombre,
                    ancho = 120,
                    gravity = android.view.Gravity.CENTER
                )
            )
        }
    }

    private fun crearCeldaHeader(
        texto: String,
        ancho: Int,
        gravity: Int
    ): TextView {
        return TextView(this).apply {
            this.text = texto
            setTextColor(android.graphics.Color.WHITE)
            textSize = 13f
            this.gravity = gravity
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END

            layoutParams = LinearLayout.LayoutParams(ancho, 48).apply {
                marginStart = 4
                marginEnd = 4
            }
        }
    }
    private fun actualizarMatriz() {
        actualizarHeaderMatriz()

        matrizAdapter.actualizarDatos(
            nuevasCanciones = cancionesSetlist,
            nuevosInstrumentos = instrumentosActuales
        )
    }
    private fun configurarCarruselInstrumentos() {
        rvInstrumentos = findViewById(R.id.rvInstrumentos)
        layoutIndicadores = findViewById(R.id.layoutIndicadoresInstrumentos)

        instrumentoAdapter = InstrumentoAdapter(
            instrumentos = mutableListOf(),
            onAddClick = {
                mostrarDialogoAgregarInstrumento()
            },
            onItemClick = { instrumento ->
                Toast.makeText(
                    this,
                    "Seleccionaste: ${instrumento.nombre}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        rvInstrumentos.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )

        rvInstrumentos.adapter = instrumentoAdapter

        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(rvInstrumentos)

        rvInstrumentos.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    actualizarIndicadorActivo()
                }
            }
        })

        dibujarIndicadores()
    }

    private fun cargarInstrumentos() {
        lifecycleScope.launch {
            try {
                val instrumentos = instrumentoRepository.obtenerInstrumentos(
                    showId = showId,
                    agrupacionId = agrupacionId
                )

                instrumentosActuales = instrumentos

                instrumentoAdapter.actualizarLista(instrumentos)
                dibujarIndicadores()
                actualizarIndicadorActivo()

                actualizarMatriz()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ShowDetailActivity,
                    "Error al cargar instrumentos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarDialogoAgregarInstrumento() {
        val input = EditText(this)
        input.hint = "Ej: Flauta 1"

        AlertDialog.Builder(this)
            .setTitle("Agregar instrumento")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = input.text.toString().trim()

                if (nombre.isBlank()) {
                    Toast.makeText(this, "Ingresá un nombre", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                agregarInstrumento(nombre)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarInstrumento(nombre: String) {
        lifecycleScope.launch {
            try {
                val instrumento = Instrumento(
                    nombre = nombre,
                    activo = true
                )

                instrumentoRepository.agregarInstrumento(
                    showId = showId,
                    agrupacionId = agrupacionId,
                    instrumento = instrumento
                )

                Toast.makeText(
                    this@ShowDetailActivity,
                    "Instrumento agregado",
                    Toast.LENGTH_SHORT
                ).show()

                cargarInstrumentos()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ShowDetailActivity,
                    "Error al agregar instrumento: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun dibujarIndicadores() {
        layoutIndicadores.removeAllViews()

        val cantidadItems = instrumentoAdapter.itemCount

        for (i in 0 until cantidadItems) {
            val indicador = TextView(this)
            indicador.text = if (i == 0) "●" else "○"
            indicador.textSize = 18f
            indicador.setTextColor(android.graphics.Color.WHITE)
            indicador.setPadding(6, 0, 6, 0)

            layoutIndicadores.addView(indicador)
        }
    }

    private fun actualizarIndicadorActivo() {
        val layoutManager = rvInstrumentos.layoutManager as LinearLayoutManager
        val posicionActual = layoutManager.findFirstCompletelyVisibleItemPosition()
            .takeIf { it != RecyclerView.NO_POSITION }
            ?: layoutManager.findFirstVisibleItemPosition()

        for (i in 0 until layoutIndicadores.childCount) {
            val indicador = layoutIndicadores.getChildAt(i) as TextView
            indicador.text = if (i == posicionActual) "●" else "○"
        }
    }
    private fun crearCancionEnSetlist(nombre: String) {
        val nuevoItem = SetlistMasterItem(
            id = java.util.UUID.randomUUID().toString(),
            nombre = nombre
        )

        val nuevaLista = cancionesSetlist.toMutableList()
        nuevaLista.add(nuevoItem)

        guardarSetlistMaster(nuevaLista)
    }
}