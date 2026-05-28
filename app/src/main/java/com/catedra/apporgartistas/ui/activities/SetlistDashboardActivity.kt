package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.ui.adapters.SetlistAdapter

class SetlistDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Vas a necesitar crear un layout llamado activity_setlist_dashboard.xml
        setContentView(R.layout.activity_setlist_dashboard)

        configurarLista()
        configurarBotonNuevo()
    }

    private fun configurarLista() {
        val rvSetlists = findViewById<RecyclerView>(R.id.rvSetlists)
        rvSetlists.layoutManager = LinearLayoutManager(this)

        val misSetlistsHardcodeados = listOf(
            Setlist("1", "Gala Sinfónica de Invierno", 12, "15/06/2026"),
            Setlist("2", "Repertorio Solista - Flauta y Saxo", 5, "22/06/2026"),
            Setlist("3", "Ensamble de Vientos - Oberturas", 8, "01/07/2026")
        )

        // Acá le pasamos el callback (la acción del clic) al adaptador
        val adapter = SetlistAdapter(misSetlistsHardcodeados) { setlistSeleccionado ->

            // Creamos el Intent para viajar a la otra pantalla
            val intent = Intent(this, SetlistDetailActivity::class.java)

            // Le "inyectamos" los datos a la mochila del Intent
            intent.putExtra("SETLIST_ID", setlistSeleccionado.id)
            intent.putExtra("SETLIST_TITULO", setlistSeleccionado.titulo)

            startActivity(intent)
        }

        rvSetlists.adapter = adapter
    }

    private fun configurarBotonNuevo() {
        val btnNuevoSetlist = findViewById<Button>(R.id.btnNuevoSetlist)
        btnNuevoSetlist.setOnClickListener {
            val intent = Intent(this, CreateSetlistActivity::class.java)
            startActivity(intent)
        }
    }
}