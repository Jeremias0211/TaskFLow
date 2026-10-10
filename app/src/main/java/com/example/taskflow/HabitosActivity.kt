package com.example.taskflow

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.adapter.HabitosAdapter
import com.example.taskflow.model.Habito
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitosActivity : AppCompatActivity() {

    private lateinit var recyclerHabitos: RecyclerView
    private lateinit var btnAgregarHabito: Button
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var tvSemanaHabitos: TextView
    private lateinit var tvRachaGeneral: TextView
    private lateinit var tvProgresoGeneral: TextView
    private lateinit var btnCalendarioHabitos: TextView
    private lateinit var btnOpcionesHabitos: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: HabitosAdapter

    private val listaHabitos = mutableListOf<Habito>()
    private val idsHabitos = mutableListOf<String>()

    private val fechaSeleccionada = Calendar.getInstance()

    private val iconosHabitos = listOf(
        "📚", "🏋", "💧", "😴",
        "🍎", "🚶", "🧘", "💻"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_habitos)

        db = FirebaseFirestore.getInstance()

        recyclerHabitos = findViewById(R.id.recyclerHabitos)
        btnAgregarHabito = findViewById(R.id.btnAgregarHabito)
        bottomNavigation = findViewById(R.id.bottomNavigation)

        tvSemanaHabitos = findViewById(R.id.tvSemanaHabitos)
        tvRachaGeneral = findViewById(R.id.tvRachaGeneral)
        tvProgresoGeneral = findViewById(R.id.tvProgresoGeneral)
        btnCalendarioHabitos = findViewById(R.id.btnCalendarioHabitos)
        btnOpcionesHabitos = findViewById(R.id.btnOpcionesHabitos)

        adapter = HabitosAdapter(
            listaHabitos = listaHabitos,
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

        recyclerHabitos.layoutManager = LinearLayoutManager(this)
        recyclerHabitos.adapter = adapter

        btnAgregarHabito.setOnClickListener {
            mostrarDialogoAgregar()
        }

        // El botón de calendario vuelve a seleccionar el día actual.
        btnCalendarioHabitos.setOnClickListener {
            fechaSeleccionada.timeInMillis = System.currentTimeMillis()
            actualizarSelectorSemana()
            actualizarResumen()
            adapter.seleccionarDia(
                fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
            )
        }

        // Opciones útiles, sin agregar funciones ficticias.
        btnOpcionesHabitos.setOnClickListener { vista ->
            val menu = PopupMenu(this, vista)
            menu.menu.add("Agregar hábito")
            menu.menu.add("Actualizar hábitos")

            menu.setOnMenuItemClickListener { opcion ->
                when (opcion.title.toString()) {
                    "Agregar hábito" -> mostrarDialogoAgregar()
                    "Actualizar hábitos" -> cargarHabitos()
                }
                true
            }

            menu.show()
        }

        configurarSelectorSemana()
        setupBottomNavigation(R.id.nav_habitos)
        cargarHabitos()
    }

    // Carga los hábitos existentes del usuario desde Firestore.
    private fun cargarHabitos() {
        val usuario = auth.currentUser ?: return

        db.collection("usuarios")
            .document(usuario.uid)
            .collection("habitos")
            .get()
            .addOnSuccessListener { documentos ->

                listaHabitos.clear()
                idsHabitos.clear()

                val diasDelMes = Calendar.getInstance()
                    .getActualMaximum(Calendar.DAY_OF_MONTH)

                for (documento in documentos) {
                    val nombre = documento.getString("nombre") ?: continue

                    val diasGuardados =
                        documento.get("dias") as? List<*>

                    val dias = MutableList(diasDelMes) { indice ->
                        diasGuardados
                            ?.getOrNull(indice) as? Boolean ?: false
                    }

                    listaHabitos.add(Habito(nombre, dias))
                    idsHabitos.add(documento.id)
                }

                adapter.notifyDataSetChanged()
                actualizarSelectorSemana()
                actualizarResumen()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudieron cargar los hábitos",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // Configura los siete días de la semana.
    private fun configurarSelectorSemana() {
        actualizarSelectorSemana()

        for (numero in 1..7) {
            val columnaId = resources.getIdentifier(
                "diaColumna$numero",
                "id",
                packageName
            )

            val columna = findViewById<LinearLayout>(columnaId)

            columna.setOnClickListener {
                val hoy = Calendar.getInstance()

                val fechaDeLaColumna = obtenerFechaDeLaColumna(numero)

                // Los datos actuales solo guardan días del mes actual.
                if (
                    fechaDeLaColumna.get(Calendar.MONTH) !=
                    hoy.get(Calendar.MONTH) ||
                    fechaDeLaColumna.get(Calendar.YEAR) !=
                    hoy.get(Calendar.YEAR)
                ) {
                    Toast.makeText(
                        this,
                        "El seguimiento actual solo admite días del mes vigente",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                fechaSeleccionada.timeInMillis =
                    fechaDeLaColumna.timeInMillis

                actualizarSelectorSemana()
                actualizarResumen()

                adapter.seleccionarDia(
                    fechaSeleccionada.get(Calendar.DAY_OF_MONTH)
                )
            }
        }
    }

    private fun obtenerFechaDeLaColumna(numero: Int): Calendar {
        val fecha = Calendar.getInstance()
        fecha.timeInMillis = fechaSeleccionada.timeInMillis

        val diaSemana = fecha.get(Calendar.DAY_OF_WEEK)
        val desplazamientoLunes = (diaSemana + 5) % 7

        fecha.add(Calendar.DAY_OF_MONTH, -desplazamientoLunes)
        fecha.add(Calendar.DAY_OF_MONTH, numero - 1)

        return fecha
    }

    private fun actualizarSelectorSemana() {
        val formato = SimpleDateFormat(
            "d MMM",
            Locale.forLanguageTag("es-AR")
        )

        val lunes = obtenerFechaDeLaColumna(1)
        val domingo = obtenerFechaDeLaColumna(7)

        tvSemanaHabitos.text =
            "${formato.format(lunes.time)} – ${formato.format(domingo.time)}"

        val diasCortos = listOf("L", "M", "X", "J", "V", "S", "D")
        val hoy = Calendar.getInstance()

        for (numero in 1..7) {
            val fecha = obtenerFechaDeLaColumna(numero)

            val nombreId = resources.getIdentifier(
                "tvNombreDia$numero", "id", packageName
            )
            val numeroId = resources.getIdentifier(
                "tvNumeroDia$numero", "id", packageName
            )
            val puntoId = resources.getIdentifier(
                "tvPuntoDia$numero", "id", packageName
            )
            val columnaId = resources.getIdentifier(
                "diaColumna$numero", "id", packageName
            )

            val nombre = findViewById<TextView>(nombreId)
            val numeroDia = findViewById<TextView>(numeroId)
            val punto = findViewById<TextView>(puntoId)
            val columna = findViewById<LinearLayout>(columnaId)

            val diaDelMes = fecha.get(Calendar.DAY_OF_MONTH)
            val esSeleccionado =
                fecha.get(Calendar.YEAR) == fechaSeleccionada.get(Calendar.YEAR) &&
                        fecha.get(Calendar.DAY_OF_YEAR) ==
                        fechaSeleccionada.get(Calendar.DAY_OF_YEAR)

            val esMesActual =
                fecha.get(Calendar.MONTH) == hoy.get(Calendar.MONTH) &&
                        fecha.get(Calendar.YEAR) == hoy.get(Calendar.YEAR)

            nombre.text = diasCortos[numero - 1]
            numeroDia.text = diaDelMes.toString()

            columna.setBackgroundResource(
                if (esSeleccionado) {
                    R.drawable.habito_day_selected
                } else {
                    R.drawable.habito_day_background
                }
            )

            nombre.setTextColor(
                if (esSeleccionado) Color.BLACK else Color.GRAY
            )

            numeroDia.setTextColor(
                if (esSeleccionado) Color.BLACK else Color.WHITE
            )

            columna.alpha = if (esMesActual) 1f else 0.45f

            val hayHabitoCompletado = listaHabitos.any { habito ->
                val indice = diaDelMes - 1
                indice in habito.dias.indices && habito.dias[indice]
            }

            punto.text = if (hayHabitoCompletado) "•" else ""
            punto.setTextColor(
                if (esSeleccionado) Color.BLACK else Color.rgb(57, 255, 20)
            )
        }
    }

    // Actualiza las tarjetas de resumen con datos reales.
    private fun actualizarResumen() {
        val indice = fechaSeleccionada.get(Calendar.DAY_OF_MONTH) - 1

        val completados = listaHabitos.count { habito ->
            indice in habito.dias.indices && habito.dias[indice]
        }

        tvProgresoGeneral.text = "$completados / ${listaHabitos.size}"
        findViewById<TextView>(R.id.tvEtiquetaProgreso).text =
            if (esHoySeleccionado()) "Progreso de hoy" else "Progreso del día"

        val mejorRacha = listaHabitos.maxOfOrNull { habito ->
            calcularRacha(habito.dias)
        } ?: 0

        tvRachaGeneral.text = "$mejorRacha días"
    }

    private fun calcularRacha(dias: List<Boolean>): Int {
        if (dias.isEmpty()) return 0

        val hoy = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1
        var indice = hoy.coerceIn(0, dias.lastIndex)

        if (!dias[indice] && indice > 0) {
            indice--
        }

        var racha = 0

        for (i in indice downTo 0) {
            if (dias[i]) {
                racha++
            } else {
                break
            }
        }

        return racha
    }

    private fun mostrarDialogoAgregar() {
        var iconoSeleccionado = iconosHabitos[0]

        AlertDialog.Builder(this)
            .setTitle("Seleccionar ícono")
            .setSingleChoiceItems(
                iconosHabitos.toTypedArray(),
                0
            ) { _, cual ->
                iconoSeleccionado = iconosHabitos[cual]
            }
            .setPositiveButton("Continuar") { _, _ ->
                mostrarDialogoNombre(null, iconoSeleccionado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoNombre(
        nombreInicial: String?,
        icono: String
    ) {
        val campo = EditText(this)
        campo.hint = "Nombre del hábito"
        campo.setHintTextColor(Color.GRAY)
        campo.setTextColor(Color.WHITE)
        campo.setPadding(40, 24, 40, 24)

        if (nombreInicial != null) {
            campo.setText(nombreInicial)
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle(
                if (nombreInicial == null) "Nuevo hábito"
                else "Editar hábito"
            )
            .setView(campo)
            .setPositiveButton(
                if (nombreInicial == null) "Agregar" else "Guardar",
                null
            )
            .setNegativeButton("Cancelar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val nombre = campo.text.toString().trim()

                    if (nombre.isEmpty()) {
                        campo.error = "Escribí un nombre"
                        return@setOnClickListener
                    }

                    if (nombreInicial == null) {
                        agregarHabitoFirestore("$icono $nombre")
                    } else {
                        val posicion = listaHabitos.indexOfFirst {
                            quitarIcono(it.nombre) == nombreInicial
                        }

                        if (posicion >= 0) {
                            actualizarHabito(
                                posicion,
                                "$icono $nombre"
                            )
                        }
                    }

                    dialogo.dismiss()
                }
        }

        dialogo.show()
    }

    private fun agregarHabitoFirestore(nombre: String) {
        val usuario = auth.currentUser ?: return

        val diasDelMes = Calendar.getInstance()
            .getActualMaximum(Calendar.DAY_OF_MONTH)

        val dias = MutableList(diasDelMes) { false }

        val datos = hashMapOf(
            "nombre" to nombre,
            "dias" to dias,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("usuarios")
            .document(usuario.uid)
            .collection("habitos")
            .add(datos)
            .addOnSuccessListener { documento ->
                listaHabitos.add(Habito(nombre, dias))
                idsHabitos.add(documento.id)

                adapter.notifyItemInserted(listaHabitos.lastIndex)
                actualizarSelectorSemana()
                actualizarResumen()

                Toast.makeText(
                    this,
                    "Hábito agregado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudo agregar el hábito",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun guardarDia(
        posicion: Int,
        dia: Int,
        estado: Boolean
    ) {
        val usuario = auth.currentUser ?: return

        if (posicion !in listaHabitos.indices) return
        if (posicion !in idsHabitos.indices) return
        if (dia !in listaHabitos[posicion].dias.indices) return

        val id = idsHabitos[posicion]
        val diasActualizados = listaHabitos[posicion].dias.toList()

        db.collection("usuarios")
            .document(usuario.uid)
            .collection("habitos")
            .document(id)
            .update("dias", diasActualizados)
            .addOnSuccessListener {
                actualizarSelectorSemana()
                actualizarResumen()
            }
            .addOnFailureListener {
                listaHabitos[posicion].dias[dia] = !estado
                adapter.notifyItemChanged(posicion)
                actualizarSelectorSemana()
                actualizarResumen()

                Toast.makeText(
                    this,
                    "No se pudo guardar el cambio",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun mostrarDialogoEditar(posicion: Int) {
        if (posicion !in listaHabitos.indices) return

        val habito = listaHabitos[posicion]
        val icono = obtenerIcono(habito.nombre)
        val nombre = quitarIcono(habito.nombre)

        val campo = EditText(this)
        campo.hint = "Nombre del hábito"
        campo.setTextColor(Color.WHITE)
        campo.setHintTextColor(Color.GRAY)
        campo.setText(nombre)
        campo.setPadding(40, 24, 40, 24)

        AlertDialog.Builder(this)
            .setTitle("Editar hábito")
            .setView(campo)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = campo.text.toString().trim()

                if (nuevoNombre.isNotEmpty()) {
                    actualizarHabito(posicion, "$icono $nuevoNombre")
                } else {
                    Toast.makeText(
                        this,
                        "El nombre no puede estar vacío",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarHabito(posicion: Int, nuevoNombre: String) {
        val usuario = auth.currentUser ?: return

        if (posicion !in idsHabitos.indices) return

        db.collection("usuarios")
            .document(usuario.uid)
            .collection("habitos")
            .document(idsHabitos[posicion])
            .update("nombre", nuevoNombre)
            .addOnSuccessListener {
                listaHabitos[posicion].nombre = nuevoNombre
                adapter.notifyItemChanged(posicion)
                actualizarResumen()

                Toast.makeText(
                    this,
                    "Hábito actualizado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudo actualizar el hábito",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun confirmarEliminacion(posicion: Int) {
        if (posicion !in listaHabitos.indices) return

        val nombre = quitarIcono(listaHabitos[posicion].nombre)

        AlertDialog.Builder(this)
            .setTitle("Eliminar hábito")
            .setMessage("¿Querés eliminar \"$nombre\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarHabito(posicion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarHabito(posicion: Int) {
        val usuario = auth.currentUser ?: return

        if (posicion !in idsHabitos.indices) return

        db.collection("usuarios")
            .document(usuario.uid)
            .collection("habitos")
            .document(idsHabitos[posicion])
            .delete()
            .addOnSuccessListener {
                listaHabitos.removeAt(posicion)
                idsHabitos.removeAt(posicion)

                adapter.notifyItemRemoved(posicion)
                actualizarSelectorSemana()
                actualizarResumen()

                Toast.makeText(
                    this,
                    "Hábito eliminado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "No se pudo eliminar el hábito",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun obtenerIcono(nombre: String): String {
        return iconosHabitos.firstOrNull { nombre.startsWith(it) } ?: "📚"
    }

    private fun quitarIcono(nombre: String): String {
        val icono = iconosHabitos.firstOrNull { nombre.startsWith(it) }
        return if (icono != null) {
            nombre.removePrefix(icono).trim()
        } else {
            nombre
        }
    }

    private fun setupBottomNavigation(itemSeleccionado: Int) {
        bottomNavigation.selectedItemId = itemSeleccionado

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    true
                }

                R.id.nav_kanban -> {
                    startActivity(Intent(this, KanbanActivity::class.java))
                    true
                }

                R.id.nav_create -> {
                    startActivity(Intent(this, CreateTaskActivity::class.java))
                    true
                }

                R.id.nav_habitos -> true

                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }
    private fun esHoySeleccionado(): Boolean {
        val hoy = Calendar.getInstance()
        return fechaSeleccionada.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                fechaSeleccionada.get(Calendar.DAY_OF_YEAR) ==
                hoy.get(Calendar.DAY_OF_YEAR)
    }
}
