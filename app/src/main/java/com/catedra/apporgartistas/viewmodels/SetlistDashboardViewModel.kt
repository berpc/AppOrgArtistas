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
class SetlistDashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _setlists = MutableLiveData<List<Setlist>>()
    val setlists: LiveData<List<Setlist>> = _setlists
    private val _suscripcionExitosa = MutableLiveData<Boolean>()
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    val suscripcionExitosa: LiveData<Boolean> = _suscripcionExitosa
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: "usuario_anonimo"
    }
    fun unirseASetlistConCodigo(codigo: String) {
        val userId = getCurrentUserId()

        _isLoading.value = true

        // Buscamos en TODOS los setlists de todos los usuarios
        firestore.collectionGroup("setlists")
            .whereEqualTo("codigoCompartir", codigo.uppercase())
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    _error.postValue("Código inválido o setlist no encontrado.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                // Encontramos el setlist compartido
                val documento = result.documents.first()
                val ownerId = documento.getString("ownerId") ?: ""

                // Firestore devuelve las listas como ArrayList, hacemos un cast seguro
                val suscriptores = documento.get("suscriptores") as? List<String> ?: emptyList()

                // Validaciones para que no haga cosas raras
                if (ownerId == userId) {
                    _error.postValue("Este setlist ya es tuyo, no necesitas unirte.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }
                if (suscriptores.contains(userId)) {
                    _error.postValue("Ya estás suscrito a este setlist.")
                    _isLoading.postValue(false)
                    return@addOnSuccessListener
                }

                // Si todo está bien, lo agregamos al array "suscriptores"
                documento.reference.update("suscriptores", FieldValue.arrayUnion(userId))
                    .addOnSuccessListener {
                        _suscripcionExitosa.postValue(true)
                        _isLoading.postValue(false)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Dashboard", "Error al actualizar suscriptores", e)
                        _error.postValue("Error al suscribirse al setlist.")
                        _isLoading.postValue(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error buscando código", e)
                _error.postValue("Error al buscar el código.")
                _isLoading.postValue(false)
            }
    }

    fun cargarSetlists() {
        val userId = getCurrentUserId()
        _isLoading.value = true

        // 1. Consulta A: Mis propios Setlists (donde soy ownerId)
        val queryPropios = firestore.collectionGroup("setlists")
            .whereEqualTo("ownerId", userId)
            .get()

        // 2. Consulta B: Setlists compartidos (donde estoy en el array suscriptores)
        val querySuscripciones = firestore.collectionGroup("setlists")
            .whereArrayContains("suscriptores", userId)
            .get()

        // 3. Ejecutamos ambas al mismo tiempo y esperamos que terminen
        Tasks.whenAllSuccess<QuerySnapshot>(queryPropios, querySuscripciones)
            .addOnSuccessListener { results ->
                val listaCombinada = mutableListOf<Setlist>()

                // results[0] tiene los tuyos, results[1] tiene las suscripciones
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
                Log.e("Dashboard", "Error al cargar los setlists combinados", e)
                _error.postValue("Error al cargar tu repertorio.")
                _isLoading.postValue(false)
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