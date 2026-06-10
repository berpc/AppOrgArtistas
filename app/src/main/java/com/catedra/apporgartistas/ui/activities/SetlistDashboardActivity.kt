package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode // OJO: Importá la de androidx.appcompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.ui.adapters.SetlistAdapter
import com.catedra.apporgartistas.viewmodels.SetlistDashboardViewModel
import com.catedra.apporgartistas.activities.LoginActivity
import com.catedra.apporgartistas.viewmodels.LoginViewModel
import androidx.appcompat.app.AlertDialog
import android.text.InputFilter
import android.view.View
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.app.ActivityOptionsCompat
import com.catedra.apporgartistas.ui.adapters.SetlistInstrumentoSuscriptoAdapter
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView


class SetlistDashboardActivity : AppCompatActivity() {
    private val viewModel: SetlistDashboardViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var adapter: SetlistAdapter
    private lateinit var adapterInstrumentos: SetlistInstrumentoSuscriptoAdapter
    private var listaSetlistsCompleta: List<Setlist> = emptyList()
    private var textoBusquedaActual: String = ""
    // Variables para el modo de selección
    private var actionMode: ActionMode? = null
    private var setlistSeleccionadoParaBorrar: Setlist? = null
    private var isFabExpanded = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            obtenerYGuardarToken()
        }
    }

    private fun obtenerYGuardarToken() {
        loginViewModel.obtenerYGuardarTokenFcm()
    }

    private fun verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                obtenerYGuardarToken()
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            obtenerYGuardarToken()
        }
    }

    private fun mostrarDialogoIngresarCodigo() {
        val input = android.widget.EditText(this).apply {
            hint = context.getString(R.string.hint_dashboard_ej_a7x9bq)
            // Forzamos mayúsculas y limitamos a 6 caracteres
            filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(6))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alerttitle_dashboard_unirse_a_un_setlist))
            .setMessage(getString(R.string.message_dashboard_ingresa_el_codigo_de_6_caracteres_que_te_compartio_el_director))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_dashboard_unirse)) { _, _ ->
                val codigo = input.text.toString().trim()
                if (codigo.length == 6) {
                    // Llamamos a la nueva función del ViewModel
                    viewModel.unirseASetlistConCodigo(codigo)
                } else {
                    Toast.makeText(this,
                        getString(R.string.message_dashboard_el_codigo_debe_tener_exactamente_6_caracteres), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_dashboard_cancelar), null)
            .show()
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflamos el tachito de basura
            mode.menuInflater.inflate(R.menu.menu_borrar_setlist, menu)
            mode.title = getString(R.string.title_dashboard_1_seleccionado)
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
            setlistSeleccionadoParaBorrar = null
            // Acá podrías decirle al adapter que quite el resaltado de fondo si lo tuvieras
        }
    }
    private fun configurarFabMenu() {
        val fabMain = findViewById<FloatingActionButton>(R.id.fab_main)

        // Capturamos los contenedores completos usando tus IDs
        val btnNuevoSetlist = findViewById<View>(R.id.btnNuevoSetlist)
        val btnUnirseSetlist = findViewById<View>(R.id.btnUnirseSetlist)

        fabMain.setOnClickListener {
            isFabExpanded = !isFabExpanded

            if (isFabExpanded) {
                // Abrir menú: hacemos visibles los contenedores
                btnNuevoSetlist.visibility = View.VISIBLE
                btnUnirseSetlist.visibility = View.VISIBLE

                btnNuevoSetlist.alpha = 0f
                btnUnirseSetlist.alpha = 0f
                btnNuevoSetlist.scaleX = 0.5f; btnNuevoSetlist.scaleY = 0.5f
                btnUnirseSetlist.scaleX = 0.5f; btnUnirseSetlist.scaleY = 0.5f

                // Animamos todo junto (botón + texto)
                btnNuevoSetlist.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                btnUnirseSetlist.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                fabMain.animate().rotation(45f).setDuration(200).start()
            } else {
                // Cerrar menú
                btnNuevoSetlist.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(200).start()
                btnUnirseSetlist.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(200).start()
                fabMain.animate().rotation(0f).setDuration(200).start()

                btnNuevoSetlist.postDelayed({ btnNuevoSetlist.visibility = View.GONE }, 200)
                btnUnirseSetlist.postDelayed({ btnUnirseSetlist.visibility = View.GONE }, 200)
            }
        }

        // Acciones asignadas directamente a tus variables
        btnNuevoSetlist.setOnClickListener {
            startActivity(Intent(this, CreateSetlistActivity::class.java))
            fabMain.performClick() // Cierra el menú automáticamente al navegar
        }

        btnUnirseSetlist.setOnClickListener {
            mostrarDialogoIngresarCodigo()
            fabMain.performClick() // Cierra el menú tras abrir el diálogo
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_dashboard)
        supportActionBar?.title = getString(R.string.title_dashboard_mis_setlists)



        // Llamamos a la configuración del menú de botones flotantes
        configurarFabMenu()
        configurarBotonLogout()
        configurarRecyclerView()
        observarViewModel()
        verificarPermisoNotificaciones()
        configurarBottomNavigation()
        configurarBusqueda()
    }
    private fun configurarBusqueda() {
        val searchBar = findViewById<SearchBar>(R.id.search_bar)
        val searchView = findViewById<SearchView>(R.id.search_view)

        searchView.setupWithSearchBar(searchBar)

        searchView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                // No usamos esto.
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                textoBusquedaActual = s?.toString().orEmpty()
                filtrarSetlists(textoBusquedaActual)
            }

            override fun afterTextChanged(s: Editable?) {
                // No usamos esto.
            }
        })

        searchView.editText.setOnEditorActionListener { _, _, _ ->
            searchBar.setText(searchView.text)
            searchView.hide()
            false
        }
    }

    private fun filtrarSetlists(texto: String) {
        val consulta = texto.trim().lowercase()

        if (consulta.isBlank()) {
            adapter.actualizarLista(listaSetlistsCompleta)
            return
        }

        val listaFiltrada = listaSetlistsCompleta.filter { setlist ->
            setlist.titulo.lowercase().contains(consulta)
        }

        adapter.actualizarLista(listaFiltrada)
    }
    // Usamos onResume para que, si el usuario vuelve de crear un setlist, la lista se actualice sola
    override fun onResume() {
        super.onResume()
        viewModel.cargarSetlists()
    }

    private fun configurarRecyclerView() {
        val rvSetlists = findViewById<RecyclerView>(R.id.rvSetlists)
        rvSetlists.layoutManager = LinearLayoutManager(this)

        adapter = SetlistAdapter(
            setlists = emptyList(),
            onItemClick = { setlistSeleccionado ->
                if (actionMode == null) {
                    val intent = Intent(this, SetlistDetailActivity::class.java).apply {
                        putExtra("SETLIST_COMPLETO", setlistSeleccionado)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { setlistSeleccionado ->
                if (actionMode == null) {
                    setlistSeleccionadoParaBorrar = setlistSeleccionado
                    actionMode = startSupportActionMode(actionModeCallback)
                }
            }
        )

        rvSetlists.adapter = adapter

        val rvSetlistsInstrumento = findViewById<RecyclerView>(R.id.rvSetlistsInstrumento)
        rvSetlistsInstrumento.layoutManager = LinearLayoutManager(this)

        adapterInstrumentos = SetlistInstrumentoSuscriptoAdapter(
            items = emptyList(),
            onItemClick = { item ->
                val intent = Intent(this, InstrumentoSetlistViewerActivity::class.java).apply {
                    putExtra("CODIGO", item.codigo)
                    putExtra("AGRUPACION_ID", item.agrupacionId)
                    putExtra("SHOW_ID", item.showId)
                    putExtra("INSTRUMENTO_ID", item.instrumentoId)
                }

                startActivity(intent)
            }
        )

        rvSetlistsInstrumento.adapter = adapterInstrumentos
    }

    private fun observarViewModel() {
        viewModel.setlists.observe(this) { listaSetlists ->
            listaSetlistsCompleta = listaSetlists
            filtrarSetlists(textoBusquedaActual)
        }

        viewModel.suscripcionExitosa.observe(this) { exito ->
            if (exito == true) {
                Toast.makeText(
                    this,
                    getString(R.string.message_dashboard_te_uniste_al_setlist_con_xito),
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.cargarSetlists()
            }
        }

        viewModel.error.observe(this) { mensaje ->
            Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
        }
    }

    private fun configurarBotonLogout() {
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            loginViewModel.logout()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
    private fun confirmarBorrado(mode: ActionMode) {
        val setlist = setlistSeleccionadoParaBorrar ?: return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_dashboard_borrar_setlist))
            .setMessage(
                getString(
                    R.string.message_dashboard_estas_seguro_de_que_queres_borrar,
                    setlist.titulo
                ))
            .setPositiveButton(getString(R.string.btn_dashboard_borrar)) { _, _ ->
                viewModel.ocultarSetlist(setlist.id)
                mode.finish() // Cierra la barra de borrado
            }
            .setNegativeButton(getString(R.string.btn_dashboard_cancelar)) { _, _ ->
                mode.finish()
            }
            .show()
    }
    private fun configurarBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.item_1 // O el ID que uses para la tab principal

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_1 -> true
                R.id.item_2 -> { // Tu ID para la pestaña del medio
                    val intent = Intent(this, DirectorDashboardActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }

                    // Creamos las opciones anulando las animaciones de entrada y salida
                    val options = ActivityOptionsCompat.makeCustomAnimation(this, 0, 0).toBundle()

                    // Lanzamos la activity pasándole las opciones
                    startActivity(intent, options)

                    true
                }
                R.id.nav_show_setlist -> {
                    startActivity(Intent(this, ShowSetlistActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
}
