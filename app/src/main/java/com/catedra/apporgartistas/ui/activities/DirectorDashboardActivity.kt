// DirectorDashboardActivity.kt
package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.ui.adapters.AgrupacionAdapter
import com.catedra.apporgartistas.viewmodels.DirectorDashboardViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DirectorDashboardActivity : AppCompatActivity() {
    private var actionMode: androidx.appcompat.view.ActionMode? = null
    private var agrupacionSeleccionadaParaBorrar: Agrupacion? = null
    private val viewModel: DirectorDashboardViewModel by viewModels()
    private lateinit var adapter: AgrupacionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_director_dashboard)

        configurarRecyclerView()
        observarViewModel()
        configurarFab()
        configurarBottomNavigation()

        viewModel.cargarAgrupaciones()
    }
    private val actionModeCallback = object : androidx.appcompat.view.ActionMode.Callback {
        override fun onCreateActionMode(mode: androidx.appcompat.view.ActionMode, menu: Menu): Boolean {
            // Reutilizamos el menú de borrado que ya tenías creado
            mode.menuInflater.inflate(R.menu.menu_borrar_setlist, menu)
            mode.title = "1 seleccionado"
            return true
        }

        override fun onPrepareActionMode(mode: androidx.appcompat.view.ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: androidx.appcompat.view.ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.action_delete -> {
                    // Llama a la función que confirma si querés borrar
                    confirmarBorrado(mode)
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: androidx.appcompat.view.ActionMode) {
            // Limpiamos las variables cuando se cierra la barra
            actionMode = null
            agrupacionSeleccionadaParaBorrar = null
        }
    }

    private fun configurarRecyclerView() {
        val rvAgrupaciones = findViewById<RecyclerView>(R.id.rvAgrupaciones)
        rvAgrupaciones.layoutManager = LinearLayoutManager(this)

        adapter = AgrupacionAdapter(
            agrupaciones = emptyList(),
            onItemClick = { agrupacion ->
                if (actionMode == null) {
                    // Lanzamos la vista de Shows pasando el ID y Nombre
                    val intent = Intent(this, ShowsDashboardActivity::class.java).apply {
                        putExtra("AGRUPACION_ID", agrupacion.id)
                        putExtra("AGRUPACION_NOMBRE", agrupacion.nombre)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { agrupacion ->
                if (actionMode == null) {
                    agrupacionSeleccionadaParaBorrar = agrupacion
                    actionMode = startSupportActionMode(actionModeCallback)
                }
            }
        )
        rvAgrupaciones.adapter = adapter
    }

    private fun configurarFab() {
        val fabAdd = findViewById<FloatingActionButton>(R.id.fab_add_agrupacion)
        fabAdd.setOnClickListener {
            mostrarDialogoCrearAgrupacion()
        }
    }

    private fun mostrarDialogoCrearAgrupacion() {
        val input = EditText(this).apply {
            hint = "Ej: Orquesta Sinfónica"
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        AlertDialog.Builder(this)
            .setTitle("Nueva Agrupación")
            .setMessage("Ingresá el nombre de la banda u orquesta")
            .setView(input)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = input.text.toString().trim()
                if (nombre.isNotEmpty()) {
                    viewModel.crearAgrupacion(nombre)
                } else {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarBorrado(mode: androidx.appcompat.view.ActionMode) {
        // Recuperamos la agrupación que guardamos al mantener presionado el ítem
        val agrupacion = agrupacionSeleccionadaParaBorrar ?: return

        AlertDialog.Builder(this)
            .setTitle("Borrar Agrupación")
            .setMessage("¿Estás seguro de que querés borrar '${agrupacion.nombre}'? Se perderán todos sus shows.")
            .setPositiveButton("Borrar") { _, _ ->
                // Le pasamos el ID correcto de la agrupación recuperada al ViewModel
                viewModel.borrarAgrupacion(agrupacion.id)
                mode.finish() // Cierra la barra superior automáticamente
            }
            .setNegativeButton("Cancelar") { _, _ ->
                mode.finish() // Cierra la barra superior si te arrepentís
            }
            .show()
    }

    private fun observarViewModel() {
        viewModel.agrupaciones.observe(this) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.mensaje.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Seleccionamos visualmente la pestaña correspondiente (asumiendo que su ID es nav_shared)
        bottomNav.selectedItemId = R.id.item_2 // Reemplazar por tu ID real del menú

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_1 -> { // Reemplazar por el ID de tu pestaña Home
                    val intent = Intent(this, SetlistDashboardActivity::class.java)
                    // Evitamos crear múltiples instancias de la Activity Home
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    overridePendingTransition(0, 0) // Quita la animación por defecto para simular que es la misma pantalla
                    true
                }
                R.id.item_2 -> {
                    // Ya estamos acá
                    true
                }
                // R.id.nav_mine -> { ... }
                else -> false
            }
        }
    }
}