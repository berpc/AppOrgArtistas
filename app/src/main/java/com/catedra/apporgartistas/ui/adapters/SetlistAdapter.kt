package com.catedra.apporgartistas.ui.adapters

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Setlist
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView

class SetlistAdapter(
    private var setlists: List<Setlist>,
    private val onItemClick: (Setlist) -> Unit,
    private val onItemLongClick: (Setlist) -> Unit
) : RecyclerView.Adapter<SetlistAdapter.ViewHolder>() {

    private val selectedIds = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view as MaterialCardView
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
        val cantidadPartituras = setlist.partituras.size
        val textoPartituras = holder.itemView.resources.getQuantityString(
            R.plurals.cantidad_partituras_setlist,
            cantidadPartituras,
            cantidadPartituras
        )

        holder.textHeader.text = setlist.titulo
        holder.textSubhead.text = holder.itemView.context.getString(
            R.string.subtitle_setlist_grupo_partituras,
            setlist.nombreGrupo,
            textoPartituras
        )
        aplicarSeleccion(holder, selectedIds.contains(setlist.id))

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

    fun actualizarSeleccion(nuevosSeleccionados: Set<String>) {
        selectedIds.clear()
        selectedIds.addAll(nuevosSeleccionados)
        notifyDataSetChanged()
    }

    private fun aplicarSeleccion(holder: ViewHolder, seleccionado: Boolean) {
        // El adapter solo pinta la seleccion; la pantalla decide que IDs estan seleccionados.
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
