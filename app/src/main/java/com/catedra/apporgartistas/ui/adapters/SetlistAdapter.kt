package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.data.models.Setlist

class SetlistAdapter(
    private val setlists: List<Setlist>,
    // 1. Agregamos este parámetro para recibir la acción del clic
    private val onSetlistClick: (Setlist) -> Unit
) : RecyclerView.Adapter<SetlistAdapter.SetlistViewHolder>() {

    class SetlistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(android.R.id.text1)
        val tvSubtitulo: TextView = view.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetlistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return SetlistViewHolder(view)
    }

    override fun onBindViewHolder(holder: SetlistViewHolder, position: Int) {
        val setlist = setlists[position]
        holder.tvTitulo.text = setlist.titulo
        holder.tvSubtitulo.text = "${setlist.cantidadPartituras} partituras | Ensayo: ${setlist.fecha}"

        // 2. Le decimos a la vista que cuando la toquen, ejecute la acción
        holder.itemView.setOnClickListener {
            onSetlistClick(setlist)
        }
    }

    override fun getItemCount() = setlists.size
}