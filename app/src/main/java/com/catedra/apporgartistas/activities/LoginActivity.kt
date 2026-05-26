package com.catedra.apporgartistas.activities

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {

                Toast.makeText(
                    this,
                    "Complete todos los campos",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            // Validar email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                Toast.makeText(
                    this,
                    "Ingrese un email válido",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {

                    if (it.isSuccessful) {

                        Toast.makeText(
                            this,
                            "Login exitoso",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(this, MainActivity::class.java)
                        )

                        finish()

                    } else {

                        Toast.makeText(
                            this,
                            it.exception?.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        btnRegister.setOnClickListener {

            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    // Mantener sesión iniciada
    override fun onStart() {

        super.onStart()

        if (auth.currentUser != null) {

            startActivity(
                Intent(this, SetlistDetailActivity::class.java)
            )

            finish()
        }
    }
}