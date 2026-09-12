package com.example.taskflow

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.adapter.HabitosAdapter
import com.example.taskflow.model.Habito
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HabitosActivity : AppCompatActivity() {

    private lateinit var recyclerHabitos: RecyclerView
    private lateinit var btnAgregarHabito: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val listaHabitos = mutableListOf<Habito>()
    private val idsHabitos = mutableListOf<String>()

    private lateinit var adapter: HabitosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_habitos)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        recyclerHabitos = findViewById(R.id.recyclerHabitos)
        btnAgregarHabito = findViewById(R.id.btnAgregarHabito)

        recyclerHabitos.layoutManager = LinearLayoutManager(this)

        adapter = HabitosAdapter(
            listaHabitos,

            onEliminar = { posicion ->
                confirmarEliminacion(posicion)
            },

            onEditar = { posicion ->
                mostrarDialogoEditar(posicion)
            },

            onDiaCambiado = { posicion, dia, estado ->
                guardarDia(posicion, dia, estado)
            }
        )

        recyclerHabitos.adapter = adapter

        btnAgregarHabito.setOnClickListener {
            mostrarDialogoAgregar()
        }

        cargarHabitos()
    }

    private fun cargarHabitos() {

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
            .collection("habitos")
            .get()
            .addOnSuccessListener { documentos ->

                listaHabitos.clear()
                idsHabitos.clear()

                for (documento in documentos) {

                    val nombre =
                        documento.getString("nombre") ?: ""

                    val datosDias =
                        documento.get("dias") as? List<*>

                    val dias = MutableList(31) { false }

                    if (datosDias != null) {

                        for (i in 0 until minOf(datosDias.size, 31)) {
                            dias[i] = datosDias[i] as? Boolean ?: false
                        }
                    }

                    listaHabitos.add(
                        Habito(
                            nombre = nombre,
                            dias = dias
                        )
                    )

                    idsHabitos.add(documento.id)
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error al cargar hábitos: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarDialogoAgregar() {

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
                mostrarDialogoNombre(
                    editText,
                    iconoSeleccionado
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoNombre(
        editText: EditText,
        icono: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("Nombre del hábito")
            .setView(editText)
            .setPositiveButton("Agregar") { _, _ ->

                val nombre =
                    editText.text.toString().trim()

                if (nombre.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Ingresá un nombre",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                agregarHabitoFirestore(
                    "$icono $nombre"
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun agregarHabitoFirestore(
        nombre: String
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
        val dias = MutableList(31) { false }

        val habito = hashMapOf(
            "nombre" to nombre,
            "dias" to dias,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("usuarios")
            .document(uid)
            .collection("habitos")
            .add(habito)
            .addOnSuccessListener { documento ->

                listaHabitos.add(
                    Habito(
                        nombre = nombre,
                        dias = dias
                    )
                )

                idsHabitos.add(documento.id)

                adapter.notifyItemInserted(
                    listaHabitos.size - 1
                )

                Toast.makeText(
                    this,
                    "Hábito agregado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error al guardar hábito: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun guardarDia(
        posicion: Int,
        dia: Int,
        estado: Boolean
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            return
        }

        if (posicion !in idsHabitos.indices) {
            return
        }

        val uid = usuario.uid
        val idHabito = idsHabitos[posicion]

        db.collection("usuarios")
            .document(uid)
            .collection("habitos")
            .document(idHabito)
            .update(
                "dias",
                listaHabitos[posicion].dias
            )
            .addOnFailureListener { error ->

                listaHabitos[posicion].dias[dia] = !estado

                adapter.notifyItemChanged(posicion)

                Toast.makeText(
                    this,
                    "No se pudo guardar el día: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun mostrarDialogoEditar(
        posicion: Int
    ) {

        if (posicion !in listaHabitos.indices) {
            return
        }

        val habito = listaHabitos[posicion]
        val editText = EditText(this)

        editText.setText(
            quitarIcono(habito.nombre)
        )

        AlertDialog.Builder(this)
            .setTitle("Editar hábito")
            .setView(editText)
            .setPositiveButton("Guardar") { _, _ ->

                val nuevoNombre =
                    editText.text.toString().trim()

                if (nuevoNombre.isEmpty()) {
                    Toast.makeText(
                        this,
                        "Ingresá un nombre",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                val icono =
                    obtenerIcono(habito.nombre)

                actualizarHabito(
                    posicion,
                    "$icono $nuevoNombre"
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarHabito(
        posicion: Int,
        nuevoNombre: String
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            return
        }

        if (posicion !in idsHabitos.indices) {
            return
        }

        val uid = usuario.uid
        val idHabito = idsHabitos[posicion]

        db.collection("usuarios")
            .document(uid)
            .collection("habitos")
            .document(idHabito)
            .update(
                "nombre",
                nuevoNombre
            )
            .addOnSuccessListener {

                listaHabitos[posicion].nombre =
                    nuevoNombre

                adapter.notifyItemChanged(posicion)

                Toast.makeText(
                    this,
                    "Hábito actualizado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error al actualizar: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun confirmarEliminacion(
        posicion: Int
    ) {

        if (posicion !in listaHabitos.indices) {
            return
        }

        val habito = listaHabitos[posicion]

        AlertDialog.Builder(this)
            .setTitle("Eliminar hábito")
            .setMessage(
                "¿Desea eliminar \"${habito.nombre}\"?"
            )
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarHabito(posicion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarHabito(
        posicion: Int
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            return
        }

        if (posicion !in idsHabitos.indices) {
            return
        }

        val uid = usuario.uid
        val idHabito = idsHabitos[posicion]

        db.collection("usuarios")
            .document(uid)
            .collection("habitos")
            .document(idHabito)
            .delete()
            .addOnSuccessListener {

                listaHabitos.removeAt(posicion)
                idsHabitos.removeAt(posicion)

                adapter.notifyItemRemoved(posicion)

                Toast.makeText(
                    this,
                    "Hábito eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Error al eliminar: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun obtenerIcono(
        nombre: String
    ): String {

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

        for (icono in iconos) {
            if (nombre.startsWith(icono)) {
                return icono
            }
        }

        return "📚"
    }

    private fun quitarIcono(
        nombre: String
    ): String {

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

        for (icono in iconos) {
            if (nombre.startsWith(icono)) {
                return nombre.removePrefix(icono).trim()
            }
        }

        return nombre
    }
}