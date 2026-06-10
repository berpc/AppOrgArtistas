package com.catedra.apporgartistas.data.repositories

import android.util.Log
import com.catedra.apporgartistas.data.models.Instrumento
import com.catedra.apporgartistas.data.models.Setlist
import com.catedra.apporgartistas.data.models.SetlistInstrumentoSuscripto
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.data.models.ShowSetlistSuscripto
import com.catedra.apporgartistas.services.AuthService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

data class NotificacionDirector(
    val tokenDirector: String,
    val nombreSetlist: String
)

sealed class ResultadoSuscripcionSetlist {
    data class Exitosa(
        val notificacionDirector: NotificacionDirector? = null
    ) : ResultadoSuscripcionSetlist()

    object SetlistPropio : ResultadoSuscripcionSetlist()
    object YaSuscripto : ResultadoSuscripcionSetlist()
    object CodigoNoEncontrado : ResultadoSuscripcionSetlist()
    object CodigoInactivo : ResultadoSuscripcionSetlist()
    object CodigoIncompleto : ResultadoSuscripcionSetlist()
    object ErrorBusqueda : ResultadoSuscripcionSetlist()
    object ErrorSuscripcion : ResultadoSuscripcionSetlist()
}

class SetlistSubscriptionRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authService: AuthService = AuthService()
) {

    fun unirseASetlistConCodigo(
        codigo: String,
        userId: String,
        onResult: (ResultadoSuscripcionSetlist) -> Unit
    ) {
        buscarSetlistViejoPorCodigo(
            codigo = codigo,
            userId = userId,
            onNoEncontrado = {
                buscarSetlistInstrumentoPorCodigo(
                    codigo = codigo,
                    userId = userId,
                    onResult = onResult
                )
            },
            onResult = onResult
        )
    }

    fun cargarSetlistsViejos(
        userId: String,
        onSuccess: (List<Setlist>) -> Unit,
        onError: () -> Unit
    ) {
        val queryPropios = firestore.collectionGroup("setlists")
            .whereEqualTo("ownerId", userId)
            .get()

        val querySuscripciones = firestore.collectionGroup("setlists")
            .whereArrayContains("suscriptores", userId)
            .get()

        Tasks.whenAllSuccess<QuerySnapshot>(queryPropios, querySuscripciones)
            .addOnSuccessListener { results ->
                val listaCombinada = mutableListOf<Setlist>()

                for (snapshot in results) {
                    for (document in snapshot) {
                        val setlist = document.toObject(Setlist::class.java)

                        if (setlist.id.isBlank()) {
                            setlist.id = document.id
                        }

                        listaCombinada.add(setlist)
                    }
                }

                listaCombinada.sortByDescending { it.fechaCreacion }
                onSuccess(listaCombinada)
            }
            .addOnFailureListener { exception ->
                Log.e("Dashboard", "Error al cargar los setlists viejos", exception)
                onError()
            }
    }

    fun cargarSetlistsInstrumentoSuscriptos(
        userId: String,
        onSuccess: (List<SetlistInstrumentoSuscripto>) -> Unit,
        onError: () -> Unit
    ) {
        firestore.collection("codigosSetlistInstrumento")
            .whereArrayContains("suscriptores", userId)
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val tareas = snapshot.documents.map { codigoDoc ->
                    val agrupacionId = codigoDoc.getString("agrupacionId") ?: ""
                    val showId = codigoDoc.getString("showId") ?: ""
                    val instrumentoId = codigoDoc.getString("instrumentoId") ?: ""

                    val showTask = firestore.collection("agrupaciones")
                        .document(agrupacionId)
                        .collection("shows")
                        .document(showId)
                        .get()

                    val instrumentoTask = firestore.collection("agrupaciones")
                        .document(agrupacionId)
                        .collection("shows")
                        .document(showId)
                        .collection("instrumentos")
                        .document(instrumentoId)
                        .get()

                    Tasks.whenAllSuccess<DocumentSnapshot>(showTask, instrumentoTask)
                        .continueWith { task ->
                            val docs = task.result
                            val showDoc = docs[0]
                            val instrumentoDoc = docs[1]
                            val show = showDoc.toObject(Show::class.java)
                            val instrumento = instrumentoDoc.toObject(Instrumento::class.java)

                            SetlistInstrumentoSuscripto(
                                codigo = codigoDoc.getString("codigo") ?: codigoDoc.id,
                                agrupacionId = agrupacionId,
                                showId = showId,
                                instrumentoId = instrumentoId,
                                nombreShow = show?.nombre ?: "Show sin nombre",
                                fechaShow = show?.fecha,
                                nombreInstrumento = instrumento?.nombre ?: "Instrumento"
                            )
                        }
                }

                Tasks.whenAllSuccess<SetlistInstrumentoSuscripto>(tareas)
                    .addOnSuccessListener { lista ->
                        onSuccess(lista)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("Dashboard", "Error armando setlists de instrumento", exception)
                        onError()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("Dashboard", "Error cargando codigosSetlistInstrumento", exception)
                onError()
            }
    }

    fun cargarShowSetlistsSuscriptos(
        userId: String,
        onSuccess: (List<ShowSetlistSuscripto>) -> Unit,
        onError: () -> Unit
    ) {
        firestore.collection("codigosSetlistInstrumento")
            .whereArrayContains("suscriptores", userId)
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onSuccess(emptyList())
                    return@addOnSuccessListener
                }

                val tareas = snapshot.documents.map { codigoDoc ->
                    val agrupacionId = codigoDoc.getString("agrupacionId") ?: ""
                    val showId = codigoDoc.getString("showId") ?: ""
                    val instrumentoId = codigoDoc.getString("instrumentoId") ?: ""

                    val showTask = firestore.collection("agrupaciones")
                        .document(agrupacionId)
                        .collection("shows")
                        .document(showId)
                        .get()

                    val instrumentoTask = firestore.collection("agrupaciones")
                        .document(agrupacionId)
                        .collection("shows")
                        .document(showId)
                        .collection("instrumentos")
                        .document(instrumentoId)
                        .get()

                    Tasks.whenAllSuccess<DocumentSnapshot>(showTask, instrumentoTask)
                        .continueWith { task ->
                            val docs = task.result
                            val showDoc = docs[0]
                            val instrumentoDoc = docs[1]
                            val show = showDoc.toObject(Show::class.java)
                            val instrumento = instrumentoDoc.toObject(Instrumento::class.java)

                            ShowSetlistSuscripto(
                                codigo = codigoDoc.getString("codigo") ?: codigoDoc.id,
                                agrupacionId = agrupacionId,
                                showId = showId,
                                instrumentoId = instrumentoId,
                                nombreShow = show?.nombre ?: "Show sin nombre",
                                fechaShow = show?.fecha,
                                nombreInstrumento = instrumento?.nombre ?: "Instrumento"
                            )
                        }
                }

                Tasks.whenAllSuccess<ShowSetlistSuscripto>(tareas)
                    .addOnSuccessListener { lista ->
                        onSuccess(lista)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("ShowSetlist", "Error armando Show Setlists", exception)
                        onError()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("ShowSetlist", "Error cargando codigos", exception)
                onError()
            }
    }

    fun ocultarSetlist(
        userId: String,
        setlistId: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        firestore.collection("usuarios").document(userId)
            .collection("setlists").document(setlistId)
            .update("isActive", false)
            .addOnSuccessListener {
                Log.d("Dashboard", "Setlist ocultado con exito")
                onSuccess()
            }
            .addOnFailureListener { exception ->
                Log.e("Dashboard", "Error al ocultar setlist", exception)
                onError()
            }
    }

    private fun buscarSetlistViejoPorCodigo(
        codigo: String,
        userId: String,
        onNoEncontrado: () -> Unit,
        onResult: (ResultadoSuscripcionSetlist) -> Unit
    ) {
        Log.d("CODIGO_DEBUG", "Buscando codigo viejo en collectionGroup setlists: $codigo")

        firestore.collectionGroup("setlists")
            .whereEqualTo("codigoCompartir", codigo)
            .get()
            .addOnSuccessListener { result ->
                Log.d("CODIGO_DEBUG", "Resultado setlists viejos: ${result.size()} documentos")

                if (result.isEmpty) {
                    onNoEncontrado()
                    return@addOnSuccessListener
                }

                val documento = result.documents.first()
                val ownerId = documento.getString("ownerId") ?: ""
                val tituloSetlist = documento.getString("titulo") ?: "un setlist"
                val suscriptores = obtenerSuscriptores(documento)

                Log.d("CODIGO_DEBUG", "Setlist viejo encontrado: ${documento.reference.path}")
                Log.d("CODIGO_DEBUG", "ownerId=$ownerId")
                Log.d("CODIGO_DEBUG", "suscriptores actuales=$suscriptores")

                if (ownerId == userId) {
                    Log.d("CODIGO_DEBUG", "El usuario actual es el duenio del setlist viejo")
                    onResult(ResultadoSuscripcionSetlist.SetlistPropio)
                    return@addOnSuccessListener
                }

                if (suscriptores.contains(userId)) {
                    Log.d("CODIGO_DEBUG", "El usuario ya esta suscrito al setlist viejo")
                    onResult(ResultadoSuscripcionSetlist.YaSuscripto)
                    return@addOnSuccessListener
                }

                documento.reference.update("suscriptores", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        Log.d("CODIGO_DEBUG", "Usuario agregado a suscriptores del setlist viejo")
                        obtenerNotificacionDirector(ownerId, tituloSetlist, onResult)
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "CODIGO_DEBUG",
                            "Error al actualizar suscriptores del setlist viejo",
                            exception
                        )
                        onResult(ResultadoSuscripcionSetlist.ErrorSuscripcion)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("CODIGO_DEBUG", "Error buscando codigo viejo", exception)
                onResult(ResultadoSuscripcionSetlist.ErrorBusqueda)
            }
    }

    private fun buscarSetlistInstrumentoPorCodigo(
        codigo: String,
        userId: String,
        onResult: (ResultadoSuscripcionSetlist) -> Unit
    ) {
        Log.d("CODIGO_DEBUG", "Buscando codigo de instrumento en codigosSetlistInstrumento/$codigo")

        firestore.collection("codigosSetlistInstrumento")
            .document(codigo)
            .get()
            .addOnSuccessListener { document ->
                Log.d("CODIGO_DEBUG", "Documento codigo instrumento existe: ${document.exists()}")

                if (!document.exists()) {
                    Log.d("CODIGO_DEBUG", "No existe documento para codigo: $codigo")
                    onResult(ResultadoSuscripcionSetlist.CodigoNoEncontrado)
                    return@addOnSuccessListener
                }

                Log.d("CODIGO_DEBUG", "Documento encontrado: ${document.reference.path}")
                Log.d("CODIGO_DEBUG", "Data: ${document.data}")

                val activo = document.getBoolean("activo") ?: true

                if (!activo) {
                    Log.d("CODIGO_DEBUG", "El codigo existe pero esta inactivo")
                    onResult(ResultadoSuscripcionSetlist.CodigoInactivo)
                    return@addOnSuccessListener
                }

                val directorId = document.getString("directorId") ?: ""
                val agrupacionId = document.getString("agrupacionId") ?: ""
                val showId = document.getString("showId") ?: ""
                val instrumentoId = document.getString("instrumentoId") ?: ""
                val suscriptores = obtenerSuscriptores(document)

                Log.d("CODIGO_DEBUG", "directorId=$directorId")
                Log.d("CODIGO_DEBUG", "agrupacionId=$agrupacionId")
                Log.d("CODIGO_DEBUG", "showId=$showId")
                Log.d("CODIGO_DEBUG", "instrumentoId=$instrumentoId")
                Log.d("CODIGO_DEBUG", "suscriptores actuales=$suscriptores")
                Log.d("CODIGO_DEBUG", "userId actual=$userId")

                if (directorId == userId) {
                    Log.d("CODIGO_DEBUG", "El usuario actual ES el director. No se agrega a suscriptores.")
                    onResult(ResultadoSuscripcionSetlist.SetlistPropio)
                    return@addOnSuccessListener
                }

                if (agrupacionId.isBlank() || showId.isBlank() || instrumentoId.isBlank()) {
                    Log.d("CODIGO_DEBUG", "Codigo incompleto")
                    onResult(ResultadoSuscripcionSetlist.CodigoIncompleto)
                    return@addOnSuccessListener
                }

                if (suscriptores.contains(userId)) {
                    Log.d("CODIGO_DEBUG", "El usuario ya estaba suscrito")
                    onResult(ResultadoSuscripcionSetlist.YaSuscripto)
                    return@addOnSuccessListener
                }

                Log.d("CODIGO_DEBUG", "Intentando agregar userId a suscriptores...")

                document.reference.update(
                    "suscriptores",
                    FieldValue.arrayUnion(userId)
                )
                    .addOnSuccessListener {
                        Log.d(
                            "CODIGO_DEBUG",
                            "Usuario agregado correctamente a suscriptores del codigo de instrumento"
                        )
                        onResult(ResultadoSuscripcionSetlist.Exitosa())
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "CODIGO_DEBUG",
                            "Error al actualizar suscriptores del codigo de instrumento",
                            exception
                        )
                        onResult(ResultadoSuscripcionSetlist.ErrorSuscripcion)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("CODIGO_DEBUG", "Error buscando codigo de instrumento", exception)
                onResult(ResultadoSuscripcionSetlist.ErrorBusqueda)
            }
    }

    private fun obtenerNotificacionDirector(
        ownerId: String,
        tituloSetlist: String,
        onResult: (ResultadoSuscripcionSetlist) -> Unit
    ) {
        authService.obtenerTokenFcmDeUsuario(
            userId = ownerId,
            onSuccess = { tokenDelDirector ->
                val notificacion = tokenDelDirector
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { token ->
                        NotificacionDirector(
                            tokenDirector = token,
                            nombreSetlist = tituloSetlist
                        )
                    }

                onResult(ResultadoSuscripcionSetlist.Exitosa(notificacion))
            },
            onError = { error ->
                Log.e(
                    "CODIGO_DEBUG",
                    "Fallo buscar usuario duenio, pero suscripcion vieja fue exitosa: $error"
                )
                onResult(ResultadoSuscripcionSetlist.Exitosa())
            }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun obtenerSuscriptores(document: DocumentSnapshot): List<String> {
        return document.get("suscriptores") as? List<String> ?: emptyList()
    }
}
