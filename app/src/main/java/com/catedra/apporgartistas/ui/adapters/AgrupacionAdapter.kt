package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Agrupacion
import com.catedra.apporgartistas.ui.models.AgrupacionDashboardItem

class AgrupacionAdapter(
    private var agrupaciones: List<AgrupacionDashboardItem>,
    private val onItemClick: (Agrupacion) -> Unit,
    private val onItemLongClick: (Agrupacion) -> Unit
) : RecyclerView.Adapter<AgrupacionAdapter.AgrupacionViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    class AgrupacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreAgrupacion)
        val tvCantidadShows: TextView = view.findViewById(R.id.tvCantidadShows)
        val selectionOverlay: View = view.findViewById(R.id.viewSelectionOverlayAgrupacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgrupacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agrupacion_card, parent, false)
        return AgrupacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgrupacionViewHolder, position: Int) {
        val item = agrupaciones[position]
        val agrupacion = item.agrupacion

        holder.tvNombre.text = agrupacion.nombre
        holder.tvCantidadShows.text = holder.itemView.resources.getQuantityString(
            R.plurals.cantidad_shows_agrupacion,
            item.cantidadShows,
            item.cantidadShows
        )
        holder.selectionOverlay.visibility = if (selectedIds.contains(agrupacion.id)) {
            View.VISIBLE
        } else {
            View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(agrupacion)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(agrupacion)
            true
        }
    }

    override fun getItemCount() = agrupaciones.size

    fun actualizarLista(nuevaLista: List<AgrupacionDashboardItem>) {
        agrupaciones = nuevaLista
        notifyDataSetChanged()
    }

    fun actualizarSeleccion(nuevosSeleccionados: Set<String>) {
        selectedIds.clear()
        selectedIds.addAll(nuevosSeleccionados)
        notifyDataSetChanged()
    }
}
