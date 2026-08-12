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
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {

            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Limpiamos errores anteriores
            etName.error = null
            etEmail.error = null
            etPassword.error = null
            etConfirmPassword.error = null

            // Comprobar campos vacíos
            var hayError = false

            if (name.isEmpty()) {
                etName.error = "Campo vacío"
                hayError = true
            }

            if (email.isEmpty()) {
                etEmail.error = "Campo vacío"
                hayError = true
            }

            if (password.isEmpty()) {
                etPassword.error = "Campo vacío"
                hayError = true
            }

            if (confirmPassword.isEmpty()) {
                etConfirmPassword.error = "Campo vacío"
                hayError = true
            }

            if (hayError) {
                mostrarMensaje(
                    "Completa todos los campos",
                    R.drawable.error
                )
                return@setOnClickListener
            }

            // Comprobar que las contraseñas coincidan
            if (password != confirmPassword) {

                etConfirmPassword.error = "Las contraseñas no coinciden"

                mostrarMensaje(
                    "Las contraseñas no coinciden",
                    R.drawable.error
                )

                return@setOnClickListener
            }

            // Comprobar longitud de contraseña
            if (password.length < 6) {

                etPassword.error = "Mínimo 6 caracteres"

                mostrarMensaje(
                    "La contraseña debe tener al menos 6 caracteres",
                    R.drawable.error
                )

                return@setOnClickListener
            }

            // Crear cuenta en Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->

                    if (task.isSuccessful) {

                        val usuarioActual = auth.currentUser

                        if (usuarioActual != null) {

                            val uid = usuarioActual.uid

                            val usuario = hashMapOf(
                                "nombre" to name,
                                "email" to email
                            )

                            // Guardar usuario en Firestore
                            db.collection("usuarios")
                                .document(uid)
                                .set(usuario)
                                .addOnSuccessListener {

                                    mostrarMensaje(
                                        "Cuenta creada correctamente",
                                        R.drawable.check
                                    )

                                    // Cerrar sesión para que tenga
                                    // que iniciar sesión manualmente
                                    auth.signOut()

                                    // Volver al Login
                                    startActivity(
                                        Intent(
                                            this,
                                            LoginActivity::class.java
                                        )
                                    )

                                    finish()
                                }
                                .addOnFailureListener { exception ->

                                    mostrarMensaje(
                                        "Error al guardar usuario: ${exception.message}",
                                        R.drawable.error
                                    )
                                }

                        }

                    } else {

                        mostrarMensaje(
                            task.exception?.localizedMessage
                                ?: "No se pudo crear la cuenta",
                            R.drawable.error
                        )
                    }
                }
        }
    }

    // Toast personalizado con imagen
    private fun mostrarMensaje(
        mensaje: String,
        imagen: Int
    ) {

        val vista = LayoutInflater.from(this)
            .inflate(R.layout.toast_custom, null)

        val icono = vista.findViewById<ImageView>(
            R.id.toastIcon
        )

        val texto = vista.findViewById<TextView>(
            R.id.toastText
        )

        icono.setImageResource(imagen)
        texto.text = mensaje

        val toast = Toast(this)

        toast.duration = Toast.LENGTH_SHORT
        toast.view = vista
        toast.show()
    }
}