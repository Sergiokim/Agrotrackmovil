package com.example.agrotrackmovil.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import com.google.firebase.firestore.FirebaseFirestore
import com.example.agrotrackmovil.ui.main.MainActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        db = FirebaseFirestore.getInstance()
        prefs = getSharedPreferences("app_session", MODE_PRIVATE)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        val loginTextView = findViewById<TextView>(R.id.loginTextView)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register_container)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fullText = "¿Ya tienes cuenta? Inicia sesión"
        val spannableString = SpannableString(fullText)
        val startIndex = fullText.indexOf("Inicia sesión")
        val endIndex = startIndex + "Inicia sesión".length
        spannableString.setSpan(StyleSpan(Typeface.BOLD), startIndex, endIndex, 0)
        spannableString.setSpan(ForegroundColorSpan(getColor(R.color.primary)), startIndex, endIndex, 0)
        loginTextView.text = spannableString

        loginTextView.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                errorTextView.text = "Todos los campos son requeridos"
                return@setOnClickListener
            }

            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        errorTextView.text = "El correo ya está registrado"
                        return@addOnSuccessListener
                    }

                    val passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())

                    val user = hashMapOf(
                        "email" to email,
                        "password" to passwordHash,
                        "name" to name
                    )

                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener { documentReference ->
                            val userId = documentReference.id
                            prefs.edit().putString("userId", userId).apply()
                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            errorTextView.text = "Error al registrar: ${exception.message}"
                        }
                }
                .addOnFailureListener { exception ->
                    errorTextView.text = "Error al verificar el correo: ${exception.message}"
                }
        }
    }
}