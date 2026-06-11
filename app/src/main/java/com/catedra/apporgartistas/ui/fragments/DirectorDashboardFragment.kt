package com.catedra.apporgartistas.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.ui.activities.ShowsDashboardActivity
import com.catedra.apporgartistas.ui.adapters.AgrupacionAdapter
import com.catedra.apporgartistas.viewmodels.DirectorDashboardViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DirectorDashboardFragment : Fragment(R.layout.fragment_director_dashboard) {

    private var actionMode: ActionMode? = null
    private var agrupacionSeleccionadaParaBorrar: Agrupacion? = null
    private val viewModel: DirectorDashboardViewModel by viewModels()
    private lateinit var adapter: AgrupacionAdapter

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_borrar_setlist, menu)
            mode.title = "1 seleccionado"
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    confirmarBorrado(mode)
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            agrupacionSeleccionadaParaBorrar = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView(view)
        observarViewModel()
        configurarFab(view)

        viewModel.cargarAgrupaciones()
    }

    private fun configurarRecyclerView(view: View) {
        val rvAgrupaciones = view.findViewById<RecyclerView>(R.id.rvAgrupaciones)
        rvAgrupaciones.layoutManager = LinearLayoutManager(requireContext())

        adapter = AgrupacionAdapter(
            agrupaciones = emptyList(),
            onItemClick = { agrupacion ->
                if (actionMode == null) {
                    val intent = Intent(requireContext(), ShowsDashboardActivity::class.java).apply {
                        putExtra("AGRUPACION_ID", agrupacion.id)
                        putExtra("AGRUPACION_NOMBRE", agrupacion.nombre)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { agrupacion ->
                if (actionMode == null) {
                    agrupacionSeleccionadaParaBorrar = agrupacion
                    actionMode = (requireActivity() as AppCompatActivity)
                        .startSupportActionMode(actionModeCallback)
                }
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
            hint = "Ej: Orquesta Sinfónica"
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Nueva Agrupación")
            .setMessage("Ingresá el nombre de la banda u orquesta")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    viewModel.crearAgrupacion(nombre)
                } else {
                    Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarBorrado(mode: ActionMode) {
        val agrupacion = agrupacionSeleccionadaParaBorrar ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Borrar Agrupación")
            .setMessage("¿Estás seguro de que querés borrar '${agrupacion.nombre}'? Se perderán todos sus shows.")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.borrarAgrupacion(agrupacion.id)
                mode.finish()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                mode.finish()
            }
            .show()
    }

    private fun observarViewModel() {
        viewModel.agrupaciones.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.mensaje.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        actionMode?.finish()
        actionMode = null
        super.onDestroyView()
    }
}
