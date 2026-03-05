package com.example.langapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.langapp.data.SessionDao
import com.example.langapp.data.WordDao

class ViewModelFactory(
    private val wordDao: WordDao,
    private val sessionDao: SessionDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(wordDao, sessionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}