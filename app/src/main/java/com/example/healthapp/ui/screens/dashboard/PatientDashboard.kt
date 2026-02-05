package com.example.healthapp.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthapp.ui.components.AnimatedGradientBackground
import com.example.healthapp.ui.components.HealthAppButton
import com.example.healthapp.ui.components.HealthAppCard
import com.example.healthapp.ui.components.HealthAppTextField
import com.example.healthapp.util.TranslationManager
import com.example.healthapp.ui.components.LanguageSelectorButton

import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDashboard(
    onLogout: () -> Unit,
    viewModel: PatientViewModel = hiltViewModel()
) {
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var showVoiceAssistant by remember { mutableStateOf(false) }
    var symptoms by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    // Translation Watcher (Triggers Recomposition)
    val currentLang by com.example.healthapp.util.TranslationManager.currentLanguage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        TranslationManager.getString("symptom_checker"),
        TranslationManager.getString("find_doctor"),
        TranslationManager.getString("pharmacy")
    )
    
    // Connectivity State
    val specialists by viewModel.specialists.collectAsState()
    val medicines by viewModel.medicineResults.collectAsState()

    var medicineQuery by remember { mutableStateOf("") }
    
    // Scheduling State
    var showBookingDialog by remember { mutableStateOf(false) }
    var selectedDoctorForBooking by remember { mutableStateOf<com.example.healthapp.data.repository.DoctorProfile?>(null) }

    // Pager & Refresh State
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // Sync Tab and Pager
    LaunchedEffect(selectedTab) {
        pagerState.animateScrollToPage(selectedTab)
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    // Refresh Logic (Simulated for MVP as data is real-time)
    fun onRefresh() {
        coroutineScope.launch {
            isRefreshing = true
            kotlinx.coroutines.delay(1500) // Simulate network fetch
            isRefreshing = false
        }
    }

    AnimatedGradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
            HealthAppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(TranslationManager.getString("patient_dashboard"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Welcome, Patient", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            
            // Feature Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        icon = { Icon(if(index==0) Icons.Default.Info else if(index==1) Icons.Default.Person else Icons.Default.Search, contentDescription = null) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Pull-to-Refresh & Pager
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = ::onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> { // Symptom Checker
                                HealthAppCard(color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Column {
                                        Text(TranslationManager.getString("symptom_checker"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        HealthAppTextField(
                                            value = symptoms,
                                            onValueChange = { symptoms = it },
                                            label = "Describe your symptoms...",
                                            leadingIcon = Icons.Default.Info, // Changed from Edit (might be missing) to Info
                                            singleLine = false,
                                            modifier = Modifier.height(100.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Voice & AI Buttons Row
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            AIVoiceButton(
                                                onSpeechResult = { text -> 
                                                    symptoms = text 
                                                    viewModel.analyzeSymptoms("25", "Male", text)
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            
                                            HealthAppButton(
                                                text = "Analyze Risk",
                                                onClick = { viewModel.analyzeSymptoms("25", "Male", symptoms) },
                                                isLoading = uiState.isLoading,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        
                                        // Emergency Button
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = { viewModel.triggerEmergency("25", "Male") },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("SOS - EMERGENCY", fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }

                                uiState.error?.let {
                                    Text("Error: $it", color = MaterialTheme.colorScheme.error)
                                }

                                uiState.triageResult?.let { result ->
                                    PremiumTriageCard(result)
                                }
                            }
                            1 -> { // Find Doctor
                                HealthAppCard(color = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Column {
                                        Text(TranslationManager.getString("find_doctor"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("${TranslationManager.getString("available")}: ${uiState.availableDoctors}", style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Availability Toggle
                                var showAvailableOnly by remember { mutableStateOf(false) }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                ) {
                                    Text("Show Available Only", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Switch(
                                        checked = showAvailableOnly,
                                        onCheckedChange = { showAvailableOnly = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF43A047))
                                    )
                                }
                                
                                val filteredSpecialists = specialists.filter { !showAvailableOnly || it.isAvailable }
                                
                                if (filteredSpecialists.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No doctors found.", color = Color.Gray)
                                    }
                                } else {
                                    filteredSpecialists.forEach { doc ->
                                        HealthAppCard {
                                            Column {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column {
                                                        Text(doc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                                        Text(doc.specialization, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    
                                                    // Status Badge
                                                    Surface(
                                                        color = if(doc.isAvailable) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                        shape = MaterialTheme.shapes.small
                                                    ) {
                                                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            Box(modifier = Modifier.size(8.dp).background(if(doc.isAvailable) Color(0xFF43A047) else Color.Red, androidx.compose.foundation.shape.CircleShape))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                if(doc.isAvailable) "Available" else "Offline",
                                                                color = if(doc.isAvailable) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("OPD: ${doc.opdSchedule}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                                
                                                Spacer(modifier = Modifier.height(12.dp))
                                                
                                                Button(
                                                    onClick = { 
                                                        selectedDoctorForBooking = doc
                                                        showBookingDialog = true
                                                    },
                                                    enabled = doc.isAvailable,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.primary,
                                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                        disabledContentColor = Color.Gray
                                                    )
                                                ) {
                                                    Text(if(doc.isAvailable) TranslationManager.getString("book") else "Currently Unavailable")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> { // Pharmacy
                                HealthAppCard(color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Column {
                                        Text(TranslationManager.getString("pharmacy"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        HealthAppTextField(
                                            value = medicineQuery,
                                            onValueChange = { 
                                                medicineQuery = it 
                                                viewModel.searchMedicine(it)
                                            },
                                            label = "Search Medicines...",
                                            leadingIcon = Icons.Default.Search
                                        )
                                    }
                                }
                                
                                medicines.forEach { med ->
                                    HealthAppCard {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Column {
                                                Text(med.name, fontWeight = FontWeight.Bold)
                                                Text("Stock: ${med.stock}", style = MaterialTheme.typography.bodySmall, color = if(med.stock > 10) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                            }
                                            Text("₹${med.price}", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } // End Inner Container
        } // End Main Column
            
            // Voice Assistant FAB
            FloatingActionButton(
                onClick = { showVoiceAssistant = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)
            ) {
                 Icon(Icons.Default.Mic, contentDescription = "Voice Assistant", tint = Color.White)
            }
        } // End Box
    } // End AnimatedGradientBackground

            if (showBookingDialog && selectedDoctorForBooking != null) {
                AppointmentSchedulingDialog(
                    doctor = selectedDoctorForBooking!!,
                    onDismiss = { showBookingDialog = false },
                    onConfirm = { timeSlot -> 
                        viewModel.bookAppointment(selectedDoctorForBooking!!.id, selectedDoctorForBooking!!.name, timeSlot)
                        showBookingDialog = false
                    }
                )
            }
            
            if (showVoiceAssistant) {
                ModalBottomSheet(onDismissRequest = { showVoiceAssistant = false }) {
                    com.example.healthapp.ui.screens.voice.VoiceAssistantScreen(
                        onDismiss = { showVoiceAssistant = false }
                    )
                }
            }
}

@Composable
fun AppointmentSchedulingDialog(
    doctor: com.example.healthapp.data.repository.DoctorProfile,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val slots = listOf("10:00 AM", "10:30 AM", "11:00 AM", "11:30 AM", "12:00 PM", "12:30 PM", "02:00 PM", "02:30 PM", "03:00 PM", "03:30 PM")
    var selectedSlot by remember { mutableStateOf(slots[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Book Appointment") },
        text = {
            Column {
                Text("Doctor: ${doctor.name}", fontWeight = FontWeight.Bold)
                Text("Specialization: ${doctor.specialization}", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Time Slot:", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simple Grid or FlowRow for slots
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Split into rows of 2 for simplicity
                    slots.chunked(2).forEach { rowSlots ->
                         Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                             rowSlots.forEach { slot ->
                                 FilterChip(
                                     selected = selectedSlot == slot,
                                     onClick = { selectedSlot = slot },
                                     label = { Text(slot) },
                                     modifier = Modifier.weight(1f)
                                 )
                             }
                         }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedSlot) }) { Text("Confirm Booking") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PremiumTriageCard(result: com.example.healthapp.data.repository.TriageResult) {
    val (color, icon) = when (result.priority) {
        "High Risk" -> Color(0xFFFFEBEE) to Icons.Default.Warning
        "Medium Risk" -> Color(0xFFFFFDE7) to Icons.Default.Info
        else -> Color(0xFFE8F5E9) to Icons.Default.CheckCircle
    }
    val textColor = when (result.priority) {
        "High Risk" -> Color(0xFFB71C1C)
        "Medium Risk" -> Color(0xFFF57F17)
        else -> Color(0xFF1B5E20)
    }

    HealthAppCard(color = color) {
        Column {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("AI Health Assessment", style = MaterialTheme.typography.labelLarge, color = textColor.copy(alpha = 0.8f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(result.priority.uppercase(), style = MaterialTheme.typography.headlineMedium, color = textColor, fontWeight = FontWeight.Bold)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = textColor.copy(alpha = 0.2f))
            
            // Explanation
            Text("Analysis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(result.explanation, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Immediate Solution
            HealthAppCard(color = Color.White.copy(alpha = 0.6f)) {
                Column {
                    Text("💡 Immediate Solution", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(result.immediateSolutions, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Preventive Measures
            HealthAppCard(color = Color.White.copy(alpha = 0.6f)) {
                Column {
                    Text("🛡️ Preventive Measures", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(result.preventiveMeasures, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Text("Recommended Action: ", fontWeight = FontWeight.Bold)
                Text(result.recommendedAction)
            }
        }
    }
}

@Composable
fun AIVoiceButton(
    onSpeechResult: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        if (!results.isNullOrEmpty()) {
            onSpeechResult(results[0])
        }
    }

    IconButton(
        onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your symptoms...")
            }
            try {
                launcher.launch(intent)
            } catch (e: Exception) {
                // Handle error
            }
        },
        modifier = modifier
    ) {
        Icon(Icons.Default.Info, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
    }
}
