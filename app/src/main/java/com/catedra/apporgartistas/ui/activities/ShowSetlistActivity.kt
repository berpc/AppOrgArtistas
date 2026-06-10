package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.ui.adapters.ShowSetlistAdapter
import com.catedra.apporgartistas.viewmodels.ShowSetlistViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class ShowSetlistActivity : AppCompatActivity() {

    private val viewModel: ShowSetlistViewModel by viewModels()

    private lateinit var rvShowSetlists: RecyclerView
    private lateinit var adapter: ShowSetlistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_setlist)

        configurarRecycler()
        configurarBottomNavigation()
        observarViewModel()

        viewModel.cargarShowSetlists()
    }

    private fun configurarRecycler() {
        rvShowSetlists = findViewById(R.id.rvShowSetlists)
        rvShowSetlists.layoutManager = LinearLayoutManager(this)

        adapter = ShowSetlistAdapter(
            items = emptyList(),
            onItemClick = { item ->
                val intent = Intent(this, ShowSetlistViewerActivity::class.java).apply {
                    putExtra("CODIGO", item.codigo)
                    putExtra("AGRUPACION_ID", item.agrupacionId)
                    putExtra("SHOW_ID", item.showId)
                    putExtra("INSTRUMENTO_ID", item.instrumentoId)
                }

                startActivity(intent)
            }
        )

        rvShowSetlists.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.showSetlists.observe(this) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.error.observe(this) { mensaje ->
            Toast.makeText(
                this,
                mensaje,
                if (mensaje == "Usuario no autenticado") Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun configurarBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigation.selectedItemId = R.id.nav_show_setlist

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.item_1 -> {
                    startActivity(Intent(this, SetlistDashboardActivity::class.java))
                    finish()
                    true
                }

                R.id.item_2 -> {
                    startActivity(Intent(this, DirectorDashboardActivity::class.java))
                    finish()
                    true
                }

                R.id.nav_show_setlist -> {
                    true
                }

                else -> false
            }
        }
    }
}
