package com.example.healthapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.repository.AshaRepository
import com.example.healthapp.data.repository.DoctorProfile
import com.example.healthapp.data.repository.DoctorRepository
import com.example.healthapp.data.repository.VillageCase
import com.example.healthapp.data.repository.Vitals
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AshaUiState(
    val cases: List<VillageCase> = emptyList(),
    val doctors: List<DoctorProfile> = emptyList(),
    val caseStats: CaseStats = CaseStats(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class CaseStats(
    val total: Int = 0,
    val pending: Int = 0,
    val completed: Int = 0, // Visited + Referred
    val highRisk: Int = 0
)

@HiltViewModel
class AshaViewModel @Inject constructor(
    private val repository: AshaRepository,
    private val doctorRepository: DoctorRepository,
    private val pharmacyRepository: com.example.healthapp.data.repository.PharmacyRepository,
    private val illnessCatalogRepository: com.example.healthapp.data.repository.IllnessCatalogRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AshaUiState())
    val uiState: StateFlow<AshaUiState> = _uiState

    // Pharmacy Inventory
    private val _medicineInventory = MutableStateFlow<List<com.example.healthapp.data.repository.Medicine>>(emptyList())
    val medicineInventory: StateFlow<List<com.example.healthapp.data.repository.Medicine>> = _medicineInventory

    // Illness Catalog
    private val _illnessCatalog = MutableStateFlow<List<com.example.healthapp.data.repository.IllnessRisk>>(emptyList())
    val illnessCatalog: StateFlow<List<com.example.healthapp.data.repository.IllnessRisk>> = _illnessCatalog

    init {
        loadCases()
        loadInventory()
        loadDoctors()
        loadIllnessCatalog()
    }

    private fun loadIllnessCatalog() {
        viewModelScope.launch {
            illnessCatalogRepository.getIllnessCatalog().collect {
                _illnessCatalog.value = it
            }
        }
    }
    
    // ... rest of methods
    private fun loadInventory() {
        viewModelScope.launch {
            pharmacyRepository.getInventory().collect {
                _medicineInventory.value = it
            }
        }
    }

    private fun loadDoctors() {
        viewModelScope.launch {
            doctorRepository.getAllSpecialists().collect { doctors ->
                _uiState.value = _uiState.value.copy(doctors = doctors)
            }
        }
    }

    private fun loadCases() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.getAssignedCases(uid).collect { cases ->
                val stats = CaseStats(
                    total = cases.size,
                    pending = cases.count { it.status == "Pending" },
                    completed = cases.count { it.status == "Visited" || it.status == "Referred" },
                    highRisk = cases.count { it.priority == "Red" }
                )
                _uiState.value = _uiState.value.copy(cases = cases, caseStats = stats, isLoading = false)
            }
        }
    }

    fun registerCase(
        name: String, age: String, gender: String, symptoms: String,
        bp: String, pulse: String, temp: String, spo2: String
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = _uiState.value.copy(error = "Error: User not logged in. Please re-login.")
            return
        }
        val ageInt = age.toIntOrNull() ?: 0
        
        // Basic AI Prioritization Logic (Mock)
        val priority = calculatePriority(symptoms, spo2, temp)

        val newCase = VillageCase(
            villagerName = name,
            age = ageInt,
            gender = gender,
            symptoms = symptoms,
            vitals = Vitals(bp, pulse, temp, spo2),
            priority = priority,
            status = "Pending",
            ashaId = uid
        )

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.registerCase(newCase)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }
    
    fun updateCaseStatus(caseId: String, status: String, notes: String) {
         viewModelScope.launch {
            repository.updateCaseStatus(caseId, status, notes)
         }
    }

    private fun calculatePriority(symptoms: String, spo2: String, temp: String): String {
        val spo2Int = spo2.toIntOrNull() ?: 99
        val tempDouble = temp.toDoubleOrNull() ?: 98.6
        
        return when {
            spo2Int < 90 || symptoms.contains("chest pain", true) || tempDouble > 103 -> "Red"
            spo2Int < 95 || tempDouble > 101 -> "Yellow"
            else -> "Green"
        }
    }
}
