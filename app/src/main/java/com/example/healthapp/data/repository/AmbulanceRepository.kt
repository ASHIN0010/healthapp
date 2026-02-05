package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import java.util.Date

data class Ambulance(
    val id: String = "",
    val driverName: String = "",
    val plateNumber: String = "",
    val status: String = "Available", // Available, Busy, Maintenance
    val currentLocation: String = "Hospital Base",
    val driverContact: String = "",
    val vehicleType: String = "Basic Support" // ICU, ALS, Basic Support
)

class AmbulanceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // Fleet Management
    fun getAmbulanceFleet(): Flow<List<Ambulance>> = flow {
         try {
             val snapshot = firestore.collection("ambulances")
                 .get()
                 .await()
             
             // Check if fleet needs initialization or update (Force 10 drivers)
             val needsUpdate = snapshot.size() != 10 || 
                               (snapshot.documents.isNotEmpty() && snapshot.documents[0].getString("driverContact").isNullOrBlank())

             if (needsUpdate) {
                 // Optimized: Direct Upsert (overwrites existing records with correct data)
                 // Removes overhead of deleting 10 docs then creating 10 docs.
                 initializeFleet()
                 emit(firestore.collection("ambulances").get().await().toObjects(Ambulance::class.java))
             } else {
                 emit(snapshot.toObjects(Ambulance::class.java))
             }
         } catch (e: Exception) {
             e.printStackTrace()
             emit(emptyList()) // Emit empty list on error to avoid crash
         }
    }

    suspend fun initializeFleet() {
        try {
            val fleet = listOf(
                Ambulance("amb_1", "Ramesh Kumar", "PB-11-A-1234", "Available", "Hospital Base", "+91 98765 43210", "ICU Express"),
                Ambulance("amb_2", "Suresh Singh", "PB-11-B-5678", "Busy", "Sector 17", "+91 98123 45678", "Basic Support"),
                Ambulance("amb_3", "Mahesh Yadav", "PB-11-C-9012", "Available", "Hospital Base", "+91 99887 76655", "Adv. Life Support"),
                Ambulance("amb_4", "Vikram Malhotra", "PB-11-D-3456", "Available", "Hospital Base", "+91 98765 12345", "ICU Express"),
                Ambulance("amb_5", "Rahul Sharma", "PB-11-E-7890", "Available", "Model Town", "+91 91234 56789", "Basic Support"),
                Ambulance("amb_6", "Amit Patel", "PB-65-F-2345", "Available", "Hospital Base", "+91 90000 11111", "Adv. Life Support"),
                Ambulance("amb_7", "Sanjay Gupta", "PB-10-G-6789", "Maintenance", "Workshop", "+91 92222 33333", "Basic Support"),
                Ambulance("amb_8", "Karan Brar", "PB-02-H-0123", "Available", "Sector 45", "+91 93333 44444", "ICU Express"),
                Ambulance("amb_9", "Arjun Gill", "PB-12-I-4567", "Available", "Hospital Base", "+91 94444 55555", "Basic Support"),
                Ambulance("amb_10", "Deepak Verma", "PB-13-J-8901", "Busy", "Highway NH1", "+91 95555 66666", "Adv. Life Support")
            )
            val batch = firestore.batch()
            fleet.forEach { amb ->
                val ref = firestore.collection("ambulances").document(amb.id)
                batch.set(ref, amb)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun updateStatus(id: String, status: String) {
        firestore.collection("ambulances").document(id)
            .update("status", status)
            .await()
    }

    // Emergency Handling
    fun getActiveEmergencies(): Flow<List<TriageResult>> = callbackFlow {
        val subscription = firestore.collection("triage_logs")
            .whereIn("priority", listOf("High Risk", "Critical (ASHA)"))
            .whereIn("priority", listOf("High Risk", "Critical (ASHA)"))
            // .orderBy("timestamp", Query.Direction.DESCENDING) // Removed to avoid Composite Index requirement
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("AmbulanceRepo", "Listen failed.", e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(TriageResult::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }
    
    suspend fun assignAmbulanceToCase(ambulanceId: String, caseTimestamp: Date) {
        // Mark ambulance busy
        updateStatus(ambulanceId, "Busy")
        
        // Update case status
        val snapshot = firestore.collection("triage_logs")
             .whereEqualTo("timestamp", caseTimestamp)
             .get()
             .await()
         
         snapshot.documents.forEach { document ->
             document.reference.update("status", "Ambulance Dispatched ($ambulanceId)").await()
         }
    }
}
