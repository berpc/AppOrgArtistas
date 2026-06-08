package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Agrupacion

class AgrupacionAdapter(
    private var agrupaciones: List<Agrupacion>,
    private val onItemClick: (Agrupacion) -> Unit,
    private val onItemLongClick: (Agrupacion) -> Unit
) : RecyclerView.Adapter<AgrupacionAdapter.AgrupacionViewHolder>() {

    class AgrupacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreAgrupacion)
        // val tvRol: TextView = view.findViewById(R.id.tvRol) // Por si querés cambiar el texto dinámicamente
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgrupacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agrupacion, parent, false)
        return AgrupacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgrupacionViewHolder, position: Int) {
        val agrupacion = agrupaciones[position]
        holder.tvNombre.text = agrupacion.nombre

        holder.itemView.setOnClickListener {
            onItemClick(agrupacion)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(agrupacion)
            true
        }
    }

    override fun getItemCount() = agrupaciones.size

    fun actualizarLista(nuevaLista: List<Agrupacion>) {
        agrupaciones = nuevaLista
        notifyDataSetChanged()
    }
}