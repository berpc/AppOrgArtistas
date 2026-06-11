package com.catedra.apporgartistas.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import com.catedra.apporgartistas.ui.activities.DashboardHostActivity
import com.catedra.apporgartistas.ui.activities.RegisterActivity
import com.catedra.apporgartistas.viewmodels.LoginState
import com.catedra.apporgartistas.viewmodels.LoginViewModel

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        setupObservers()

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            viewModel.loginUser(email, password)
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginState.Loading -> {
                }
                is LoginState.Success -> {
                    Toast.makeText(this, "Login exitoso", Toast.LENGTH_SHORT).show()
                    viewModel.obtenerYGuardarTokenFcm(
                        onError = { mensaje ->
                            println("Error al obtener token de FCM: $mensaje")
                        },
                        onComplete = {
                            // 3. Navegamos a la siguiente pantalla sin importar si el token falló o no
                            startActivity(Intent(this, DashboardHostActivity::class.java))
                            finish()
                        }
                    )
                }
                is LoginState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is LoginState.Idle -> { /* No hacer nada */ }
            }
        }
    }
}
