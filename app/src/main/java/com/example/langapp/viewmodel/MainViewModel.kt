package com.example.langapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langapp.data.FirebaseManager
import com.example.langapp.data.SessionHistory
import com.example.langapp.data.Word
import com.example.langapp.data.WordList
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(private val firebaseManager: FirebaseManager) : ViewModel() {

    private val _pseudo = MutableStateFlow("")
    val pseudo: StateFlow<String> = _pseudo

    @OptIn(ExperimentalCoroutinesApi::class)
    val allLists = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllWordLists(p).map { lists ->
            val favoris = WordList(id = "favorites", name = "Favoris", difficulty = 1)
            listOf(favoris) + lists
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allWords = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllWords(p)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessions = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllSessions(p)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setPseudo(newPseudo: String) {
        _pseudo.value = newPseudo
    }

    fun addWordList(name: String, difficulty: Int) {
        firebaseManager.addWordList(_pseudo.value, name, difficulty)
    }

    fun addWord(listId: String, en: String, fr: String) {
        firebaseManager.addWord(_pseudo.value, listId, en, fr)
    }

    fun deleteWord(word: Word) {
        firebaseManager.deleteWord(_pseudo.value, word.id)
    }

    fun toggleFavorite(word: Word) {
        firebaseManager.toggleFavorite(_pseudo.value, word.id, !word.isFavorite)
    }

    fun saveSession(listName: String, score: Int, total: Int, duration: Int) {
        firebaseManager.saveSession(_pseudo.value, listName, score, total, duration)
    }

    suspend fun getQuizWords(listId: String, limit: Int): List<Word> {
        return firebaseManager.getRandomWords(_pseudo.value, listId, limit)
    }

    fun updateWordList(listId: String, name: String, difficulty: Int) {
        firebaseManager.updateWordList(_pseudo.value, listId, name, difficulty)
    }

    fun deleteWordList(listId: String) {
        firebaseManager.deleteWordList(_pseudo.value, listId)
    }

    fun deleteSession(session: SessionHistory) {
        firebaseManager.deleteSession(_pseudo.value, session.id)
    }
}