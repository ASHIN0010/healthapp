package com.example.healthapp.data.repository

import java.util.Date

data class VoiceConsultation(
    val id: String = "",
    val userId: String = "",
    val userRole: String = "Patient", // Patient, ASHA
    val spokenText: String = "",
    val aiResponse: String = "",
    val aiResponseAudioUrl: String? = null,
    val riskLevel: String = "Low Risk",
    val timestamp: Date = Date(),
    val language: String = "en" // en, hi, pa
)
