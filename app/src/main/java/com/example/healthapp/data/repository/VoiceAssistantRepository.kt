package com.example.healthapp.data.repository

import com.example.healthapp.data.remote.api.GrokApiService
import com.example.healthapp.data.remote.api.GrokRequest
import com.example.healthapp.data.remote.api.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

data class VoiceLog(
    val id: String = "",
    val text: String = "",
    val language: String = "en", // Default en
    val timestamp: Date = Date(),
    val patientId: String = ""
)

data class AiAnalysisResult(
    val symptoms: String = "",
    val preventionAdvice: String = "",
    val rxDraft: String = "", // "Paracetamol 500mg, Twice a day, 3 days"
    val riskLevel: String = "Low",
    val isEmergency: Boolean = false
)

data class PrescriptionDraft(
    val id: String = "",
    val patientId: String = "",
    val patientName: String = "Unknown", // Added for Hospital Dashboard visibility
    val content: String = "",
    val status: String = "Pending", // Pending, Approved, Rejected
    val doctorId: String = "",
    val timestamp: Date = Date()
)

    // Removed Duplicate VoiceConsultation


class VoiceAssistantRepository @Inject constructor(
    private val apiService: GrokApiService,
    private val firestore: FirebaseFirestore,
    private val illnessCatalogRepository: IllnessCatalogRepository
) {

    suspend fun analyzeSymptoms(text: String, userId: String, userRole: String): Flow<Result<AiAnalysisResult>> = flow {
        // Fetch Catalog Context for better accuracy
        val catalog = try {
            kotlinx.coroutines.withTimeoutOrNull(2000) {
                illnessCatalogRepository.getIllnessCatalog().firstOrNull() ?: emptyList()
            } ?: emptyList()
        } catch (e: Exception) { emptyList() }

        try {
            // Grok Prompt with Catalog Context
            val systemPrompt = """
                You are a medical consultant.
                Use this verified illness catalog if applicable: ${catalog.map { "${it.name}: ${it.warningSigns.joinToString()}" }.joinToString("; ")}
                
                Analyze the symptoms and provide:
                1. SYMPTOMS: List the identified symptoms.
                2. PREVENTION: Specific preventive measures (from catalog if matching).
                3. RX_DRAFT: Suggested generic medication (Dosage, Frequency, Duration) - LABEL THIS "DRAFT".
                4. RISK: Risk Level (Low/Medium/High/Emergency).
                
                Format response exactly as:
                SYMPTOMS: ...
                PREVENTION: ...
                RX_DRAFT: ...
                RISK: ...
            """.trimIndent()

            val userPrompt = "Patient Symptoms: $text"

            val request = GrokRequest(
                messages = listOf(Message("system", systemPrompt), Message("user", userPrompt))
            )

            // API Call
            val response = apiService.chat(request)
            val content = response.choices.firstOrNull()?.message?.content ?: ""

            // Parse Response
            val result = parseGrokResponse(content)
            
            // Save Draft logic
            if (result.rxDraft.isNotBlank() && result.rxDraft != "No specific Rx") {
                val draft = PrescriptionDraft(
                    patientId = userId,
                    patientName = "Patient ($userId)", 
                    content = result.rxDraft,
                    status = "Pending"
                )
                firestore.collection("doctor_prescription_drafts").add(draft)
            }

            emit(Result.success(result))

        } catch (e: Exception) {
            android.util.Log.e("VoiceAssistant", "API Error: ${e.message}", e)
            // Intelligent Fallback using Catalog (OFFLINE MODE / API FAILURE)
            val fallback = createIntelligentFallback(text, catalog)
            emit(Result.success(fallback))
        }
    }

    private fun createIntelligentFallback(text: String, catalog: List<IllnessRisk>): AiAnalysisResult {
        val lowerText = text.lowercase()
        
        // 1. Find Match in Catalog
        val match = catalog.find { illness ->
            illness.name.lowercase() in lowerText || 
            illness.symptoms.any { it.lowercase() in lowerText } ||
            illness.warningSigns.any { it.lowercase() in lowerText }
        }

        if (match != null) {
            return AiAnalysisResult(
                symptoms = match.symptoms.joinToString(", ") + " (Detected from: $text)",
                preventionAdvice = match.preventionMethods.joinToString("\n• ", prefix = "• "),
                rxDraft = "Suggested: ${match.action} (Consult Doctor)",
                riskLevel = match.riskLevel,
                isEmergency = match.isEmergency
            )
        }
        
        // 2. Generic Keyword Matching (if no catalog match)
        return when {
            lowerText.contains("fever") -> AiAnalysisResult(
                symptoms = "Fever detected",
                preventionAdvice = "• Stay hydrated\n• Rest\n• Monitor temperature\n• Cold compress",
                rxDraft = "Paracetamol 650mg (Consult Doctor)",
                riskLevel = "Medium"
            )
            lowerText.contains("pain") || lowerText.contains("ache") -> AiAnalysisResult(
                symptoms = "Pain/Ache reported",
                preventionAdvice = "• Rest the affected area\n• Apply hot/cold pack\n• Avoid strain",
                rxDraft = "Analgesic gel / Paracetamol (Consult Doctor)",
                riskLevel = "Low"
            )
            else -> AiAnalysisResult(
                symptoms = text,
                preventionAdvice = "• Stay hydrated\n• Rest well\n• Maintain hygiene\n• Consult doctor if symptoms persist",
                rxDraft = "No specific medication suggested without diagnosis.",
                riskLevel = "Low"
            )
        }
    }

    private fun parseGrokResponse(content: String): AiAnalysisResult {
        val symptoms = content.substringAfter("SYMPTOMS:", "").substringBefore("PREVENTION:", "").trim()
        val prevention = content.substringAfter("PREVENTION:", "").substringBefore("RX_DRAFT:", "").trim()
        val rxDraft = content.substringAfter("RX_DRAFT:", "").substringBefore("RISK:", "").trim()
        val risk = content.substringAfter("RISK:", "").trim()
        
        return AiAnalysisResult(
            symptoms = if (symptoms.isBlank()) "Detected from voice" else symptoms,
            preventionAdvice = if (prevention.isBlank()) "Consult Doctor" else prevention,
            rxDraft = if (rxDraft.isBlank()) "No specific Rx" else rxDraft,
            riskLevel = risk,
            isEmergency = risk.contains("Emergency", ignoreCase = true) || risk.contains("High", ignoreCase = true)
        )
    }
    
    // For Hospital Dashboard
     fun getPendingDrafts(): Flow<List<PrescriptionDraft>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = firestore.collection("doctor_prescription_drafts")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.let {
                    val drafts = it.documents.map { doc ->
                        doc.toObject(PrescriptionDraft::class.java)!!.copy(id = doc.id)
                    }
                    trySend(drafts)
                }
            }
        awaitClose { listener.remove() }
    }
    
    suspend fun approveDraft(draftId: String, doctorId: String) {
        firestore.collection("doctor_prescription_drafts").document(draftId)
            .update("status", "Approved", "doctorId", doctorId)
            .await()
    }
}
