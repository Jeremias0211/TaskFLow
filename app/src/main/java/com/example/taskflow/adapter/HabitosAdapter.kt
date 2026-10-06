package com.example.taskflow.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    class HabitoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.tvNombreHabito)
        val racha: TextView = itemView.findViewById(R.id.tvRacha)
        val gridDias: GridLayout = itemView.findViewById(R.id.gridDias)
        val progreso: ProgressBar = itemView.findViewById(R.id.progresoHabito)
        val textoProgreso: TextView = itemView.findViewById(R.id.tvProgreso)
    }

    private fun calcularRacha(dias: List<Boolean>): Int {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        var racha = 0
        var checkIndex = (currentDay - 1).coerceIn(0, dias.size - 1)

        if (checkIndex in dias.indices && !dias[checkIndex] && checkIndex > 0) {
            checkIndex--
        }

        for (i in checkIndex downTo 0) {
            if (i in dias.indices && dias[i]) {
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

    override fun getItemCount(): Int {
        return listaHabitos.size
    }

    override fun onBindViewHolder(
        holder: HabitoViewHolder,
        position: Int
    ) {
        val habito = listaHabitos[position]
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        while (habito.dias.size < daysInMonth) {
            habito.dias.add(false)
        }
        if (habito.dias.size > daysInMonth) {
            while (habito.dias.size > daysInMonth) {
                habito.dias.removeAt(habito.dias.size - 1)
            }
        }

        val diasCompletados = habito.dias.count { it }

        holder.progreso.max = daysInMonth
        holder.progreso.progress = diasCompletados
        holder.textoProgreso.text = "$diasCompletados / $daysInMonth días"
        holder.nombre.text = habito.nombre

        val racha = calcularRacha(habito.dias)
        holder.racha.text = "🔥 Racha actual: $racha días"

        holder.gridDias.removeAllViews()

        for (i in habito.dias.indices) {
            val dia = TextView(holder.itemView.context)

            dia.text = (i + 1).toString()
            dia.textAlignment = View.TEXT_ALIGNMENT_CENTER
            dia.textSize = 14f
            dia.setPadding(10, 10, 10, 10)

            val params = GridLayout.LayoutParams()
            params.width = 110
            params.height = 110
            params.setMargins(8, 8, 8, 8)

            dia.layoutParams = params

            if (habito.dias[i]) {
                dia.setBackgroundResource(R.drawable.dia_completado)
                dia.setTextColor(Color.BLACK)
            } else {
                dia.setBackgroundResource(R.drawable.dia_pendiente)
                dia.setTextColor(Color.WHITE)
            }

            dia.setOnClickListener {
                val posicionActual = holder.adapterPosition
                if (posicionActual == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }

                val nuevoEstado = !habito.dias[i]
                habito.dias[i] = nuevoEstado
                notifyItemChanged(posicionActual)

                onDiaCambiado(
                    posicionActual,
                    i,
                    nuevoEstado
                )
            }

            holder.gridDias.addView(dia)
        }

        holder.nombre.setOnClickListener {
            val posicionActual = holder.adapterPosition
            if (posicionActual == RecyclerView.NO_POSITION) {
                return@setOnClickListener
            }
            onEditar(posicionActual)
        }

        holder.itemView.setOnLongClickListener {
            val posicionActual = holder.adapterPosition
            if (posicionActual == RecyclerView.NO_POSITION) {
                return@setOnLongClickListener true
            }
            onEliminar(posicionActual)
            true
        }
    }
}
