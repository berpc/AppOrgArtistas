package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Partitura
import com.catedra.apporgartistas.data.models.Setlist

class SetlistDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setlist_detail)

        val setlist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("SETLIST_COMPLETO", Setlist::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra("SETLIST_COMPLETO") as? Setlist
        }
        if (setlist != null) {
            // Ponemos el título real del Setlist en la barra superior de la app
            supportActionBar?.title = setlist.titulo

            // 2. Armamos la interfaz con los datos reales
            configurarListaDePartituras(setlist)
        } else {
            supportActionBar?.title = "Error al cargar Setlist"
        }

    }
    private fun configurarListaDePartituras(setlist: Setlist) {
        val listView = findViewById<ListView>(R.id.lvPartituras)

        // Generamos una lista de nombres visuales usando la posición en el array
        val titulosVisuales = setlist.partituras.mapIndexed { index, _ ->
            "Partitura ${index + 1}"
        }

        // Conectamos los títulos al ListView
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titulosVisuales)
        listView.adapter = adapter

        // 3. Evento de clic: Abrir la partitura de Cloudinary
        listView.setOnItemClickListener { _, _, position, _ ->
            val partituraSeleccionada = setlist.partituras[position]
            val tituloDinamico = titulosVisuales[position]

            // Reutilizamos la Activity que creamos para renderizar PDFs sin salir de la app
            val intent = Intent(this, PdfViewerActivity::class.java).apply {
                // Pasamos la URL optimizada que nos devolvió Cloudinary
                putExtra("PDF_URL", partituraSeleccionada.url)
                putExtra("OBRA_TITULO", tituloDinamico)
            }
            startActivity(intent)
        }
    }


}