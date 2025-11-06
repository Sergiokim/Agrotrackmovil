package com.example.agrotrackmovil.ui.plant.edit

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.agrotrackmovil.ui.plant.detail.PlantDetailActivity
import com.example.agrotrackmovil.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.agrotrackmovil.domain.model.Plant
import com.example.agrotrackmovil.ui.dashboard.DashboardActivity

class EditPlantActivity : AppCompatActivity() {

    private lateinit var nombreEditText: EditText
    private lateinit var nombreCientificoEditText: EditText
    private lateinit var latEditText: EditText
    private lateinit var lonEditText: EditText
    private lateinit var cuidadosEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var plantId: String
    private lateinit var plant: Plant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_plant)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        try {
            nombreEditText = findViewById(R.id.nombre_edit_text)
            nombreCientificoEditText = findViewById(R.id.nombre_cientifico_edit_text)
            latEditText = findViewById(R.id.lat_edit_text)
            lonEditText = findViewById(R.id.lon_edit_text)
            saveButton = findViewById(R.id.save_button)
            cancelButton = findViewById(R.id.cancel_button)
        } catch (e: Exception) {
            Log.e("EditPlantActivity", "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "Error al cargar la interfaz", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Get plant data from intent
        plantId = intent.getStringExtra("PLANT_ID") ?: run {
            Log.e("EditPlantActivity", "No plantId provided")
            Toast.makeText(this, "Planta no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        plant = intent.getSerializableExtra("PLANT_DATA") as? Plant ?: run {
            Log.e("EditPlantActivity", "No plant data provided")
            Toast.makeText(this, "Datos de planta no encontrados", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Populate fields
        nombreEditText.setText(plant.nombre)
        nombreCientificoEditText.setText(plant.nombreCientifico)
        latEditText.setText(plant.lat)
        lonEditText.setText(plant.lon)

        // Set up buttons
        saveButton.setOnClickListener {
            savePlant()
        }
        cancelButton.setOnClickListener {
            startActivity(Intent(this, PlantDetailActivity::class.java).apply {
                putExtra("PLANT_ID", plantId)
            })
            Log.d("EditPlantActivity", "Cancelled editing, returned to PlantDetailActivity")
            finish()
        }
    }

    private fun savePlant() {
        val nombre = nombreEditText.text.toString().trim()
        if (nombre.isEmpty()) {
            Toast.makeText(this, "El nombre es obligatorio", Toast.LENGTH_SHORT).show()
            return
        }

        val plantData = hashMapOf(
            "nombre" to nombre,
            "nombreCientifico" to nombreCientificoEditText.text.toString().trim().takeIf { it.isNotEmpty() },
            "lat" to latEditText.text.toString().trim().takeIf { it.isNotEmpty() },
            "lon" to lonEditText.text.toString().trim().takeIf { it.isNotEmpty() },
            "userId" to plant.userId,
            "createdAt" to plant.createdAt,
            "imagen" to plant.imagen,
            "datosClima" to plant.datosClima
        )

        db.collection("plantas").document(plantId)
            .set(plantData)
            .addOnSuccessListener {
                Log.d("EditPlantActivity", "Plant updated: $plantId")
                Toast.makeText(this, "Planta actualizada correctamente", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                Log.e("EditPlantActivity", "Failed to update plant: ${e.message}", e)
                Toast.makeText(this, "Error al actualizar la planta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}