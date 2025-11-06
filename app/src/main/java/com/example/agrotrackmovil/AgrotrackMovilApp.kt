package com.example.agrotrackmovil

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AgrotrackMovilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
            Log.d("FirebaseInit", "Firebase initialized successfully")
        } else {
            Log.d("FirebaseInit", "Firebase already initialized")
        }
    }
}