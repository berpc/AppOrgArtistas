package com.catedra.apporgartistas.ui.adapters

import android.R.attr.singleLine
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.SetlistMasterItem

class SetlistMatrixAdapter(
    private var canciones: MutableList<SetlistMasterItem>,
    private var instrumentos: List<Instrumento>,
    private val onCrearCancionConfirmada: (nombre: String) -> Unit,
    private val onEditarCancionConfirmada: (setlistItem: SetlistMasterItem, nuevoNombre: String) -> Unit,
    private val onBorrarCancionConfirmada: (setlistItem: SetlistMasterItem) -> Unit,
    private val onCeldaClick: (instrumento: Instrumento, setlistItem: SetlistMasterItem) -> Unit
) : RecyclerView.Adapter<SetlistMatrixAdapter.MatrixViewHolder>(){

    class MatrixViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MatrixViewHolder {
        val layout = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                64
            )
        }

        return MatrixViewHolder(layout)
    }

    override fun onBindViewHolder(holder: MatrixViewHolder, position: Int) {
        holder.layout.removeAllViews()

        val esFilaNueva = position == canciones.size

        if (esFilaNueva) {
            crearFilaNueva(holder)
        } else {
            crearFilaExistente(holder, position)
        }
    }

    private fun crearFilaNueva(holder: MatrixViewHolder) {
        val context = holder.itemView.context

        val tvOrdenVacio = TextView(context).apply {
            text = ""
            layoutParams = LinearLayout.LayoutParams(
                40,
                56
            ).apply {
                marginStart = 4
            }
        }

        val inputNuevaObra = EditText(context).apply {
            hint = "Escribir obra..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f

            maxLines = 1
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            background = null
            imeOptions = EditorInfo.IME_ACTION_DONE

            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            setPadding(0, 0, 0, 0)

            layoutParams = LinearLayout.LayoutParams(
                216,
                56
            ).apply {
                marginEnd = 4
            }
        }

        inputNuevaObra.setOnEditorActionListener { _, actionId, event ->
            val presionoEnterFisico =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP

            val presionoDoneTeclado =
                actionId == EditorInfo.IME_ACTION_DONE

            if (presionoEnterFisico || presionoDoneTeclado) {
                val nombre = inputNuevaObra.text.toString().trim()

                if (nombre.isNotBlank()) {
                    onCrearCancionConfirmada(nombre)
                    inputNuevaObra.setText("")
                }

                true
            } else {
                false
            }
        }

        holder.layout.addView(tvOrdenVacio)
        holder.layout.addView(inputNuevaObra)

        instrumentos.forEach {
            holder.layout.addView(crearCeldaVacia(context))
        }
    }

    private fun crearFilaExistente(holder: MatrixViewHolder, position: Int) {
        val context = holder.itemView.context
        val setlistItem = canciones[position]

        val tvOrden = TextView(context).apply {
            text = "${position + 1}-"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false

            layoutParams = LinearLayout.LayoutParams(
                40,
                56
            ).apply {
                marginStart = 4
            }
        }

        val inputObra = EditText(context).apply {
            setText(setlistItem.nombre)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f

            maxLines = 1
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            background = null
            imeOptions = EditorInfo.IME_ACTION_DONE

            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = false
            setPadding(0, 0, 0, 0)

            layoutParams = LinearLayout.LayoutParams(
                216,
                56
            ).apply {
                marginEnd = 4
            }
        }

        inputObra.setOnEditorActionListener { _, actionId, event ->
            val presionoEnterFisico =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP

            val presionoDoneTeclado =
                actionId == EditorInfo.IME_ACTION_DONE

            if (presionoEnterFisico || presionoDoneTeclado) {
                val nuevoNombre = inputObra.text.toString().trim()

                if (nuevoNombre.isBlank()) {
                    onBorrarCancionConfirmada(setlistItem)
                } else if (nuevoNombre != setlistItem.nombre) {
                    onEditarCancionConfirmada(setlistItem, nuevoNombre)
                }

                inputObra.clearFocus()
                true
            } else {
                false
            }
        }

        holder.layout.addView(tvOrden)
        holder.layout.addView(inputObra)

        instrumentos.forEach { instrumento ->
            val tienePdf = instrumento.pdfsPorSetlistItem[setlistItem.id].isNullOrBlank().not()

            val celda = TextView(context).apply {
                text = if (tienePdf) "✓" else "✗"
                setTextColor(
                    if (tienePdf) Color.parseColor("#22C55E")
                    else Color.parseColor("#EF4444")
                )
                textSize = 22f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.parseColor("#1E293B"))

                layoutParams = LinearLayout.LayoutParams(
                    120,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    marginStart = 4
                    marginEnd = 4
                    topMargin = 4
                    bottomMargin = 4
                }

                setOnClickListener {
                    onCeldaClick(instrumento, setlistItem)
                }
            }

            holder.layout.addView(celda)
        }
    }

    private fun crearCeldaVacia(context: android.content.Context): TextView {
        return TextView(context).apply {
            text = ""
            setBackgroundColor(Color.parseColor("#0F172A"))

            layoutParams = LinearLayout.LayoutParams(120, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginStart = 4
                marginEnd = 4
                topMargin = 4
                bottomMargin = 4
            }
        }
    }

    override fun getItemCount(): Int {
        return canciones.size + 1
    }

    fun actualizarDatos(
        nuevasCanciones: List<SetlistMasterItem>,
        nuevosInstrumentos: List<Instrumento>
    ) {
        canciones = nuevasCanciones.toMutableList()
        instrumentos = nuevosInstrumentos
        notifyDataSetChanged()
    }
}