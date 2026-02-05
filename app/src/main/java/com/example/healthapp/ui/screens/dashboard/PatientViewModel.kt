package com.example.healthapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.repository.TriageResult
import com.example.healthapp.domain.usecase.AnalyzeSymptomsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.healthapp.data.repository.DoctorRepository

data class PatientUiState(
    val isLoading: Boolean = false,
    val triageResult: TriageResult? = null,
    val error: String? = null,
    val availableDoctors: Int = 0
)

@HiltViewModel
class PatientViewModel @Inject constructor(
    private val analyzeSymptomsUseCase: AnalyzeSymptomsUseCase,
    private val doctorRepository: DoctorRepository,
    private val triageRepository: com.example.healthapp.data.repository.TriageRepository,
    private val pharmacyRepository: com.example.healthapp.data.repository.PharmacyRepository,
    private val appointmentRepository: com.example.healthapp.data.repository.AppointmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PatientUiState())
    val uiState: StateFlow<PatientUiState> = _uiState

    // Connectivity Features
    private val _specialists = MutableStateFlow<List<com.example.healthapp.data.repository.DoctorProfile>>(emptyList())
    val specialists: StateFlow<List<com.example.healthapp.data.repository.DoctorProfile>> = _specialists

    private val _medicineResults = MutableStateFlow<List<com.example.healthapp.data.repository.Medicine>>(emptyList())
    val medicineResults: StateFlow<List<com.example.healthapp.data.repository.Medicine>> = _medicineResults

    init {
        viewModelScope.launch {
            doctorRepository.getAvailableDoctors().collect { doctors ->
                _uiState.value = _uiState.value.copy(availableDoctors = doctors.size)
            }
        }
        // Load specialists for the "Find Doctor" tab
        viewModelScope.launch {
            doctorRepository.getAllSpecialists().collect {
                _specialists.value = it
            }
        }
    }

    fun searchMedicine(query: String) {
        viewModelScope.launch {
            pharmacyRepository.searchMedicine(query).collect {
                _medicineResults.value = it
            }
        }
    }
    
    // Manual injection of PharmacyRepo for this step to keep changes minimal or I should inject it properly.
    // Let's rely on the fact that I can't easily change the constructor without viewing the file imports again or assuming Hilt setup.
    // Actually, I should check if I can add PharmacyRepository to constructor.
    // I entered "com.example.healthapp.data.repository.PharmacyRepository" usage above which is bad practice if not injected.
    // I will try to update constructor in a separate call or just suppress for now and do it cleanly.
    // Let's assume I can add it to constructor.
    // Wait, I will just use the replace_file_content to adding it to constructor and imports.

    fun triggerEmergency(age: String, gender: String, symptoms: String = "EMERGENCY ALERT") {
         viewModelScope.launch {
             _uiState.value = _uiState.value.copy(isLoading = true)
             val ageInt = age.toIntOrNull() ?: 0
             val EmergencyResult = TriageResult(
                 priority = "High Risk",
                 explanation = "EMERGENCY BUTTON TRIGGERED BY PATIENT",
                 recommendedAction = "Immediate Ambulance Dispatch Required",
                 preventiveMeasures = "N/A",
                 immediateSolutions = "Wait for Ambulance",
                 patientAge = ageInt,
                 patientGender = gender,
                 symptoms = "$symptoms - ONE TAP ALERT"
             )
             
             triageRepository.saveTriageLog(EmergencyResult)
             _uiState.value = PatientUiState(triageResult = EmergencyResult)
         }
    }

    fun analyzeSymptoms(age: String, gender: String, symptoms: String) {
        val ageInt = age.toIntOrNull()
        if (ageInt == null) {
            _uiState.value = _uiState.value.copy(error = "Invalid Age")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = PatientUiState(isLoading = true)
            analyzeSymptomsUseCase(ageInt, gender, symptoms).collect { result ->
                result.onSuccess { triage ->
                    _uiState.value = PatientUiState(triageResult = triage)
                }.onFailure { e ->
                    _uiState.value = PatientUiState(error = e.message)
                }
            }
        }
    }
    
    fun bookAppointment(doctorId: String, doctorName: String, timeSlot: String) {
        viewModelScope.launch {
            try {
                // AI Logic: Attach Triage Risk Level to Appointment for Priority Queuing
                val riskLevel = _uiState.value.triageResult?.priority ?: "Standard"
                appointmentRepository.bookAppointment(doctorId, doctorName, timeSlot, riskLevel)
                // In real app, show success message via side effect
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
}
