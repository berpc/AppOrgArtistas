package com.catedra.apporgartistas.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.activities.LoginActivity
import com.catedra.apporgartistas.viewmodels.MainViewModel
import kotlin.jvm.java

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Verificación del Router
        if (!viewModel.isUserLoggedIn()) {
            // Usuario sin sesión -> Lo mandamos a loguearse
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            // Usuario con sesión -> Lo mandamos al Dashboard principal
            startActivity(Intent(this, SetlistDashboardActivity::class.java))
        }

        // 2. Destruimos esta Activity
        // Finalizamos MainActivity para que el usuario no pueda "volver" a esta
        // pantalla invisible usando el botón de Atrás del celular.
        finish()
    }
}