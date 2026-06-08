package com.catedra.apporgartistas.data.models

data class Agrupacion(
    val id: String = "",
    val nombre: String = "",
    val directorId: String = "",
    val isActive: Boolean = true // Nueva flag para el soft delete
)