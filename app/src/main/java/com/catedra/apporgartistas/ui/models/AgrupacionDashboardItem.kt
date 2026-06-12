package com.catedra.apporgartistas.ui.models

import com.catedra.apporgartistas.data.models.Agrupacion

data class AgrupacionDashboardItem(
    val agrupacion: Agrupacion,
    val cantidadShows: Int = 0
)
