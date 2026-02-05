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
    val preventionMethods: List<String> = emptyList(), // Lifestyle, Hygiene, etc.
    val warningSigns: List<String> = emptyList(),
    val action: String = "",
    val isEmergency: Boolean = false,
    val description: String = "",
    val seasonalTag: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

class IllnessCatalogRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getIllnessCatalog(): Flow<List<IllnessRisk>> = callbackFlow {
        val listener = firestore.collection("illness_prevention_catalog")
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
        val collection = firestore.collection("illness_prevention_catalog")
        val snapshot = collection.limit(1).get().await()
        
        if (snapshot.isEmpty) {
            val initialCatalog = listOf(
                // LOW RISK
                IllnessRisk(
                    id = "common_cold",
                    name = "Common Cold",
                    riskLevel = "Low",
                    symptoms = listOf("runny nose", "sore throat", "mild cough", "sneezing", "low fever"),
                    preventionMethods = listOf("Wash hands frequently.", "Drink warm fluids.", "Wear mask if coughing."),
                    warningSigns = listOf("Difficulty breathing", "High fever > 102F"),
                    action = "Rest, hydration, and steam inhalation.",
                    description = "Viral infection of the nose and throat.",
                    seasonalTag = "Winter"
                ),
                IllnessRisk(
                    id = "mild_dehydration",
                    name = "Mild Dehydration",
                    riskLevel = "Low",
                    symptoms = listOf("thirst", "dry mouth", "yellow urine", "mild fatigue"),
                    preventionMethods = listOf("Drink 8 glasses of water daily.", "Avoid excessive sun exposure.", "Drink ORS if sweating heavily."),
                    warningSigns = listOf("No urine for 8 hours", "Dizziness", "Confusion"),
                    action = "Drink plenty of water and ORS.",
                    description = "Loss of body fluids due to heat or illness.",
                    seasonalTag = "Summer"
                ),
                IllnessRisk(
                    id = "seasonal_allergy",
                    name = "Seasonal Allergy",
                    riskLevel = "Low",
                    symptoms = listOf("itchy eyes", "sneezing", "runny nose", "congestion"),
                    preventionMethods = listOf("Keep windows closed during pollen season.", "Wear mask outdoors.", "Shower after being outside."),
                    warningSigns = listOf("Wheezing", "Severe shortness of breath"),
                    action = "Avoid allergens, take antihistamines if prescribed.",
                    description = "Reaction to airborne substances.",
                    seasonalTag = "Spring"
                ),

                // MEDIUM RISK
                IllnessRisk(
                    id = "viral_fever",
                    name = "Viral Fever (Persistent)",
                    riskLevel = "Medium",
                    symptoms = listOf("high fever", "body ache", "chills", "fatigue", "headache"),
                    preventionMethods = listOf("Avoid crowded places.", "Eat cooked food.", "Maintain good hygiene."),
                    warningSigns = listOf("Fever > 3 days", "Severe headache", "Rash"),
                    action = "Visit OPD. Paracetamol and rest.",
                    description = "Fever lasting more than 3 days.",
                    seasonalTag = "Monsoon"
                ),
                IllnessRisk(
                    id = "moderate_diarrhea",
                    name = "Moderate Diarrhea",
                    riskLevel = "Medium",
                    symptoms = listOf("loose stools", "cramps", "nausea", "moderate weakness"),
                    preventionMethods = listOf("Wash hands before eating.", "Drink boiled/filtered water.", "Avoid street food."),
                    warningSigns = listOf("Blood in stool", "Severe pain", "Cannot keep fluids down"),
                    action = "Visit OPD. Increase fluids and zinc supplements.",
                    description = "Frequent loose bowel movements."
                ),
                IllnessRisk(
                    id = "dengue_suspected",
                    name = "Suspected Dengue",
                    riskLevel = "Medium",
                    symptoms = listOf("high fever", "pain behind eyes", "joint pain", "rash"),
                    preventionMethods = listOf("Use mosquito repellent.", "Wear long sleeves.", "Remove stagnant water."),
                    warningSigns = listOf("Bleeding gums", "Severe abdominal pain", "Vomiting blood"),
                    action = "Visit Doctor Check platelet count.",
                    description = "Mosquito-borne viral infection.",
                    seasonalTag = "Monsoon"
                ),

                // HIGH RISK
                IllnessRisk(
                    id = "severe_pneumonia",
                    name = "Severe Pneumonia",
                    riskLevel = "High",
                    symptoms = listOf("difficulty breathing", "chest pain", "high fever", "cough with phlegm"),
                    preventionMethods = listOf("Vaccination (CVV/Flu).", "Avoid smoking.", "Good hand hygiene."),
                    warningSigns = listOf("Blue lips/nails", "Confusion", "Rapid breathing"),
                    action = "IMMEDIATE HOSPITALIZATION. Oxygen support may be needed.",
                    isEmergency = true,
                    description = "Severe lung infection affecting breathing.",
                    seasonalTag = "Winter"
                ),
                IllnessRisk(
                    id = "heart_attack",
                    name = "Heart Attack Warning",
                    riskLevel = "High",
                    symptoms = listOf("chest pain", "pain in arm", "shortness of breath", "cold sweat", "nausea"),
                    preventionMethods = listOf("Healthy diet (low salt/fat).", "Regular exercise.", "Manage stress.", "No smoking."),
                    warningSigns = listOf("Pain lasting > 15 mins", "Fainting"),
                    action = "EMERGENCY: Call Ambulance immediately.",
                    isEmergency = true,
                    description = "Blockage of blood flow to the heart."
                ),
                IllnessRisk(
                    id = "severe_dehydration",
                    name = "Severe Dehydration",
                    riskLevel = "High",
                    symptoms = listOf("unconsciousness", "sunken eyes", "rapid heartbeat", "no urine output"),
                    preventionMethods = listOf("Drink water frequently.", "Avoid heat exhaustion."),
                    warningSigns = listOf("Unresponsive", "Seizures"),
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
