package com.catedra.apporgartistas.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.SetlistMasterItem

class InstrumentoPartituraAdapter(
    private var setlistMaster: MutableList<SetlistMasterItem>,
    private var pdfsPorSetlistItem: Map<String, PartituraCloud>,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit,
    private val onEliminarClick: (SetlistMasterItem) -> Unit,
    private val onArchivoClick: (SetlistMasterItem) -> Unit
) : RecyclerView.Adapter<InstrumentoPartituraAdapter.PartituraViewHolder>() {

    companion object {
        private const val ROW_HEIGHT = 96
        private const val ORDER_WIDTH = 70
        private const val OBRA_WIDTH = 360
        private const val HANDLE_WIDTH = 70
        private const val ARCHIVO_WIDTH = 460
        private const val DELETE_WIDTH = 90
    }

    class PartituraViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartituraViewHolder {
        val layout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ROW_HEIGHT
            )
        }

        return PartituraViewHolder(layout)
    }

    override fun onBindViewHolder(holder: PartituraViewHolder, position: Int) {
        holder.layout.removeAllViews()

        val context = holder.itemView.context
        val setlistItem = setlistMaster[position]
        val partitura = pdfsPorSetlistItem[setlistItem.id]

        val tvOrden = TextView(context).apply {
            text = "${position + 1}"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                ORDER_WIDTH,
                ROW_HEIGHT
            )
        }

        val tvObra = TextView(context).apply {
            text = setlistItem.nombre
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            setPadding(12, 0, 12, 0)

            layoutParams = LinearLayout.LayoutParams(
                OBRA_WIDTH,
                ROW_HEIGHT
            )
        }

        val tvDrag = crearDragHandle(holder)

        val tvArchivo = TextView(context).apply {
            text = partitura?.nombre?.takeIf { it.isNotBlank() } ?: "Tacet"

            setTextColor(
                if (partitura == null) Color.parseColor("#94A3B8")
                else Color.WHITE
            )

            textSize = 22f
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            setPadding(20, 0, 20, 0)
            setBackgroundColor(Color.parseColor("#1E293B"))

            layoutParams = LinearLayout.LayoutParams(
                ARCHIVO_WIDTH,
                ROW_HEIGHT
            ).apply {
                marginStart = 8
                marginEnd = 8
                topMargin = 8
                bottomMargin = 8
            }

            setOnClickListener {
                onArchivoClick(setlistItem)
            }
        }

        val tvEliminar = TextView(context).apply {
            text = "X"
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 24f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#1E293B"))

            layoutParams = LinearLayout.LayoutParams(
                DELETE_WIDTH,
                ROW_HEIGHT
            ).apply {
                marginStart = 8
                topMargin = 8
                bottomMargin = 8
            }

            setOnClickListener {
                onEliminarClick(setlistItem)
            }
        }

        holder.layout.addView(tvOrden)
        holder.layout.addView(tvObra)
        holder.layout.addView(tvDrag)
        holder.layout.addView(tvArchivo)
        holder.layout.addView(tvEliminar)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun crearDragHandle(holder: PartituraViewHolder): TextView {
        val context = holder.itemView.context

        return TextView(context).apply {
            text = "⋮⋮"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 24f
            gravity = Gravity.CENTER
            contentDescription = "Arrastrar archivo"

            layoutParams = LinearLayout.LayoutParams(
                HANDLE_WIDTH,
                ROW_HEIGHT
            )

            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onStartDrag(holder)
                }
                false
            }
        }
    }

    override fun getItemCount(): Int = setlistMaster.size

    fun actualizarDatos(
        nuevoSetlistMaster: List<SetlistMasterItem>,
        nuevosPdfsPorSetlistItem: Map<String, PartituraCloud>
    ) {
        setlistMaster = nuevoSetlistMaster.toMutableList()
        pdfsPorSetlistItem = nuevosPdfsPorSetlistItem
        notifyDataSetChanged()
    }

    fun moverVisualmente(fromPosition: Int, toPosition: Int) {
        if (fromPosition !in setlistMaster.indices) return
        if (toPosition !in setlistMaster.indices) return

        val item = setlistMaster.removeAt(fromPosition)
        setlistMaster.add(toPosition, item)

        notifyItemMoved(fromPosition, toPosition)
    }
}