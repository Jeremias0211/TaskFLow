package com.example.taskflow

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class KanbanActivity : AppCompatActivity() {


    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var layoutPending: LinearLayout
    private lateinit var layoutProgress: LinearLayout
    private lateinit var layoutCompleted: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_kanban)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        layoutPending = findViewById(R.id.layoutPending)
        layoutProgress = findViewById(R.id.layoutProgress)
        layoutCompleted = findViewById(R.id.layoutCompleted)

        cargarTareas()
    }

    private fun cargarTareas() {

        val usuario = auth.currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "No hay ningún usuario iniciado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val uid = usuario.uid

        db.collection("usuarios")
            .document(uid)
            .collection("tareas")
            .get()
            .addOnSuccessListener { documentos ->

                limpiarColumnas()

                if (documentos.isEmpty) {
                    mostrarMensajeVacio(layoutPending)
                    mostrarMensajeVacio(layoutProgress)
                    mostrarMensajeVacio(layoutCompleted)
                    return@addOnSuccessListener
                }

                val tareas = documentos.map { documento ->

                    TareaFirestore(
                        id = documento.id,
                        title = documento.getString("title") ?: "",
                        description = documento.getString("description") ?: "",
                        category = documento.getString("category") ?: "",
                        priority = documento.getLong("priority")?.toInt() ?: 1,
                        status = documento.getString("status") ?: "Pendiente"
                    )
                }.sortedByDescending {
                    it.priority
                }

                for (tarea in tareas) {

                    when (tarea.status) {

                        "Pendiente" -> {
                            agregarTarjeta(
                                layoutPending,
                                tarea
                            )
                        }

                        "En progreso" -> {
                            agregarTarjeta(
                                layoutProgress,
                                tarea
                            )
                        }

                        "Completado" -> {
                            agregarTarjeta(
                                layoutCompleted,
                                tarea
                            )
                        }
                    }
                }

                if (layoutPending.childCount == 0) {
                    mostrarMensajeVacio(layoutPending)
                }

                if (layoutProgress.childCount == 0) {
                    mostrarMensajeVacio(layoutProgress)
                }

                if (layoutCompleted.childCount == 0) {
                    mostrarMensajeVacio(layoutCompleted)
                }
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error al cargar las tareas: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun agregarTarjeta(
        contenedor: LinearLayout,
        tarea: TareaFirestore
    ) {

        val tarjeta = LinearLayout(this)

        tarjeta.orientation = LinearLayout.VERTICAL
        tarjeta.setPadding(20, 20, 20, 20)
        tarjeta.setBackgroundResource(R.drawable.neon_background)

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        parametros.setMargins(0, 0, 0, 16)

        tarjeta.layoutParams = parametros

        val titulo = TextView(this)

        titulo.text = tarea.title
        titulo.setTextColor(Color.WHITE)
        titulo.textSize = 20f
        titulo.setTypeface(null, Typeface.BOLD)

        val categoria = TextView(this)

        categoria.text = "Categoría: ${tarea.category}"
        categoria.setTextColor(Color.LTGRAY)
        categoria.textSize = 14f

        val descripcion = TextView(this)

        descripcion.text = tarea.description
        descripcion.setTextColor(Color.WHITE)
        descripcion.textSize = 15f

        val prioridad = TextView(this)

        prioridad.text =
            "Prioridad: ${obtenerTextoPrioridad(tarea.priority)}"

        prioridad.setTextColor(Color.rgb(57, 255, 20))
        prioridad.textSize = 14f

        tarjeta.addView(titulo)
        tarjeta.addView(categoria)

        if (tarea.description.isNotEmpty()) {
            tarjeta.addView(descripcion)
        }

        tarjeta.addView(prioridad)

        val botones = LinearLayout(this)

        botones.orientation = LinearLayout.HORIZONTAL
        botones.gravity = Gravity.CENTER

        val editar = Button(this)

        editar.text = "EDITAR"
        editar.setTextColor(Color.BLACK)
        editar.setBackgroundColor(Color.rgb(57, 255, 20))

        val eliminar = Button(this)

        eliminar.text = "ELIMINAR"
        eliminar.setTextColor(Color.WHITE)

        val parametrosEditar = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        parametrosEditar.setMargins(0, 16, 8, 0)

        editar.layoutParams = parametrosEditar

        val parametrosEliminar = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        parametrosEliminar.setMargins(8, 16, 0, 0)

        eliminar.layoutParams = parametrosEliminar

        botones.addView(editar)
        botones.addView(eliminar)

        tarjeta.addView(botones)

        editar.setOnClickListener {
            mostrarDialogoEditar(tarea)
        }

        eliminar.setOnClickListener {
            confirmarEliminacion(tarea)
        }

        contenedor.addView(tarjeta)
    }

    private fun mostrarDialogoEditar(
        tarea: TareaFirestore
    ) {

        val contenido = LinearLayout(this)

        contenido.orientation = LinearLayout.VERTICAL
        contenido.setPadding(40, 10, 40, 10)

        val etTitle = EditText(this)
        etTitle.hint = "Título"
        etTitle.setText(tarea.title)

        val etDescription = EditText(this)
        etDescription.hint = "Descripción"
        etDescription.setText(tarea.description)

        val etCategory = EditText(this)
        etCategory.hint = "Categoría"
        etCategory.setText(tarea.category)

        val spPriority = Spinner(this)

        val prioridades = arrayOf(
            "Alta",
            "Media",
            "Baja"
        )

        spPriority.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            prioridades
        )

        spPriority.setSelection(
            when (tarea.priority) {
                3 -> 0
                2 -> 1
                else -> 2
            }
        )

        val spStatus = Spinner(this)

        val estados = arrayOf(
            "Pendiente",
            "En progreso",
            "Completado"
        )

        spStatus.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            estados
        )

        spStatus.setSelection(
            when (tarea.status) {
                "Pendiente" -> 0
                "En progreso" -> 1
                "Completado" -> 2
                else -> 0
            }
        )

        contenido.addView(etTitle)
        contenido.addView(etDescription)
        contenido.addView(etCategory)
        contenido.addView(spPriority)
        contenido.addView(spStatus)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Editar tarea")
            .setView(contenido)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {

            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val title =
                        etTitle.text.toString().trim()

                    val description =
                        etDescription.text.toString().trim()

                    val category =
                        etCategory.text.toString().trim()

                    if (title.isEmpty()) {
                        etTitle.error = "Ingresá un título"
                        return@setOnClickListener
                    }

                    if (category.isEmpty()) {
                        etCategory.error = "Ingresá una categoría"
                        return@setOnClickListener
                    }

                    val priority = when (
                        spPriority.selectedItem.toString()
                    ) {
                        "Alta" -> 3
                        "Media" -> 2
                        else -> 1
                    }

                    val status =
                        spStatus.selectedItem.toString()

                    val isCompleted =
                        status == "Completado"

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

                    val cambios: Map<String, Any> = hashMapOf(
                        "title" to title,
                        "description" to description,
                        "category" to category,
                        "priority" to priority,
                        "status" to status,
                        "isCompleted" to isCompleted
                    )

                    db.collection("usuarios")
                        .document(uid)
                        .collection("tareas")
                        .document(tarea.id)
                        .update(cambios)
                        .addOnSuccessListener {

                            Toast.makeText(
                                this,
                                "Tarea actualizada",
                                Toast.LENGTH_SHORT
                            ).show()

                            dialogo.dismiss()
                            cargarTareas()
                        }
                        .addOnFailureListener { error ->

                            Toast.makeText(
                                this,
                                "Error al actualizar: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
        }

        dialogo.show()
    }

    private fun confirmarEliminacion(
        tarea: TareaFirestore
    ) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage(
                "¿Seguro que querés eliminar \"${tarea.title}\"?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarTarea(tarea.id)
            }
            .show()
    }

    private fun eliminarTarea(
        id: String
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            Toast.makeText(
                this,
                "No hay ningún usuario iniciado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val uid = usuario.uid

        db.collection("usuarios")
            .document(uid)
            .collection("tareas")
            .document(id)
            .delete()
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Tarea eliminada",
                    Toast.LENGTH_SHORT
                ).show()

                cargarTareas()
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error al eliminar: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun obtenerTextoPrioridad(
        priority: Int
    ): String {

        return when (priority) {
            3 -> "Alta"
            2 -> "Media"
            else -> "Baja"
        }
    }

    private fun limpiarColumnas() {

        layoutPending.removeAllViews()
        layoutProgress.removeAllViews()
        layoutCompleted.removeAllViews()
    }

    private fun mostrarMensajeVacio(
        contenedor: LinearLayout
    ) {

        val mensaje = TextView(this)

        mensaje.text = "No hay tareas"
        mensaje.setTextColor(Color.WHITE)
        mensaje.textSize = 15f
        mensaje.gravity = Gravity.CENTER

        contenedor.addView(mensaje)
    }

    data class TareaFirestore(
        val id: String,
        val title: String,
        val description: String,
        val category: String,
        val priority: Int,
        val status: String
    )


}
