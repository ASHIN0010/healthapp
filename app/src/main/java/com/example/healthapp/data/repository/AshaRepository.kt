package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.Date

data class VillageCase(
    val id: String = "",
    val villagerName: String = "",
    val age: Int = 0,
    val gender: String = "",
    val symptoms: String = "",
    val vitals: Vitals = Vitals(),
    val priority: String = "Green", // Green, Yellow, Red
    val status: String = "Pending", // Pending, Visited, Referred, Completed
    val assignedDoctorId: String = "",
    val assignedDoctorName: String = "",
    val visitNotes: String = "",
    val timestamp: Date = Date(),
    val ashaId: String = ""
)

data class Vitals(
    val bp: String = "",
    val pulse: String = "",
    val temperature: String = "",
    val spo2: String = ""
)

data class AshaWorker(
    val id: String = "",
    val name: String = "",
    val assignedVillage: String = "Rural Zone A",
    val activeCases: Int = 0
)

class AshaRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun registerCase(case: VillageCase) {
        val ref = firestore.collection("village_cases").document()
        val newCase = case.copy(id = ref.id)
        ref.set(newCase).await()
        
        // If Red priority, also trigger global emergency
        if (case.priority == "Red") {
            val emergency = hashMapOf(
                "priority" to "Critical (ASHA)",
                "explanation" to "Reported by ASHA: ${case.villagerName}",
                "recommendedAction" to "Immediate Ambulance Dispatch",
                "patientAge" to case.age,
                "patientGender" to case.gender,
                "symptoms" to "${case.symptoms} | Vitals: BP=${case.vitals.bp}, SpO2=${case.vitals.spo2}",
                "timestamp" to case.timestamp
            )
            firestore.collection("triage_logs").add(emergency).await()
        }
    }

    fun getAssignedCases(ashaId: String): Flow<List<VillageCase>> = callbackFlow {
        val listener = firestore.collection("village_cases")
            //.whereEqualTo("ashaId", ashaId) // Comment out for now to see all cases for demo
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val cases = it.toObjects(VillageCase::class.java).sortedByDescending { c -> c.timestamp }
                    trySend(cases)
                }
            }
        awaitClose { listener.remove() }
    }
    
    fun getAllAshaWorkers(): Flow<List<AshaWorker>> = callbackFlow {
        // Mock seeding for demo purposes if list is empty
        val listener = firestore.collection("users")
            .whereEqualTo("role", "ASHA")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                     // Fallback to mock if no users found or error
                    trySend(listOf(
                        AshaWorker("asha1", "Geeta Devi", "Pipri Village", 5),
                        AshaWorker("asha2", "Sunita Bai", "Rampur Village", 3)
                    ))
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val workers = it.toObjects(AshaWorker::class.java)
                    if (workers.isEmpty()) {
                         trySend(listOf(
                            AshaWorker("asha1", "Geeta Devi", "Pipri Village", 5),
                            AshaWorker("asha2", "Sunita Bai", "Rampur Village", 3)
                        ))
                    } else {
                        trySend(workers)
                    }
                }
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun updateCaseStatus(caseId: String, status: String, notes: String) {
        firestore.collection("village_cases").document(caseId)
            .update(mapOf(
                "status" to status,
                "visitNotes" to notes
            )).await()
    }
}
