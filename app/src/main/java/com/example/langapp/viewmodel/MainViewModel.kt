package com.example.langapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.langapp.data.SessionDao
import com.example.langapp.data.SessionHistory
import com.example.langapp.data.Word
import com.example.langapp.data.WordDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val wordDao: WordDao,
    private val sessionDao: SessionDao
) : ViewModel() {

    val allWords = wordDao.getAllWords().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allSessions = sessionDao.getAllSessions().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addWord(en: String, fr: String) {
        viewModelScope.launch {
            wordDao.insertWord(Word(en = en, fr = fr))
        }
    }

    fun deleteWord(word: Word) {
        viewModelScope.launch {
            wordDao.deleteWord(word)
        }
    }

    fun saveSession(score: Int, total: Int) {
        viewModelScope.launch {
            sessionDao.insertSession(SessionHistory(score = score, total = total))
        }
    }

    suspend fun getQuizWords(): List<Word> {
        return wordDao.getRandomWords()
    }
}