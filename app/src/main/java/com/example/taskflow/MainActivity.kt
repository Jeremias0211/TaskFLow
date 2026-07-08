package com.example.taskflow

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.taskflow.database.TaskDatabaseHelper
import com.example.taskflow.model.Task
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: TaskDatabaseHelper
    private lateinit var tvTasks: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        val auth = FirebaseAuth.getInstance()

        val addOnCompleteListener = auth.createUserWithEmailAndPassword(
            "prueba@taskflow.com",
            "123456"
        ).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Usuario creado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Error: ${task.exception?.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        println("Firebase inicializado correctamente: $auth")

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        dbHelper = TaskDatabaseHelper(this)

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etCategory = findViewById<EditText>(R.id.etCategory)
        val etPriority = findViewById<EditText>(R.id.etPriority)
        val spStatus = findViewById<Spinner>(R.id.spStatus)
        val btnAdd = findViewById<Button>(R.id.btnAdd)

        tvTasks = findViewById(R.id.tvTasks)

        val estados = arrayOf(
            "Pendiente",
            "En progreso",
            "En revisión",
            "Completado"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            estados
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spStatus.adapter = adapter

        loadTasks()

        btnAdd.setOnClickListener {

            val title = etTitle.text.toString()
            val description = etDescription.text.toString()
            val category = etCategory.text.toString()
            val priority = etPriority.text.toString().toIntOrNull() ?: 1
            val status = spStatus.selectedItem.toString()

            if (title.isNotEmpty()) {

                val task = Task(
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    status = status
                )

                dbHelper.insertTask(task)

                loadTasks()

                etTitle.text.clear()
                etDescription.text.clear()
                etCategory.text.clear()
                etPriority.text.clear()

                spStatus.setSelection(0)

            } else {

                Toast.makeText(
                    this,
                    "Poné un título",
                    Toast.LENGTH_SHORT
                ).show()

            }
        }
    }

    private fun loadTasks() {

        val tasks = dbHelper.getAllTasks()

        val text = tasks.joinToString("\n\n") {

            "📌 ${it.title}\n" +
                    "📝 ${it.description}\n" +
                    "📂 ${it.category}\n" +
                    "⚡ Prioridad: ${it.priority}\n" +
                    "🔄 Estado: ${it.status}"

        }

        tvTasks.text = text
    }
}