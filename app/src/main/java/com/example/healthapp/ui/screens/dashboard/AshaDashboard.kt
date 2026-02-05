package com.example.healthapp.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthapp.data.repository.VillageCase
import com.example.healthapp.ui.components.HealthAppButton
import com.example.healthapp.ui.components.HealthAppCard
import com.example.healthapp.ui.components.HealthAppTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AshaDashboard(
    onLogout: () -> Unit,
    viewModel: AshaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val medicines by viewModel.medicineInventory.collectAsState()
    val illnessCatalog by viewModel.illnessCatalog.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Tabs
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("My Cases", "Find Doctor", "Pharmacy", "Illness Guide")

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Case", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            TopAppBar(
                title = { 
                    Column {
                        Text("Village Health Saathi", fontWeight = FontWeight.Bold)
                        Text("ASHA Portal • Offline Ready", style = MaterialTheme.typography.labelSmall)
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
            
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                when (selectedTab) {
                    0 -> CasesTab(uiState, onUpdateStatus = { id, status -> viewModel.updateCaseStatus(id, status, "User Update") })
                    1 -> DoctorsTab(uiState.doctors)
                    2 -> PharmacyTab(medicines)
                    3 -> IllnessGuideTab(illnessCatalog)
                }
            }
        }
    }

    if (showAddDialog) {
        RegisterCaseDialog(
            onDismiss = { showAddDialog = false },
            onRegister = { name, age, gender, symptoms, bp, pulse, temp, spo2 ->
                viewModel.registerCase(name, age, gender, symptoms, bp, pulse, temp, spo2)
                showAddDialog = false
            }
        )
    }
}

// ... CasesTab, StatCard, DoctorsTab, DoctorAvailabilityCard, PharmacyTab ...

@Composable
fun IllnessGuideTab(catalog: List<com.example.healthapp.data.repository.IllnessRisk>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            HealthAppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Illness Risk Guide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Standardized protocols for triage", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Group by Risk Level
        val grouped = catalog.groupBy { it.riskLevel }
        val order = listOf("High", "Medium", "Low")
        
        order.forEach { level ->
            val illnesses = grouped[level] ?: emptyList()
            if (illnesses.isNotEmpty()) {
                item {
                    val color = when(level) {
                        "High" -> Color(0xFFB71C1C)
                        "Medium" -> Color(0xFFF57F17)
                        else -> Color(0xFF1B5E20)
                    }
                    Text("$level Risk Conditions", color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                }
                
                items(illnesses) { illness ->
                    IllnessCard(illness)
                }
            }
        }
    }
}

@Composable
fun IllnessCard(illness: com.example.healthapp.data.repository.IllnessRisk) {
    val borderColor = when(illness.riskLevel) {
        "High" -> Color.Red
        "Medium" -> Color(0xFFFFA000)
        else -> Color(0xFF43A047)
    }
    
    HealthAppCard(border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))) {
        Column {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(illness.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if(illness.isEmergency) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) { Text("EMERGENCY", color = Color.White) }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(illness.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text(" Symptoms: ${illness.symptoms.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(" Action: ${illness.action}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// ... VillageCaseCard, RegisterCaseDialog ...

@Composable
fun CasesTab(uiState: AshaUiState, onUpdateStatus: (String, String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Stats
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Total", uiState.caseStats.total.toString(), Color.Blue, Modifier.weight(1f))
                StatCard("Pending", uiState.caseStats.pending.toString(), Color(0xFFFFA000), Modifier.weight(1f))
                StatCard("Done", uiState.caseStats.completed.toString(), Color(0xFF43A047), Modifier.weight(1f))
            }
        }
        
        item {
            Text("Assigned Cases", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        items(uiState.cases) { case ->
            VillageCaseCard(case, onUpdateStatus)
        }
    }
}

@Composable
fun StatCard(title: String, value: String, color: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Black)
        }
    }
}

@Composable
fun DoctorsTab(doctors: List<com.example.healthapp.data.repository.DoctorProfile>) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredDoctors = doctors.filter { 
        it.name.contains(searchQuery, ignoreCase = true) || it.specialization.contains(searchQuery, ignoreCase = true)
    }

    Column {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by Name or Specialization") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredDoctors) { doc ->
                DoctorAvailabilityCard(doc)
            }
        }
    }
}

@Composable
fun DoctorAvailabilityCard(doctor: com.example.healthapp.data.repository.DoctorProfile) {
    HealthAppCard(
        border = if (doctor.isAvailable) BorderStroke(1.dp, Color(0xFF43A047)) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(doctor.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(doctor.specialization, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(doctor.opdSchedule, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (doctor.isAvailable) {
                    Badge(containerColor = Color(0xFF43A047)) { Text("AVAILABLE", color = Color.White) }
                } else {
                    Badge(containerColor = MaterialTheme.colorScheme.error) { Text("OFFLINE", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun PharmacyTab(medicines: List<com.example.healthapp.data.repository.Medicine>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Pharmacy Stock (Live)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        
        items(medicines) { med ->
            HealthAppCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(med.name, fontWeight = FontWeight.Bold)
                        Text("Exp: ${med.expiryDate}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        val color = if(med.stock < 10) Color.Red else Color(0xFF43A047)
                        Text("${med.stock} Units", color = color, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // For SmallTopAppBar
@Composable
fun VillageCaseCard(case: VillageCase, onUpdateStatus: (String, String) -> Unit) {
    val color = when (case.priority) {
        "Red" -> Color(0xFFFFEBEE)
        "Yellow" -> Color(0xFFFFFDE7)
        else -> Color(0xFFE8F5E9)
    }
    
    // Status Logic
    val isCompleted = case.status == "Visited" || case.status == "Referred"
    
    HealthAppCard(color = if(isCompleted) Color(0xFFF5F5F5) else color) {
        Column {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(case.villagerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (isCompleted) {
                     Icon(Icons.Default.Check, contentDescription = "Done", tint = Color(0xFF43A047))
                } else {
                    Text(case.priority, fontWeight = FontWeight.Bold, color = if(case.priority=="Red") Color.Red else Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${case.age} yrs / ${case.gender}", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Symptoms: ${case.symptoms}", style = MaterialTheme.typography.bodyMedium)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("BP: ${case.vitals.bp}", style = MaterialTheme.typography.labelSmall)
                Text("SpO2: ${case.vitals.spo2}%", style = MaterialTheme.typography.labelSmall)
                Text("Temp: ${case.vitals.temperature}°F", style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (case.status == "Pending") {
                    OutlinedButton(
                        onClick = { onUpdateStatus(case.id, "Visited") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Mark Visited")
                    }
                    Button(
                        onClick = { onUpdateStatus(case.id, "Referred") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000))
                    ) {
                        Text("Refer Hosp.")
                    }
                } else {
                    Text("Status: ${case.status}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun RegisterCaseDialog(
    onDismiss: () -> Unit,
    onRegister: (String, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var symptoms by remember { mutableStateOf("") }
    var bp by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var temp by remember { mutableStateOf("") }
    var spo2 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Village Case") },
        text = {
            Column(modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                HealthAppTextField(value = name, onValueChange = { name = it }, label = "Villager Name")
                Row {
                    HealthAppTextField(value = age, onValueChange = { age = it }, label = "Age", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    // Gender simplified
                    HealthAppTextField(value = gender, onValueChange = { gender = it }, label = "Gender", modifier = Modifier.weight(1f))
                }
                HealthAppTextField(value = symptoms, onValueChange = { symptoms = it }, label = "Symptoms")
                
                Text("Vitals", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top=8.dp))
                Row {
                    HealthAppTextField(value = bp, onValueChange = { bp = it }, label = "BP", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    HealthAppTextField(value = spo2, onValueChange = { spo2 = it }, label = "SpO2", modifier = Modifier.weight(1f))
                }
                Row {
                    HealthAppTextField(value = temp, onValueChange = { temp = it }, label = "Temp", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(4.dp))
                    HealthAppTextField(value = pulse, onValueChange = { pulse = it }, label = "Pulse", modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onRegister(name, age, gender, symptoms, bp, pulse, temp, spo2) }) { Text("Submit") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
