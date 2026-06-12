package com.catedra.apporgartistas.ui.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.ui.activities.ShowsDashboardActivity
import com.catedra.apporgartistas.ui.adapters.AgrupacionAdapter
import com.catedra.apporgartistas.viewmodels.DirectorDashboardViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DirectorDashboardFragment : Fragment(R.layout.fragment_director_dashboard) {

    private val viewModel: DirectorDashboardViewModel by viewModels()
    private lateinit var adapter: AgrupacionAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var selectionToolbar: MaterialToolbar

    private var agrupacionesActuales: List<Agrupacion> = emptyList()
    // Guardamos IDs para que la seleccion sobreviva a rebindeos del RecyclerView.
    private val agrupacionesSeleccionadasIds = mutableSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.progressBarDirectorDashboard)
        configurarSelectionToolbar(view)
        configurarRecyclerView(view)
        observarViewModel()
        configurarFab(view)

        viewModel.cargarAgrupaciones()
    }

    private fun configurarSelectionToolbar(view: View) {
        selectionToolbar = view.findViewById(R.id.selection_toolbar_agrupaciones)
        for (index in 0 until selectionToolbar.menu.size()) {
            selectionToolbar.menu.getItem(index).icon?.setTint(Color.WHITE)
        }
        selectionToolbar.setNavigationOnClickListener {
            limpiarSeleccionAgrupaciones()
        }
        selectionToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    editarAgrupacionSeleccionada()
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

    private fun configurarRecyclerView(view: View) {
        val rvAgrupaciones = view.findViewById<RecyclerView>(R.id.rvAgrupaciones)
        rvAgrupaciones.layoutManager = LinearLayoutManager(requireContext())

        adapter = AgrupacionAdapter(
            agrupaciones = emptyList(),
            onItemClick = { agrupacion ->
                if (haySeleccionActiva()) {
                    alternarSeleccionAgrupacion(agrupacion)
                } else {
                    val intent = Intent(requireContext(), ShowsDashboardActivity::class.java).apply {
                        putExtra("AGRUPACION_ID", agrupacion.id)
                        putExtra("AGRUPACION_NOMBRE", agrupacion.nombre)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { agrupacion ->
                alternarSeleccionAgrupacion(agrupacion)
            }
        )
        rvAgrupaciones.adapter = adapter
    }

    private fun configurarFab(view: View) {
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fab_add_agrupacion)
        fabAdd.setOnClickListener {
            mostrarDialogoCrearAgrupacion()
        }
    }

    private fun mostrarDialogoCrearAgrupacion() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.hint_director_ej_orquesta_sinfonica)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_director_nueva_agrupacion)
            .setMessage(R.string.message_director_ingresa_nombre_banda_orquesta)
            .setView(input)
            .setPositiveButton(getString(R.string.btn_common_crear)) { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    viewModel.crearAgrupacion(nombre)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.message_director_nombre_vacio),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_common_cancelar), null)
            .show()
    }

    private fun mostrarDialogoEditarAgrupacion(agrupacion: Agrupacion) {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.hint_director_nombre_agrupacion)
            setText(agrupacion.nombre)
            selectAll()
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_director_editar_agrupacion)
            .setMessage(R.string.message_director_actualiza_nombre_agrupacion)
            .setView(input)
            .setPositiveButton(getString(R.string.btn_common_guardar)) { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    viewModel.editarAgrupacion(agrupacion.id, nombre)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.message_director_nombre_vacio),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_common_cancelar), null)
            .show()
    }

    private fun observarViewModel() {
        viewModel.agrupaciones.observe(viewLifecycleOwner) { lista ->
            agrupacionesActuales = lista.map { it.agrupacion }
            agrupacionesSeleccionadasIds.retainAll(agrupacionesActuales.map { it.id }.toSet())
            adapter.actualizarLista(lista)
            actualizarEstadoSeleccionAgrupaciones()
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { mensajeResId ->
            if (mensajeResId != null) {
                Toast.makeText(requireContext(), getString(mensajeResId), Toast.LENGTH_SHORT).show()
                viewModel.limpiarMensaje()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            mostrarLoading(loading)
        }
    }

    private fun haySeleccionActiva(): Boolean {
        return agrupacionesSeleccionadasIds.isNotEmpty()
    }

    private fun alternarSeleccionAgrupacion(agrupacion: Agrupacion) {
        if (agrupacion.id.isBlank()) return

        if (agrupacionesSeleccionadasIds.contains(agrupacion.id)) {
            agrupacionesSeleccionadasIds.remove(agrupacion.id)
        } else {
            agrupacionesSeleccionadasIds.add(agrupacion.id)
        }

        actualizarEstadoSeleccionAgrupaciones()
    }

    private fun actualizarEstadoSeleccionAgrupaciones() {
        val cantidad = agrupacionesSeleccionadasIds.size
        adapter.actualizarSeleccion(agrupacionesSeleccionadasIds)

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

    private fun limpiarSeleccionAgrupaciones() {
        agrupacionesSeleccionadasIds.clear()
        actualizarEstadoSeleccionAgrupaciones()
    }

    private fun editarAgrupacionSeleccionada() {
        val agrupacion = agrupacionesActuales.firstOrNull {
            it.id == agrupacionesSeleccionadasIds.firstOrNull()
        } ?: return

        limpiarSeleccionAgrupaciones()
        mostrarDialogoEditarAgrupacion(agrupacion)
    }

    private fun confirmarBorradoSeleccionado() {
        val idsSeleccionados = agrupacionesSeleccionadasIds.toList()
        val cantidad = idsSeleccionados.size

        if (cantidad == 0) return

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_director_borrar_agrupacion)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.message_director_confirmar_borrado_multiple_agrupaciones,
                    cantidad,
                    cantidad
                )
            )
            .setPositiveButton(getString(R.string.btn_common_borrar)) { _, _ ->
                viewModel.borrarAgrupaciones(idsSeleccionados)
                limpiarSeleccionAgrupaciones()
            }
            .setNegativeButton(getString(R.string.btn_common_cancelar), null)
            .show()
    }

    override fun onDestroyView() {
        agrupacionesSeleccionadasIds.clear()
        super.onDestroyView()
    }

    private fun mostrarLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
