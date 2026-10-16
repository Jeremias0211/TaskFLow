package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
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

    private lateinit var vBar1: View
    private lateinit var vBar2: View
    private lateinit var vBar3: View
    private lateinit var vBar4: View
    private lateinit var tvSecurityStatus: TextView

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
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        vBar1 = findViewById(R.id.vBar1)
        vBar2 = findViewById(R.id.vBar2)
        vBar3 = findViewById(R.id.vBar3)
        vBar4 = findViewById(R.id.vBar4)
        tvSecurityStatus = findViewById(R.id.tvSecurityStatus)

        tvBackToLogin.setOnClickListener {
            finish()
        }

        tvLogin.setOnClickListener {
            finish()
        }

        // Validación en tiempo real de la seguridad de la contraseña
        etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkPasswordStrength(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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

                                    auth.signOut()

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

    private fun checkPasswordStrength(password: String) {
        val colorActive = android.graphics.Color.parseColor("#39FF14")
        val colorInactive = android.graphics.Color.parseColor("#333333")

        when {
            password.isEmpty() -> {
                vBar1.setBackgroundColor(colorInactive)
                vBar2.setBackgroundColor(colorInactive)
                vBar3.setBackgroundColor(colorInactive)
                vBar4.setBackgroundColor(colorInactive)
                tvSecurityStatus.text = "Seguridad: -"
                tvSecurityStatus.setTextColor(android.graphics.Color.parseColor("#8C8C8C"))
            }
            password.length < 6 -> {
                vBar1.setBackgroundColor(colorActive)
                vBar2.setBackgroundColor(colorInactive)
                vBar3.setBackgroundColor(colorInactive)
                vBar4.setBackgroundColor(colorInactive)
                tvSecurityStatus.text = "Seguridad: Débil (mín. 6 caracteres)"
                tvSecurityStatus.setTextColor(colorActive)
            }
            password.length < 8 -> {
                vBar1.setBackgroundColor(colorActive)
                vBar2.setBackgroundColor(colorActive)
                vBar3.setBackgroundColor(colorInactive)
                vBar4.setBackgroundColor(colorInactive)
                tvSecurityStatus.text = "Seguridad: Media"
                tvSecurityStatus.setTextColor(colorActive)
            }
            password.length >= 8 && password.any { it.isDigit() } && password.any { it.isUpperCase() } -> {
                vBar1.setBackgroundColor(colorActive)
                vBar2.setBackgroundColor(colorActive)
                vBar3.setBackgroundColor(colorActive)
                vBar4.setBackgroundColor(colorActive)
                tvSecurityStatus.text = "Seguridad: Muy fuerte"
                tvSecurityStatus.setTextColor(colorActive)
            }
            else -> {
                vBar1.setBackgroundColor(colorActive)
                vBar2.setBackgroundColor(colorActive)
                vBar3.setBackgroundColor(colorActive)
                vBar4.setBackgroundColor(colorInactive)
                tvSecurityStatus.text = "Seguridad: Fuerte"
                tvSecurityStatus.setTextColor(colorActive)
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
