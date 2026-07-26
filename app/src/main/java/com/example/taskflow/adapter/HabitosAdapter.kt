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

class HabitosAdapter(
    private val listaHabitos: MutableList<Habito>,
    private val onEliminar: (Int) -> Unit,
    private val onEditar: (Int) -> Unit
) : RecyclerView.Adapter<HabitosAdapter.HabitoViewHolder>() {

    class HabitoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nombre: TextView = itemView.findViewById(R.id.tvNombreHabito)
        val racha: TextView = itemView.findViewById(R.id.tvRacha)
        val gridDias: GridLayout = itemView.findViewById(R.id.gridDias)
        val progreso: ProgressBar = itemView.findViewById(R.id.progresoHabito)
        val textoProgreso: TextView = itemView.findViewById(R.id.tvProgreso)
    }

    private fun calcularRacha(dias: List<Boolean>): Int {
        var racha = 0

        for (i in dias.indices.reversed()) {
            if (dias[i]) {
                racha++
            } else {
                break
            }
        }

        return racha
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habito, parent, false)

        return HabitoViewHolder(vista)
    }

    override fun getItemCount(): Int {
        return listaHabitos.size
    }

    override fun onBindViewHolder(holder: HabitoViewHolder, position: Int) {

        val habito = listaHabitos[position]

        val diasCompletados = habito.dias.count { it }

        holder.progreso.progress = diasCompletados
        holder.textoProgreso.text = "$diasCompletados / 31 días"

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

                habito.dias[i] = !habito.dias[i]

                notifyItemChanged(position)
            }

            holder.gridDias.addView(dia)
        }

        // Tocar el nombre para editar
        holder.nombre.setOnClickListener {
            onEditar(position)
        }

        // Mantener presionado para eliminar
        holder.itemView.setOnLongClickListener {

            onEliminar(position)

            true
        }
    }
}