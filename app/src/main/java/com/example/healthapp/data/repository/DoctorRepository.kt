package com.example.healthapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class DoctorProfile(
    val id: String = "",
    val name: String = "",
    val specialization: String = "General Physician",
    val isAvailable: Boolean = false,
    val experienceYears: Int = 0,
    val opdSchedule: String = "Mon-Fri: 9AM-1PM",
    val isEmergencyOnCall: Boolean = false
)

class DoctorRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    // Pre-populate specialists for the Rural Hospital MVP
    // initializeSpecialists removed to ensure data persistence

    suspend fun updateSpecialistStatus(id: String, isAvailable: Boolean) {
        try {
            firestore.collection("doctors").document(id)
                .update("isAvailable", isAvailable)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addDoctor(doctor: DoctorProfile) {
        val ref = firestore.collection("doctors").document()
        val newDoctor = doctor.copy(id = ref.id)
        ref.set(newDoctor).await()
    }


    fun getAllSpecialists(): Flow<List<DoctorProfile>> = kotlinx.coroutines.flow.callbackFlow {
        // initializeSpecialists() // REMOVED: Rely on seeding logic to prevent overwriting status
        
        val listener = firestore.collection("doctors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val allDoctors = it.toObjects(DoctorProfile::class.java)
                    // FILTER: Show all valid doctors
                    val validDoctors = allDoctors.filter { doc -> doc.name.isNotBlank() }
                    trySend(validDoctors)
                }
            }
        awaitClose { listener.remove() }
    }

    fun getAvailableDoctors(): Flow<List<DoctorProfile>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = firestore.collection("doctors")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val allDoctors = it.toObjects(DoctorProfile::class.java)
                    // FILTER: Show all valid doctors
                    val validDoctors = allDoctors.filter { doc -> doc.name.isNotBlank() }
                    trySend(validDoctors)
                }
            }
        awaitClose { listener.remove() }
    }
}
