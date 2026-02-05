package com.example.healthapp.data.repository

import com.example.healthapp.data.remote.api.GrokApiService
import com.example.healthapp.data.remote.api.GrokRequest
import com.example.healthapp.data.remote.api.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DoctorAssistantRepository @Inject constructor(
    private val apiService: GrokApiService
) {
    suspend fun getClinicalSummary(age: Int, gender: String, symptoms: String, history: String): Flow<Result<String>> = flow {
        try {
            val systemPrompt = """
                You are a Clinical AI Assistant for a doctor. 
                Task: Summarize patient history, highlight red flags, and suggest condition categories.
                Disclaimer: You DO NOT diagnose. Always append "Doctor has final authority."
                Format: Markdown.
                Red Flags: List any urgent symptoms.
            """.trimIndent()

            val userPrompt = """
                Patient: $age, $gender
                Symptoms: $symptoms
                History: $history
            """.trimIndent()

            val request = GrokRequest(
                messages = listOf(Message("system", systemPrompt), Message("user", userPrompt))
            )

            val response = apiService.chat(request)
            val content = response.choices.firstOrNull()?.message?.content ?: "No analysis generated."
            emit(Result.success(content))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
