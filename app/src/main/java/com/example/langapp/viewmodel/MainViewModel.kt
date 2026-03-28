// Emplacement : codingt0m/langapp/LangApp-ffae75213ec4e325161e96d7412b21eb86381be5/app/src/main/java/com/example/langapp/viewmodel/MainViewModel.kt

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

// Le ViewModel sert à séparer la logique de gestion des données de l'interface utilisateur.
// Il a l'avantage de survivre aux changements de configuration (comme la rotation de l'écran).
class MainViewModel(private val firebaseManager: FirebaseManager) : ViewModel() {

    // Utilisation de StateFlow pour conserver et observer l'état actuel du pseudo.
    // L'encapsulation stricte est respectée : _pseudo (Mutable) est privé pour empêcher les modifications externes,
    // tandis que 'pseudo' expose publiquement une version en lecture seule.
    private val _pseudo = MutableStateFlow("")
    val pseudo: StateFlow<String> = _pseudo

    // flatMapLatest écoute les changements du flux source (_pseudo).
    // Si l'utilisateur change de pseudo, la requête Firebase précédente est immédiatement annulée pour lancer la nouvelle.
    @OptIn(ExperimentalCoroutinesApi::class)
    val allLists = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllWordLists(p).map { lists ->
            // Injection d'une liste virtuelle "Favoris" dans le flux de données pour centraliser cette fonctionnalité
            // de manière transparente pour l'interface graphique.
            val favoris = WordList(id = "favorites", name = "Favoris", difficulty = 1)
            listOf(favoris) + lists
        }
        // stateIn convertit le Flow "froid" en StateFlow "chaud".
        // SharingStarted.Lazily permet d'économiser les ressources en ne démarrant l'observation que si la vue est active.
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allWords = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllWords(p)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSessions = _pseudo.flatMapLatest { p ->
        firebaseManager.getAllSessions(p)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Point d'entrée sécurisé pour modifier l'état interne du pseudo
    fun setPseudo(newPseudo: String) {
        _pseudo.value = newPseudo
    }

    // Délégation systématique des opérations CRUD (Create, Read, Update, Delete) à FirebaseManager.
    // Cela permet de découpler la logique métier de l'implémentation spécifique de la base de données.
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

    // Fonction marquée avec le mot-clé "suspend" indiquant une opération asynchrone (potentiellement longue).
    // Elle ne bloquera pas le thread principal et devra être appelée depuis un contexte de coroutine.
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