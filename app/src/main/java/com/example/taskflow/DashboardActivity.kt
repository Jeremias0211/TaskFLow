package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val usuario = auth.currentUser

        if (usuario != null) {
            tvWelcome.text = "Hola 👋\n${usuario.email}"
        }

        // Marcamos Inicio como seleccionado
        bottomNavigation.selectedItemId = R.id.nav_home

        bottomNavigation.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    true
                }

                R.id.nav_kanban -> {
                    startActivity(
                        Intent(this, KanbanActivity::class.java)
                    )
                    true
                }

                R.id.nav_create -> {
                    startActivity(
                        Intent(this, CreateTaskActivity::class.java)
                    )
                    true
                }

                R.id.nav_habitos -> {
                    startActivity(
                        Intent(this, HabitosActivity::class.java)
                    )
                    true
                }

                R.id.nav_profile -> {
                    startActivity(
                        Intent(this, ProfileActivity::class.java)
                    )
                    true
                }

                else -> false
            }
        }
    }
}