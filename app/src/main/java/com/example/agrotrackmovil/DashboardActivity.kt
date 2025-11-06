package com.example.agrotrackmovil

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
import com.google.android.gms.wearable.Wearable
import com.google.firebase.firestore.FirebaseFirestore

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
                Toast.makeText(this, "Error al abrir detalle de planta: ${e.message}", Toast.LENGTH_SHORT).show()
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
        progressBar.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE
        emptyTextView.visibility = View.GONE
        recyclerView.visibility = View.GONE

        db.collection("plantas")
            .whereEqualTo("userId", savedUserId)
            .get()
            .addOnSuccessListener { documents ->
                try {
                    documents.forEach { doc ->
                        Log.d(TAG, "Datos del documento: ${doc.data}")
                    }
                    val plants = documents.map { it.toObject(Plant::class.java).apply { id = it.id } }
                    Log.d(TAG, "Plantas cargadas: ${plants.size}")
                    progressBar.visibility = View.GONE
                    if (plants.isEmpty()) {
                        emptyTextView.text = "El usuario no tiene plantas registradas"
                        emptyTextView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        plantAdapter.setPlants(plants)
                        recyclerView.visibility = View.VISIBLE
                        emptyTextView.visibility = View.GONE
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

        // Simular conexión con Wear OS
        wearConnectButton.setOnClickListener {
            Log.d(WEAR_TAG, "Botón de conexión a Wear OS clicado")
            Toast.makeText(this, "Conectando a Wear OS...", Toast.LENGTH_SHORT).show()
            try {
                val nodeClient = Wearable.getNodeClient(this)
                nodeClient.connectedNodes.addOnCompleteListener { task ->
                    if (task.isSuccessful && !task.result.isEmpty()) {
                        Log.d(WEAR_TAG, "Nodos Wear OS encontrados: ${task.result.size}")
                        Toast.makeText(this, "Wear OS conectado", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(WEAR_TAG, "No se encontraron nodos Wear OS")
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.e(WEAR_TAG, "Conexión con Wear OS fallida: Error interno")
                            Toast.makeText(this, "No fue posible conectar a Wear OS debido a un error interno", Toast.LENGTH_LONG).show()
                        }, 2000)
                    }
                }.addOnFailureListener { e ->
                    Log.e(WEAR_TAG, "Error al intentar conectar con Wear OS: ${e.message}", e)
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.e(WEAR_TAG, "Conexión con Wear OS fallida: Error interno")
                        Toast.makeText(this, "No fue posible conectar a Wear OS debido a un error interno", Toast.LENGTH_LONG).show()
                    }, 2000)
                }
            } catch (e: Exception) {
                Log.e(WEAR_TAG, "Error inesperado al intentar conectar con Wear OS: ${e.message}", e)
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.e(WEAR_TAG, "Conexión con Wear OS fallida: Error interno")
                    Toast.makeText(this, "No fue posible conectar a Wear OS debido a un error interno", Toast.LENGTH_LONG).show()
                }, 2000)
            }
        }

        // Simular sincronización de datos con Wear OS
        wearSyncButton.setOnClickListener {
            Log.d(WEAR_TAG, "Botón de sincronización con Wear OS clicado")
            if (savedUserId == null) {
                Log.e(WEAR_TAG, "Sincronización con Wear OS fallida: No se encontró sesión de usuario")
                Toast.makeText(this, "Error: Inicia sesión para sincronizar con Wear OS", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Sincronizando datos con Wear OS...", Toast.LENGTH_SHORT).show()
            try {
                db.collection("plantas").whereEqualTo("userId", savedUserId).get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            Log.d(WEAR_TAG, "Sincronización simulada con Wear OS: Datos de plantas obtenidos para userId=$savedUserId")
                            Toast.makeText(this, "Datos de plantas sincronizados con Wear OS", Toast.LENGTH_SHORT).show()
                        } else {
                            Log.e(WEAR_TAG, "Sincronización simulada con Wear OS fallida: No se encontraron plantas")
                            Handler(Looper.getMainLooper()).postDelayed({
                                Log.e(WEAR_TAG, "Sincronización con Wear OS fallida: Error interno")
                                Toast.makeText(this, "No fue posible sincronizar con Wear OS debido a un error interno", Toast.LENGTH_LONG).show()
                            }, 1500)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(WEAR_TAG, "Error al intentar sincronizar con Wear OS: ${e.message}", e)
                        Handler(Looper.getMainLooper()).postDelayed({
                            Log.e(WEAR_TAG, "Sincronización con Wear OS fallida: Error interno")
                            Toast.makeText(this, "No fue posible sincronizar con Wear OS debido a un error interno", Toast.LENGTH_LONG).show()
                        }, 1500)
                    }
            } catch (e: Exception) {
                Log.e(WEAR_TAG, "Error inesperado en sincronización simulada: ${e.message}", e)
                Handler(Looper.getMainLooper()).postDelayed({
                    Log.e(WEAR_TAG, "Sincronización con Wear OS fallida: Error interno")
                    Toast.makeText(this, "No fue posible sincronizar con Wear OS debido a un error interno", Toast.LENGTH_LONG).show()
                }, 1500)
            }
        }
    }
}