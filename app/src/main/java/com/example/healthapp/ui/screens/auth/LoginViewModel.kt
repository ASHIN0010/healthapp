package com.example.healthapp.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val isOtpSent: Boolean = false,
    val verificationId: String? = null,
    val error: String? = null,
    val userId: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun sendOtp(phoneNumber: String, activity: Activity) {
        if (phoneNumber.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter phone number")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.sendOtp(phoneNumber, activity).collect { result ->
                result.onSuccess { verificationId ->
                    if (verificationId == "instant_success") {
                        // Handle instant verification (rare but possible)
                         authRepository.currentUser.collect { uid ->
                             if (uid != null) {
                                 _uiState.value = _uiState.value.copy(isLoading = false, userId = uid)
                             }
                         }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isOtpSent = true,
                            verificationId = verificationId
                        )
                    }
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun verifyOtp(code: String) {
        val verificationId = _uiState.value.verificationId ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.verifyOtp(verificationId, code)
            result.onSuccess { userId ->
                 _uiState.value = _uiState.value.copy(isLoading = false, userId = userId)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signInWithEmail(email, pass)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, userId = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signUpWithEmail(email, pass)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, userId = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signInWithGoogle(idToken)
                .onSuccess { _uiState.value = _uiState.value.copy(isLoading = false, userId = it) }
                .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message) }
        }
    }
}
