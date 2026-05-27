package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Partitura

class SetlistDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_detail)

        // 1. Abrimos la mochila del Intent y sacamos los datos
        val setlistId = intent.getStringExtra("SETLIST_ID") ?: "1"
        val setlistTitulo = intent.getStringExtra("SETLIST_TITULO") ?: "Repertorio"

        // 2. Actualizamos los textos de la interfaz
        val tvDetailTitle = findViewById<TextView>(R.id.tvDetailTitle)
        tvDetailTitle.text = setlistTitulo

        // 3. Cargamos la lista
        configurarListaDePartituras(setlistId)
    }

    private fun configurarListaDePartituras(setId: String) {
        val listView = findViewById<ListView>(R.id.lvPartituras)

        // Simulación de Base de Datos: Traemos distintas obras según el ID del setlist
        val partiturasDelSetlist = obtenerPartiturasFalsas(setId)

        // Extraemos solo los títulos para la lista visual
        val titulos = partiturasDelSetlist.map { it.titulo }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titulos)
        listView.adapter = adapter

        // Evento de clic: Abrir en Drive
        listView.setOnItemClickListener { _, _, position, _ ->
            val partituraSeleccionada = partiturasDelSetlist[position]
            abrirPdfEnDrive(partituraSeleccionada.urlDrive)
        }
    }

    // Este método simula lo que a futuro hará tu ViewModel consultando a Firestore
    private fun obtenerPartiturasFalsas(setId: String): List<Partitura> {
        return when (setId) {
            "1" -> listOf( // Gala Sinfónica
                Partitura("p1", "Sinfonía No. 5 - Beethoven", "https://drive.google.com/file/d/1F99I5E8LDuCE5xOze9ZASgSEGGwtYvr7/view?usp=drive_link"),
                Partitura("p2", "Danza Húngara No. 5 - Brahms", "https://drive.google.com/file/d/1fEsqlVo6-EAk5LZnep9_ZoLJZQeigKAf/view?usp=drive_link")
            )
            "2" -> listOf( // Repertorio Solista
                Partitura("p3", "Concierto para Flauta en Sol Mayor - Mozart", "https://drive.google.com/file/d/TU_ID/view"),
                Partitura("p4", "Estudio para Saxo Alto - Piazzolla", "https://drive.google.com/file/d/TU_ID/view")
            )
            "3" -> listOf( // Ensamble
                Partitura("p5", "Obertura de Guillermo Tell", "https://drive.google.com/file/d/TU_ID/view")
            )
            else -> emptyList()
        }
    }

    private fun abrirPdfEnDrive(url: String) {
        try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el archivo", Toast.LENGTH_SHORT).show()
        }
    }
}