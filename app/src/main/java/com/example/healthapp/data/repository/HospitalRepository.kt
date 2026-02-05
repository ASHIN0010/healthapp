package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class HospitalRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun getRecentTriageLogs(): Flow<List<TriageResult>> = callbackFlow {
        val subscription = firestore.collection("ai_triage_logs")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val logs = snapshot.toObjects(TriageResult::class.java)
                    trySend(logs)
                }
            }
        awaitClose { subscription.remove() }
    }
}
