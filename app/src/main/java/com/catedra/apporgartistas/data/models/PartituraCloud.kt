package com.catedra.apporgartistas.data.models
import java.io.Serializable
data class PartituraCloud(
    var nombre: String = "",
    var url: String = "",
    var publicId: String = ""
) : Serializable