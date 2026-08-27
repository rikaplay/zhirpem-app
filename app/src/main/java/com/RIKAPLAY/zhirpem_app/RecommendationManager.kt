package com.RIKAPLAY.zhirpem_app

import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

object RecommendationManager {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Отслеживает взаимодействие пользователя с контентом.
     * interactionWeight: 2 для лайка, 1 для просмотра, 3 для репоста/сохранения.
     */
    fun trackInteraction(username: String, tags: List<String>?, interactionWeight: Int) {
        if (username.isBlank() || tags.isNullOrEmpty()) return

        val userInterestsRef = db.collection("user_interests").document(username)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userInterestsRef)
            val data = snapshot.data ?: emptyMap<String, Any>()
            val currentInterests = data["scores"] as? Map<String, Long> ?: emptyMap()
            val newInterests = currentInterests.toMutableMap()

            tags.forEach { tag ->
                val currentScore = newInterests[tag] ?: 0L
                newInterests[tag] = currentScore + interactionWeight
            }
            
            // Ограничиваем количество тегов, чтобы документ не раздувался
            val sortedInterests = newInterests.toList()
                .sortedByDescending { it.second }
                .take(50) // Оставляем только топ-50 интересов
                .toMap()

            transaction.set(userInterestsRef, mapOf("scores" to sortedInterests))
        }.addOnFailureListener { e ->
            Log.e("RecommendationManager", "Error updating interests: ${e.message}")
        }
    }
}
