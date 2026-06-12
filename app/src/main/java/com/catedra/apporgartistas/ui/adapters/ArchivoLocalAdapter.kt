package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.viewmodels.ArchivoLocal

class ArchivoLocalAdapter(
    private var archivos: List<ArchivoLocal> = emptyList()
) : RecyclerView.Adapter<ArchivoLocalAdapter.ArchivoLocalViewHolder>() {

    class ArchivoLocalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreArchivo: TextView = view.findViewById(R.id.tvNombreArchivoLocal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchivoLocalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_archivo_local, parent, false)
        return ArchivoLocalViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchivoLocalViewHolder, position: Int) {
        holder.tvNombreArchivo.text = archivos[position].nombre
    }

    override fun getItemCount(): Int = archivos.size

    fun actualizarLista(nuevaLista: List<ArchivoLocal>) {
        archivos = nuevaLista
        notifyDataSetChanged()
    }
}
