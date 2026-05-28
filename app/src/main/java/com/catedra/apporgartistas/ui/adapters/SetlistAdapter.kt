package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Setlist

class SetlistAdapter(
    private var setlists: List<Setlist>,
    private val onClick: (Setlist) -> Unit
) : RecyclerView.Adapter<SetlistAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTituloSetlist)
        val tvCantidad: TextView = view.findViewById(R.id.tvCantidadPartituras)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_setlist, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val setlist = setlists[position]
        holder.tvTitulo.text = setlist.titulo
        holder.tvCantidad.text = "${setlist.partituras.size} partituras"

        // Al hacer clic, disparamos la función que nos pasaron desde la Activity
        holder.itemView.setOnClickListener {
            onClick(setlist)
        }
    }
    override fun getItemCount() = setlists.size

    fun actualizarLista(nuevaLista: List<Setlist>) {
        setlists = nuevaLista
        notifyDataSetChanged()
    }
}