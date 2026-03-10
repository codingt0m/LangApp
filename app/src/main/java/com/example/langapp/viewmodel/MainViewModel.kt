package com.example.langapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langapp.data.FirebaseManager
import com.example.langapp.data.SessionHistory
import com.example.langapp.data.Word
import com.example.langapp.data.WordList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
class MainViewModel(private val firebaseManager: FirebaseManager) : ViewModel() {

    private val _pseudo = MutableStateFlow("")
    val pseudo: StateFlow<String> = _pseudo

    val allLists = firebaseManager.getAllWordLists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allWords = firebaseManager.getAllWords()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessions = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllSessions(p)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setPseudo(newPseudo: String) {
        _pseudo.value = newPseudo
    }

    fun addWordList(name: String, difficulty: Int) {
        firebaseManager.addWordList(name, difficulty)
    }

    fun addWord(listId: String, en: String, fr: String) {
        firebaseManager.addWord(listId, en, fr)
    }

    fun deleteWord(word: Word) {
        firebaseManager.deleteWord(word.id)
    }

    fun saveSession(listName: String, score: Int, total: Int) {
        firebaseManager.saveSession(_pseudo.value, listName, score, total)
    }

    suspend fun getQuizWords(listId: String, limit: Int): List<Word> {
        return firebaseManager.getRandomWords(listId, limit)
    }
    fun updateWordList(listId: String, name: String, difficulty: Int) {
        firebaseManager.updateWordList(listId, name, difficulty)
    }

    fun deleteWordList(listId: String) {
        firebaseManager.deleteWordList(listId)
    }

}