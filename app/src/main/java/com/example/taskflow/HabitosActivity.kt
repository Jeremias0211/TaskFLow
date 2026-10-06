package com.example.taskflow

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.adapter.HabitosAdapter
import com.example.taskflow.model.Habito
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class HabitosActivity : AppCompatActivity() {

    private lateinit var recyclerHabitos: RecyclerView
    private lateinit var btnAgregarHabito: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val listaHabitos = mutableListOf<Habito>()
    private val idsHabitos = mutableListOf<String>()

    private lateinit var adapter: HabitosAdapter

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

        setContentView(R.layout.activity_habitos)

        db = FirebaseFirestore.getInstance()

        recyclerHabitos = findViewById(R.id.recyclerHabitos)
        btnAgregarHabito = findViewById(R.id.btnAgregarHabito)
        bottomNavigation = findViewById(R.id.bottomNavigation)

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
        setupBottomNavigation(bottomNavigation, R.id.nav_habitos)
    }

    private fun cargarHabitos() {

        val usuario = auth.currentUser

        if (usuario == null) {
            return
        }

        val uid = usuario.uid
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

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

                    val dias = MutableList(daysInMonth) { false }

                    if (datosDias != null) {
                        for (i in 0 until minOf(datosDias.size, daysInMonth)) {
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
        editText.setTextColor(android.graphics.Color.WHITE)
        editText.setHintTextColor(android.graphics.Color.GRAY)

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

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Nombre del hábito")
            .setView(editText)
            .setPositiveButton("Agregar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nombre = editText.text.toString().trim()

                if (nombre.isEmpty()) {
                    editText.error = "Ingresá un nombre"
                    Toast.makeText(
                        this,
                        "Ingresá un nombre",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                agregarHabitoFirestore(
                    "$icono $nombre"
                )
                dialogo.dismiss()
            }
        }

        dialogo.show()
    }

    private fun agregarHabitoFirestore(
        nombre: String
    ) {

        val usuario = auth.currentUser

        if (usuario == null) {
            return
        }

        val uid = usuario.uid
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dias = MutableList(daysInMonth) { false }

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
        editText.setTextColor(android.graphics.Color.WHITE)
        editText.setHintTextColor(android.graphics.Color.GRAY)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Editar hábito")
            .setView(editText)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val nuevoNombre =
                    editText.text.toString().trim()

                if (nuevoNombre.isEmpty()) {
                    editText.error = "Ingresá un nombre"
                    Toast.makeText(
                        this,
                        "Ingresá un nombre",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val icono =
                    obtenerIcono(habito.nombre)

                actualizarHabito(
                    posicion,
                    "$icono $nuevoNombre"
                )
                dialogo.dismiss()
            }
        }

        dialogo.show()
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
