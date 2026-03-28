package com.example.langapp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.PropertyName

data class Word(
    var id: String = "",
    var listId: String = "",
    val en: String = "",
    val fr: String = "",
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

class FirebaseManager {
    private val db = FirebaseFirestore.getInstance()

    fun getAllWordLists(pseudo: String): Flow<List<WordList>> {
        if (pseudo.isBlank()) return flowOf(emptyList())

        return callbackFlow {
            val listener = db.collection("users").document(pseudo).collection("wordLists")
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val lists = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(WordList::class.java)?.apply { id = doc.id }
                        }
                        trySend(lists)
                    }
                }
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

    suspend fun getRandomWords(pseudo: String, listId: String?, limit: Int): List<Word> {
        if (pseudo.isBlank()) return emptyList()
        val baseQuery = db.collection("users").document(pseudo).collection("words")
        val query = when (listId) {
            null, "all" -> baseQuery
            "favorites" -> baseQuery.whereEqualTo("isFavorite", true)
            else -> baseQuery.whereEqualTo("listId", listId)
        }

        val snapshot = query.get().await()
        val allWords = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Word::class.java)?.apply { id = doc.id }
        }
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
        db.collection("users").document(pseudo).collection("wordLists").document(listId).delete()

        db.collection("users").document(pseudo).collection("words").whereEqualTo("listId", listId).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit()
            }
    }
}