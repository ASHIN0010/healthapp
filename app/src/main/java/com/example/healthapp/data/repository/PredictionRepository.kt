package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import java.util.Calendar
import kotlin.math.roundToInt

data class MedicineUsageLog(
    val medicineId: String = "",
    val quantity: Int = 0,
    val date: Date = Date(),
    val note: String = ""
)

data class PredictionResult(
    val medicineId: String = "",
    val medicineName: String = "",
    val currentStock: Int = 0,
    val predictedDemand: Int = 0,
    val recommendedStock: Int = 0,
    val riskLevel: String = "Normal", // Normal, shortage_risk, overstock_risk
    val seasonTag: String = "None",
    val explanation: String = ""
)

class PredictionRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun logUsage(medicineId: String, quantity: Int) {
        val log = MedicineUsageLog(
            medicineId = medicineId,
            quantity = quantity,
            date = Date()
        )
        firestore.collection("medicine_usage_logs").add(log).await()
    }

    suspend fun generateForecast(inventory: List<Medicine>): List<PredictionResult> {
        // 1. Determine Current Season
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) // 0-indexed (0 = Jan)
        
        val currentSeason = when(month) {
            in 2..5 -> "Summer" // Mar-Jun
            in 6..8 -> "Monsoon" // Jul-Sep
            in 9..10 -> "Post-Monsoon" // Oct-Nov
            else -> "Winter" // Dec-Feb
        }

        val forecastList = mutableListOf<PredictionResult>()

        for (med in inventory) {
            // 2. Base Demand (Mocked Historical Average - for now flat random or base on stock)
            // in a real app, we'd query "medicine_usage_logs" for the last 3 months
            var baseDemand = 50 // Default monthly demand
            
            // 3. Apply Seasonal Rules
            var seasonalMultiplier = 1.0
            var reason = "Normal demand pattern."
            var seasonTag = "None"

            // Rule 1: Monsoon Spikes (Fever, Infection, Diarrhea)
            if (currentSeason == "Monsoon") {
                if (med.name.contains("Paracetamol", ignoreCase = true) || 
                    med.name.contains("Dolo", ignoreCase = true)) {
                    seasonalMultiplier = 2.5 // +150%
                    reason = "High fever cases predicted due to Monsoon dengue/malaria trends."
                    seasonTag = "Monsoon Spike"
                } else if (med.name.contains("Azithromycin", ignoreCase = true) || 
                           med.name.contains("Antibiotic", ignoreCase = true)) {
                    seasonalMultiplier = 1.8
                    reason = "Increased viral/bacterial infections predicted."
                    seasonTag = "Monsoon Spike"
                }
            }

            // Rule 2: Winter Spikes (Cough, Cold, Asthma)
            if (currentSeason == "Winter") {
                 if (med.name.contains("Cough", ignoreCase = true) || 
                    med.name.contains("Cetirizine", ignoreCase = true) ||
                    med.name.contains("Montelukast", ignoreCase = true)) {
                    seasonalMultiplier = 2.0
                    reason = "Cold wave expected to increase respiratory cases."
                    seasonTag = "Winter Spike"
                }
            }
            
            // Rule 3: Summer Spikes (Dehydration)
            if (currentSeason == "Summer") {
                 if (med.name.contains("ORS", ignoreCase = true) || 
                    med.name.contains("Electral", ignoreCase = true)) {
                    seasonalMultiplier = 3.0
                    reason = "Heatwave conditions; dehydration cases likely to surge."
                    seasonTag = "Summer Spike"
                }
            }

            val predictedDemand = (baseDemand * seasonalMultiplier).roundToInt()
            val recommendedStock = (predictedDemand * 1.2).roundToInt() // 20% safety buffer
            
            // 4. Calculate Risk
            val riskLevel = when {
                med.stock < predictedDemand -> "Shortage Risk"
                med.stock > (predictedDemand * 3) -> "Overstock Risk"
                else -> "Normal"
            }
            
            // Store Forecast
            val prediction = PredictionResult(
                medicineId = med.id,
                medicineName = med.name,
                currentStock = med.stock,
                predictedDemand = predictedDemand,
                recommendedStock = recommendedStock,
                riskLevel = riskLevel,
                seasonTag = seasonTag,
                explanation = reason
            )
            
            // Save to Firestore for analytics
            savePrediction(prediction)
            
            forecastList.add(prediction)
        }
        
        return forecastList
    }

    private suspend fun savePrediction(prediction: PredictionResult) {
        try {
            // Upsert based on medicineId + current month (to avoid dups)
            // For MVP, just adding a new record
            firestore.collection("ai_medicine_predictions").add(prediction)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
