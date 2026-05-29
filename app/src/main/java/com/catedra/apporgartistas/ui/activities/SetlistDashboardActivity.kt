package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.ui.adapters.SetlistAdapter
import com.catedra.apporgartistas.viewmodels.SetlistDashboardViewModel
import com.google.firebase.auth.FirebaseAuth
import com.catedra.apporgartistas.activities.LoginActivity
import com.catedra.apporgartistas.viewmodels.LoginViewModel



class SetlistDashboardActivity : AppCompatActivity() {
    private val viewModel: SetlistDashboardViewModel by viewModels()

    private val loginViewModel: LoginViewModel by viewModels()
    private lateinit var adapter: SetlistAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_dashboard)

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

        adapter = SetlistAdapter(emptyList()) {setlistSeleccionado ->
            val intent = Intent(this, SetlistDetailActivity::class.java).apply{
                putExtra("SETLIST_COMPLETO", setlistSeleccionado)
            }
            startActivity(intent)
        }
        rvSetlists.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.setlists.observe(this){listaSetlists ->
            adapter.actualizarLista(listaSetlists)
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
}