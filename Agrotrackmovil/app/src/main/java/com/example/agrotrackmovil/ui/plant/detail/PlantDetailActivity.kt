package com.example.agrotrackmovil.ui.plant.detail

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.agrotrackmovil.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.agrotrackmovil.data.remote.OpenMeteoResponse
import com.example.agrotrackmovil.data.remote.RetrofitClient
import com.example.agrotrackmovil.domain.model.Plant
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.agrotrackmovil.ui.dashboard.DashboardActivity
import com.example.agrotrackmovil.ui.main.MainActivity
import com.example.agrotrackmovil.ui.plant.edit.AddPlantActivity
import com.example.agrotrackmovil.ui.plant.edit.EditPlantActivity
import java.text.SimpleDateFormat
import java.util.Locale

class PlantDetailActivity : AppCompatActivity() {

    private lateinit var heroTitle: TextView
    private lateinit var plantaImage: ImageView
    private lateinit var nombreCientifico: TextView
    private lateinit var cuidados: TextView
    private lateinit var coordenadas: TextView
    private lateinit var fechaCreacion: TextView
    private lateinit var temperature: TextView
    private lateinit var humidity: TextView
    private lateinit var precipitation: TextView
    private lateinit var climateCard: CardView
    private lateinit var noClimateData: TextView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var errorMessage: TextView
    private lateinit var notFoundMessage: TextView
    private lateinit var backButton: Button
    private lateinit var addPlantButton: Button
    private lateinit var deleteButton: Button
    private lateinit var editButton: Button
    private lateinit var planta_card: CardView
    private lateinit var prefs: SharedPreferences
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_plant_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            heroTitle = findViewById(R.id.hero_title)
            plantaImage = findViewById(R.id.planta_image)
            nombreCientifico = findViewById(R.id.nombre_cientifico)
            coordenadas = findViewById(R.id.coordenadas)
            fechaCreacion = findViewById(R.id.fecha_creacion)
            temperature = findViewById(R.id.temperature)
            humidity = findViewById(R.id.humidity)
            climateCard = findViewById(R.id.climate_card)
            noClimateData = findViewById(R.id.no_climate_data)
            loadingSpinner = findViewById(R.id.loading_spinner)
            errorMessage = findViewById(R.id.error_message)
            notFoundMessage = findViewById(R.id.not_found_message)
            backButton = findViewById(R.id.back_button)
            addPlantButton = findViewById(R.id.add_plant_button)
            deleteButton = findViewById(R.id.delete_button)
            editButton = findViewById(R.id.edit_button)
            planta_card = findViewById(R.id.planta_card)
        } catch (e: Exception) {
            Log.e("PlantDetailActivity", "Error initializing views: ${e.message}", e)
            Toast.makeText(this, "Error al cargar la interfaz", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        prefs = getSharedPreferences("app_session", MODE_PRIVATE)
        db = FirebaseFirestore.getInstance()

        val savedUserId = prefs.getString("userId", null)
        if (savedUserId == null) {
            Log.e("PlantDetailActivity", "No user session found")
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val plantId = intent.getStringExtra("PLANT_ID")
        if (plantId == null) {
            Log.e("PlantDetailActivity", "No plantId provided")
            showNotFound()
            return
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            Log.d("PlantDetailActivity", "Navigated to DashboardActivity")
            finish()
        }
        addPlantButton.setOnClickListener {
            startActivity(Intent(this, AddPlantActivity::class.java))
            Log.d("PlantDetailActivity", "Navigated to AddPlantActivity")
            finish()
        }
        deleteButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Eliminar Planta")
                .setMessage("¿Estás seguro de que deseas eliminar esta planta?")
                .setPositiveButton("Sí") { _, _ ->
                    deletePlant(plantId, savedUserId)
                }
                .setNegativeButton("No") { _, _ ->
                    Log.d("PlantDetailActivity", "Deletion canceled for plant ID: $plantId")
                }
                .setCancelable(true)
                .show()
        }
        editButton.setOnClickListener {
            db.collection("plantas").document(plantId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val plant = document.toObject(Plant::class.java)?.apply { id = document.id }
                        if (plant != null && plant.userId == savedUserId) {
                            val intent = Intent(this, EditPlantActivity::class.java).apply {
                                putExtra("PLANT_ID", plantId)
                                putExtra("PLANT_DATA", plant)
                            }
                            startActivity(intent)
                            Log.d("PlantDetailActivity", "Navigated to EditPlantActivity for plant ID: $plantId")
                            finish()
                        } else {
                            Log.e("PlantDetailActivity", "Plant not found or does not belong to user")
                            showNotFound()
                        }
                    } else {
                        Log.e("PlantDetailActivity", "Document does not exist for plant ID: $plantId")
                        showNotFound()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PlantDetailActivity", "Failed to fetch plant for editing: ${e.message}", e)
                    Toast.makeText(this, "Error al cargar datos para editar", Toast.LENGTH_SHORT).show()
                }
        }

        loadPlantData(plantId, savedUserId)
    }

    private fun deletePlant(plantId: String, userId: String) {
        setLoading(true)
        db.collection("plantas").document(plantId).get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.toObject(Plant::class.java)?.userId == userId) {
                    db.collection("plantas").document(plantId).delete()
                        .addOnSuccessListener {
                            Log.d("PlantDetailActivity", "Plant deleted: $plantId")
                            Toast.makeText(this, "Planta eliminada correctamente", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("PlantDetailActivity", "Failed to delete plant: ${e.message}", e)
                            Toast.makeText(this, "Error al eliminar la planta: ${e.message}", Toast.LENGTH_SHORT).show()
                            setLoading(false)
                        }
                } else {
                    Log.e("PlantDetailActivity", "Plant not found or does not belong to user")
                    showNotFound()
                    setLoading(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PlantDetailActivity", "Failed to verify plant for deletion: ${e.message}", e)
                Toast.makeText(this, "Error al verificar la planta: ${e.message}", Toast.LENGTH_SHORT).show()
                setLoading(false)
            }
    }

    private fun loadPlantData(plantId: String, userId: String) {
        setLoading(true)
        Log.d("PlantDetailActivity", "Starting query for plant ID: $plantId, User ID: $userId")
        db.collection("plantas").document(plantId).get()
            .addOnSuccessListener { document ->
                try {
                    if (document.exists()) {
                        val plant = document.toObject(Plant::class.java)?.apply { id = document.id }
                        if (plant != null && plant.userId == userId) {
                            Log.d("PlantDetailActivity", "Plant found: ${document.data}")
                            if (plant.lat != null && plant.lon != null) {
                                fetchClimateData(plant, userId)
                            } else {
                                Log.w("PlantDetailActivity", "No coordinates for plant ${plant.id}: lat=${plant.lat}, lon=${plant.lon}")
                                displayPlantData(plant, null)
                                setLoading(false)
                            }
                        } else {
                            Log.e("PlantDetailActivity", "Plant not found or does not belong to user")
                            showNotFound()
                            setLoading(false)
                        }
                    } else {
                        Log.e("PlantDetailActivity", "Document does not exist for plant ID: $plantId")
                        showNotFound()
                        setLoading(false)
                    }
                } catch (e: Exception) {
                    Log.e("PlantDetailActivity", "Error mapping plant data: ${e.message}", e)
                    showError("Error al cargar los datos: ${e.message}")
                    setLoading(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("PlantDetailActivity", "Firestore query failed: ${e.message}", e)
                showError("Error al cargar los datos: ${e.message}")
                setLoading(false)
            }
    }

    private fun fetchClimateData(plant: Plant, userId: String) {
        val lat = plant.lat?.toDoubleOrNull()
        val lon = plant.lon?.toDoubleOrNull()
        if (lat == null || lon == null || lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            Log.e("PlantDetailActivity", "Invalid coordinates: lat=${plant.lat}, lon=${plant.lon}")
            displayPlantData(plant, null)
            setLoading(false)
            return
        }

        Log.d("PlantDetailActivity", "Fetching climate data from Open-Meteo for lat=$lat, lon=$lon")
        RetrofitClient.openMeteoApi.getCurrentWeather(lat, lon)
            .enqueue(object : Callback<OpenMeteoResponse> {
                override fun onResponse(call: Call<OpenMeteoResponse>, response: Response<OpenMeteoResponse>) {
                    when (response.code()) {
                        200 -> {
                            val current = response.body()?.current
                            if (current != null) {
                                val datosClima = mapOf(
                                    "temperature" to current.temperature_2m.toString(),
                                    "humidity" to current.relative_humidity_2m.toString(),
                                    "precipitation" to current.precipitation.toString()
                                )
                                displayPlantData(plant, datosClima)
                            } else {
                                Toast.makeText(this@PlantDetailActivity, "No hay datos climáticos", Toast.LENGTH_SHORT).show()
                                displayPlantData(plant, null)
                            }
                        }
                        401 -> showError("Error 401: No autorizado.")
                        404 -> showError("Error 404: Recurso no encontrado.")
                        500 -> showError("Error 500: Problema del servidor.")
                        else -> showError("Error HTTP ${response.code()}: ${response.message()}")
                    }
                    setLoading(false)
                }

                override fun onFailure(call: Call<OpenMeteoResponse>, t: Throwable) {
                    showError("Error de conexión: ${t.localizedMessage}")
                    displayPlantData(plant, null)
                    setLoading(false)
                }
            })
    }

    private fun displayPlantData(plant: Plant, datosClima: Map<String, String>?, imageWidthDp: Int = 500, imageHeightDp: Int = 400) {
        heroTitle.text = plant.nombre
        nombreCientifico.text = "Nombre científico: ${plant.nombreCientifico ?: "No disponible"}"
        coordenadas.text = "Coordenadas: Lat: ${plant.lat ?: "No disponible"}, Lon: ${plant.lon ?: "No disponible"}"
        fechaCreacion.text = "Fecha de creación: ${
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .parse(plant.createdAt)?.toLocaleString() ?: "No disponible"
            } catch (e: Exception) {
                "No disponible"
            }
        }"

        val density = resources.displayMetrics.density
        val imageWidthPx = (imageWidthDp * density).toInt()
        val imageHeightPx = (imageHeightDp * density).toInt()

        if (plant.imagen != null) {
            plantaImage.visibility = View.VISIBLE
            val layoutParams = plantaImage.layoutParams
            layoutParams.width = imageWidthPx
            layoutParams.height = imageHeightPx
            plantaImage.layoutParams = layoutParams

            Glide.with(this)
                .load(plant.imagen)
                .centerCrop()
                .error(R.drawable.placeholder_plant)
                .into(plantaImage)
            Log.d("PlantDetailActivity", "Loading image: ${plant.imagen} with width=${imageWidthPx}px, height=${imageHeightPx}px")
        } else {
            plantaImage.visibility = View.GONE
            Log.d("PlantDetailActivity", "No image available")
        }

        if (datosClima != null) {
            climateCard.visibility = View.VISIBLE
            noClimateData.visibility = View.GONE
            temperature.text = "Temperatura: ${datosClima["temperature"] ?: "No disponible"} °C"
            humidity.text = "Humedad: ${datosClima["humidity"] ?: "No disponible"} %"
            Log.d("PlantDetailActivity", "Climate data displayed: $datosClima")
        } else {
            climateCard.visibility = View.GONE
            noClimateData.visibility = View.VISIBLE
            Log.d("PlantDetailActivity", "No climate data available")
        }
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
        notFoundMessage.visibility = View.GONE
        planta_card.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showNotFound() {
        notFoundMessage.visibility = View.VISIBLE
        errorMessage.visibility = View.GONE
        planta_card.visibility = View.GONE
        Toast.makeText(this, "Planta no encontrada", Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(isLoading: Boolean) {
        loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        planta_card.visibility = if (isLoading) View.GONE else View.VISIBLE
        errorMessage.visibility = View.GONE
        notFoundMessage.visibility = View.GONE
    }
}