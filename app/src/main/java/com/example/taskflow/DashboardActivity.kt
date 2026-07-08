package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        val btnKanban = findViewById<Button>(R.id.btnKanban)
        val btnCreateTask = findViewById<Button>(R.id.btnCreateTask)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)

        val usuario = auth.currentUser

        if (usuario != null) {
            tvWelcome.text = "Hola 👋\n${usuario.email}"
        }

        btnKanban.setOnClickListener {
            startActivity(
                Intent(this, KanbanActivity::class.java)
            )
        }

        btnCreateTask.setOnClickListener {
            startActivity(
                Intent(this, CreateTaskActivity::class.java)
            )
        }

        btnLogout.setOnClickListener {

            auth.signOut()

            startActivity(
                Intent(this, LoginActivity::class.java)
            )

            finish()
        }
    }
}