package com.example.agrotrackmovil.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrotrackmovil.R
import com.example.agrotrackmovil.data.remote.ApiClient
import com.example.agrotrackmovil.data.remote.UnsplashResponse
import com.example.agrotrackmovil.domain.model.Plant
import com.example.agrotrackmovil.ui.list.PlantAdapter
import com.example.agrotrackmovil.ui.main.MainActivity
import com.example.agrotrackmovil.ui.plant.detail.PlantDetailActivity
import com.example.agrotrackmovil.ui.plant.edit.AddPlantActivity
import com.google.android.gms.wearable.Wearable
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var emptyTextView: TextView
    private lateinit var wearConnectButton: Button
    private lateinit var wearSyncButton: Button
    private val TAG = "DashboardActivity"
    private val WEAR_TAG = "WearOSFacade"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate iniciado")
        setContentView(R.layout.activity_dashboard)

        // Inicializar Firebase
        try {
            db = FirebaseFirestore.getInstance()
            Log.d(TAG, "FirebaseFirestore inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar Firestore: ${e.message}", e)
            Toast.makeText(this, "Error inicializando Firestore: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Inicializar vistas
        try {
            recyclerView = findViewById(R.id.plantsRecyclerView)
            progressBar = findViewById(R.id.progressBar)
            errorTextView = findViewById(R.id.errorTextView)
            emptyTextView = findViewById(R.id.emptyTextView)
            wearConnectButton = findViewById(R.id.wearConnectButton)
            wearSyncButton = findViewById(R.id.wearSyncButton)
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar vistas: ${e.message}", e)
            Toast.makeText(this, "Error al cargar la interfaz", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Validar vistas
        if (recyclerView == null || progressBar == null || errorTextView == null ||
            emptyTextView == null || wearConnectButton == null || wearSyncButton == null) {
            Log.e(TAG, "Uno o más elementos de la UI son nulos")
            Toast.makeText(this, "Error: Elementos de interfaz no encontrados", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Log.d(TAG, "Vistas inicializadas correctamente")

        // Configurar RecyclerView
        plantAdapter = PlantAdapter { plantId ->
            try {
                val intent = Intent(this, PlantDetailActivity::class.java).apply {
                    putExtra("PLANT_ID", plantId)
                }
                startActivity(intent)
                Log.d(TAG, "Navegado a PlantDetailActivity para planta ID: $plantId")
            } catch (e: Exception) {
                Log.e(TAG, "Error al navegar a PlantDetailActivity: ${e.message}", e)
                Toast.makeText(
                    this,
                    "Error al abrir detalle de planta: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = plantAdapter

        // Verificar sesión
        val prefs = getSharedPreferences("app_session", MODE_PRIVATE)
        val savedUserId = prefs.getString("userId", null)
        if (savedUserId == null) {
            Log.e(TAG, "No se encontró sesión de usuario")
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        Log.d(TAG, "Sesión de usuario verificada: $savedUserId")

        // Cargar plantas desde Firestore
        loadPlantsFromFirestore(savedUserId)

        // Botón de agregar planta
        val addPlantButton = findViewById<Button>(R.id.addPlantButton)
        addPlantButton?.setOnClickListener {
            try {
                startActivity(Intent(this, AddPlantActivity::class.java))
                Log.d(TAG, "Navegado a AddPlantActivity")
            } catch (e: Exception) {
                Log.e(TAG, "Error al navegar a AddPlantActivity: ${e.message}", e)
                Toast.makeText(this, "Error al abrir agregar planta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Botón de cerrar sesión
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        logoutButton?.setOnClickListener {
            try {
                prefs.edit().remove("userId").apply()
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Cierre de sesión completado")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar sesión: ${e.message}", e)
                Toast.makeText(this, "Error al cerrar sesión: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Botones de Wear OS
        setupWearOSButtons(savedUserId)

        // 🔹 NUEVO: Cargar fotos desde Unsplash con manejo HTTP
        loadUnsplashPhotos("plants")
    }

    private fun loadPlantsFromFirestore(savedUserId: String) {
        progressBar.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE
        emptyTextView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        db.collection("plantas")
            .whereEqualTo("userId", savedUserId)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    val plants = documents.map { it.toObject(Plant::class.java).apply { id = it.id } }
                    progressBar.visibility = View.GONE
                    if (plants.isEmpty()) {
                        emptyTextView.text = "El usuario no tiene plantas registradas"
                        emptyTextView.visibility = View.VISIBLE
                    } else {
                        plantAdapter.setPlants(plants)
                        recyclerView.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al mapear plantas: ${e.message}", e)
                    progressBar.visibility = View.GONE
                    errorTextView.text = "Error al cargar plantas: ${e.message}"
                    errorTextView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Consulta a Firestore fallida: ${exception.message}", exception)
                progressBar.visibility = View.GONE
                errorTextView.text = "Error al cargar plantas: ${exception.message}"
                errorTextView.visibility = View.VISIBLE
            }
    }

    private fun setupWearOSButtons(savedUserId: String?) {
        wearConnectButton.setOnClickListener {
            Toast.makeText(this, "Conectando a Wear OS...", Toast.LENGTH_SHORT).show()
            val nodeClient = Wearable.getNodeClient(this)
            nodeClient.connectedNodes.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isEmpty()) {
                    Toast.makeText(this, "Wear OS conectado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se encontraron dispositivos Wear OS", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al conectar con Wear OS", Toast.LENGTH_SHORT).show()
            }
        }

        wearSyncButton.setOnClickListener {
            if (savedUserId == null) {
                Toast.makeText(this, "Inicia sesión para sincronizar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Sincronizando datos con Wear OS...", Toast.LENGTH_SHORT).show()
            db.collection("plantas").whereEqualTo("userId", savedUserId).get()
                .addOnSuccessListener {
                    Toast.makeText(this, "Datos sincronizados con Wear OS", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al sincronizar con Wear OS", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadUnsplashPhotos(query: String) {
        val call = ApiClient.unsplashApiService.searchPhotos(ApiClient.getUnsplashApiKey(), query)

        call.enqueue(object : Callback<UnsplashResponse> {
            override fun onResponse(call: Call<UnsplashResponse>, response: Response<UnsplashResponse>) {
                when (response.code()) {
                    200 -> {
                        val photos = response.body()?.results
                        if (!photos.isNullOrEmpty()) {
                            Log.d("UnsplashAPI", "Fotos obtenidas: ${photos.size}")
                            Toast.makeText(this@DashboardActivity, "Fotos cargadas exitosamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.w("UnsplashAPI", "Respuesta 200 pero sin resultados")
                            Toast.makeText(this@DashboardActivity, "No se encontraron fotos", Toast.LENGTH_SHORT).show()
                        }
                    }
                    401 -> {
                        Log.e("UnsplashAPI", "Error 401: No autorizado")
                        Toast.makeText(this@DashboardActivity, "Error: No autorizado (401)", Toast.LENGTH_SHORT).show()
                    }
                    404 -> {
                        Log.e("UnsplashAPI", "Error 404: No encontrado")
                        Toast.makeText(this@DashboardActivity, "Error: No encontrado (404)", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.e("UnsplashAPI", "Error ${response.code()}: ${response.message()}")
                        Toast.makeText(this@DashboardActivity, "Error HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<UnsplashResponse>, t: Throwable) {
                Log.e("UnsplashAPI", "Error de red o conexión: ${t.message}", t)
                Toast.makeText(this@DashboardActivity, "Error de conexión: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
