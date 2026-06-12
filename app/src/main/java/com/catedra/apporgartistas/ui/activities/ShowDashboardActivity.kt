package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
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
import com.catedra.apporgartistas.ui.adapters.ShowAdapter
import com.catedra.apporgartistas.viewmodels.ShowsDashboardViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ShowsDashboardActivity : AppCompatActivity() {

    private val viewModel: ShowsDashboardViewModel by viewModels()
    private lateinit var adapter: ShowAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var selectionToolbar: MaterialToolbar

    private var showsActuales: List<Show> = emptyList()
    // Guardamos IDs seleccionados para alternar seleccion sin modificar el modelo Show.
    private val showsSeleccionadosIds = mutableSetOf<String>()

    private lateinit var agrupacionId: String
    private lateinit var agrupacionNombre: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shows_dashboard)

        agrupacionId = intent.getStringExtra("AGRUPACION_ID") ?: return finish()
        agrupacionNombre = intent.getStringExtra("AGRUPACION_NOMBRE")
            ?: getString(R.string.default_show_agrupacion)

        progressBar = findViewById(R.id.progressBarShowsDashboard)
        configurarCabecera()
        configurarSelectionToolbar()
        configurarFab()
        configurarRecyclerView()
        observarViewModel()

        viewModel.cargarShows(agrupacionId)
    }

    private fun configurarCabecera() {
        val tvBack = findViewById<TextView>(R.id.tvBackHeader)
        tvBack.text = getString(R.string.text_shows_back_agrupacion, agrupacionNombre)
        tvBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun configurarSelectionToolbar() {
        selectionToolbar = findViewById(R.id.selection_toolbar_shows)
        tintarIconosSelectionToolbar()
        selectionToolbar.setNavigationOnClickListener {
            limpiarSeleccionShows()
        }
        selectionToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    editarShowSeleccionado()
                    true
                }
                R.id.action_delete -> {
                    confirmarBorradoSeleccionado()
                    true
                }
                else -> false
            }
        }
    }

    private fun tintarIconosSelectionToolbar() {
        for (index in 0 until selectionToolbar.menu.size()) {
            selectionToolbar.menu.getItem(index).icon?.setTint(Color.WHITE)
        }
    }

    private fun configurarFab() {
        findViewById<FloatingActionButton>(R.id.fab_add_show).setOnClickListener {
            mostrarDialogoShow(null)
        }
    }

    private fun mostrarDialogoShow(showExistente: Show?) {
        val isEditMode = showExistente != null

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputNombre = EditText(this).apply {
            hint = getString(R.string.hint_show_nombre)
            setText(showExistente?.nombre ?: "")
        }

        val inputFecha = EditText(this).apply {
            hint = getString(R.string.hint_show_fecha)
            setText(showExistente?.fecha ?: "")
        }

        layout.addView(inputNombre)
        layout.addView(inputFecha)

        AlertDialog.Builder(this)
            .setTitle(if (isEditMode) R.string.title_show_editar else R.string.title_show_nuevo)
            .setView(layout)
            .setPositiveButton(
                if (isEditMode) getString(R.string.btn_common_guardar) else getString(R.string.btn_common_crear)
            ) { _, _ ->
                val nombre = inputNombre.text.toString().trim()
                val fecha = inputFecha.text.toString().trim().takeIf { it.isNotEmpty() }

                if (nombre.isNotEmpty()) {
                    if (isEditMode) {
                        viewModel.editarShow(agrupacionId, showExistente!!.id, nombre, fecha)
                    } else {
                        viewModel.crearShow(agrupacionId, nombre, fecha)
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.message_show_nombre_obligatorio),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_common_cancelar), null)
            .show()
    }

    private fun observarViewModel() {
        viewModel.shows.observe(this) { lista ->
            showsActuales = lista
            showsSeleccionadosIds.retainAll(lista.map { it.id }.toSet())
            adapter.actualizarLista(lista)
            actualizarEstadoSeleccionShows()
        }

        viewModel.mensaje.observe(this) { mensajeResId ->
            if (mensajeResId != null) {
                Toast.makeText(this, getString(mensajeResId), Toast.LENGTH_SHORT).show()
                viewModel.limpiarMensaje()
            }
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
                if (haySeleccionActiva()) {
                    alternarSeleccionShow(show)
                } else {
                    val intent = Intent(this, ShowDetailActivity::class.java).apply {
                        putExtra("SHOW_ID", show.id)
                        putExtra("AGRUPACION_ID", agrupacionId)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { show ->
                alternarSeleccionShow(show)
            }
        )
        rvShows.adapter = adapter
    }

    private fun haySeleccionActiva(): Boolean {
        return showsSeleccionadosIds.isNotEmpty()
    }

    private fun alternarSeleccionShow(show: Show) {
        if (show.id.isBlank()) return

        if (showsSeleccionadosIds.contains(show.id)) {
            showsSeleccionadosIds.remove(show.id)
        } else {
            showsSeleccionadosIds.add(show.id)
        }

        actualizarEstadoSeleccionShows()
    }

    private fun actualizarEstadoSeleccionShows() {
        val cantidad = showsSeleccionadosIds.size
        adapter.actualizarSeleccion(showsSeleccionadosIds)

        if (cantidad == 0) {
            selectionToolbar.visibility = View.GONE
            return
        }

        selectionToolbar.title = resources.getQuantityString(
            R.plurals.cantidad_items_seleccionados,
            cantidad,
            cantidad
        )
        selectionToolbar.menu.findItem(R.id.action_edit)?.isVisible = cantidad == 1
        selectionToolbar.menu.findItem(R.id.action_delete)?.isVisible = true
        selectionToolbar.visibility = View.VISIBLE
    }

    private fun limpiarSeleccionShows() {
        showsSeleccionadosIds.clear()
        actualizarEstadoSeleccionShows()
    }

    private fun editarShowSeleccionado() {
        val show = showsActuales.firstOrNull { it.id == showsSeleccionadosIds.firstOrNull() }
            ?: return

        limpiarSeleccionShows()
        mostrarDialogoShow(show)
    }

    private fun confirmarBorradoSeleccionado() {
        val idsSeleccionados = showsSeleccionadosIds.toList()
        val cantidad = idsSeleccionados.size

        if (cantidad == 0) return

        AlertDialog.Builder(this)
            .setTitle(R.string.title_show_borrar)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.message_shows_confirmar_borrado_multiple,
                    cantidad,
                    cantidad
                )
            )
            .setPositiveButton(getString(R.string.btn_common_borrar)) { _, _ ->
                viewModel.borrarShows(agrupacionId, idsSeleccionados)
                limpiarSeleccionShows()
            }
            .setNegativeButton(getString(R.string.btn_common_cancelar), null)
            .show()
    }
}
