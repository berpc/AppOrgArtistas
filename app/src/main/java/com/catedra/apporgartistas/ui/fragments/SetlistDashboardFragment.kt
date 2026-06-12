package com.catedra.apporgartistas.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.ui.activities.CreateSetlistActivity
import com.catedra.apporgartistas.ui.activities.InstrumentoSetlistViewerActivity
import com.catedra.apporgartistas.ui.activities.SetlistDetailActivity
import com.catedra.apporgartistas.ui.adapters.SetlistAdapter
import com.catedra.apporgartistas.ui.adapters.SetlistInstrumentoSuscriptoAdapter
import com.catedra.apporgartistas.viewmodels.LoginViewModel
import com.catedra.apporgartistas.viewmodels.SetlistDashboardViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView

class SetlistDashboardFragment : Fragment(R.layout.fragment_setlist_dashboard) {

    private val viewModel: SetlistDashboardViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var adapter: SetlistAdapter
    private lateinit var adapterInstrumentos: SetlistInstrumentoSuscriptoAdapter
    private lateinit var progressBar: ProgressBar
    private var listaSetlistsCompleta: List<Setlist> = emptyList()
    private var textoBusquedaActual: String = ""
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

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
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
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title =
            getString(R.string.title_dashboard_mis_setlists)

        progressBar = view.findViewById(R.id.progressBarSetlistDashboard)
        configurarFabMenu(view)
        configurarRecyclerView(view)
        observarViewModel()
        verificarPermisoNotificaciones()
        configurarBusqueda(view)
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarSetlists()
    }

    private fun obtenerYGuardarToken() {
        loginViewModel.obtenerYGuardarTokenFcm()
    }

    private fun verificarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
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
        val input = EditText(requireContext()).apply {
            hint = context.getString(R.string.hint_dashboard_ej_a7x9bq)
            filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(6))
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.alerttitle_dashboard_unirse_a_un_setlist))
            .setMessage(getString(R.string.message_dashboard_ingresa_el_codigo_de_6_caracteres_que_te_compartio_el_director))
            .setView(input)
            .setPositiveButton(getString(R.string.btn_dashboard_unirse)) { _, _ ->
                val codigo = input.text.toString().trim()
                if (codigo.length == 6) {
                    viewModel.unirseASetlistConCodigo(codigo)
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.message_dashboard_el_codigo_debe_tener_exactamente_6_caracteres),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_dashboard_cancelar), null)
            .show()
    }

    private fun configurarFabMenu(view: View) {
        val fabMain = view.findViewById<FloatingActionButton>(R.id.fab_main)
        val btnNuevoSetlist = view.findViewById<View>(R.id.btnNuevoSetlist)
        val btnUnirseSetlist = view.findViewById<View>(R.id.btnUnirseSetlist)

        fabMain.setOnClickListener {
            isFabExpanded = !isFabExpanded

            if (isFabExpanded) {
                btnNuevoSetlist.visibility = View.VISIBLE
                btnUnirseSetlist.visibility = View.VISIBLE

                btnNuevoSetlist.alpha = 0f
                btnUnirseSetlist.alpha = 0f
                btnNuevoSetlist.scaleX = 0.5f
                btnNuevoSetlist.scaleY = 0.5f
                btnUnirseSetlist.scaleX = 0.5f
                btnUnirseSetlist.scaleY = 0.5f

                btnNuevoSetlist.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                btnUnirseSetlist.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start()
                fabMain.animate().rotation(45f).setDuration(200).start()
            } else {
                btnNuevoSetlist.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(200).start()
                btnUnirseSetlist.animate().alpha(0f).scaleX(0.5f).scaleY(0.5f).setDuration(200).start()
                fabMain.animate().rotation(0f).setDuration(200).start()

                btnNuevoSetlist.postDelayed({ btnNuevoSetlist.visibility = View.GONE }, 200)
                btnUnirseSetlist.postDelayed({ btnUnirseSetlist.visibility = View.GONE }, 200)
            }
        }

        btnNuevoSetlist.setOnClickListener {
            startActivity(Intent(requireContext(), CreateSetlistActivity::class.java))
            fabMain.performClick()
        }

        btnUnirseSetlist.setOnClickListener {
            mostrarDialogoIngresarCodigo()
            fabMain.performClick()
        }
    }

    private fun configurarBusqueda(view: View) {
        val searchBar = view.findViewById<SearchBar>(R.id.search_bar)
        val searchView = view.findViewById<SearchView>(R.id.search_view)

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

    private fun configurarRecyclerView(view: View) {
        val rvSetlists = view.findViewById<RecyclerView>(R.id.rvSetlists)
        rvSetlists.layoutManager = LinearLayoutManager(requireContext())

        adapter = SetlistAdapter(
            setlists = emptyList(),
            onItemClick = { setlistSeleccionado ->
                if (actionMode == null) {
                    val intent = Intent(requireContext(), SetlistDetailActivity::class.java).apply {
                        putExtra("SETLIST_COMPLETO", setlistSeleccionado)
                    }
                    startActivity(intent)
                }
            },
            onItemLongClick = { setlistSeleccionado ->
                if (actionMode == null) {
                    setlistSeleccionadoParaBorrar = setlistSeleccionado
                    actionMode = (requireActivity() as AppCompatActivity)
                        .startSupportActionMode(actionModeCallback)
                }
            }
        )

        rvSetlists.adapter = adapter

        val rvSetlistsInstrumento = view.findViewById<RecyclerView>(R.id.rvSetlistsInstrumento)
        rvSetlistsInstrumento.layoutManager = LinearLayoutManager(requireContext())

        adapterInstrumentos = SetlistInstrumentoSuscriptoAdapter(
            items = emptyList(),
            onItemClick = { item ->
                val intent = Intent(requireContext(), InstrumentoSetlistViewerActivity::class.java).apply {
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
        viewModel.setlists.observe(viewLifecycleOwner) { listaSetlists ->
            listaSetlistsCompleta = listaSetlists
            filtrarSetlists(textoBusquedaActual)
        }

        viewModel.suscripcionExitosa.observe(viewLifecycleOwner) { exito ->
            if (exito == true) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_dashboard_te_uniste_al_setlist_con_xito),
                    Toast.LENGTH_SHORT
                ).show()

                viewModel.cargarSetlists()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            mostrarLoading(loading)
        }
    }

    private fun confirmarBorrado(mode: ActionMode) {
        val setlist = setlistSeleccionadoParaBorrar ?: return

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_dashboard_borrar_setlist))
            .setMessage(
                getString(
                    R.string.message_dashboard_estas_seguro_de_que_queres_borrar,
                    setlist.titulo
                )
            )
            .setPositiveButton(getString(R.string.btn_dashboard_borrar)) { _, _ ->
                viewModel.ocultarSetlist(setlist.id)
                mode.finish()
            }
            .setNegativeButton(getString(R.string.btn_dashboard_cancelar)) { _, _ ->
                mode.finish()
            }
            .show()
    }

    override fun onDestroyView() {
        actionMode?.finish()
        actionMode = null
        super.onDestroyView()
    }

    private fun mostrarLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
