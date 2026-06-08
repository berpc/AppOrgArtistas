package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Setlist
import com.google.android.material.imageview.ShapeableImageView

class SetlistAdapter(
    private var setlists: List<Setlist>,
    private val onItemClick: (Setlist) -> Unit,
    private val onItemLongClick: (Setlist) -> Unit
) : RecyclerView.Adapter<SetlistAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // 1. Enlazamos los nuevos IDs exactos de tu XML
        val textHeader: TextView = view.findViewById(R.id.text_header)
        val textSubhead: TextView = view.findViewById(R.id.text_subhead)
        val avatarImage: ImageView = view.findViewById(R.id.avatar_image)
        val rightMedia: ShapeableImageView = view.findViewById(R.id.right_media)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_setlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val setlist = setlists[position]

        // 2. Asignamos el título principal
        holder.textHeader.text = setlist.titulo

        // 3. Combinamos la información restante en el subtítulo (Grupo + Cantidad)
        // Usamos el símbolo "•" para separar visualmente los datos
        holder.textSubhead.text = "${setlist.nombreGrupo} • ${setlist.partituras.size} partituras"

        // EVENTOS DE CLIC
        holder.itemView.setOnClickListener {
            onItemClick(setlist)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(setlist)
            true
        }
    }

    override fun getItemCount() = setlists.size

    fun actualizarLista(nuevaLista: List<Setlist>) {
        setlists = nuevaLista
        notifyDataSetChanged()
    }
}