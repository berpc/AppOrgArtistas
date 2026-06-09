package com.catedra.apporgartistas.data.models

data class Instrumento(
    val id: String = "",
    val nombre: String = "",
    val codigoAcceso: String = "",
    val pdfsPorSetlistItem: Map<String, PartituraCloud> = emptyMap(),
    val activo: Boolean = true
)