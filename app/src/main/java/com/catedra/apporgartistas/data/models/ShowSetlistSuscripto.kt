package com.catedra.apporgartistas.data.models

data class ShowSetlistSuscripto(
    val codigo: String = "",
    val agrupacionId: String = "",
    val showId: String = "",
    val instrumentoId: String = "",
    val nombreShow: String = "",
    val fechaShow: String? = null,
    val nombreInstrumento: String = ""
)