package com.catedra.apporgartistas.utils

import com.catedra.apporgartistas.data.models.SetlistMasterItem
import com.catedra.apporgartistas.data.models.Show
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ShowDetailRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun getShowRef(
        agrupacionId: String,
        showId: String
    ) = db.collection("agrupaciones")
        .document(agrupacionId)
        .collection("shows")
        .document(showId)

    suspend fun actualizarSetlistMaster(
        agrupacionId: String,
        showId: String,
        nuevoSetlistMaster: List<SetlistMasterItem>
    ) {
        getShowRef(agrupacionId, showId)
            .update("setlistMaster", nuevoSetlistMaster)
            .await()
    }

    suspend fun guardarPdfDeInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        setlistItemId: String,
        urlPdf: String
    ) {
        getShowRef(agrupacionId, showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .update("pdfsPorSetlistItem.$setlistItemId", urlPdf)
            .await()
    }
    fun observarShow(
        agrupacionId: String,
        showId: String,
        onChange: (Show?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onChange(snapshot?.toObject(Show::class.java))
            }
    }
}