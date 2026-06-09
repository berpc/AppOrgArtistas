package com.catedra.apporgartistas.ui.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.data.models.ShowSetlistSuscripto

class ShowSetlistAdapter(
    private var items: List<ShowSetlistSuscripto>,
    private val onItemClick: (ShowSetlistSuscripto) -> Unit
) : RecyclerView.Adapter<ShowSetlistAdapter.ViewHolder>() {

    class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val context = parent.context

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
            setBackgroundColor(Color.parseColor("#1E293B"))

            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 18
            }
        }

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val item = items[position]
        val context = holder.itemView.context

        holder.layout.removeAllViews()

        val tvShow = TextView(context).apply {
            text = item.nombreShow.ifBlank { "Show sin nombre" }
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.START
        }

        val tvInstrumento = TextView(context).apply {
            text = item.nombreInstrumento.ifBlank { "Instrumento" }
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 15f
            setPadding(0, 8, 0, 0)
        }

        val tvFecha = TextView(context).apply {
            text = item.fechaShow ?: "Sin fecha"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 14f
            setPadding(0, 6, 0, 0)
        }

        val tvCodigo = TextView(context).apply {
            text = "Código: ${item.codigo}"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, 8, 0, 0)
        }

        holder.layout.addView(tvShow)
        holder.layout.addView(tvInstrumento)
        holder.layout.addView(tvFecha)
        holder.layout.addView(tvCodigo)

        holder.layout.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun actualizarLista(nuevaLista: List<ShowSetlistSuscripto>) {
        items = nuevaLista
        notifyDataSetChanged()
    }
}