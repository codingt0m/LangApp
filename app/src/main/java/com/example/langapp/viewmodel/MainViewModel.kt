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

    fun populateTestData() {
        val db = FirebaseFirestore.getInstance()
        val pseudo = "test"

        val categories = mapOf(
            "Les couleurs" to listOf(
                "Red" to "Rouge", "Blue" to "Bleu", "Green" to "Vert", "Yellow" to "Jaune",
                "Black" to "Noir", "White" to "Blanc", "Orange" to "Orange", "Purple" to "Violet",
                "Pink" to "Rose", "Brown" to "Marron"
            ),
            "Les animaux" to listOf(
                "Dog" to "Chien", "Cat" to "Chat", "Bird" to "Oiseau", "Fish" to "Poisson",
                "Horse" to "Cheval", "Cow" to "Vache", "Pig" to "Cochon", "Sheep" to "Mouton",
                "Mouse" to "Souris", "Elephant" to "Éléphant"
            ),
            "Le vocabulaire de l'école" to listOf(
                "Pen" to "Stylo", "Pencil" to "Crayon", "Book" to "Livre", "Notebook" to "Cahier",
                "Desk" to "Bureau", "Chair" to "Chaise", "Teacher" to "Professeur", "Student" to "Élève",
                "Board" to "Tableau", "Eraser" to "Gomme"
            ),
            "Le vocabulaire des vacances" to listOf(
                "Beach" to "Plage", "Sun" to "Soleil", "Sea" to "Mer", "Sand" to "Sable",
                "Hotel" to "Hôtel", "Plane" to "Avion", "Train" to "Train", "Suitcase" to "Valise",
                "Ticket" to "Billet", "Map" to "Carte"
            )
        )

        categories.forEach { (listName, words) ->
            val listData = hashMapOf("name" to listName, "difficulty" to 1)
            db.collection("users").document(pseudo).collection("wordLists").add(listData)
                .addOnSuccessListener { documentReference ->
                    Log.d("FirebaseTest", "Liste $listName créée avec succès")
                    val listId = documentReference.id
                    words.forEach { (en, fr) ->
                        val wordData = hashMapOf(
                            "listId" to listId,
                            "en" to en,
                            "fr" to fr,
                            "isFavorite" to false
                        )
                        db.collection("users").document(pseudo).collection("words").add(wordData)
                            .addOnFailureListener { e ->
                                Log.e("FirebaseTest", "Erreur lors de l'ajout du mot $en", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirebaseTest", "Erreur lors de la création de la liste $listName", e)
                }
        }
    }
}