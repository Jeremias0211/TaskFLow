package com.example.taskflow

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val usuario = auth.currentUser

        if (usuario == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_profile)

        db = FirebaseFirestore.getInstance()

        val tvProfileName = findViewById<TextView>(R.id.tvProfileName)
        val tvProfileEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnEditName = findViewById<Button>(R.id.btnEditName)
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        tvProfileEmail.text = usuario.email ?: "Sin correo"
        tvProfileName.text = "Cargando..."

        val uid = usuario.uid

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre")
                if (!nombre.isNullOrEmpty()) {
                    tvProfileName.text = nombre
                } else {
                    tvProfileName.text = "Sin nombre"
                }
            }
            .addOnFailureListener {
                tvProfileName.text = "Sin nombre"
            }

        btnEditName.setOnClickListener {
            mostrarDialogoEditarNombre(uid, tvProfileName)
        }

        btnResetPassword.setOnClickListener {
            val email = usuario.email
            if (!email.isNullOrEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Correo de restablecimiento enviado a $email",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        setupBottomNavigation(bottomNavigation, R.id.nav_profile)
    }

    private fun mostrarDialogoEditarNombre(uid: String, tvProfileName: TextView) {
        val editText = EditText(this)
        editText.hint = "Nuevo nombre"
        editText.setText(tvProfileName.text.toString())
        editText.setTextColor(android.graphics.Color.WHITE)
        editText.setHintTextColor(android.graphics.Color.GRAY)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Editar nombre")
            .setView(editText)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nuevoNombre = editText.text.toString().trim()
                if (nuevoNombre.isEmpty()) {
                    editText.error = "Ingresá un nombre"
                    return@setOnClickListener
                }

                db.collection("usuarios").document(uid)
                    .update("nombre", nuevoNombre)
                    .addOnSuccessListener {
                        tvProfileName.text = nuevoNombre
                        Toast.makeText(this, "Nombre actualizado", Toast.LENGTH_SHORT).show()
                        dialogo.dismiss()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
        dialogo.show()
    }

    private fun setupBottomNavigation(bottomNavigation: BottomNavigationView, currentItemId: Int) {
        bottomNavigation.selectedItemId = currentItemId
        bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == currentItemId) {
                return@setOnItemSelectedListener true
            }
            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(this, DashboardActivity::class.java)
                R.id.nav_kanban -> Intent(this, KanbanActivity::class.java)
                R.id.nav_create -> Intent(this, CreateTaskActivity::class.java)
                R.id.nav_habitos -> Intent(this, HabitosActivity::class.java)
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                else -> return@setOnItemSelectedListener false
            }
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
            true
        }
    }
}
