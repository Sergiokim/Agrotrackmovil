package com.example.agrotrackmovil.ui.plant.detail

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agrotrackmovil.R
import com.example.agrotrackmovil.ui.main.MainActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val emailTextView = findViewById<TextView>(R.id.emailTextView)
        val nameTextView = findViewById<TextView>(R.id.nameTextView)
        val logoutButton = findViewById<Button>(R.id.logoutButton)

        if (user != null) {
            emailTextView.text = "Email: ${user.email}"
            nameTextView.text = "Nombre: ${user.displayName ?: "No establecido"}"
        } else {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            finish()
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}