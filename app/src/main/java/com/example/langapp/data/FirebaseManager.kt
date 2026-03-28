package com.example.langapp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.PropertyName

// Utilisation des data classes Kotlin pour faciliter la sérialisation et désérialisation automatique
// avec les documents Firestore grâce à la méthode toObject().
data class Word(
    var id: String = "",
    var listId: String = "",
    val en: String = "",
    val fr: String = "",
    // Les annotations PropertyName sont nécessaires ici car Firestore gère parfois mal
    // le préfixe "is" des variables booléennes lors du mapping automatique.
    @get:PropertyName("isFavorite")
    @set:PropertyName("isFavorite")
    var isFavorite: Boolean = false
)

data class WordList(
    var id: String = "",
    val name: String = "",
    val difficulty: Int = 1
)

data class SessionHistory(
    var id: String = "",
    val listName: String = "",
    val score: Int = 0,
    val total: Int = 0,
    val duration: Int = 0,
    val date: Long = System.currentTimeMillis()
)

// Classe centralisant l'accès aux données. Ce pattern Repository permet de découpler
// la logique de l'application (ViewModel) de l'implémentation spécifique de Firebase.
class FirebaseManager {
    // Initialisation du client Firestore en tant que singleton
    private val db = FirebaseFirestore.getInstance()

    // Retourne un Flow pour permettre à l'interface de réagir en temps réel aux modifications de la base de données.
    fun getAllWordLists(pseudo: String): Flow<List<WordList>> {
        if (pseudo.isBlank()) return flowOf(emptyList())

        // Utilisation de callbackFlow pour convertir l'API basée sur des callbacks de Firebase
        // en un flux de données Kotlin Coroutines (Flow).
        return callbackFlow {
            // Structuration des données par sous-collections liées à un pseudo pour isoler les données de chaque utilisateur.
            val listener = db.collection("users").document(pseudo).collection("wordLists")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        // mapNotNull permet d'ignorer les documents qui ne pourraient pas être convertis correctement.
                        val lists = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(WordList::class.java)?.apply { id = doc.id }
                        }
                        trySend(lists)
                    }
                }
            // awaitClose est crucial : il garantit que le listener Firebase est supprimé
            // dès que le Flow n'est plus collecté, évitant ainsi les fuites de mémoire.
            awaitClose { listener.remove() }
        }
    }

    fun addWordList(pseudo: String, name: String, difficulty: Int) {
        if (pseudo.isBlank()) return
        val list = hashMapOf("name" to name, "difficulty" to difficulty)
        db.collection("users").document(pseudo).collection("wordLists").add(list)
    }

    fun getAllWords(pseudo: String): Flow<List<Word>> {
        if (pseudo.isBlank()) return flowOf(emptyList())

        return callbackFlow {
            val listener = db.collection("users").document(pseudo).collection("words")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val words = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Word::class.java)?.apply { id = doc.id }
                        }
                        trySend(words)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    // L'utilisation du mot-clé suspend indique une opération asynchrone "one-shot" (une seule requête)
    // plutôt qu'une écoute continue.
    suspend fun getRandomWords(pseudo: String, listId: String?, limit: Int): List<Word> {
        if (pseudo.isBlank()) return emptyList()
        val baseQuery = db.collection("users").document(pseudo).collection("words")

        // Construction dynamique de la requête en fonction des filtres sélectionnés
        val query = when (listId) {
            null, "all" -> baseQuery
            "favorites" -> baseQuery.whereEqualTo("isFavorite", true)
            else -> baseQuery.whereEqualTo("listId", listId)
        }

        // await() suspend l'exécution de la coroutine jusqu'à ce que la requête réseau soit terminée
        val snapshot = query.get().await()
        val allWords = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Word::class.java)?.apply { id = doc.id }
        }
        // Le mélange (shuffled) et la limitation (take) sont effectués côté client car
        // Firestore ne propose pas de fonction native performante pour obtenir des documents aléatoires.
        return allWords.shuffled().take(limit)
    }

    fun addWord(pseudo: String, listId: String, en: String, fr: String) {
        if (pseudo.isBlank()) return
        val word = hashMapOf("listId" to listId, "en" to en, "fr" to fr, "isFavorite" to false)
        db.collection("users").document(pseudo).collection("words").add(word)
    }

    fun deleteWord(pseudo: String, wordId: String) {
        if (pseudo.isBlank()) return
        db.collection("users").document(pseudo).collection("words").document(wordId).delete()
    }

    // Mise à jour partielle du document (uniquement le champ isFavorite) pour limiter la consommation de bande passante.
    fun toggleFavorite(pseudo: String, wordId: String, isFavorite: Boolean) {
        if (pseudo.isBlank()) return
        db.collection("users").document(pseudo).collection("words").document(wordId).update("isFavorite", isFavorite)
    }

    fun getAllSessions(pseudo: String): Flow<List<SessionHistory>> {
        if (pseudo.isBlank()) return flowOf(emptyList())

        return callbackFlow {
            val listener = db.collection("users").document(pseudo).collection("sessions")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val sessions = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(SessionHistory::class.java)?.apply { id = doc.id }
                        }
                        trySend(sessions)
                    }
                }
            awaitClose { listener.remove() }
        }
    }

    fun saveSession(pseudo: String, listName: String, score: Int, total: Int, duration: Int) {
        if (pseudo.isBlank()) return
        val session = hashMapOf(
            "listName" to listName,
            "score" to score,
            "total" to total,
            "duration" to duration,
            "date" to System.currentTimeMillis()
        )
        db.collection("users").document(pseudo).collection("sessions").add(session)
    }

    fun updateWordList(pseudo: String, listId: String, name: String, difficulty: Int) {
        if (pseudo.isBlank()) return
        db.collection("users").document(pseudo).collection("wordLists").document(listId)
            .update(mapOf("name" to name, "difficulty" to difficulty))
    }

    fun deleteSession(pseudo: String, sessionId: String) {
        if (pseudo.isBlank()) return
        db.collection("users").document(pseudo).collection("sessions").document(sessionId).delete()
    }

    fun deleteWordList(pseudo: String, listId: String) {
        if (pseudo.isBlank()) return

        // Suppression de l'entité liste
        db.collection("users").document(pseudo).collection("wordLists").document(listId).delete()

        // Récupération de tous les mots associés à cette liste pour les supprimer en cascade
        db.collection("users").document(pseudo).collection("words").whereEqualTo("listId", listId).get()
            .addOnSuccessListener { snapshot ->
                // Utilisation d'un batch pour exécuter toutes les suppressions de mots en une seule transaction réseau.
                // Cela garantit l'atomicité de l'opération et optimise les performances.
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
    }
}