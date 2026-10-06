package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

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

        setContentView(R.layout.activity_dashboard)

        db = FirebaseFirestore.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvPending = findViewById<TextView>(R.id.tvPending)
        val tvProgress = findViewById<TextView>(R.id.tvProgress)
        val tvCompleted = findViewById<TextView>(R.id.tvCompleted)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        tvWelcome.text = "Hola 👋\n${usuario.email}"

        val uid = usuario.uid

        db.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val nombre = doc.getString("nombre")
                if (!nombre.isNullOrEmpty()) {
                    tvWelcome.text = "Hola 👋\n$nombre"
                }
            }

        db.collection("usuarios").document(uid).collection("tareas")
            .get()
            .addOnSuccessListener { querySnapshot ->
                var pending = 0
                var progress = 0
                var completed = 0

                for (doc in querySnapshot) {
                    val status = doc.getString("status") ?: "Pendiente"
                    val isCompleted = doc.getBoolean("isCompleted") ?: (status == "Completado")
                    if (isCompleted || status == "Completado") {
                        completed++
                    } else if (status == "En progreso") {
                        progress++
                    } else {
                        pending++
                    }
                }

                tvPending.text = "Pendientes\n$pending"
                tvProgress.text = "En progreso\n$progress"
                tvCompleted.text = "Completadas\n$completed"
            }

        setupBottomNavigation(bottomNavigation, R.id.nav_home)
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
