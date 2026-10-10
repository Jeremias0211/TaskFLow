
package com.example.taskflow

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateTaskActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val usuario = auth.currentUser

        if (usuario == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_create_task)

        db = FirebaseFirestore.getInstance()

        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etDescription = findViewById<EditText>(R.id.etDescription)

        val btnSaveTask = findViewById<Button>(R.id.btnSaveTask)
        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val btnClose = findViewById<TextView>(R.id.btnClose)
        val btnClear = findViewById<TextView>(R.id.btnClear)

        val chipWork = findViewById<TextView>(R.id.chipWork)
        val chipStudy = findViewById<TextView>(R.id.chipStudy)
        val chipPersonal = findViewById<TextView>(R.id.chipPersonal)

        val btnPriorityLow = findViewById<TextView>(R.id.btnPriorityLow)
        val btnPriorityMedium = findViewById<TextView>(R.id.btnPriorityMedium)
        val btnPriorityHigh = findViewById<TextView>(R.id.btnPriorityHigh)

        val btnStatusPending = findViewById<TextView>(R.id.btnStatusPending)
        val btnStatusProgress = findViewById<TextView>(R.id.btnStatusProgress)
        val btnStatusCompleted = findViewById<TextView>(R.id.btnStatusCompleted)

        val btnDeadline = findViewById<LinearLayout>(R.id.btnDeadline)
        val tvDeadlineValue = findViewById<TextView>(R.id.tvDeadlineValue)

        var selectedCategory: String? = "Trabajo"
        var selectedPriority = "Alta"
        var selectedStatus = "Pendiente"
        var selectedDeadline: Long? = null

        val green = Color.parseColor("#39FF14")
        val gray = Color.parseColor("#BDBDBD")
        val muted = Color.parseColor("#8C8C8C")

        // Selección de categoría.

        fun updateCategorySelection(category: String?) {
            selectedCategory = category

            val chips = listOf(
                chipWork to "Trabajo",
                chipStudy to "Estudio",
                chipPersonal to "Personal"
            )

            chips.forEach { (chip, value) ->
                if (value == category) {
                    chip.setBackgroundResource(R.drawable.task_chip_active)
                    chip.setTextColor(green)

                    if (value == "Trabajo") {
                        chip.text = "●  Trabajo"
                    } else {
                        chip.text = value
                    }
                } else {
                    chip.setBackgroundResource(
                        R.drawable.login_secondary_button
                    )
                    chip.setTextColor(gray)
                    chip.text = value
                }
            }
        }

        chipWork.setOnClickListener {
            updateCategorySelection("Trabajo")
        }

        chipStudy.setOnClickListener {
            updateCategorySelection("Estudio")
        }

        chipPersonal.setOnClickListener {
            updateCategorySelection("Personal")
        }

        // Selección de prioridad.

        fun updatePrioritySelection(priority: String) {
            selectedPriority = priority

            val options = listOf(
                btnPriorityLow to "Baja",
                btnPriorityMedium to "Media",
                btnPriorityHigh to "Alta"
            )

            options.forEach { (button, value) ->
                val active = value == priority

                if (active) {
                    button.setBackgroundResource(R.drawable.task_chip_active)
                    button.setTextColor(green)
                    button.text = "⚑  $value"
                } else {
                    button.setBackgroundResource(
                        R.drawable.login_secondary_button
                    )
                    button.setTextColor(gray)
                    button.text = "⚑  $value"
                }
            }
        }

        btnPriorityLow.setOnClickListener {
            updatePrioritySelection("Baja")
        }

        btnPriorityMedium.setOnClickListener {
            updatePrioritySelection("Media")
        }

        btnPriorityHigh.setOnClickListener {
            updatePrioritySelection("Alta")
        }

        // Selección de estado.

        fun updateStatusSelection(status: String) {
            selectedStatus = status

            val options = listOf(
                btnStatusPending to "Pendiente",
                btnStatusProgress to "En progreso",
                btnStatusCompleted to "Completada"
            )

            options.forEach { (button, value) ->
                if (value == status) {
                    button.setBackgroundResource(
                        R.drawable.task_chip_active
                    )
                    button.setTextColor(green)
                } else {
                    button.setBackgroundColor(Color.TRANSPARENT)
                    button.setTextColor(muted)
                }
            }
        }

        btnStatusPending.setOnClickListener {
            updateStatusSelection("Pendiente")
        }

        btnStatusProgress.setOnClickListener {
            updateStatusSelection("En progreso")
        }

        btnStatusCompleted.setOnClickListener {
            updateStatusSelection("Completada")
        }

        // Selector de fecha y hora.

        val dateFormat = SimpleDateFormat(
            "EEE d MMM · HH:mm",
            Locale("es", "AR")
        )

        btnDeadline.setOnClickListener {
            val calendar = Calendar.getInstance()

            if (selectedDeadline != null) {
                calendar.timeInMillis = selectedDeadline!!
            }

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)

                    TimePickerDialog(
                        this,
                        { _, hour, minute ->
                            calendar.set(Calendar.HOUR_OF_DAY, hour)
                            calendar.set(Calendar.MINUTE, minute)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)

                            selectedDeadline = calendar.timeInMillis
                            tvDeadlineValue.text =
                                dateFormat.format(calendar.time)
                                    .replaceFirstChar { it.uppercase() }

                            tvDeadlineValue.setTextColor(green)
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Botón para cerrar la pantalla.

        btnClose.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Borrar los datos ingresados.

        btnClear.setOnClickListener {
            etTitle.text.clear()
            etDescription.text.clear()

            updateCategorySelection(null)
            updatePrioritySelection("Alta")
            updateStatusSelection("Pendiente")

            selectedDeadline = null
            tvDeadlineValue.text = "Elegir fecha y hora"
            tvDeadlineValue.setTextColor(gray)

            etTitle.clearFocus()
            etDescription.clearFocus()
        }

        // Guardar la tarea en Firebase.

        btnSaveTask.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val category = selectedCategory

            if (title.isEmpty()) {
                etTitle.error = "Ingresá un título"
                etTitle.requestFocus()
                return@setOnClickListener
            }

            if (category.isNullOrEmpty()) {
                Toast.makeText(
                    this,
                    "Seleccioná una categoría",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val uid = usuario.uid

            val priority = when (selectedPriority) {
                "Alta" -> 3
                "Media" -> 2
                "Baja" -> 1
                else -> 1
            }

            // Se conserva el valor "Completado" utilizado anteriormente
            // para mantener la compatibilidad con los datos existentes.
            val statusForDatabase =
                if (selectedStatus == "Completada") {
                    "Completado"
                } else {
                    selectedStatus
                }

            val isCompleted = statusForDatabase == "Completado"

            val tarea = hashMapOf<String, Any>(
                "title" to title,
                "description" to description,
                "category" to category,
                "priority" to priority,
                "status" to statusForDatabase,
                "createdAt" to System.currentTimeMillis(),
                "isCompleted" to isCompleted
            )

            // La fecha solo se guarda si el usuario seleccionó una.
            selectedDeadline?.let { deadline ->
                tarea["dueAt"] = deadline
            }

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

        // Valores visuales iniciales.

        updateCategorySelection("Trabajo")
        updatePrioritySelection("Alta")
        updateStatusSelection("Pendiente")

        setupBottomNavigation(bottomNavigation, R.id.nav_create)
    }

    private fun setupBottomNavigation(
        bottomNavigation: BottomNavigationView,
        currentItemId: Int
    ) {
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

            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
            true
        }
    }
}
