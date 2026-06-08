package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Show

class ShowAdapter(
    private var shows: List<Show>,
    private val onItemClick: (Show) -> Unit,
    private val onItemLongClick: (Show) -> Unit
) : RecyclerView.Adapter<ShowAdapter.ShowViewHolder>() {

    class ShowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreShow)
        val tvFecha: TextView = view.findViewById(R.id.tvFechaShow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_show, parent, false)
        return ShowViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShowViewHolder, position: Int) {
        val show = shows[position]
        holder.tvNombre.text = show.nombre

        // Lógica para mostrar/ocultar la fecha
        if (show.fecha.isNullOrBlank()) {
            holder.tvFecha.visibility = View.GONE
        } else {
            holder.tvFecha.visibility = View.VISIBLE
            holder.tvFecha.text = show.fecha
        }

        holder.itemView.setOnClickListener {
            onItemClick(show)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(show)
            true
        }
    }

    override fun getItemCount() = shows.size

    fun actualizarLista(nuevaLista: List<Show>) {
        shows = nuevaLista
        notifyDataSetChanged()
    }
}