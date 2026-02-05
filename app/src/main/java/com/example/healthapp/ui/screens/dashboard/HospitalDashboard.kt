package com.example.healthapp.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Phone
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthapp.data.repository.TriageResult
import com.example.healthapp.ui.components.AnimatedGradientBackground
import com.example.healthapp.ui.components.HealthAppCard
import com.example.healthapp.util.TranslationManager
import com.example.healthapp.ui.components.LanguageSelectorButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.filled.Check
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDashboard(
    onLogout: () -> Unit,
    viewModel: HospitalViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ambulanceDrivers by viewModel.ambulances.collectAsState()
    val activeEmergencies by viewModel.activeEmergencies.collectAsState()
    
    val dbStatus by viewModel.dbConnectionStatus.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    
    // Translation Watcher
    val currentLang by TranslationManager.currentLanguage.collectAsState()
    
    // Doctor Filters
    val specialists by viewModel.specialists.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var showAvailableOnly by remember { mutableStateOf(false) }
    var showAddDoctorDialog by remember { mutableStateOf(false) }
    val specializationFilters = listOf("All", "Cardiologist", "Pediatrician", "Gynecologist", "Orthopedic", "General Physician")

    // Refresh State
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    fun onRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            viewModel.refreshAllData() // Assuming this function exists in your ViewModel
            kotlinx.coroutines.delay(1500) // Simulate refresh
            isRefreshing = false
        }
    }

    AnimatedGradientBackground {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = ::onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Header
                HealthAppCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(TranslationManager.getString("hospital_dashboard"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Dr. User", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LanguageSelectorButton()
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = onLogout) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // System Status
                HealthAppCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("System Status", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(dbStatus, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                        Button(onClick = { viewModel.checkDatabaseHealth() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                            Text("Test Connection")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Upcoming Appointments
                com.example.healthapp.ui.components.SectionHeader(title = TranslationManager.getString("upcoming_appointments"))
                
                if (appointments.isEmpty()) {
                    Text("No upcoming appointments.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(appointments) { appt ->
                            val priorityColor = when(appt.priority) {
                                "High Risk" -> MaterialTheme.colorScheme.error
                                "Medium Risk" -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.primary
                            }
                            HealthAppCard(
                                border = if(appt.priority == "High Risk") BorderStroke(1.dp, MaterialTheme.colorScheme.error) else null
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(appt.timeSlot, fontWeight = FontWeight.Bold, color = priorityColor)
                                            if (appt.priority == "High Risk") {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("⚠️ AI PRIORITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(appt.patientName, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Dr. ${appt.doctorName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
                                        Text(appt.status, color = Color(0xFF43A047), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Doctors Management
                com.example.healthapp.ui.components.SectionHeader(title = TranslationManager.getString("doctors_management"))
                
                Button(
                    onClick = { showAddDoctorDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Doctor")
                }
    
                ScrollableTabRow(
                    selectedTabIndex = specializationFilters.indexOf(selectedFilter),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = {}
                ) {
                    specializationFilters.forEach { filter ->
                        InputChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter) },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
    
                val filteredSpecialists = if (selectedFilter == "All") specialists else specialists.filter { it.specialization == selectedFilter }
                
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSpecialists, key = { it.id }) { doctor ->
                        HealthAppCard {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(doctor.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(doctor.specialization, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    
                                    Switch(
                                        checked = doctor.isAvailable,
                                        onCheckedChange = { viewModel.toggleSpecialist(doctor.id, it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF43A047))
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("OPD: ${doctor.opdSchedule}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        if(doctor.isAvailable) "Accepting" else "Off Duty",
                                        color = if(doctor.isAvailable) Color(0xFF43A047) else Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pharmacy Analytics
                val predictions by viewModel.predictions.collectAsState()
                com.example.healthapp.ui.components.SectionHeader(title = "Pharmacy Analytics (High Risk)")
                
                if (predictions.any { it.riskLevel != "Normal" }) {
                    LazyColumn(modifier = Modifier.height(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((predictions.filter { it.riskLevel != "Normal" })) { pred ->
                            // Inline Card Logic since PredictionCard is in PharmacyDashboard (to avoid dependency or duplicate)
                            val riskColor = if(pred.riskLevel == "Shortage Risk") MaterialTheme.colorScheme.error else Color(0xFFFFA000)
                            HealthAppCard(border = BorderStroke(1.dp, riskColor)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(pred.medicineName, fontWeight = FontWeight.Bold)
                                        Text("${pred.riskLevel}", color = riskColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Current: ${pred.currentStock}", style = MaterialTheme.typography.bodySmall)
                                        Text("Rec: ${pred.recommendedStock}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                     Text("No pharmacy risks detected.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }

                // Live Triage Queue
                val triageLogs by viewModel.logs.collectAsState()
                val ashaWorkers by viewModel.ashaWorkers.collectAsState()
                var showAssignDialog by remember { mutableStateOf<TriageResult?>(null) }
                
                com.example.healthapp.ui.components.SectionHeader(title = "Live AI Triage Queue")
                
                if (triageLogs.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.height(200.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(triageLogs) { log ->
                            HealthAppCard(color = if(log.priority == "High Risk") Color(0xFFFFEBEE) else Color.White) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                     Column(modifier = Modifier.weight(1f)) {
                                         Text("Symptoms: ${log.symptoms.take(30)}...", fontWeight = FontWeight.Bold)
                                         Text("Risk: ${log.priority}", color = if(log.priority=="High Risk") Color.Red else Color.Black)
                                     }
                                     Button(onClick = { showAssignDialog = log }) {
                                         Text("Assign ASHA")
                                     }
                                }
                            }
                        }
                    }
                } else {
                    Text("No active triage logs.", color = Color.Gray)
                }
                
                if (showAssignDialog != null) {
                    AlertDialog(
                        onDismissRequest = { showAssignDialog = null },
                        title = { Text("Assign Case to ASHA") },
                        text = {
                            Column {
                                Text("Select ASHA Worker:")
                                Spacer(modifier = Modifier.height(8.dp))
                                ashaWorkers.forEach { worker ->
                                    Button(
                                        onClick = { 
                                            viewModel.assignTriageCase(showAssignDialog!!, worker.id)
                                            showAssignDialog = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("${worker.name} (${worker.activeCases} active)")
                                    }
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = { TextButton(onClick = { showAssignDialog = null }) { Text("Cancel") } }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // AI Prescription Drafts
                val pendingDrafts by viewModel.pendingDrafts.collectAsState()
                com.example.healthapp.ui.components.SectionHeader(title = "Pending AI Prescriptions")
                
                if (pendingDrafts.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.height(150.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(pendingDrafts) { draft ->
                             HealthAppCard(color = Color(0xFFFFF8E1)) {
                                 Column {
                                     Text("Patient: ${draft.patientName}", fontWeight = FontWeight.Bold)
                                     Text("Rx: ${draft.content}", style = MaterialTheme.typography.bodyMedium)
                                     Spacer(modifier = Modifier.height(8.dp))
                                     Button(
                                         onClick = { viewModel.approveDraft(draft.id) },
                                         colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                                         modifier = Modifier.fillMaxWidth()
                                     ) {
                                         Text("Approve Draft")
                                     }
                                 }
                             }
                        }
                    }
                } else {
                    Text("No pending drafts.", color = Color.Gray)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Emergency Control
                com.example.healthapp.ui.components.SectionHeader(title = TranslationManager.getString("emergency_control"))
                
                if (activeEmergencies.isNotEmpty()) {
                     activeEmergencies.forEach { emergency ->
                         HealthAppCard(color = Color(0xFFFFEBEE)) {
                             Column {
                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                     Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                                     Spacer(modifier = Modifier.width(8.dp))
                                     Text("CRITICAL ALERT", color = Color.Red, fontWeight = FontWeight.Black)
                                 }
                                 Spacer(modifier = Modifier.height(8.dp))
                                 Text("Patient: ${emergency.patientAge} Y / ${emergency.patientGender}")
                                 Text("Symptoms: ${emergency.symptoms}")
                                 Text("Priority: ${emergency.priority}")
                                 Spacer(modifier = Modifier.height(8.dp))
                                 
                                 val availableAmbulance = ambulanceDrivers.find { it.status == "Available" }
                                 Button(
                                     onClick = { availableAmbulance?.let { viewModel.dispatchAmbulance(it.id, emergency.timestamp) } },
                                     colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                     enabled = availableAmbulance != null,
                                     modifier = Modifier.fillMaxWidth()
                                 ) {
                                     Text(if (availableAmbulance != null) "Dispatch Amb. ${availableAmbulance.plateNumber}" else "No Ambulance Available")
                                 }
                             }
                         }
                         Spacer(modifier = Modifier.height(8.dp))
                     }
                } else {
                    Text("No active emergencies.", color = Color.Gray)
                }
    
                Spacer(modifier = Modifier.height(16.dp))
                
            // Ambulance Fleet Status
            Text("Ambulance Fleet Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(ambulanceDrivers) { driver ->
                    HealthAppCard {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Ambulance #${driver.plateNumber}", fontWeight = FontWeight.Bold)
                                Text("Driver: ${driver.driverName}", style = MaterialTheme.typography.bodySmall)
                                Text("Contact: ${driver.driverContact}", style = MaterialTheme.typography.labelSmall, color = Color.Gray) 
                            }
                            Text(
                                driver.status, 
                                color = if(driver.status == "Available") Color(0xFF43A047) else Color(0xFFFFA000),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } // End Column
      } // End PullToRefresh
    } // End AnimatedGradientBackground
    
    if (showAddDoctorDialog) {
         AddDoctorDialog(
             onDismiss = { showAddDoctorDialog = false },
             onAdd = { name, spec, exp, schedule ->
                 viewModel.addDoctor(name, spec, exp, schedule)
                 showAddDoctorDialog = false
             }
         )
    }
}

@Composable
fun AddDoctorDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("Dr. ") }
    var specialization by remember { mutableStateOf("General Physician") }
    var experience by remember { mutableStateOf("") }
    var schedule by remember { mutableStateOf("Mon-Fri: 9AM-1PM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Doctor") },
        text = {
            Column {
                com.example.healthapp.ui.components.HealthAppTextField(value = name, onValueChange = { name = it }, label = "Doctor Name")
                Spacer(modifier = Modifier.height(8.dp))
                com.example.healthapp.ui.components.HealthAppTextField(value = specialization, onValueChange = { specialization = it }, label = "Specialization")
                Spacer(modifier = Modifier.height(8.dp))
                com.example.healthapp.ui.components.HealthAppTextField(value = experience, onValueChange = { experience = it }, label = "Experience (Years)")
                Spacer(modifier = Modifier.height(8.dp))
                com.example.healthapp.ui.components.HealthAppTextField(value = schedule, onValueChange = { schedule = it }, label = "OPD Schedule")
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, specialization, experience, schedule) }) {
                Text("Add Doctor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
