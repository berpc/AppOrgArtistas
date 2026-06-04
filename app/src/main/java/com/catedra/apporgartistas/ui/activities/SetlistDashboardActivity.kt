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
import com.google.firebase.messaging.FirebaseMessaging


class SetlistDashboardActivity : AppCompatActivity() {
    private val viewModel: SetlistDashboardViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var adapter: SetlistAdapter

    // Variables para el modo de selección
    private var actionMode: ActionMode? = null
    private var setlistSeleccionadoParaBorrar: Setlist? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            obtenerYGuardarToken()
        }
    }

    private fun obtenerYGuardarToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                loginViewModel.guardarTokenFcm(token)
            }
        }
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_dashboard)
        supportActionBar?.title = getString(R.string.title_dashboard_mis_setlists)
        val btnUnirse = findViewById<Button>(R.id.btnUnirseSetlist)

        configurarBotonNuevo()
        configurarBotonLogout()
        configurarRecyclerView()
        observarViewModel()
        verificarPermisoNotificaciones()
        btnUnirse.setOnClickListener {
            mostrarDialogoIngresarCodigo()
        }
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
                // Si el modo de borrado está activo, un clic normal podría seleccionarlo también
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
                    // Arrancamos la barra de menú contextual
                    actionMode = startSupportActionMode(actionModeCallback)
                }
            }
        )
        rvSetlists.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.setlists.observe(this){listaSetlists ->
            adapter.actualizarLista(listaSetlists)
        }
        viewModel.suscripcionExitosa.observe(this) { exito ->
            if (exito == true) {
                Toast.makeText(this,
                    getString(R.string.message_dashboard_te_uniste_al_setlist_con_xito), Toast.LENGTH_SHORT).show()

                viewModel.cargarSetlists()
            }
        }
    }

    private fun configurarBotonNuevo() {
        val btnNuevoSetlist = findViewById<Button>(R.id.btnNuevoSetlist)
        btnNuevoSetlist.setOnClickListener {
            val intent = Intent(this, CreateSetlistActivity::class.java)
            startActivity(intent)
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
}