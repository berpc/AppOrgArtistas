package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.data.models.ShowSetlistSuscripto
import com.catedra.apporgartistas.ui.adapters.ShowSetlistAdapter
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java

class ShowSetlistActivity : AppCompatActivity() {

    private lateinit var rvShowSetlists: RecyclerView
    private lateinit var adapter: ShowSetlistAdapter

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_setlist)

        configurarRecycler()
        configurarBottomNavigation()
        cargarShowSetlists()
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
                    startActivity(Intent(this, ShowsDashboardActivity::class.java))
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

    private fun cargarShowSetlists() {
        val userId = getCurrentUserId()

        if (userId.isBlank()) {
            Toast.makeText(
                this,
                "Usuario no autenticado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        firestore.collection("codigosSetlistInstrumento")
            .whereArrayContains("suscriptores", userId)
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    adapter.actualizarLista(emptyList())
                    return@addOnSuccessListener
                }

                val tareas = snapshot.documents.map { codigoDoc ->
                    val agrupacionId = codigoDoc.getString("agrupacionId") ?: ""
                    val showId = codigoDoc.getString("showId") ?: ""
                    val instrumentoId = codigoDoc.getString("instrumentoId") ?: ""

                    val showTask = firestore.collection("agrupaciones")
                        .document(agrupacionId)
                        .collection("shows")
                        .document(showId)
                        .get()

                    val instrumentoTask = firestore.collection("agrupaciones")
                        .document(agrupacionId)
                        .collection("shows")
                        .document(showId)
                        .collection("instrumentos")
                        .document(instrumentoId)
                        .get()

                    Tasks.whenAllSuccess<DocumentSnapshot>(
                        showTask,
                        instrumentoTask
                    ).continueWith { task ->
                        val docs = task.result

                        val showDoc = docs[0]
                        val instrumentoDoc = docs[1]

                        val show = showDoc.toObject(Show::class.java)
                        val instrumento = instrumentoDoc.toObject(Instrumento::class.java)

                        ShowSetlistSuscripto(
                            codigo = codigoDoc.getString("codigo") ?: codigoDoc.id,
                            agrupacionId = agrupacionId,
                            showId = showId,
                            instrumentoId = instrumentoId,
                            nombreShow = show?.nombre ?: "Show sin nombre",
                            fechaShow = show?.fecha,
                            nombreInstrumento = instrumento?.nombre ?: "Instrumento"
                        )
                    }
                }

                Tasks.whenAllSuccess<ShowSetlistSuscripto>(tareas)
                    .addOnSuccessListener { lista ->
                        adapter.actualizarLista(lista)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ShowSetlist", "Error armando Show Setlists", e)

                        Toast.makeText(
                            this,
                            "Error al cargar Show Setlists",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ShowSetlist", "Error cargando códigos", e)

                Toast.makeText(
                    this,
                    "Error al cargar Show Setlists",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}