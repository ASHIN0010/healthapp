package com.example.healthapp.data.repository

import com.example.healthapp.data.remote.api.GrokApiService
import com.example.healthapp.data.remote.api.GrokRequest
import com.example.healthapp.data.remote.api.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

class VoiceAssistantRepository @Inject constructor(
    private val apiService: GrokApiService,
    private val firestore: FirebaseFirestore
) {

    suspend fun analyzeSymptoms(text: String, userId: String, userName: String): Flow<Result<AiAnalysisResult>> = flow {
        try {
            // 1. Log Voice Input
            val log = VoiceLog(text = text, patientId = userId)
            firestore.collection("voice_symptom_logs").add(log) // Fire and forget logic

            // 2. AI Prompt
            val systemPrompt = """
                You are a medical assistant AI.
                Analyze the patient's symptoms and provide:
                1. Prevention Advice (Lifestyle/Diet/Hygiene)
                2. Prescription Draft (Generic medicine names, Dosage, Frequency, Duration) - LABEL THIS "DRAFT".
                3. Risk Level (Low/Medium/High/Emergency)
                
                Format response as:
                SYMPTOMS: ...
                PREVENTION: ...
                RX_DRAFT: ...
                RISK: ...
            """.trimIndent()

            val userPrompt = "Patient Symptoms: $text"

            val request = GrokRequest(
                messages = listOf(Message("system", systemPrompt), Message("user", userPrompt))
            )

            // Mocking API call logic - assuming apiService works similar to Triage
            val response = apiService.chat(request)
            val content = response.choices.firstOrNull()?.message?.content ?: ""

            // 3. Parse Response
            val result = parseGrokResponse(content)
            
            // 4. Save Draft if Risk is not Emergency (Emergencies go to Triage/Alerts typically, but here we save draft for review)
            if (result.rxDraft.isNotBlank()) {
                val draft = PrescriptionDraft(
                    patientId = userId,
                    patientName = userName,
                    content = result.rxDraft,
                    status = "Pending"
                )
                firestore.collection("doctor_prescription_drafts").add(draft)
            }

            emit(Result.success(result))

        } catch (e: Exception) {
            // Fallback Logic
            val fallback = AiAnalysisResult(
                symptoms = text,
                preventionAdvice = "Consult a doctor immediately. Stay hydrated and rest.",
                rxDraft = "Paracetamol 650mg if fever > 100F (Consult Doctor)",
                riskLevel = "Medium"
            )
            emit(Result.success(fallback))
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
