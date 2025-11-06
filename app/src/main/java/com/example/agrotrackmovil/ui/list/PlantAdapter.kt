package com.example.agrotrackmovil.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.agrotrackmovil.R
import com.example.agrotrackmovil.domain.model.Plant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantAdapter(private val onPlantClick: (String) -> Unit) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    private var plants: List<Plant> = emptyList()

    fun setPlants(newPlants: List<Plant>) {
        plants = newPlants
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        holder.bind(plant)
    }

    override fun getItemCount(): Int = plants.size

    inner class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.plantNameTextView)
        private val scientificNameTextView: TextView = itemView.findViewById(R.id.scientificNameTextView)
        private val createdAtTextView: TextView = itemView.findViewById(R.id.createdAtTextView)
        private val plantImageView: ImageView = itemView.findViewById(R.id.plantImageView)

        fun bind(plant: Plant) {
            nameTextView.text = plant.nombre
            scientificNameTextView.text = plant.nombreCientifico ?: "Sin nombre científico"
            // Try multiple date formats for createdAt
            createdAtTextView.text = try {
                val possibleFormats = listOf(
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                )
                var date: Date? = null
                for (format in possibleFormats) {
                    try {
                        date = format.parse(plant.createdAt)
                        if (date != null) break
                    } catch (e: Exception) {
                        continue
                    }
                }
                if (date != null) {
                    "Creada: ${SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES")).format(date)}"
                } else {
                    "Creada: ${plant.createdAt}" // Fallback to raw string if all formats fail
                }
            } catch (e: Exception) {
                "Creada: ${plant.createdAt}" // Fallback to raw string if parsing fails
            }
            if (!plant.imagen.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(plant.imagen)
                    .into(plantImageView)
            } else {
                plantImageView.setImageResource(R.drawable.placeholder_plant)
            }

            itemView.setOnClickListener {
                onPlantClick(plant.id)
            }
        }
    }
}