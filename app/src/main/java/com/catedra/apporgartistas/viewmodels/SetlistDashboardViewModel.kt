package com.catedra.apporgartistas.viewmodels
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Setlist
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.QuerySnapshot
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.catedra.apporgartistas.data.models.SetlistInstrumentoSuscripto
import com.catedra.apporgartistas.data.models.Show
import com.catedra.apporgartistas.data.models.Instrumento
import com.google.firebase.firestore.DocumentSnapshot
class SetlistDashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _setlists = MutableLiveData<List<Setlist>>()
    private val _setlistsInstrumentoSuscriptos = MutableLiveData<List<SetlistInstrumentoSuscripto>>()
    val setlistsInstrumentoSuscriptos: LiveData<List<SetlistInstrumentoSuscripto>> = _setlistsInstrumentoSuscriptos
    val setlists: LiveData<List<Setlist>> = _setlists
    private val _suscripcionExitosa = MutableLiveData<Boolean>()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    val suscripcionExitosa: LiveData<Boolean> = _suscripcionExitosa
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Espera 60 segundos para conectar
        .readTimeout(60, TimeUnit.SECONDS)    // Espera 60 segundos por la respuesta
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"
    }
    fun unirseASetlistConCodigo(codigo: String) {
        val userId = getCurrentUserId()
        val codigoNormalizado = codigo.trim().uppercase()

        Log.d("CODIGO_DEBUG", "Código ingresado original: '$codigo'")
        Log.d("CODIGO_DEBUG", "Código normalizado: '$codigoNormalizado'")
        Log.d("CODIGO_DEBUG", "Usuario actual: '$userId'")

        if (codigoNormalizado.isBlank()) {
            Log.d("CODIGO_DEBUG", "Código vacío")
            _error.postValue("Ingresá un código.")
            return
        }

        if (userId.isBlank()) {
            Log.d("CODIGO_DEBUG", "Usuario no autenticado")
            _error.postValue("Usuario no autenticado.")
            return
        }

        _isLoading.value = true

        buscarSetlistViejoPorCodigo(
            codigo = codigoNormalizado,
            userId = userId,
            onNoEncontrado = {
                Log.d("CODIGO_DEBUG", "No era código viejo. Buscando código de instrumento...")
                buscarSetlistInstrumentoPorCodigo(
                    codigo = codigoNormalizado,
                    userId = userId
                )
            }
        )
    }
    private fun buscarSetlistViejoPorCodigo(
        codigo: String,
        userId: String,
        onNoEncontrado: () -> Unit
    ) {
        Log.d("CODIGO_DEBUG", "Buscando código viejo en collectionGroup setlists: $codigo")

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
                val suscriptores = documento.get("suscriptores") as? List<String> ?: emptyList()

                Log.d("CODIGO_DEBUG", "Setlist viejo encontrado: ${documento.reference.path}")
                Log.d("CODIGO_DEBUG", "ownerId=$ownerId")
                Log.d("CODIGO_DEBUG", "suscriptores actuales=$suscriptores")

                if (ownerId == userId) {
                    Log.d("CODIGO_DEBUG", "El usuario actual es el dueño del setlist viejo")
                    _error.postValue("Este setlist ya es tuyo, no necesitas unirte.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                if (suscriptores.contains(userId)) {
                    Log.d("CODIGO_DEBUG", "El usuario ya está suscrito al setlist viejo")
                    _error.postValue("Ya estás suscrito a este setlist.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                documento.reference.update("suscriptores", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        Log.d("CODIGO_DEBUG", "Usuario agregado a suscriptores del setlist viejo")

                        firestore.collection("usuarios").document(ownerId).get()
                            .addOnSuccessListener { userDoc ->
                                val tokenDelDirector = userDoc.getString("fcmToken")

                                if (!tokenDelDirector.isNullOrEmpty()) {
                                    enviarNotificacionAlDirector(
                                        tokenDelDirector = tokenDelDirector,
                                        nombreInvitado = "Un compañero",
                                        nombreSetlist = tituloSetlist
                                    )
                                }

                                _suscripcionExitosa.postValue(true)
                                _isLoading.postValue(false)
                            }
                            .addOnFailureListener { e ->
                                Log.e("CODIGO_DEBUG", "Falló buscar usuario dueño, pero suscripción vieja fue exitosa", e)
                                _suscripcionExitosa.postValue(true)
                                _isLoading.postValue(false)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("CODIGO_DEBUG", "Error al actualizar suscriptores del setlist viejo", e)
                        _error.postValue("Error al suscribirse al setlist.")
                        _isLoading.postValue(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CODIGO_DEBUG", "Error buscando código viejo", e)
                _error.postValue("Error al buscar el código.")
                _isLoading.postValue(false)
            }
    }
    private fun buscarSetlistInstrumentoPorCodigo(
        codigo: String,
        userId: String
    ) {
        Log.d("CODIGO_DEBUG", "Buscando código de instrumento en codigosSetlistInstrumento/$codigo")

        firestore.collection("codigosSetlistInstrumento")
            .document(codigo)
            .get()
            .addOnSuccessListener { document ->
                Log.d("CODIGO_DEBUG", "Documento código instrumento existe: ${document.exists()}")

                if (!document.exists()) {
                    Log.d("CODIGO_DEBUG", "No existe documento para código: $codigo")
                    _error.postValue("Código inválido o setlist no encontrado.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                Log.d("CODIGO_DEBUG", "Documento encontrado: ${document.reference.path}")
                Log.d("CODIGO_DEBUG", "Data: ${document.data}")

                val activo = document.getBoolean("activo") ?: true

                if (!activo) {
                    Log.d("CODIGO_DEBUG", "El código existe pero está inactivo")
                    _error.postValue("Este código ya no está activo.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                val directorId = document.getString("directorId") ?: ""
                val agrupacionId = document.getString("agrupacionId") ?: ""
                val showId = document.getString("showId") ?: ""
                val instrumentoId = document.getString("instrumentoId") ?: ""
                val suscriptores = document.get("suscriptores") as? List<String> ?: emptyList()

                Log.d("CODIGO_DEBUG", "directorId=$directorId")
                Log.d("CODIGO_DEBUG", "agrupacionId=$agrupacionId")
                Log.d("CODIGO_DEBUG", "showId=$showId")
                Log.d("CODIGO_DEBUG", "instrumentoId=$instrumentoId")
                Log.d("CODIGO_DEBUG", "suscriptores actuales=$suscriptores")
                Log.d("CODIGO_DEBUG", "userId actual=$userId")

                if (directorId == userId) {
                    Log.d("CODIGO_DEBUG", "El usuario actual ES el director. No se agrega a suscriptores.")
                    _error.postValue("Este setlist ya es tuyo, no necesitas unirte.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                if (agrupacionId.isBlank() || showId.isBlank() || instrumentoId.isBlank()) {
                    Log.d("CODIGO_DEBUG", "Código incompleto")
                    _error.postValue("El código está incompleto.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                if (suscriptores.contains(userId)) {
                    Log.d("CODIGO_DEBUG", "El usuario ya estaba suscrito")
                    _error.postValue("Ya estás suscrito a este setlist.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                Log.d("CODIGO_DEBUG", "Intentando agregar userId a suscriptores...")

                document.reference.update(
                    "suscriptores",
                    FieldValue.arrayUnion(userId)
                )
                    .addOnSuccessListener {
                        Log.d("CODIGO_DEBUG", "Usuario agregado correctamente a suscriptores del código de instrumento")

                        _suscripcionExitosa.postValue(true)
                        _isLoading.postValue(false)
                    }
                    .addOnFailureListener { e ->
                        Log.e("CODIGO_DEBUG", "Error al actualizar suscriptores del código de instrumento", e)

                        _error.postValue("Error al suscribirse al setlist.")
                        _isLoading.postValue(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("CODIGO_DEBUG", "Error buscando código de instrumento", e)

                _error.postValue("Error al buscar el código.")
                _isLoading.postValue(false)
            }
    }
    private fun enviarNotificacionAlDirector(tokenDelDirector: String, nombreInvitado: String, nombreSetlist: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // REEMPLAZÁ ESTA URL POR LA TUYA DE RENDER
                val url = "https://messaging-service-lfyh.onrender.com/send-notification"

                val jsonBody = JSONObject().apply {
                    put("token", tokenDelDirector)
                    put("title", "¡Nuevo músico en tu setlist!")
                    put("body", "$nombreInvitado se unió a: $nombreSetlist")
                }

                val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute()

            } catch (e: Exception) {
                println("Error al disparar notificación: ${e.message}")
            }
        }
    }

    fun cargarSetlists() {
        val userId = getCurrentUserId()
        _isLoading.value = true

        cargarSetlistsViejos(userId)
        cargarSetlistsInstrumentoSuscriptos(userId)
    }
    private fun cargarSetlistsViejos(userId: String) {
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

                _setlists.postValue(listaCombinada)
                _isLoading.postValue(false)
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error al cargar los setlists viejos", e)
                _error.postValue("Error al cargar tu repertorio.")
                _isLoading.postValue(false)
            }
    }
    private fun cargarSetlistsInstrumentoSuscriptos(userId: String) {
        firestore.collection("codigosSetlistInstrumento")
            .whereArrayContains("suscriptores", userId)
            .whereEqualTo("activo", true)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    _setlistsInstrumentoSuscriptos.postValue(emptyList())
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
                        _setlistsInstrumentoSuscriptos.postValue(lista)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Dashboard", "Error armando setlists de instrumento", e)
                        _setlistsInstrumentoSuscriptos.postValue(emptyList())
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error cargando codigosSetlistInstrumento", e)
                _setlistsInstrumentoSuscriptos.postValue(emptyList())
            }
    }
    // soft delete
    fun ocultarSetlist(setlistId: String) {
        val userId = getCurrentUserId()
        firestore.collection("usuarios").document(userId)
            .collection("setlists").document(setlistId)
            // Actualizamos el flag a false
            .update("isActive", false)
            .addOnSuccessListener {
                Log.d("Dashboard", "Setlist ocultado con éxito")
                // Recargamos la lista para que desaparezca de la UI
                cargarSetlists()
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error al ocultar setlist", e)
            }
    }
}