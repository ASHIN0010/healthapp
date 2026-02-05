package com.example.healthapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<String?> // Returns user ID or null
    suspend fun sendOtp(phoneNumber: String, activity: android.app.Activity): Flow<Result<String>>
    suspend fun verifyOtp(verificationId: String, code: String): Result<String>
    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signUpWithEmail(email: String, password: String): Result<String>
    suspend fun signInWithGoogle(idToken: String): Result<String>
    suspend fun signOut()
}
