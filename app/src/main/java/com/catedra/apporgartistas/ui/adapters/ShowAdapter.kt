package com.catedra.apporgartistas.ui.adapters

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Show
import com.google.android.material.card.MaterialCardView

class ShowAdapter(
    private var shows: List<Show>,
    private val onItemClick: (Show) -> Unit,
    private val onItemLongClick: (Show) -> Unit
) : RecyclerView.Adapter<ShowAdapter.ShowViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    class ShowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
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
        aplicarSeleccion(holder, selectedIds.contains(show.id))

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

    fun actualizarSeleccion(nuevosSeleccionados: Set<String>) {
        selectedIds.clear()
        selectedIds.addAll(nuevosSeleccionados)
        notifyDataSetChanged()
    }

    private fun aplicarSeleccion(holder: ShowViewHolder, seleccionado: Boolean) {
        // El adapter solo pinta la seleccion; la Activity mantiene el estado.
        holder.card.setCardBackgroundColor(
            Color.parseColor(if (seleccionado) "#DBEAFE" else "#FFFFFF")
        )
        holder.card.strokeColor = Color.parseColor(if (seleccionado) "#2563EB" else "#FFFFFF")
        holder.card.strokeWidth = if (seleccionado) 2.dp(holder.itemView) else 0
    }

    private fun Int.dp(view: View): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            view.resources.displayMetrics
        ).toInt()
    }
}
