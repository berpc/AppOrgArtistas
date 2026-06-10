package com.catedra.apporgartistas.data.repositories

import android.util.Log
import com.catedra.apporgartistas.data.models.PartituraCloud
import com.catedra.apporgartistas.data.models.Setlist
import com.google.firebase.firestore.FirebaseFirestore

class SetlistRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun obtenerTodasLasPartiturasDeLaNube(
        userId: String,
        onSuccess: (List<PartituraCloud>) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("usuarios")
            .document(userId)
            .collection("setlists")
            .get()
            .addOnSuccessListener { result ->
                val todasLasPartituras = mutableListOf<PartituraCloud>()

                for (document in result) {
                    val setlist = document.toObject(Setlist::class.java)
                    todasLasPartituras.addAll(setlist.partituras)
                }

                val partiturasUnicas = todasLasPartituras.distinctBy { it.url }

                onSuccess(partiturasUnicas)
            }
            .addOnFailureListener { exception ->
                Log.e("SetlistRepository", "Error al obtener partituras", exception)
                onError("Error al cargar partituras de la nube")
            }
    }
    fun actualizarPartituras(
        userId: String,
        setlistId: String,
        nuevasPartituras: List<PartituraCloud>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ){
        if(setlistId.isBlank()){
            onError("Error: El Setlist no tiene un ID válido")
            return
        }
        firestore.collection("usuarios")
            .document(userId)
            .collection("setlists")
            .document(setlistId)
            .update("partituras", nuevasPartituras)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("SetlistRepository", "Error al actualizar partituras", exception)
                onError("Error al guardar en la base de datos")
            }

    }
    fun crearSetlist(
        userId: String,
        titulo: String,
        nombreGrupo: String,
        ubicacion: String,
        partituras: List<PartituraCloud>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ){
        val codigo = generarCodigoAleatorio()
        val datosSetlist = hashMapOf(
            "titulo" to titulo,
            "nombreGrupo" to nombreGrupo,
            "ubicacion" to ubicacion,
            "fechaCreacion" to System.currentTimeMillis(),
            "partituras" to partituras,
            "isActive" to true,
            "ownerId" to userId,
            "codigoCompartir" to codigo,
            "suscriptores" to emptyList<String>()
        )
        firestore.collection("usuarios").document(userId)
            .collection("setlists")
            .add(datosSetlist)
            .addOnSuccessListener { documentReference ->
                Log.d("SetlistRepository", "Setlist guardado con ID: ${documentReference.id}")
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { exception ->
                Log.e("SetlistRepository", "Error al guardar setlist", exception)
                onError("Error al guardar en Firestore")
            }
    }
    private fun generarCodigoAleatorio(): String {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { caracteres.random() }.joinToString("")
    }

}