package com.example.langapp

import android.app.Application
import com.example.langapp.data.FirebaseManager

class LangApp : Application() {
    val firebaseManager: FirebaseManager by lazy { FirebaseManager() }
}