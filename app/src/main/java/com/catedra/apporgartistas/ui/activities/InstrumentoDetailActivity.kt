package com.catedra.apporgartistas.ui.activities

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.services.AuthService
import com.catedra.apporgartistas.ui.adapters.InstrumentoPartituraAdapter
import com.catedra.apporgartistas.utils.CloudinaryManager
import com.catedra.apporgartistas.utils.InstrumentoRepository
import com.catedra.apporgartistas.utils.ShowDetailRepository
import kotlinx.coroutines.launch

class InstrumentoDetailActivity : AppCompatActivity() {
    private var codigoCompartirInstrumento: String = ""

    private lateinit var agrupacionId: String
    private lateinit var showId: String
    private lateinit var instrumentoId: String

    private val showRepository = ShowDetailRepository()
    private val instrumentoRepository = InstrumentoRepository()
    private val authService = AuthService()

    private var showActual: Show? = null
    private var setlistMaster: List<SetlistMasterItem> = emptyList()

    private lateinit var adapter: InstrumentoPartituraAdapter
    private var itemTouchHelper: ItemTouchHelper? = null

    private var dragFromPosition: Int = -1
    private var dragToPosition: Int = -1

    private var setlistItemSeleccionadoParaPdf: SetlistMasterItem? = null

    private val cloudinaryManager = CloudinaryManager(uploadPreset = "upload_from_local")

    private var pdfsLocales: MutableMap<String, PartituraCloud> = mutableMapOf()
    private var hayCambiosPendientes = false
    private var ignorarProximaActualizacionInstrumento = false
    private lateinit var progressBar: ProgressBar
    private var operacionesLoading = 0
    private var showInicialRecibido = false
    private var instrumentoInicialRecibido = false

    private val seleccionarPdfLocalLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult

            val setlistItem = setlistItemSeleccionadoParaPdf ?: return@registerForActivityResult
            subirPdfLocal(setlistItem, uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instrumento_detail)

        agrupacionId = intent.getStringExtra("AGRUPACION_ID") ?: return finish()
        showId = intent.getStringExtra("SHOW_ID") ?: return finish()
        instrumentoId = intent.getStringExtra("INSTRUMENTO_ID") ?: return finish()

        progressBar = findViewById(R.id.progressBarInstrumentoDetail)
        configurarBack()
        configurarBotonGuardar()
        configurarRecycler()
        configurarDragAndDrop()
        observarDatos()
        cargarOCrearCodigoCompartir()
    }

    private fun configurarBack() {
        findViewById<TextView>(R.id.tvBackInstrumento).setOnClickListener {
            if (hayCambiosPendientes) {
                confirmarSalidaConCambios()
            } else {
                finish()
            }
        }
    }

    private fun configurarBotonGuardar() {
        findViewById<Button>(R.id.btnGuardarCambiosPartituras).setOnClickListener {
            guardarCambiosPendientes()
        }

        actualizarEstadoBotonGuardar()
    }

    private fun actualizarEstadoBotonGuardar() {
        val btnGuardar = findViewById<Button>(R.id.btnGuardarCambiosPartituras)

        btnGuardar.isEnabled = hayCambiosPendientes
        btnGuardar.alpha = if (hayCambiosPendientes) 1f else 0.5f
    }

    private fun configurarRecycler() {
        val rv = findViewById<RecyclerView>(R.id.rvPartiturasInstrumento)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = InstrumentoPartituraAdapter(
            setlistMaster = mutableListOf(),
            pdfsPorSetlistItem = emptyMap(),

            onStartDrag = { viewHolder ->
                itemTouchHelper?.startDrag(viewHolder)
            },

            onEliminarClick = { setlistItem ->
                eliminarArchivoLocalmente(setlistItem)
            },

            onArchivoClick = { setlistItem ->
                mostrarOpcionesParaPartitura(setlistItem)
            }
        )

        rv.adapter = adapter
    }

    private fun configurarDragAndDrop() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition

                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                    return false
                }

                if (dragFromPosition == -1) {
                    dragFromPosition = from
                }

                dragToPosition = to

                adapter.moverVisualmente(from, to)
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

                val from = dragFromPosition
                val to = dragToPosition

                dragFromPosition = -1
                dragToPosition = -1

                if (from != -1 && to != -1 && from != to) {
                    intercambiarArchivosLocalmente(from, to)
                } else {
                    adapter.actualizarDatos(setlistMaster, pdfsLocales)
                }
            }

            override fun isLongPressDragEnabled(): Boolean {
                return false
            }
        }

        itemTouchHelper = ItemTouchHelper(callback)

        val rv = findViewById<RecyclerView>(R.id.rvPartiturasInstrumento)
        itemTouchHelper?.attachToRecyclerView(rv)
    }

    private fun observarDatos() {
        iniciarLoading()
        iniciarLoading()

        showRepository.observarShow(
            agrupacionId = agrupacionId,
            showId = showId,
            onChange = { show ->
                if (!showInicialRecibido) {
                    showInicialRecibido = true
                    finalizarLoading()
                }

                showActual = show
                setlistMaster = show?.setlistMaster ?: emptyList()

                actualizarCabecera()
                adapter.actualizarDatos(setlistMaster, pdfsLocales)
            },
            onError = {
                if (!showInicialRecibido) {
                    showInicialRecibido = true
                    finalizarLoading()
                }
                Toast.makeText(this, "Error al observar show", Toast.LENGTH_SHORT).show()
            }
        )

        instrumentoRepository.observarInstrumento(
            agrupacionId = agrupacionId,
            showId = showId,
            instrumentoId = instrumentoId,
            onChange = { instrumento ->
                if (!instrumentoInicialRecibido) {
                    instrumentoInicialRecibido = true
                    finalizarLoading()
                }

                if (instrumento == null) return@observarInstrumento

                actualizarInstrumento(
                    nombreInstrumento = instrumento.nombre,
                    codigoAcceso = instrumento.codigoAcceso
                )

                if (!hayCambiosPendientes) {
                    pdfsLocales = instrumento.pdfsPorSetlistItem.toMutableMap()
                    adapter.actualizarDatos(setlistMaster, pdfsLocales)
                } else if (ignorarProximaActualizacionInstrumento) {
                    ignorarProximaActualizacionInstrumento = false
                }
            },
            onError = {
                if (!instrumentoInicialRecibido) {
                    instrumentoInicialRecibido = true
                    finalizarLoading()
                }
                Toast.makeText(this, "Error al observar instrumento", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun actualizarCabecera() {
        val show = showActual ?: return

        findViewById<TextView>(R.id.tvTituloShowInstrumento).text = show.nombre
        findViewById<TextView>(R.id.tvFechaShowInstrumento).text = show.fecha ?: "Sin fecha"
    }

    private fun actualizarInstrumento(
        nombreInstrumento: String,
        codigoAcceso: String
    ) {
        findViewById<TextView>(R.id.tvNombreInstrumentoDetalle).text = nombreInstrumento
    }

    private fun mostrarOpcionesParaPartitura(setlistItem: SetlistMasterItem) {
        val opciones = arrayOf(
            "Subir archivo local",
            "Elegir partitura ya subida"
        )

        AlertDialog.Builder(this)
            .setTitle("Partitura para ${setlistItem.nombre}")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> seleccionarPdfLocal(setlistItem)
                    1 -> elegirPartituraYaSubida(setlistItem)
                }
            }
            .show()
    }

    private fun seleccionarPdfLocal(setlistItem: SetlistMasterItem) {
        setlistItemSeleccionadoParaPdf = setlistItem
        seleccionarPdfLocalLauncher.launch("application/pdf")
    }

    private fun subirPdfLocal(setlistItem: SetlistMasterItem, uri: Uri) {
        val userId = authService.getCurrentUserId()

        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val nombreArchivo = obtenerNombreArchivo(uri)

        Toast.makeText(this, "Subiendo PDF...", Toast.LENGTH_SHORT).show()
        iniciarLoading()

        cloudinaryManager.subirPartitura(
            fileUri = uri,
            userId = userId,
            onSuccess = { urlSegura, publicId ->
                val partitura = PartituraCloud(
                    nombre = nombreArchivo,
                    url = urlSegura,
                    publicId = publicId
                )

                asociarPartituraLocalmente(setlistItem, partitura)
                finalizarLoading()

                Toast.makeText(
                    this,
                    "PDF subido. Tocá Guardar para confirmar.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { mensaje ->
                finalizarLoading()
                Toast.makeText(
                    this,
                    "Error al subir PDF: $mensaje",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun elegirPartituraYaSubida(setlistItem: SetlistMasterItem) {
        lifecycleScope.launch {
            try {
                val userId = authService.getCurrentUserId()

                if (userId == null) {
                    Toast.makeText(
                        this@InstrumentoDetailActivity,
                        "Usuario no autenticado",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                iniciarLoading()
                val partituras = instrumentoRepository.obtenerTodasLasPartiturasCloudDelUsuario(userId)

                if (partituras.isEmpty()) {
                    Toast.makeText(
                        this@InstrumentoDetailActivity,
                        "No tenés partituras subidas todavía",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val nombres = partituras.map { it.nombre }.toTypedArray()

                AlertDialog.Builder(this@InstrumentoDetailActivity)
                    .setTitle("Elegir partitura")
                    .setItems(nombres) { _, index ->
                        val partituraElegida = partituras[index]
                        asociarPartituraLocalmente(setlistItem, partituraElegida)
                    }
                    .show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@InstrumentoDetailActivity,
                    "Error al cargar partituras: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                finalizarLoading()
            }
        }
    }

    private fun asociarPartituraLocalmente(
        setlistItem: SetlistMasterItem,
        partitura: PartituraCloud
    ) {
        pdfsLocales[setlistItem.id] = partitura
        marcarCambiosPendientes()
    }

    private fun eliminarArchivoLocalmente(setlistItem: SetlistMasterItem) {
        val existePartitura = pdfsLocales.containsKey(setlistItem.id)

        if (!existePartitura) {
            Toast.makeText(this, "Esta canción ya está en Tacet", Toast.LENGTH_SHORT).show()
            return
        }

        pdfsLocales.remove(setlistItem.id)
        marcarCambiosPendientes()
    }

    private fun intercambiarArchivosLocalmente(fromPosition: Int, toPosition: Int) {
        val fromItem = setlistMaster.getOrNull(fromPosition) ?: return
        val toItem = setlistMaster.getOrNull(toPosition) ?: return

        val partituraFrom = pdfsLocales[fromItem.id]
        val partituraTo = pdfsLocales[toItem.id]

        if (partituraTo != null) {
            pdfsLocales[fromItem.id] = partituraTo
        } else {
            pdfsLocales.remove(fromItem.id)
        }

        if (partituraFrom != null) {
            pdfsLocales[toItem.id] = partituraFrom
        } else {
            pdfsLocales.remove(toItem.id)
        }

        marcarCambiosPendientes()
    }

    private fun marcarCambiosPendientes() {
        hayCambiosPendientes = true
        adapter.actualizarDatos(setlistMaster, pdfsLocales)
        actualizarEstadoBotonGuardar()
    }

    private fun guardarCambiosPendientes() {
        lifecycleScope.launch {
            iniciarLoading()

            try {
                instrumentoRepository.guardarMapaPartiturasInstrumento(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    instrumentoId = instrumentoId,
                    nuevoMapa = pdfsLocales
                )

                hayCambiosPendientes = false
                ignorarProximaActualizacionInstrumento = true

                actualizarEstadoBotonGuardar()

                Toast.makeText(
                    this@InstrumentoDetailActivity,
                    "Cambios guardados correctamente",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@InstrumentoDetailActivity,
                    "Error al guardar cambios: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                finalizarLoading()
            }
        }
    }

    private fun confirmarSalidaConCambios() {
        AlertDialog.Builder(this)
            .setTitle("Cambios sin guardar")
            .setMessage("Tenés cambios pendientes. Si salís ahora, se perderán los cambios no guardados.")
            .setPositiveButton("Salir sin guardar") { _, _ ->
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun obtenerNombreArchivo(uri: Uri): String {
        var nombre = "partitura.pdf"

        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            val indiceNombre = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (indiceNombre >= 0 && it.moveToFirst()) {
                nombre = it.getString(indiceNombre)
            }
        }

        return nombre
    }
    private fun cargarOCrearCodigoCompartir() {
        lifecycleScope.launch {
            iniciarLoading()

            try {
                val directorId = authService.getCurrentUserId()

                if (directorId == null) {
                    Toast.makeText(
                        this@InstrumentoDetailActivity,
                        "Usuario no autenticado",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val codigo = instrumentoRepository.obtenerOCrearCodigoSetlistInstrumento(
                    agrupacionId = agrupacionId,
                    showId = showId,
                    instrumentoId = instrumentoId,
                    directorId = directorId
                )

                codigoCompartirInstrumento = codigo
                mostrarCodigoCompartir(codigo)

            } catch (e: Exception) {
                Toast.makeText(
                    this@InstrumentoDetailActivity,
                    "Error al cargar código: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                mostrarCodigoCompartir("")
            } finally {
                finalizarLoading()
            }
        }
    }

    private fun mostrarCodigoCompartir(codigo: String) {
        val codigoVisible = codigo.ifBlank { "Sin código" }

        findViewById<TextView>(R.id.tvCodigoInstrumento).text = "Código: $codigoVisible"

        findViewById<Button>(R.id.btnCopiarCodigoInstrumento).setOnClickListener {
            if (codigoCompartirInstrumento.isBlank()) {
                Toast.makeText(
                    this,
                    "Todavía no hay código para copiar",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(
                "Código de acceso",
                codigoCompartirInstrumento
            )

            clipboard.setPrimaryClip(clip)

            Toast.makeText(
                this,
                "Código copiado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun iniciarLoading() {
        operacionesLoading++
        progressBar.visibility = View.VISIBLE
    }

    private fun finalizarLoading() {
        if (operacionesLoading > 0) {
            operacionesLoading--
        }
        progressBar.visibility = if (operacionesLoading > 0) View.VISIBLE else View.GONE
    }
}
