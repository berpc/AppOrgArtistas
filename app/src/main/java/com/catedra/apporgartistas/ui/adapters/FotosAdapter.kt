package com.catedra.apporgartistas.ui.adapters

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R

class FotosAdapter(private var fotos: List<Uri> = emptyList()) :
    RecyclerView.Adapter<FotosAdapter.FotoViewHolder>() {

    class FotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivFoto: ImageView = view.findViewById(R.id.ivItemFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto_partitura, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val uri = fotos[position]
        // Limpiamos la imagen anterior por las dudas (reciclaje de vistas)
        holder.ivFoto.setImageURI(null)
        // Cargamos la nueva
        holder.ivFoto.setImageURI(uri)
    }

    override fun getItemCount() = fotos.size

    @SuppressLint("NotifyDataSetChanged")
    fun actualizarFotos(nuevasFotos: List<Uri>) {
        fotos = nuevasFotos
        notifyDataSetChanged()
    }
}