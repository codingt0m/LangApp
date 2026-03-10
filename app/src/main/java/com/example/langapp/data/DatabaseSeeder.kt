package com.example.langapp.data

import com.google.firebase.firestore.FirebaseFirestore

object DatabaseSeeder {
    fun seedTestEnvironment() {
        val db = FirebaseFirestore.getInstance()
        val batch = db.batch()

        val lists = mapOf(
            "list_animals" to mapOf("name" to "Animaux", "difficulty" to 1),
            "list_colors" to mapOf("name" to "Couleurs", "difficulty" to 1),
            "list_city" to mapOf("name" to "Ville", "difficulty" to 2),
            "list_school" to mapOf("name" to "École", "difficulty" to 1)
        )

        lists.forEach { (id, data) ->
            val ref = db.collection("wordLists").document(id)
            batch.set(ref, data)
        }

        val words = mutableListOf<Map<String, Any>>()

        val animals = listOf(
            "dog" to "chien", "cat" to "chat", "bird" to "oiseau", "fish" to "poisson",
            "mouse" to "souris", "horse" to "cheval", "cow" to "vache", "pig" to "cochon",
            "sheep" to "mouton", "lion" to "lion", "tiger" to "tigre", "bear" to "ours",
            "elephant" to "éléphant", "monkey" to "singe", "snake" to "serpent",
            "rabbit" to "lapin", "duck" to "canard", "frog" to "grenouille",
            "spider" to "araignée", "turtle" to "tortue"
        )
        animals.forEach { words.add(mapOf("listId" to "list_animals", "en" to it.first, "fr" to it.second)) }

        val colors = listOf(
            "red" to "rouge", "blue" to "bleu", "green" to "vert", "yellow" to "jaune",
            "black" to "noir", "white" to "blanc", "brown" to "marron", "orange" to "orange",
            "pink" to "rose", "purple" to "violet", "gray" to "gris"
        )
        colors.forEach { words.add(mapOf("listId" to "list_colors", "en" to it.first, "fr" to it.second)) }

        val city = listOf(
            "street" to "rue", "car" to "voiture", "bus" to "bus", "building" to "bâtiment",
            "house" to "maison", "shop" to "magasin", "restaurant" to "restaurant",
            "park" to "parc", "hospital" to "hôpital", "police" to "police",
            "bank" to "banque", "bridge" to "pont", "road" to "route",
            "traffic" to "circulation", "station" to "gare", "airport" to "aéroport",
            "hotel" to "hôtel", "cinema" to "cinéma", "museum" to "musée", "subway" to "métro"
        )
        city.forEach { words.add(mapOf("listId" to "list_city", "en" to it.first, "fr" to it.second)) }

        val school = listOf(
            "pen" to "stylo", "pencil" to "crayon", "book" to "livre", "notebook" to "cahier",
            "desk" to "bureau", "chair" to "chaise", "board" to "tableau",
            "teacher" to "professeur", "student" to "élève", "class" to "classe",
            "lesson" to "leçon", "homework" to "devoirs", "exam" to "examen",
            "bag" to "sac", "paper" to "papier", "eraser" to "gomme",
            "ruler" to "règle", "scissors" to "ciseaux", "library" to "bibliothèque",
            "computer" to "ordinateur"
        )
        school.forEach { words.add(mapOf("listId" to "list_school", "en" to it.first, "fr" to it.second)) }

        words.forEach { wordData ->
            val ref = db.collection("words").document()
            batch.set(ref, wordData)
        }

        val currentTime = System.currentTimeMillis()
        val oneDay = 86400000L
        val sessions = listOf(
            mapOf("score" to 15, "total" to 20, "date" to currentTime - (oneDay * 2)),
            mapOf("score" to 18, "total" to 20, "date" to currentTime - oneDay),
            mapOf("score" to 20, "total" to 20, "date" to currentTime)
        )

        sessions.forEach { sessionData ->
            val ref = db.collection("users").document("tom").collection("sessions").document()
            batch.set(ref, sessionData)
        }

        batch.commit()
    }
}