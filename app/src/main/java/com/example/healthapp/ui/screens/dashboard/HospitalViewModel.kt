package com.example.healthapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.repository.DoctorRepository
import com.example.healthapp.data.repository.HospitalRepository
import com.example.healthapp.data.repository.TriageResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class HospitalViewModel @Inject constructor(
    private val repository: HospitalRepository,
    private val doctorRepository: DoctorRepository,
    private val triageRepository: com.example.healthapp.data.repository.TriageRepository,
    private val ashaRepository: com.example.healthapp.data.repository.AshaRepository,
    private val ambulanceRepository: com.example.healthapp.data.repository.AmbulanceRepository,
    private val seedingRepository: com.example.healthapp.data.repository.SeedingRepository,
    private val appointmentRepository: com.example.healthapp.data.repository.AppointmentRepository,
    private val pharmacyRepository: com.example.healthapp.data.repository.PharmacyRepository,
    private val predictionRepository: com.example.healthapp.data.repository.PredictionRepository,
    private val voiceRepository: com.example.healthapp.data.repository.VoiceAssistantRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _dbConnectionStatus = MutableStateFlow<String>("Initializing...")
    val dbConnectionStatus: StateFlow<String> = _dbConnectionStatus

    private val _predictions = MutableStateFlow<List<com.example.healthapp.data.repository.PredictionResult>>(emptyList())
    val predictions: StateFlow<List<com.example.healthapp.data.repository.PredictionResult>> = _predictions

    private val _pendingDrafts = MutableStateFlow<List<com.example.healthapp.data.repository.PrescriptionDraft>>(emptyList())
    val pendingDrafts: StateFlow<List<com.example.healthapp.data.repository.PrescriptionDraft>> = _pendingDrafts

    private val _logs = MutableStateFlow<List<TriageResult>>(emptyList())
    val logs: StateFlow<List<TriageResult>> = _logs
    
    // ASHA Workers
    private val _ashaWorkers = MutableStateFlow<List<com.example.healthapp.data.repository.AshaWorker>>(emptyList())
    val ashaWorkers: StateFlow<List<com.example.healthapp.data.repository.AshaWorker>> = _ashaWorkers
    
    // Appointments
    private val _appointments = MutableStateFlow<List<com.example.healthapp.data.repository.Appointment>>(emptyList())
    val appointments: StateFlow<List<com.example.healthapp.data.repository.Appointment>> = _appointments
    
    // Specialist Management
    private val _specialists = MutableStateFlow<List<com.example.healthapp.data.repository.DoctorProfile>>(emptyList())
    val specialists: StateFlow<List<com.example.healthapp.data.repository.DoctorProfile>> = _specialists

    private val _isDoctorAvailable = MutableStateFlow(false)
    val isDoctorAvailable: StateFlow<Boolean> = _isDoctorAvailable
    
    // Emergency Control
    private val _ambulances = MutableStateFlow<List<com.example.healthapp.data.repository.Ambulance>>(emptyList())
    val ambulances: StateFlow<List<com.example.healthapp.data.repository.Ambulance>> = _ambulances
    
    private val _activeEmergencies = MutableStateFlow<List<TriageResult>>(emptyList())
    val activeEmergencies: StateFlow<List<TriageResult>> = _activeEmergencies

    init {
        viewModelScope.launch {
            seedingRepository.seedDoctors()
            seedingRepository.seedPatients()
            seedingRepository.seedMedicines()
            seedingRepository.seedIllnessCatalog() // NEW
            
            // Wait for seeding then fetch
            doctorRepository.getAllSpecialists().collect {
                _specialists.value = it
            }
        }
        viewModelScope.launch {
            triageRepository.getRealTimeTriageLogs().collect {
                _logs.value = it
            }
        }
        // ASHA Workers
        viewModelScope.launch {
            ashaRepository.getAllAshaWorkers().collect {
                _ashaWorkers.value = it
            }
        }
        // Fleet
        viewModelScope.launch {
            ambulanceRepository.getAmbulanceFleet().collect {
                _ambulances.value = it
            }
        }
        // Emergencies
        viewModelScope.launch {
            ambulanceRepository.getActiveEmergencies().collect {
                _activeEmergencies.value = it
            }
        }
        
        // Appointments
        viewModelScope.launch {
            firestore.collection("appointments")
                .whereEqualTo("status", "Confirmed")
                //.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING) // Skip sort to avoid index issues
                .addSnapshotListener { snapshot, _ ->
                     snapshot?.let {
                         val list = it.toObjects(com.example.healthapp.data.repository.Appointment::class.java)
                         // AI Queue Management: Sort by Risk Priority
                         val sortedList = list.sortedWith(compareBy<com.example.healthapp.data.repository.Appointment> { 
                             when (it.priority) {
                                 "High Risk" -> 0
                                 "Medium Risk" -> 1
                                 "Low Risk" -> 2
                                 else -> 3
                             }
                         }.thenBy { apt -> apt.timeSlot }) // Then by time
                         
                         _appointments.value = sortedList
                     }
                }
        }
        
        // Pharmacy Analytics
        viewModelScope.launch {
            pharmacyRepository.getInventory().collect { inventory ->
                 _predictions.value = predictionRepository.generateForecast(inventory)
            }
        }

        // Pending AI Drafts
        viewModelScope.launch {
            voiceRepository.getPendingDrafts().collect {
                _pendingDrafts.value = it
            }
        }
        
        // Auto-check health on load
        checkDatabaseHealth()
    }
    
    fun toggleSpecialist(id: String, isAvailable: Boolean) {
        Log.d("HospitalViewModel", "Toggling doctor $id to $isAvailable")
        
        // Optimistic Update for instant UI feedback
        _specialists.value = _specialists.value.map { 
            if (it.id == id) it.copy(isAvailable = isAvailable) else it 
        }

        viewModelScope.launch {
            try {
                doctorRepository.updateSpecialistStatus(id, isAvailable)
                Log.d("HospitalViewModel", "Firestore update successful for $id")
            } catch (e: Exception) {
                Log.e("HospitalViewModel", "Failed to update $id", e)
                // Revert on failure
                _specialists.value = _specialists.value.map { 
                    if (it.id == id) it.copy(isAvailable = !isAvailable) else it 
                }
            }
        }
    }
    
    fun assignTriageCase(result: TriageResult, ashaId: String) {
        viewModelScope.launch {
             val villageCase = com.example.healthapp.data.repository.VillageCase(
                 villagerName = "AI: ${result.symptoms.take(15)}...", // Fallback name
                 age = result.patientAge,
                 gender = result.patientGender,
                 symptoms = result.symptoms,
                 priority = if(result.priority.contains("High")) "Red" else if(result.priority.contains("Medium")) "Yellow" else "Green",
                 status = "Pending",
                 ashaId = ashaId,
                 visitNotes = "Assigned from Hospital Triage: ${result.explanation}"
             )
             ashaRepository.registerCase(villageCase)
        }
    }
    
    fun approveDraft(draftId: String) {
        viewModelScope.launch {
            voiceRepository.approveDraft(draftId, "current_doc_id")
        }
    }

    fun addDoctor(name: String, specialization: String, experience: String, schedule: String) {
        viewModelScope.launch {
            val expInt = experience.toIntOrNull() ?: 0
            val newDoctor = com.example.healthapp.data.repository.DoctorProfile(
                name = name,
                specialization = specialization,
                experienceYears = expInt,
                opdSchedule = schedule,
                isAvailable = true // Default to available
            )
            doctorRepository.addDoctor(newDoctor)
            // No need to manually refresh - the real-time listener will auto-update
        }
    }
    
    fun dispatchAmbulance(ambulanceId: String, caseTimestamp: java.util.Date) {
        viewModelScope.launch {
            ambulanceRepository.assignAmbulanceToCase(ambulanceId, caseTimestamp)
            // Refresh fleet
             ambulanceRepository.getAmbulanceFleet().collect {
                _ambulances.value = it
            }
        }
    }

    fun checkDatabaseHealth() {
        viewModelScope.launch {
            _dbConnectionStatus.value = "Checking..."
            try {
                withTimeout(5000L) { // 5 second timeout
                    val testRef = firestore.collection("health_check").document("connection_test")
                    val data = hashMapOf("timestamp" to com.google.firebase.Timestamp.now())
                    testRef.set(data).await()
                    val snapshot = testRef.get().await()
                    if (snapshot.exists()) {
                        _dbConnectionStatus.value = "Connected ✅"
                    } else {
                        _dbConnectionStatus.value = "Read Failed ❌"
                    }
                }
            } catch (e: Exception) {
                Log.e("HospitalViewModel", "DB Check Failed", e)
                _dbConnectionStatus.value = "Error: ${e.message ?: "Timeout"}"
            }
        }
    }
    fun refreshAllData() {
        checkDatabaseHealth()
    }
}
