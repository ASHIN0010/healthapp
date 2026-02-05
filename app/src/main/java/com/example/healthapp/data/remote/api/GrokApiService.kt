package com.example.healthapp.data.remote.api

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class GrokRequest(
    val messages: List<Message>,
    val model: String = "grok-beta", // Or appropriate model name
    val stream: Boolean = false,
    val temperature: Double = 0.7
)

data class Message(
    val role: String,
    val content: String
)

data class GrokResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: Message,
    val finish_reason: String?
)

interface GrokApiService {
    @POST("chat/completions")
    suspend fun chat(@Body request: GrokRequest): GrokResponse
}
