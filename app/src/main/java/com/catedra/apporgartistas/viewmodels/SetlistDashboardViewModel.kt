package com.catedra.apporgartistas.viewmodels
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.catedra.apporgartistas.data.models.Setlist
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
class SetlistDashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _setlists = MutableLiveData<List<Setlist>>()
    val setlists: LiveData<List<Setlist>> = _setlists

    fun cargarSetlists(userId: String) {
        Log.d("Dashboard", "Buscando setlists para el usuario: $userId")

        firestore.collection("usuarios").document(userId)
            .collection("setlists")
            .whereEqualTo("isActive", true)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                // ¡Acá sabremos si Firebase nos devuelve datos!
                Log.d("Dashboard", "Se encontraron ${snapshot.documents.size} setlists en la base de datos.")

                val listaTemporal = mutableListOf<Setlist>()

                for (documento in snapshot.documents) {
                    try {
                        val setlist = documento.toObject(Setlist::class.java)
                        if (setlist != null) {
                            setlist.id = documento.id
                            listaTemporal.add(setlist)
                        }
                    } catch (e: Exception) {
                        Log.e("Dashboard", "Error al convertir documento: ${documento.id}", e)
                    }
                }
                _setlists.value = listaTemporal
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error al conectarse a Firestore", e)
            }
    }
    // soft delete
    fun ocultarSetlist(userId: String, setlistId: String) {
        firestore.collection("usuarios").document(userId)
            .collection("setlists").document(setlistId)
            // Actualizamos el flag a false
            .update("isActive", false)
            .addOnSuccessListener {
                Log.d("Dashboard", "Setlist ocultado con éxito")
                // Recargamos la lista para que desaparezca de la UI
                cargarSetlists(userId)
            }
            .addOnFailureListener { e ->
                Log.e("Dashboard", "Error al ocultar setlist", e)
            }
    }
}