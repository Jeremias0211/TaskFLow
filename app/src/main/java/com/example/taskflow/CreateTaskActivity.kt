package com.example.taskflow

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateTaskActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_create_task)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etCategory = findViewById<EditText>(R.id.etCategory)

        val spPriority = findViewById<Spinner>(R.id.spPriority)
        val spStatus = findViewById<Spinner>(R.id.spStatus)

        val btnSaveTask = findViewById<Button>(R.id.btnSaveTask)

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

        btnSaveTask.setOnClickListener {

            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val category = etCategory.text.toString().trim()

            val priorityText = spPriority.selectedItem.toString()
            val status = spStatus.selectedItem.toString()

            if (title.isEmpty()) {
                etTitle.error = "Ingresá un título"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            if (category.isEmpty()) {
                etCategory.error = "Ingresá una categoría"
                etCategory.requestFocus()
                return@setOnClickListener
            }

            val usuario = auth.currentUser

            if (usuario == null) {
                Toast.makeText(
                    this,
                    "No hay ningún usuario iniciado",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val uid = usuario.uid

            val priority = when (priorityText) {
                "Alta" -> 3
                "Media" -> 2
                "Baja" -> 1
                else -> 1
            }

            val isCompleted = status == "Completado"

            val tarea = hashMapOf(
                "title" to title,
                "description" to description,
                "category" to category,
                "priority" to priority,
                "status" to status,
                "createdAt" to System.currentTimeMillis(),
                "isCompleted" to isCompleted
            )

            btnSaveTask.isEnabled = false

            db.collection("usuarios")
                .document(uid)
                .collection("tareas")
                .add(tarea)
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Tarea guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(
                        this,
                        DashboardActivity::class.java
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP

                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { error ->

                    btnSaveTask.isEnabled = true

                    Toast.makeText(
                        this,
                        "Error al guardar la tarea: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}