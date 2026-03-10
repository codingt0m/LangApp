package com.example.langapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.langapp.data.FirebaseManager

class ViewModelFactory(
    private val firebaseManager: FirebaseManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(firebaseManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}