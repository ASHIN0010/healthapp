package com.example.healthapp.data.repository

import com.example.healthapp.data.remote.api.GrokApiService
import com.example.healthapp.data.remote.api.GrokRequest
import com.example.healthapp.data.remote.api.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import java.util.Date

data class TriageResult(
    val priority: String = "",
    val explanation: String = "",
    val recommendedAction: String = "",
    val preventiveMeasures: String = "",
    val immediateSolutions: String = "",
    val patientAge: Int = 0,
    val patientGender: String = "",
    val symptoms: String = "",
    val timestamp: Date = Date(),
    val status: String = "New" // New, Pending, Ambulance Dispatched, Closed
)

class TriageRepository @Inject constructor(
    private val apiService: GrokApiService,
    private val firestore: FirebaseFirestore,
    private val illnessCatalogRepository: IllnessCatalogRepository // Injected
) {

    suspend fun analyzeSymptoms(
        age: Int,
        gender: String,
        symptoms: String
    ): Flow<Result<TriageResult>> = flow {
        // Fetch Catalog for AI context or Fallback
        var catalog: List<IllnessRisk> = emptyList()
        try {
            // Quick fetch of latest catalog
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                 illnessCatalogRepository.getIllnessCatalog().collect { 
                     catalog = it
                     throw CancellationException("Got data") // Hack to stop collection or use first()
                 }
            }
        } catch (e: Exception) { 
            // generic catch
        }
        
        // Better way: use firstOrNull() if flow emits
        val safeCatalog = try {
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                illnessCatalogRepository.getIllnessCatalog().firstOrNull() ?: emptyList()
            } ?: emptyList()
        } catch(e: Exception) { emptyList() }


        try {
            // 1. AI Analysis
            val systemPrompt = """
                You are a triage assistant. 
                Classify risk as: "Low Risk", "Medium Risk", or "High Risk".
                Use this verified illness catalog if applicable: ${safeCatalog.joinToString { "${it.name} (${it.riskLevel})" }}
                Provide 4 clearly labeled sections:
                1. Risk Level: (Low/Medium/High)
                2. Explanation: (Brief reason)
                3. Immediate Solution: (What to do right now)
                4. Preventive Measures: (How to avoid this in future)
            """.trimIndent()

            val userPrompt = """
                Patient: $age years, $gender.
                Symptoms: $symptoms
            """.trimIndent()

            val request = GrokRequest(
                messages = listOf(Message("system", systemPrompt), Message("user", userPrompt))
            )

            // Mocking API call if key is invalid, but assuming it works.
            val response = apiService.chat(request)
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            
            val result = parseGrokResponse(content, age, gender, symptoms)
            
            // 2. Save to Firestore
            saveTriageLog(result)
            
            emit(Result.success(result))

        } catch (e: Exception) {
            // Intelligent fallback based on symptoms if API fails
            val fallback = createIntelligentFallback(age, gender, symptoms, safeCatalog)
            saveTriageLog(fallback) // Save fallback too
            emit(Result.success(fallback))
        }
    }

    private fun createIntelligentFallback(age: Int, gender: String, symptoms: String, catalog: List<IllnessRisk>): TriageResult {
        val symptomsLower = symptoms.lowercase()
        
        // 1. Critical Check: Match against specific Warning Signs first (Highest Priority)
        val warningMatch = catalog.find { illness ->
            illness.warningSigns.any { sign -> symptomsLower.contains(sign.lowercase()) }
        }

        if (warningMatch != null) {
            return TriageResult(
                priority = "High Risk",
                explanation = "Critical Warning Sign Detected: ${warningMatch.name}. ${warningMatch.description}",
                recommendedAction = warningMatch.action,
                preventiveMeasures = warningMatch.preventionMethods.joinToString(". "),
                immediateSolutions = "Immediate Emergency Care Required.",
                patientAge = age,
                patientGender = gender,
                symptoms = symptoms
            )
        }

        // 2. Symptom Matching
        // Sort catalog by Risk (High->Medium->Low)
        val highRiskInfo = catalog.filter { it.riskLevel == "High" }
        val mediumRiskInfo = catalog.filter { it.riskLevel == "Medium" }
        val lowRiskInfo = catalog.filter { it.riskLevel == "Low" }
        
        var matchedIllness: IllnessRisk? = null
        
        // Check High Risk Symptoms
        matchedIllness = highRiskInfo.find { illness -> 
            illness.symptoms.any { symptomsLower.contains(it.lowercase()) } || symptomsLower.contains(illness.name.lowercase())
        }
        
        // Check Medium Risk
        if (matchedIllness == null) {
            matchedIllness = mediumRiskInfo.find { illness -> 
                illness.symptoms.any { symptomsLower.contains(it.lowercase()) } || symptomsLower.contains(illness.name.lowercase())
            }
        }
        
        // Check Low Risk
        if (matchedIllness == null) {
            matchedIllness = lowRiskInfo.find { illness -> 
                illness.symptoms.any { symptomsLower.contains(it.lowercase()) } || symptomsLower.contains(illness.name.lowercase())
            }
        }

        // Default if no match
        val priority = matchedIllness?.riskLevel ?: "Low Risk"
        val explanation = matchedIllness?.description ?: "Unclassified symptoms. Please consult a doctor."
        val action = matchedIllness?.action ?: "Monitor and rest."
        val preventive = matchedIllness?.preventionMethods?.joinToString(". ") ?: "Maintain hygiene and balanced diet."
        val solution = matchedIllness?.action ?: "Consult a specialist."

        return TriageResult(
            priority = if(priority.contains("Risk")) priority else "$priority Risk",
            explanation = explanation,
            recommendedAction = action, 
            preventiveMeasures = preventive,
            immediateSolutions = solution,
            patientAge = age,
            patientGender = gender,
            symptoms = symptoms
        )
    }

    suspend fun saveTriageLog(result: TriageResult) {
        try {
            firestore.collection("ai_triage_logs")
                .add(result)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // ... getRealTimeTriageLogs ...


    fun getRealTimeTriageLogs(): Flow<List<TriageResult>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = firestore.collection("ai_triage_logs")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val logs = it.toObjects(TriageResult::class.java)
                    trySend(logs)
                }
            }
        awaitClose { listener.remove() }
    }

    private fun parseGrokResponse(content: String, age: Int, gender: String, symptoms: String): TriageResult {
        val priority = when {
            content.contains("High Risk", ignoreCase = true) -> "High Risk"
            content.contains("Medium Risk", ignoreCase = true) -> "Medium Risk"
            else -> "Low Risk"
        }
        
        // Simple string parsing (Robust JSON parsing would be better but keeping it compatible with current string response)
        val explanation = content.substringAfter("Explanation:", "See details below").substringBefore("Immediate Solution:", "").trim()
        val solution = content.substringAfter("Immediate Solution:", "").substringBefore("Preventive Measures:", "").trim()
        val preventive = content.substringAfter("Preventive Measures:", "").trim()
        
        return TriageResult(
            priority = priority,
            explanation = if(explanation.isBlank()) content else explanation,
            recommendedAction = "Follow AI Advice",
            preventiveMeasures = if(preventive.isBlank()) "Consult Doctor" else preventive,
            immediateSolutions = if(solution.isBlank()) "Consult Doctor" else solution,
            patientAge = age,
            patientGender = gender,
            symptoms = symptoms
        )
    }
}
