package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.ui.adapters.AgrupacionAdapter
import com.catedra.apporgartistas.ui.adapters.ShowAdapter
import com.catedra.apporgartistas.viewmodels.ShowsDashboardViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton


class ShowsDashboardActivity : AppCompatActivity() {

    private val viewModel: ShowsDashboardViewModel by viewModels()
    private lateinit var adapter: ShowAdapter
    private lateinit var progressBar: ProgressBar

    private var actionMode: androidx.appcompat.view.ActionMode? = null
    private var showSeleccionado: Show? = null

    // Variables que recibimos por Intent
    private lateinit var agrupacionId: String
    private lateinit var agrupacionNombre: String

    private val actionModeCallback = object : androidx.appcompat.view.ActionMode.Callback {
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_editar_borrar, menu) // El menú nuevo
            mode.title = "1 seleccionado"
            return true
        }

        override fun onPrepareActionMode(mode: androidx.appcompat.view.ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: androidx.appcompat.view.ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_edit -> {
                    mostrarDialogoShow(showSeleccionado)
                    mode.finish()
                    true
                }
                R.id.action_delete -> {
                    confirmarBorrado(mode)
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode) {
            actionMode = null
            showSeleccionado = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shows_dashboard) // Tu nuevo layout

        // Recuperar datos pasados por Intent
        agrupacionId = intent.getStringExtra("AGRUPACION_ID") ?: return finish()
        agrupacionNombre = intent.getStringExtra("AGRUPACION_NOMBRE") ?: "Agrupación"

        progressBar = findViewById(R.id.progressBarShowsDashboard)
        configurarCabecera()
        configurarFab()
        configurarRecyclerView()
        observarViewModel()

        viewModel.cargarShows(agrupacionId)
    }

    private fun configurarCabecera() {
        val tvBack = findViewById<TextView>(R.id.tvBackHeader)
        tvBack.text = "← $agrupacionNombre"
        tvBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Vuelve a la pantalla anterior
        }
    }

    private fun configurarFab() {
        findViewById<FloatingActionButton>(R.id.fab_add_show).setOnClickListener {
            mostrarDialogoShow(null) // Pasamos null porque es un show nuevo
        }
    }

    // Usamos el mismo diálogo para CREAR y EDITAR
    private fun mostrarDialogoShow(showExistente: Show?) {
        val context = this
        val isEditMode = showExistente != null

        // Layout por código para poner dos EditTexts
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputNombre = EditText(context).apply {
            hint = "Nombre del show (Ej: Concierto Aniversario)"
            setText(showExistente?.nombre ?: "")
        }

        val inputFecha = EditText(context).apply {
            hint = "Fecha (Opcional - Ej: 24/09/2026)"
            setText(showExistente?.fecha ?: "")
        }

        layout.addView(inputNombre)
        layout.addView(inputFecha)

        AlertDialog.Builder(context)
            .setTitle(if (isEditMode) "Editar Show" else "Nuevo Show")
            .setView(layout)
            .setPositiveButton(if (isEditMode) "Guardar" else "Crear") { _, _ ->
                val nombre = inputNombre.text.toString().trim()
                var fecha: String? = inputFecha.text.toString().trim()
                if (fecha!!.isEmpty()) fecha = null // Convertimos vacío a null

                if (nombre.isNotEmpty()) {
                    if (isEditMode) {
                        viewModel.editarShow(agrupacionId, showExistente!!.id, nombre, fecha)
                    } else {
                        viewModel.crearShow(agrupacionId, nombre, fecha)
                    }
                } else {
                    Toast.makeText(context, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarBorrado(mode: androidx.appcompat.view.ActionMode) {
        val show = showSeleccionado ?: return
        AlertDialog.Builder(this)
            .setTitle("Borrar Show")
            .setMessage("¿Querés borrar '${show.nombre}'? Se perderán todas sus partituras e instrumentos.")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.borrarShow(agrupacionId, show.id)
                mode.finish()
            }
            .setNegativeButton("Cancelar") { _, _ -> mode.finish() }
            .show()
    }

    private fun observarViewModel() {
        viewModel.shows.observe(this) { lista ->
            android.util.Log.d("SHOWS_DEBUG", "Activity recibió ${lista.size} shows")
            adapter.actualizarLista(lista)
        }

        viewModel.mensaje.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.isLoading.observe(this) { loading ->
            mostrarLoading(loading)
        }
    }

    private fun mostrarLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun configurarRecyclerView() {
        val rvShows = findViewById<RecyclerView>(R.id.rvShows)
        rvShows.layoutManager = LinearLayoutManager(this)

        adapter = ShowAdapter(
            shows = emptyList(),
            onItemClick = { show ->
                if (actionMode == null) {
                    val intent = Intent(this, ShowDetailActivity::class.java)

                    intent.putExtra("SHOW_ID", show.id)
                    intent.putExtra("AGRUPACION_ID", agrupacionId)

                    startActivity(intent)
                }
            },
            onItemLongClick = { show ->
                if (actionMode == null) {
                    showSeleccionado = show
                    // Esto abre la barra superior de borrado que configuraste arriba
                    actionMode = startSupportActionMode(actionModeCallback)
                }
            }
        )
        rvShows.adapter = adapter
    }
}
