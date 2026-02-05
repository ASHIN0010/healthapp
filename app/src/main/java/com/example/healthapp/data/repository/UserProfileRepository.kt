package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UserProfile(
    val userId: String = "",
    val role: String = "", // Hospital, Patient, ASHA, Pharmacy
    val name: String = "",
    val email: String = ""
)

class UserProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun saveUserProfile(userId: String, role: String, name: String = "", email: String = "") {
        val profile = UserProfile(userId, role, name, email)
        firestore.collection("user_profiles")
            .document(userId)
            .set(profile)
            .await()
    }

    suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val doc = firestore.collection("user_profiles")
                .document(userId)
                .get()
                .await()
            doc.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
