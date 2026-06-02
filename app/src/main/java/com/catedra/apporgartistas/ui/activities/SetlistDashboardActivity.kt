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
import com.google.firebase.auth.FirebaseAuth
import com.catedra.apporgartistas.activities.LoginActivity
import com.catedra.apporgartistas.viewmodels.LoginViewModel
import androidx.appcompat.app.AlertDialog
import android.widget.TextView


class SetlistDashboardActivity : AppCompatActivity() {
    private val viewModel: SetlistDashboardViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var adapter: SetlistAdapter

    //private lateinit var tvCantidadSetlists: TextView

    // Variables para el modo de selección
    private var actionMode: ActionMode? = null
    private var setlistSeleccionadoParaBorrar: Setlist? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            // Inflamos el tachito de basura
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
            setlistSeleccionadoParaBorrar = null
            // Acá podrías decirle al adapter que quite el resaltado de fondo si lo tuvieras
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_dashboard)
        //tvCantidadSetlists = findViewById(R.id.tvCantidadSetlists)
        supportActionBar?.title = "Mis Setlists"

        configurarBotonNuevo()
        configurarBotonLogout()
        configurarRecyclerView()
        observarViewModel()
    }
    // Usamos onResume para que, si el usuario vuelve de crear un setlist, la lista se actualice sola
    override fun onResume() {
        super.onResume()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModel.cargarSetlists(userId)
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
           // tvCantidadSetlists.text =
                //"${listaSetlists.size} setlists creados"
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        AlertDialog.Builder(this)
            .setTitle("Borrar Setlist")
            .setMessage("¿Estás seguro de que querés borrar '${setlist.titulo}'?")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.ocultarSetlist(userId, setlist.id)
                mode.finish() // Cierra la barra de borrado
            }
            .setNegativeButton("Cancelar") { _, _ ->
                mode.finish()
            }
            .show()
    }
}