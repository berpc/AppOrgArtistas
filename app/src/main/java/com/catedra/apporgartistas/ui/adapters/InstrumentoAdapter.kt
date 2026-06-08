package com.catedra.apporgartistas.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.data.models.Instrumento

class InstrumentoAdapter(
    private val instrumentos: MutableList<Instrumento>,
    private val onAddClick: () -> Unit,
    private val onItemClick: (Instrumento) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ADD = 0
        private const val VIEW_TYPE_INSTRUMENTO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_ADD else VIEW_TYPE_INSTRUMENTO
    }

    override fun getItemCount(): Int {
        return instrumentos.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return if (viewType == VIEW_TYPE_ADD) {
            val view = inflater.inflate(R.layout.item_add_instrumento, parent, false)
            AddInstrumentoViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_instrumento, parent, false)
            InstrumentoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AddInstrumentoViewHolder) {
            holder.itemView.setOnClickListener {
                onAddClick()
            }
        }

        if (holder is InstrumentoViewHolder) {
            val instrumento = instrumentos[position - 1]
            holder.bind(instrumento, onItemClick)
        }
    }

    fun actualizarLista(nuevaLista: List<Instrumento>) {
        instrumentos.clear()
        instrumentos.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    class AddInstrumentoViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class InstrumentoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = view.findViewById(R.id.tvNombreInstrumento)

        fun bind(
            instrumento: Instrumento,
            onClick: (Instrumento) -> Unit
        ) {
            tvNombre.text = instrumento.nombre

            itemView.setOnClickListener {
                onClick(instrumento)
            }
        }
    }
}