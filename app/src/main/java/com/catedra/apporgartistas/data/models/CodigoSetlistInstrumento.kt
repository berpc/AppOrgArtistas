package com.catedra.apporgartistas.data.models

data class CodigoSetlistInstrumento(
    val codigo: String = "",
    val agrupacionId: String = "",
    val showId: String = "",
    val instrumentoId: String = "",
    val directorId: String = "",
    val suscriptores: List<String> = emptyList(),
    val activo: Boolean = true
)