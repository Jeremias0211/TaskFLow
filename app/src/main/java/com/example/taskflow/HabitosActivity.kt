package com.example.taskflow

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.adapter.HabitosAdapter
import com.example.taskflow.model.Habito

class HabitosActivity : AppCompatActivity() {

    private lateinit var recyclerHabitos: RecyclerView
    private lateinit var btnAgregarHabito: Button

    private val listaHabitos = mutableListOf<Habito>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habitos)

        recyclerHabitos = findViewById(R.id.recyclerHabitos)
        btnAgregarHabito = findViewById(R.id.btnAgregarHabito)

        recyclerHabitos.layoutManager = LinearLayoutManager(this)


        lateinit var adapter: HabitosAdapter

        adapter = HabitosAdapter(

            listaHabitos,

            // ELIMINAR
            onEliminar = { posicion ->

                AlertDialog.Builder(this)
                    .setTitle("Eliminar hábito")
                    .setMessage("¿Desea eliminar este hábito?")
                    .setPositiveButton("Eliminar") { _, _ ->

                        listaHabitos.removeAt(posicion)
                        adapter.notifyItemRemoved(posicion)

                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

            },

            // EDITAR
            onEditar = { posicion ->

                val habito = listaHabitos[posicion]

                val editText = EditText(this)
                editText.setText(habito.nombre)

                AlertDialog.Builder(this)
                    .setTitle("Editar hábito")
                    .setView(editText)

                    .setPositiveButton("Guardar") { _, _ ->

                        val nuevoNombre = editText.text.toString().trim()

                        if (nuevoNombre.isNotEmpty()) {

                            habito.nombre = nuevoNombre

                            adapter.notifyItemChanged(posicion)

                        }

                    }

                    .setNegativeButton("Cancelar", null)
                    .show()

            }

        )

        recyclerHabitos.adapter = adapter

        btnAgregarHabito.setOnClickListener {

            val editText = EditText(this)
            editText.hint = "Nombre del hábito"

            val iconos = arrayOf(
                "📚",
                "🏋",
                "💧",
                "😴",
                "🍎",
                "🚶",
                "🧘",
                "💻"
            )

            var iconoSeleccionado = iconos[0]

            AlertDialog.Builder(this)
                .setTitle("Seleccionar ícono")
                .setSingleChoiceItems(iconos, 0) { _, which ->
                    iconoSeleccionado = iconos[which]
                }

                .setPositiveButton("Siguiente") { _, _ ->

                    AlertDialog.Builder(this)
                        .setTitle("Nombre del hábito")
                        .setView(editText)

                        .setPositiveButton("Agregar") { _, _ ->

                            val nombre = editText.text.toString().trim()

                            if (nombre.isNotEmpty()) {

                                listaHabitos.add(
                                    Habito("$iconoSeleccionado $nombre")
                                )

                                adapter.notifyItemInserted(listaHabitos.size - 1)

                            }

                        }

                        .setNegativeButton("Cancelar", null)
                        .show()

                }

                .setNegativeButton("Cancelar", null)
                .show()

        }

    }
}