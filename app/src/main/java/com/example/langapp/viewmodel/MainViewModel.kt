package com.example.langapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langapp.data.FirebaseManager
import com.example.langapp.data.SessionHistory
import com.example.langapp.data.Word
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class MainViewModel(private val firebaseManager: FirebaseManager) : ViewModel() {

    private val _pseudo = MutableStateFlow("")
    val pseudo: StateFlow<String> = _pseudo

    val allWords = firebaseManager.getAllWords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessions = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllSessions(p)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setPseudo(newPseudo: String) {
        _pseudo.value = newPseudo
    }

    fun addWord(en: String, fr: String) {
        firebaseManager.addWord(en, fr)
    }

    fun deleteWord(word: Word) {
        firebaseManager.deleteWord(word.id)
    }

    fun saveSession(score: Int, total: Int) {
        firebaseManager.saveSession(_pseudo.value, score, total)
    }

    suspend fun getQuizWords(): List<Word> {
        return firebaseManager.getRandomWords()
    }
}