package com.example.agrotrackmovil.ui.plant.edit

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.agrotrackmovil.ui.dashboard.DashboardActivity
import com.example.agrotrackmovil.ui.main.MainActivity
import com.example.agrotrackmovil.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import com.example.agrotrackmovil.data.remote.ApiClient
import com.example.agrotrackmovil.data.remote.UnsplashResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddPlantActivity : AppCompatActivity() {

    private lateinit var nombreInput: EditText
    private lateinit var nombreCientificoInput: EditText
    private lateinit var latInput: EditText
    private lateinit var lonInput: EditText
    private lateinit var cuidadosInput: EditText
    private lateinit var errorMessage: TextView
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var prefs: SharedPreferences
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_plant)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            nombreInput = findViewById(R.id.nombre_input)
            nombreCientificoInput = findViewById(R.id.nombre_cientifico_input)
            latInput = findViewById(R.id.lat_input)
            lonInput = findViewById(R.id.lon_input)
            errorMessage = findViewById(R.id.error_message)
            submitButton = findViewById(R.id.submit_button)
            cancelButton = findViewById(R.id.cancel_button)
            loadingSpinner = findViewById(R.id.loading_spinner)
        } catch (e: Exception) {
            Log.e("AddPlantActivity", "Error inicializando vistas: ${e.message}", e)
            Toast.makeText(this, "Error al cargar la interfaz", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        prefs = getSharedPreferences("app_session", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val savedUserId = prefs.getString("userId", null)
        if (savedUserId == null) {
            Log.e("AddPlantActivity", "No se encontró sesión de usuario")
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        getUserLocation()

        submitButton.setOnClickListener { handleSubmit(savedUserId) }
        cancelButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            Log.d("AddPlantActivity", "Navigated to DashboardActivity")
            finish()
        }
    }

    private fun getUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fetchLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation()
            } else {
                showError("Permiso de ubicación denegado. Ingrese las coordenadas manualmente.")
            }
        }
    }

    private fun fetchLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    latInput.setText(location.latitude.toString())
                    lonInput.setText(location.longitude.toString())
                } else {
                    showError("No se pudo obtener la ubicación. Ingrese las coordenadas manualmente.")
                }
            }.addOnFailureListener { e ->
                Log.e("AddPlantActivity", "Error al obtener ubicación: ${e.message}")
                showError("No se pudo obtener la ubicación: ${e.message}")
            }
        } catch (e: SecurityException) {
            Log.e("AddPlantActivity", "Error de seguridad al obtener ubicación: ${e.message}")
            showError("Permiso de ubicación requerido.")
        }
    }

    private fun handleSubmit(userId: String) {
        val nombre = nombreInput.text.toString().trim()
        val nombreCientifico = nombreCientificoInput.text.toString().trim()
        val lat = latInput.text.toString().trim()
        val lon = lonInput.text.toString().trim()

        if (nombre.isEmpty()) {
            showError("El nombre es obligatorio")
            return
        }

        val latNum = lat.toDoubleOrNull()
        val lonNum = lon.toDoubleOrNull()
        if (latNum == null || lonNum == null) {
            showError("Latitud y Longitud deben ser números válidos")
            return
        }

        setLoading(true)

        searchPlantImage(nombre) { imageUrl ->

            val plantData = hashMapOf(
                "nombre" to nombre,
                "nombreCientifico" to (nombreCientifico.takeIf { it.isNotEmpty() } ?: null),
                "lat" to lat,
                "lon" to lon,
                "imagen" to (imageUrl ?: null),
                "userId" to userId,
                "createdAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(
                    Date()
                )
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("plantas").add(plantData)
                .addOnSuccessListener {
                    Log.d("AddPlantActivity", "Planta guardada con éxito para userId: $userId")
                    Toast.makeText(this, "Planta guardada con éxito", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("AddPlantActivity", "Error al guardar planta: ${e.message}")
                    showError("Error al guardar la planta: ${e.message}")
                    setLoading(false)
                }
        }
    }

    private fun searchPlantImage(query: String, callback: (String?) -> Unit) {
        val call = ApiClient.unsplashApiService.searchPhotos(
            clientId = ApiClient.getUnsplashApiKey(),
            query = query
        )
        call.enqueue(object : Callback<UnsplashResponse> {
            override fun onResponse(call: Call<UnsplashResponse>, response: Response<UnsplashResponse>) {
                if (response.isSuccessful) {
                    val photos = response.body()?.results
                    if (!photos.isNullOrEmpty()) {
                        val imageUrl = photos[0].urls.regular
                        callback(imageUrl)
                    } else {
                        Log.w("AddPlantActivity", "No se encontraron imágenes para: $query")
                        callback(null)
                    }
                } else {
                    Log.e("AddPlantActivity", "Error en la respuesta de Unsplash: ${response.code()}")
                    callback(null)
                }
            }

            override fun onFailure(call: Call<UnsplashResponse>, t: Throwable) {
                Log.e("AddPlantActivity", "Error al buscar imagen: ${t.message}")
                callback(null)
                showError("Error al buscar imagen: ${t.message}")
                setLoading(false)
            }
        })
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setLoading(isLoading: Boolean) {
        loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        submitButton.isEnabled = !isLoading
        cancelButton.isEnabled = !isLoading
    }
}