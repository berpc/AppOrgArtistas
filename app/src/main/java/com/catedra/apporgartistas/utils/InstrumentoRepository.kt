package com.catedra.apporgartistas.utils

import com.catedra.apporgartistas.data.models.CodigoSetlistInstrumento
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.Partitura
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.Setlist
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom

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
    suspend fun actualizarPartituraDeInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        setlistItemId: String,
        partitura: PartituraCloud
    ) {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .update("pdfsPorSetlistItem.$setlistItemId", partitura)
            .await()
    }
    fun observarInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        onChange: (Instrumento?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onChange(snapshot?.toObject(Instrumento::class.java))
            }
    }

    suspend fun actualizarPartituraDeInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        setlistItemId: String,
        partitura: Partitura
    ) {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .update("pdfsPorSetlistItem.$setlistItemId", partitura)
            .await()
    }

    suspend fun eliminarPartituraDeInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        setlistItemId: String
    ) {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .update("pdfsPorSetlistItem.$setlistItemId", FieldValue.delete())
            .await()
    }

    suspend fun intercambiarPartituras(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        fromSetlistItemId: String,
        toSetlistItemId: String,
        pdfsActuales: Map<String, PartituraCloud>
    ) {
        val partituraFrom = pdfsActuales[fromSetlistItemId]
        val partituraTo = pdfsActuales[toSetlistItemId]

        val updates = mutableMapOf<String, Any>()

        if (partituraTo != null) {
            updates["pdfsPorSetlistItem.$fromSetlistItemId"] = partituraTo
        } else {
            updates["pdfsPorSetlistItem.$fromSetlistItemId"] = FieldValue.delete()
        }

        if (partituraFrom != null) {
            updates["pdfsPorSetlistItem.$toSetlistItemId"] = partituraFrom
        } else {
            updates["pdfsPorSetlistItem.$toSetlistItemId"] = FieldValue.delete()
        }

        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .update(updates)
            .await()
    }
    suspend fun guardarMapaPartiturasInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        nuevoMapa: Map<String, PartituraCloud>
    ) {
        db.collection("agrupaciones")
            .document(agrupacionId)
            .collection("shows")
            .document(showId)
            .collection("instrumentos")
            .document(instrumentoId)
            .update("pdfsPorSetlistItem", nuevoMapa)
            .await()
    }

    suspend fun obtenerTodasLasPartiturasCloudDelUsuario(
        userId: String
    ): List<PartituraCloud> {
        val snapshot = db.collection("usuarios")
            .document(userId)
            .collection("setlists")
            .get()
            .await()

        val todasLasPartituras = mutableListOf<PartituraCloud>()

        snapshot.documents.forEach { document ->
            val setlist = document.toObject(Setlist::class.java)

            if (setlist != null) {
                todasLasPartituras.addAll(setlist.partituras)
            }
        }

        return todasLasPartituras.distinctBy { it.url }
    }
    suspend fun obtenerOCrearCodigoSetlistInstrumento(
        agrupacionId: String,
        showId: String,
        instrumentoId: String,
        directorId: String
    ): String {
        val codigosRef = db.collection("codigosSetlistInstrumento")

        val codigoExistente = codigosRef
            .whereEqualTo("agrupacionId", agrupacionId)
            .whereEqualTo("showId", showId)
            .whereEqualTo("instrumentoId", instrumentoId)
            .whereEqualTo("activo", true)
            .limit(1)
            .get()
            .await()
            .documents
            .firstOrNull()
            ?.getString("codigo")

        if (!codigoExistente.isNullOrBlank()) {
            return codigoExistente
        }

        repeat(10) {
            val nuevoCodigo = generarCodigoAcceso()

            val docExistente = codigosRef
                .document(nuevoCodigo)
                .get()
                .await()

            if (!docExistente.exists()) {
                val nuevoDocumento = CodigoSetlistInstrumento(
                    codigo = nuevoCodigo,
                    agrupacionId = agrupacionId,
                    showId = showId,
                    instrumentoId = instrumentoId,
                    directorId = directorId,
                    suscriptores = emptyList(),
                    activo = true
                )

                codigosRef
                    .document(nuevoCodigo)
                    .set(nuevoDocumento)
                    .await()

                return nuevoCodigo
            }
        }

        throw Exception("No se pudo generar un código único")
    }

    private fun generarCodigoAcceso(): String {
        val caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()

        return (1..6)
            .map { caracteres[random.nextInt(caracteres.length)] }
            .joinToString("")
    }

}