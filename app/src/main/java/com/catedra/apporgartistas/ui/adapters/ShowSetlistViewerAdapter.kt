package com.catedra.apporgartistas.ui.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.SetlistMasterItem

class ShowSetlistViewerAdapter(
    private var setlistMaster: List<SetlistMasterItem>,
    private var pdfsPorSetlistItem: Map<String, PartituraCloud>,
    private val onObraClick: (SetlistMasterItem, PartituraCloud?) -> Unit
) : RecyclerView.Adapter<ShowSetlistViewerAdapter.ViewHolder>() {

    class ViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val context = parent.context

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 18, 20, 18)
            setBackgroundColor(Color.parseColor("#1E293B"))

            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }

        return ViewHolder(layout)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val context = holder.itemView.context
        val setlistItem = setlistMaster[position]
        val partitura = pdfsPorSetlistItem[setlistItem.id]

        holder.layout.removeAllViews()

        val tvOrden = TextView(context).apply {
            text = "${position + 1}"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 18f
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                56,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tvNombreObra = TextView(context).apply {
            text = setlistItem.nombre.ifBlank { "Obra sin nombre" }
            setTextColor(Color.WHITE)
            textSize = 20f
            maxLines = 2

            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val tvEstado = TextView(context).apply {
            text = if (partitura != null && partitura.url.isNotBlank()) "PDF" else "Tacet"

            setTextColor(
                if (partitura != null && partitura.url.isNotBlank()) {
                    Color.parseColor("#22C55E")
                } else {
                    Color.parseColor("#64748B")
                }
            )

            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(12, 0, 0, 0)
        }

        holder.layout.addView(tvOrden)
        holder.layout.addView(tvNombreObra)
        holder.layout.addView(tvEstado)

        holder.layout.setOnClickListener {
            onObraClick(setlistItem, partitura)
        }
    }

    override fun getItemCount(): Int {
        return setlistMaster.size
    }

    fun actualizarDatos(
        nuevoSetlistMaster: List<SetlistMasterItem>,
        nuevosPdfsPorSetlistItem: Map<String, PartituraCloud>
    ) {
        setlistMaster = nuevoSetlistMaster
        pdfsPorSetlistItem = nuevosPdfsPorSetlistItem
        notifyDataSetChanged()
    }
}