package com.example.langapp.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Word(
    var id: String = "",
    val en: String = "",
    val fr: String = ""
)

data class SessionHistory(
    var id: String = "",
    val score: Int = 0,
    val total: Int = 0,
    val date: Long = System.currentTimeMillis()
)

class FirebaseManager {
    private val db = FirebaseFirestore.getInstance()

    fun getAllWords(): Flow<List<Word>> = callbackFlow {
        val listener = db.collection("words")
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

    suspend fun getRandomWords(): List<Word> {
        val snapshot = db.collection("words").get().await()
        val allWords = snapshot.documents.mapNotNull { doc ->
            doc.toObject(Word::class.java)?.apply { id = doc.id }
        }
        return allWords.shuffled().take(10)
    }

    fun addWord(en: String, fr: String) {
        val word = hashMapOf("en" to en, "fr" to fr)
        db.collection("words").add(word)
    }

    fun deleteWord(wordId: String) {
        db.collection("words").document(wordId).delete()
    }

    fun getAllSessions(pseudo: String): Flow<List<SessionHistory>> = callbackFlow {
        if (pseudo.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }
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

    fun saveSession(pseudo: String, score: Int, total: Int) {
        if (pseudo.isBlank()) return
        val session = hashMapOf("score" to score, "total" to total, "date" to System.currentTimeMillis())
        db.collection("users").document(pseudo).collection("sessions").add(session)
    }
}