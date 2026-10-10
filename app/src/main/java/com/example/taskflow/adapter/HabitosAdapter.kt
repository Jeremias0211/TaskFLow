package com.example.taskflow.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.taskflow.R
import com.example.taskflow.model.Habito
import java.util.Calendar

class HabitosAdapter(
    private val listaHabitos: MutableList<Habito>,
    private val onEliminar: (Int) -> Unit,
    private val onEditar: (Int) -> Unit,
    private val onDiaCambiado: (Int, Int, Boolean) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<HabitosAdapter.HabitoViewHolder>() {

    private var diaSeleccionado: Int = Calendar.getInstance()
        .get(Calendar.DAY_OF_MONTH) - 1

    class HabitoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.tvNombreHabito)
        val racha: TextView = itemView.findViewById(R.id.tvRacha)
        val gridDias: GridLayout = itemView.findViewById(R.id.gridDias)
        val progreso: ProgressBar = itemView.findViewById(R.id.progresoHabito)
        val textoProgreso: TextView = itemView.findViewById(R.id.tvProgreso)
        val checkHabito: CheckBox = itemView.findViewById(R.id.checkHabitoHoy)
        val icono: TextView = itemView.findViewById(R.id.tvIconoHabito)
    }

    fun seleccionarDia(diaDelMes: Int) {
        diaSeleccionado = diaDelMes - 1
        notifyDataSetChanged()
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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HabitoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habito, parent, false)
        return HabitoViewHolder(vista)
    }

    override fun getItemCount(): Int = listaHabitos.size

    override fun onBindViewHolder(holder: HabitoViewHolder, position: Int) {
        val habito = listaHabitos[position]
        val calendar = Calendar.getInstance()
        val diasDelMes = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        while (habito.dias.size < diasDelMes) {
            habito.dias.add(false)
        }
        while (habito.dias.size > diasDelMes) {
            habito.dias.removeAt(habito.dias.lastIndex)
        }

        val completados = habito.dias.count { it }
        holder.nombre.text = quitarIcono(habito.nombre)
        holder.icono.text = obtenerIcono(habito.nombre)
        holder.progreso.max = diasDelMes
        holder.progreso.progress = completados
        holder.textoProgreso.text = "$completados / $diasDelMes días"

        val racha = calcularRacha(habito.dias)
        holder.racha.text = "🔥 Racha actual: $racha días"

        val indiceDia = diaSeleccionado.coerceIn(0, habito.dias.lastIndex.coerceAtLeast(0))
        holder.checkHabito.setOnCheckedChangeListener(null)
        holder.checkHabito.isChecked =
            habito.dias.isNotEmpty() && habito.dias[indiceDia]

        holder.checkHabito.setOnCheckedChangeListener { _, marcado ->
            val posicionActual = holder.adapterPosition
            if (posicionActual == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener

            val habitoActual = listaHabitos[posicionActual]
            if (indiceDia !in habitoActual.dias.indices) return@setOnCheckedChangeListener

            if (habitoActual.dias[indiceDia] != marcado) {
                habitoActual.dias[indiceDia] = marcado
                notifyItemChanged(posicionActual)
                onDiaCambiado(posicionActual, indiceDia, marcado)
            }
        }

        holder.nombre.setOnClickListener {
            val posicionActual = holder.adapterPosition
            if (posicionActual != RecyclerView.NO_POSITION) {
                onEditar(posicionActual)
            }
        }

        holder.itemView.setOnLongClickListener {
            val posicionActual = holder.adapterPosition
            if (posicionActual != RecyclerView.NO_POSITION) {
                onEliminar(posicionActual)
            }
            true
        }

        holder.gridDias.removeAllViews()
    }

    private fun obtenerIcono(nombre: String): String {
        val iconos = listOf("📚", "🏋", "💧", "😴", "🍎", "🚶", "🧘", "💻")
        return iconos.firstOrNull { nombre.startsWith(it) } ?: "●"
    }

    private fun quitarIcono(nombre: String): String {
        val iconos = listOf("📚", "🏋", "💧", "😴", "🍎", "🚶", "🧘", "💻")
        return iconos.firstOrNull { nombre.startsWith(it) }
            ?.let { nombre.removePrefix(it).trim() }
            ?: nombre
    }
}
