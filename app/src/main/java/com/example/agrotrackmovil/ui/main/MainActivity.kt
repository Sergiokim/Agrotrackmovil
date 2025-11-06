package com.example.agrotrackmovil.ui.main

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import at.favre.lib.crypto.bcrypt.BCrypt
import com.example.agrotrackmovil.R
import com.example.agrotrackmovil.ui.auth.RegisterActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.example.agrotrackmovil.ui.dashboard.DashboardActivity

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("app_session", MODE_PRIVATE)

        // Verifica si ya hay un usuario logueado
        val savedUserId = prefs.getString("userId", null)
        if (savedUserId != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main) // Changed to activity_login

        // Referencias a los elementos del layout
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerTextView = findViewById<TextView>(R.id.registerTextView)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)

        // Configura el padding para los insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Acción del botón de login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                errorTextView.text = "Email y contraseña son requeridos"
                return@setOnClickListener
            }

            Log.d("FirebaseAuth", "Attempting login for email: $email")
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val userDoc = documents.documents[0]
                        val storedPassword = userDoc.getString("password")

                        if (storedPassword != null &&
                            BCrypt.verifyer().verify(password.toCharArray(), storedPassword).verified
                        ) {
                            // Guardar sesión local
                            prefs.edit().putString("userId", userDoc.id).apply()
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } else {
                            errorTextView.text = "Contraseña incorrecta"
                            Log.e("FirebaseAuth", "Login failed: Invalid password")
                        }
                    } else {
                        errorTextView.text = "Usuario no encontrado"
                        Log.e("FirebaseAuth", "Login failed: User not found")
                    }
                }
                .addOnFailureListener { exception ->
                    errorTextView.text = "Error al consultar: ${exception.message}"
                    Log.e("FirebaseAuth", "Login failed: ${exception.message}")
                }
        }

        // Acción para navegar al registro
        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}