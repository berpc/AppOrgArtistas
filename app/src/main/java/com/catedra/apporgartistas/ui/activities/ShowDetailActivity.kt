package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.ui.adapters.InstrumentoAdapter
import com.catedra.apporgartistas.ui.adapters.SetlistMatrixAdapter
import com.catedra.apporgartistas.utils.InstrumentoRepository
import com.catedra.apporgartistas.utils.ShowDetailRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.UUID

class ShowDetailActivity : AppCompatActivity() {

    private lateinit var showId: String
    private lateinit var agrupacionId: String

    private lateinit var rvInstrumentos: RecyclerView
    private lateinit var layoutIndicadores: LinearLayout
    private lateinit var instrumentoAdapter: InstrumentoAdapter

    private lateinit var layoutHeaderMatriz: LinearLayout
    private lateinit var rvMatrizSetlist: RecyclerView
    private lateinit var matrizAdapter: SetlistMatrixAdapter

    private val db = FirebaseFirestore.getInstance()
    private val instrumentoRepository = InstrumentoRepository()
    private val showDetailRepository = ShowDetailRepository()

    private var showActual: Show? = null
    private var cancionesSetlist: List<SetlistMasterItem> = emptyList()
    private var instrumentosActuales: List<Instrumento> = emptyList()

    private var itemTouchHelper: ItemTouchHelper? = null

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
        configurarMatrizSetlist()

        cargarShow()
        cargarInstrumentos()
    }

    private fun configurarCabecera() {
        findViewById<TextView>(R.id.tvBackShow).setOnClickListener {
            finish()
        }
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
                abrirDetalleInstrumento(instrumento)
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
            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    actualizarIndicadorActivo()
                }
            }
        })

        dibujarIndicadores()
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
                editarCancionEnSetlist(
                    setlistItem = setlistItem,
                    nuevoNombre = nuevoNombre
                )
            },

            onBorrarCancionConfirmada = { setlistItem ->
                borrarCancionDeSetlist(setlistItem)
            },

            onCeldaClick = { instrumento, _ ->
                abrirDetalleInstrumento(instrumento)
            },

            onStartDrag = { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
            }
        )

        rvMatrizSetlist.adapter = matrizAdapter

        configurarDragAndDropSetlist()
    }

    private fun abrirDetalleInstrumento(instrumento: Instrumento) {
        val intent = Intent(this, InstrumentoDetailActivity::class.java)

        intent.putExtra("AGRUPACION_ID", agrupacionId)
        intent.putExtra("SHOW_ID", showId)
        intent.putExtra("INSTRUMENTO_ID", instrumento.id)

        startActivity(intent)
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
                Toast.makeText(
                    this,
                    "Error al cargar show",
                    Toast.LENGTH_SHORT
                ).show()
            }
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

    private fun actualizarMatriz() {
        actualizarHeaderMatriz()

        matrizAdapter.actualizarDatos(
            nuevasCanciones = cancionesSetlist,
            nuevosInstrumentos = instrumentosActuales
        )
    }

    private fun actualizarHeaderMatriz() {
        layoutHeaderMatriz.removeAllViews()

        layoutHeaderMatriz.addView(
            crearCeldaHeader(
                texto = "Obra",
                ancho = 48 + 48 + 240,
                gravity = Gravity.CENTER_VERTICAL
            )
        )

        instrumentosActuales.forEach { instrumento ->
            layoutHeaderMatriz.addView(
                crearCeldaHeader(
                    texto = instrumento.nombre,
                    ancho = 140,
                    gravity = Gravity.CENTER
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
            ellipsize = TextUtils.TruncateAt.END

            layoutParams = LinearLayout.LayoutParams(ancho, 48).apply {
                marginStart = 4
                marginEnd = 4
            }
        }
    }

    private fun crearCancionEnSetlist(nombre: String) {
        val nuevoItem = SetlistMasterItem(
            id = UUID.randomUUID().toString(),
            nombre = nombre
        )

        val nuevaLista = cancionesSetlist.toMutableList()
        nuevaLista.add(nuevoItem)

        guardarSetlistMaster(nuevaLista)
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

    private fun configurarDragAndDropSetlist() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.bindingAdapterPosition
                val toPosition = target.bindingAdapterPosition

                if (
                    fromPosition == RecyclerView.NO_POSITION ||
                    toPosition == RecyclerView.NO_POSITION
                ) {
                    return false
                }

                if (
                    matrizAdapter.esFilaNueva(fromPosition) ||
                    matrizAdapter.esFilaNueva(toPosition)
                ) {
                    return false
                }

                matrizAdapter.moverCancion(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                // No usamos swipe.
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                val nuevoOrden = matrizAdapter.obtenerCancionesActuales()

                if (nuevoOrden != cancionesSetlist) {
                    guardarNuevoOrdenSetlist(nuevoOrden)
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper?.attachToRecyclerView(rvMatrizSetlist)
    }

    private fun guardarNuevoOrdenSetlist(
        nuevoOrden: List<SetlistMasterItem>
    ) {
        lifecycleScope.launch {
            try {
                showDetailRepository.actualizarSetlistMaster(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    nuevoSetlistMaster = nuevoOrden
                )

                cancionesSetlist = nuevoOrden
                actualizarMatriz()

                Toast.makeText(
                    this@ShowDetailActivity,
                    "Orden actualizado",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ShowDetailActivity,
                    "Error al actualizar orden: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                actualizarMatriz()
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
                    Toast.makeText(
                        this,
                        "Ingresá un nombre",
                        Toast.LENGTH_SHORT
                    ).show()
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
}