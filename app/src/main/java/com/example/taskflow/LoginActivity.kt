package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Si ya hay una sesión iniciada, entra directamente
        if (auth.currentUser != null) {
            startActivity(
                Intent(this, DashboardActivity::class.java)
            )
            finish()
            return
        }

        btnLogin.setOnClickListener {

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Verificar campos vacíos
            if (email.isEmpty() || password.isEmpty()) {

                if (email.isEmpty()) {
                    etEmail.error = "Campo vacío"
                }

                if (password.isEmpty()) {
                    etPassword.error = "Campo vacío"
                }

                mostrarMensaje(
                    "Completa todos los campos",
                    R.drawable.error
                )

                return@setOnClickListener
            }

            // Iniciar sesión con Firebase
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        mostrarMensaje(
                            "Inicio de sesión correcto",
                            R.drawable.check
                        )

                        startActivity(
                            Intent(this, DashboardActivity::class.java)
                        )

                        finish()

                    } else {

                        mostrarMensaje(
                            task.exception?.localizedMessage
                                ?: "Correo o contraseña incorrectos",
                            R.drawable.error
                        )
                    }
                }
        }

        // Ir al registro
        tvRegister.setOnClickListener {

            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    // Toast personalizado con imagen
    private fun mostrarMensaje(mensaje: String, imagen: Int) {

        val vista = LayoutInflater.from(this)
            .inflate(R.layout.toast_custom, null)

        val icono = vista.findViewById<ImageView>(R.id.toastIcon)
        val texto = vista.findViewById<TextView>(R.id.toastText)

        icono.setImageResource(imagen)
        texto.text = mensaje

        val toast = Toast(this)

        toast.duration = Toast.LENGTH_SHORT
        toast.view = vista
        toast.show()
    }
}