package com.example.agrotrackmovil

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val errorTextView = findViewById<TextView>(R.id.errorTextView)
        val backTextView = findViewById<TextView>(R.id.backTextView)

        resetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                errorTextView.text = "Por favor, ingresa tu email"
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Email de recuperación enviado", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        errorTextView.text = "Error: ${task.exception?.message}"
                    }
                }
        }

        backTextView.setOnClickListener {
            finish()
        }
    }
}