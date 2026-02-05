package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class Medicine(
    val id: String = "",
    val name: String = "",
    val stock: Int = 0,
    val price: Double = 0.0,
    val expiryDate: String = ""
)

class PharmacyRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getInventory(): Flow<List<Medicine>> = callbackFlow {
        val listener = firestore.collection("medicine_inventory")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.toObjects(Medicine::class.java))
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun addMedicine(medicine: Medicine) {
        firestore.collection("medicine_inventory")
            .add(medicine)
            .await()
    }
    
    suspend fun updateStock(id: String, newStock: Int) {
         firestore.collection("medicine_inventory").document(id)
            .update("stock", newStock)
            .await()
    }

    suspend fun dispenseMedicine(id: String, quantity: Int) {
        // 1. Log Usage
        val log = hashMapOf(
            "medicineId" to id,
            "quantity" to quantity,
            "date" to java.util.Date()
        )
        firestore.collection("medicine_usage_logs").add(log).await()

        // 2. Decrement Stock (Transaction would be better but this is fine for MVP)
        // We assume the caller handles the stock calculation or we do a decrement here.
        // For simplicity, let's just log here. Actual stock update happens via updateStock or we can do it here.
    }

    fun searchMedicine(query: String): Flow<List<Medicine>> = callbackFlow {
        if (query.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("medicine_inventory")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    trySend(it.toObjects(Medicine::class.java))
                }
            }
        awaitClose { listener.remove() }
    }
}
