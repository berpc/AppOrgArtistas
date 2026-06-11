package com.catedra.apporgartistas.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.ui.activities.ShowSetlistViewerActivity
import com.catedra.apporgartistas.ui.adapters.ShowSetlistAdapter
import com.catedra.apporgartistas.viewmodels.ShowSetlistViewModel

class ShowSetlistFragment : Fragment(R.layout.fragment_show_setlist) {

    private val viewModel: ShowSetlistViewModel by viewModels()

    private lateinit var rvShowSetlists: RecyclerView
    private lateinit var adapter: ShowSetlistAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecycler(view)
        observarViewModel()

        viewModel.cargarShowSetlists()
    }

    private fun configurarRecycler(view: View) {
        rvShowSetlists = view.findViewById(R.id.rvShowSetlists)
        rvShowSetlists.layoutManager = LinearLayoutManager(requireContext())

        adapter = ShowSetlistAdapter(
            items = emptyList(),
            onItemClick = { item ->
                val intent = Intent(requireContext(), ShowSetlistViewerActivity::class.java).apply {
                    putExtra("CODIGO", item.codigo)
                    putExtra("AGRUPACION_ID", item.agrupacionId)
                    putExtra("SHOW_ID", item.showId)
                    putExtra("INSTRUMENTO_ID", item.instrumentoId)
                }

                startActivity(intent)
            }
        )

        rvShowSetlists.adapter = adapter
    }

    private fun observarViewModel() {
        viewModel.showSetlists.observe(viewLifecycleOwner) { lista ->
            adapter.actualizarLista(lista)
        }

        viewModel.error.observe(viewLifecycleOwner) { mensaje ->
            Toast.makeText(
                requireContext(),
                mensaje,
                if (mensaje == "Usuario no autenticado") Toast.LENGTH_SHORT else Toast.LENGTH_LONG
            ).show()
        }
    }
}
