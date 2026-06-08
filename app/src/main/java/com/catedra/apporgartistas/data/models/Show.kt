package com.catedra.apporgartistas.data.models


data class Show(
    val id: String = "",
    val nombre: String = "",
    val fecha: String? = null, // Puede ser nulo como pediste
    val setlistMaster: List<SetlistMasterItem> = emptyList(),
    val active: Boolean = true
)