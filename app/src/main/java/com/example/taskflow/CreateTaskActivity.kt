package com.example.taskflow

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity

class CreateTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_task)

        val spPriority = findViewById<android.widget.Spinner>(R.id.spPriority)
        val spStatus = findViewById<android.widget.Spinner>(R.id.spStatus)

        val priorities = arrayOf(
            "Alta",
            "Media",
            "Baja"
        )

        val statuses = arrayOf(
            "Pendiente",
            "En progreso",
            "Completado"
        )

        spPriority.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            priorities
        )

        spStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
    }
}