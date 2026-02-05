package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class IllnessRisk(
    val id: String = "",
    val name: String = "",
    val riskLevel: String = "Low", // Low, Medium, High
    val symptoms: List<String> = emptyList(),
    val action: String = "",
    val isEmergency: Boolean = false,
    val description: String = "" // Added description for UI display
)

class IllnessCatalogRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getIllnessCatalog(): Flow<List<IllnessRisk>> = callbackFlow {
        val listener = firestore.collection("illness_risk_catalog")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val catalog = it.toObjects(IllnessRisk::class.java)
                    trySend(catalog)
                }
            }
        awaitClose { listener.remove() }
    }

    suspend fun seedInitialCatalog() {
        val collection = firestore.collection("illness_risk_catalog")
        val snapshot = collection.limit(1).get().await()
        
        if (snapshot.isEmpty) {
            val initialCatalog = listOf(
                // Low Risk
                IllnessRisk(
                    id = "common_cold",
                    name = "Common Cold",
                    riskLevel = "Low",
                    symptoms = listOf("runny nose", "sore throat", "mild cough", "sneezing", "low fever"),
                    action = "Rest, hydration, and steam inhalation.",
                    description = "Viral infection of the nose and throat."
                ),
                IllnessRisk(
                    id = "mild_dehydration",
                    name = "Mild Dehydration",
                    riskLevel = "Low",
                    symptoms = listOf("thirst", "dry mouth", "yellow urine", "mild fatigue"),
                    action = "Drink plenty of water and ORS.",
                    description = "Loss of body fluids."
                ),
                 IllnessRisk(
                    id = "seasonal_allergy",
                    name = "Seasonal Allergy",
                    riskLevel = "Low",
                    symptoms = listOf("itchy eyes", "sneezing", "runny nose", "congestion"),
                    action = "Avoid allergens, take antihistamines if prescribed.",
                    description = "Reaction to airborne substances."
                ),
                
                // Medium Risk
                IllnessRisk(
                    id = "viral_fever",
                    name = "Viral Fever (Persistent)",
                    riskLevel = "Medium",
                    symptoms = listOf("high fever", "body ache", "chills", "fatigue", "headache"),
                    action = "Visit OPD. Paracetamol and rest.",
                    description = "Fever lasting more than 3 days."
                ),
                IllnessRisk(
                    id = "moderate_diarrhea",
                    name = "Moderate Diarrhea",
                    riskLevel = "Medium",
                    symptoms = listOf("loose stools", "cramps", "nausea", "moderate weakness"),
                    action = "Visit OPD. Increase fluids and zinc supplements.",
                    description = "Frequent loose bowel movements."
                ),
                 IllnessRisk(
                    id = "uti",
                    name = "Urinary Tract Infection",
                    riskLevel = "Medium",
                    symptoms = listOf("burning urination", "frequent urination", "pelvic pain", "cloudy urine"),
                    action = "Consult doctor for antibiotics.",
                    description = "Infection in the urinary system."
                ),

                // High Risk
                IllnessRisk(
                    id = "severe_pneumonia",
                    name = "Severe Pneumonia",
                    riskLevel = "High",
                    symptoms = listOf("difficulty breathing", "chest pain", "high fever", "cough with phlegm"),
                    action = "IMMEDIATE HOSPITALIZATION. Oxygen support may be needed.",
                    isEmergency = true,
                    description = "Severe lung infection."
                ),
                 IllnessRisk(
                    id = "heart_attack",
                    name = "Heart Attack Warning",
                    riskLevel = "High",
                    symptoms = listOf("chest pain", "pain in arm", "shortness of breath", "cold sweat", "nausea"),
                    action = "EMERGENCY: Call Ambulance immediately.",
                    isEmergency = true,
                    description = "Blockage of blood flow to the heart."
                ),
                IllnessRisk(
                    id = "severe_dehydration",
                    name = "Severe Dehydration",
                    riskLevel = "High",
                    symptoms = listOf("unconsciousness", "sunken eyes", "rapid heartbeat", "no urine output"),
                    action = "IV Fluids required immediately. Transport to hospital.",
                    isEmergency = true,
                    description = "Critical loss of body fluids."
                )
            )

            initialCatalog.forEach { item ->
                collection.document(item.id).set(item).await()
            }
        }
    }
}
