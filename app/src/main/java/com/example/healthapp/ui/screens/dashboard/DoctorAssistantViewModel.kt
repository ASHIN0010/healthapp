package com.example.healthapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.repository.DoctorAssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DoctorAssistantViewModel @Inject constructor(
    private val repository: DoctorAssistantRepository
) : ViewModel() {

    private val _summary = MutableStateFlow<String?>(null)
    val summary: StateFlow<String?> = _summary
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun generateSummary(age: String, gender: String, symptoms: String, history: String) {
        val ageInt = age.toIntOrNull() ?: 0
        viewModelScope.launch {
            _isLoading.value = true
            repository.getClinicalSummary(ageInt, gender, symptoms, history).collect { result ->
                result.onSuccess { 
                    _summary.value = it
                }
                _isLoading.value = false
            }
        }
    }
}
