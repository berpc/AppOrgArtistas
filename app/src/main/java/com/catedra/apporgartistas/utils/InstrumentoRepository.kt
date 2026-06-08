package com.catedra.apporgartistas.utils

import com.catedra.apporgartistas.data.models.Instrumento
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class InstrumentoRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun obtenerInstrumentos(
        showId: String,
        agrupacionId: String
    ): List<Instrumento> {
        val snapshot = db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .whereEqualTo("activo", true)
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(Instrumento::class.java)
        }
    }

    suspend fun agregarInstrumento(
        showId: String,
        agrupacionId: String,
        instrumento: Instrumento
    ) {
        val docRef = db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document()

        val nuevoInstrumento = instrumento.copy(id = docRef.id)

        docRef.set(nuevoInstrumento).await()
    }
    suspend fun actualizarPdfDeInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        setlistItemId: String,
        urlPdf: String
    ) {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .update("pdfsPorSetlistItem.$setlistItemId", urlPdf)
            .await()
    }
}