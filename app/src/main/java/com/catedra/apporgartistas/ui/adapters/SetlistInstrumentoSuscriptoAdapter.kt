package com.catedra.apporgartistas.ui.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.data.models.SetlistInstrumentoSuscripto

class SetlistInstrumentoSuscriptoAdapter(
    private var items: List<SetlistInstrumentoSuscripto>,
    private val onItemClick: (SetlistInstrumentoSuscripto) -> Unit
) : RecyclerView.Adapter<SetlistInstrumentoSuscriptoAdapter.ViewHolder>() {

    class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val context = parent.context

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.layout.removeAllViews()

        val tvTitulo = TextView(context).apply {
            text = item.nombreShow.ifBlank { "Show sin nombre" }
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.START
        }

        val tvInstrumento = TextView(context).apply {
            text = "Instrumento: ${item.nombreInstrumento.ifBlank { "Sin nombre" }}"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 14f
            setPadding(0, 8, 0, 0)
        }

        val tvCodigo = TextView(context).apply {
            text = "Código: ${item.codigo}"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 13f
            setPadding(0, 6, 0, 0)
        }

        holder.layout.addView(tvTitulo)
        holder.layout.addView(tvInstrumento)
        holder.layout.addView(tvCodigo)

        holder.layout.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun actualizarLista(nuevaLista: List<SetlistInstrumentoSuscripto>) {
        items = nuevaLista
        notifyDataSetChanged()
    }
}