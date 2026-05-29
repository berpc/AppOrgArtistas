package com.catedra.apporgartistas.data.models
import java.io.Serializable
data class Setlist(
    var id: String = "", // El ID único del documento en Firebase
    var titulo: String = "",
    var nombreGrupo: String = "",
    var ubicacion: String = "",
    var fechaCreacion: Long = 0,
    var partituras: List<PartituraCloud> = emptyList()
) : Serializable