package com.example.agrotrackmovil.ui.plant.detail

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.agrotrackmovil.data.remote.OpenMeteoResponse
import com.example.agrotrackmovil.data.remote.RetrofitClient
import com.example.agrotrackmovil.domain.model.Plant
import com.example.agrotrackmovil.ui.dashboard.DashboardActivity
import com.example.agrotrackmovil.ui.main.MainActivity
import com.example.agrotrackmovil.ui.plant.edit.AddPlantActivity
import com.example.agrotrackmovil.ui.plant.edit.EditPlantActivity
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class PlantDetailActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("app_session", MODE_PRIVATE)
        db = FirebaseFirestore.getInstance()

        val savedUserId = prefs.getString("userId", null)
        val plantId = intent.getStringExtra("PLANT_ID")

        if (savedUserId == null) {
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        if (plantId == null) {
            Toast.makeText(this, "Planta no encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                PlantDetailScreen(
                    db = db,
                    plantId = plantId,
                    userId = savedUserId,
                    onBack = {
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    },
                    onAdd = {
                        startActivity(Intent(this, AddPlantActivity::class.java))
                        finish()
                    },
                    onEdit = { plant ->
                        val intent = Intent(this, EditPlantActivity::class.java).apply {
                            putExtra("PLANT_ID", plant.id)
                            putExtra("PLANT_DATA", plant)
                        }
                        startActivity(intent)
                        finish()
                    },
                    onDelete = { id ->
                        deletePlant(id, savedUserId)
                    }
                )
            }
        }
    }

    private fun deletePlant(plantId: String, userId: String) {
        db.collection("plantas").document(plantId).get()
            .addOnSuccessListener { document ->
                val plant = document.toObject(Plant::class.java)
                if (document.exists() && plant?.userId == userId) {
                    db.collection("plantas").document(plantId).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Planta eliminada correctamente", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al eliminar: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Planta no encontrada o no pertenece al usuario", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar la planta: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    db: FirebaseFirestore,
    plantId: String,
    userId: String,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Plant) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var plant by remember { mutableStateOf<Plant?>(null) }
    var climateData by remember { mutableStateOf<Map<String, String>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Cargar datos de planta
    LaunchedEffect(plantId) {
        db.collection("plantas").document(plantId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.toObject(Plant::class.java)?.apply { id = document.id }
                    if (data != null && data.userId == userId) {
                        plant = data
                        if (data.lat != null && data.lon != null)
                            fetchClimateData(context, data) { result -> climateData = result }
                    } else errorMessage = "Planta no encontrada o no pertenece al usuario"
                } else errorMessage = "Documento no existe"
                isLoading = false
            }
            .addOnFailureListener { e ->
                errorMessage = "Error al cargar datos: ${e.message}"
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Planta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                errorMessage != null -> Text(errorMessage ?: "Error desconocido", color = MaterialTheme.colorScheme.error)
                plant != null -> PlantDetailContent(plant!!, climateData, onAdd, onEdit, onDelete)
            }
        }
    }
}

@Composable
fun PlantDetailContent(
    plant: Plant,
    datosClima: Map<String, String>?,
    onAdd: () -> Unit,
    onEdit: (Plant) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(plant.nombre, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        AsyncImage(
            model = plant.imagen,
            contentDescription = "Imagen planta",
            modifier = Modifier
                .height(220.dp)
                .fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text("Nombre científico: ${plant.nombreCientifico ?: "No disponible"}")
        Text("Coordenadas: ${plant.lat ?: "?"}, ${plant.lon ?: "?"}")
        Text("Fecha de creación: ${plant.createdAt?.let {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    .parse(it)?.toLocaleString()
            } catch (e: Exception) {
                "No disponible"
            }
        } ?: "No disponible"}")

        Spacer(Modifier.height(16.dp))

        if (datosClima != null) {
            Card(Modifier.fillMaxWidth().padding(8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Clima actual:", fontWeight = FontWeight.Bold)
                    Text("Temperatura: ${datosClima["temperature"]} °C")
                    Text("Humedad: ${datosClima["humidity"]} %")
                    Text("Precipitación: ${datosClima["precipitation"]} mm")
                }
            }
        } else {
            Text("No hay datos climáticos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onAdd) { Text("Añadir Planta") }
            Button(onClick = { onEdit(plant) }) { Text("Editar") }
            Button(onClick = {
                onDelete(plant.id ?: "")
                Toast.makeText(context, "Eliminando planta...", Toast.LENGTH_SHORT).show()
            }) { Text("Eliminar") }
        }
    }
}

private fun fetchClimateData(
    context: android.content.Context,
    plant: Plant,
    callback: (Map<String, String>?) -> Unit
) {
    val lat = plant.lat?.toDoubleOrNull()
    val lon = plant.lon?.toDoubleOrNull()
    if (lat == null || lon == null) return callback(null)

    RetrofitClient.openMeteoApi.getCurrentWeather(lat, lon)
        .enqueue(object : Callback<OpenMeteoResponse> {
            override fun onResponse(call: Call<OpenMeteoResponse>, response: Response<OpenMeteoResponse>) {
                when (response.code()) {
                    200 -> {
                        val current = response.body()?.current
                        if (current != null) {
                            callback(
                                mapOf(
                                    "temperature" to current.temperature_2m.toString(),
                                    "humidity" to current.relative_humidity_2m.toString(),
                                    "precipitation" to current.precipitation.toString()
                                )
                            )
                        } else callback(null)
                    }
                    400 -> Toast.makeText(context, "Error 400: Solicitud incorrecta", Toast.LENGTH_SHORT).show()
                    401 -> Toast.makeText(context, "Error 401: No autorizado", Toast.LENGTH_SHORT).show()
                    404 -> Toast.makeText(context, "Error 404: No encontrado", Toast.LENGTH_SHORT).show()
                    500 -> Toast.makeText(context, "Error 500: Servidor", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, "Error HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<OpenMeteoResponse>, t: Throwable) {
                Toast.makeText(context, "Error de conexión: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
                callback(null)
            }
        })
}
