package com.example.langapp

import android.app.Application
import com.example.langapp.data.AppDatabase

class LangApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}