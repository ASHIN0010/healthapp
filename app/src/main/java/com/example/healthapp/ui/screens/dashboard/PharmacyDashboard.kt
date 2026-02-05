package com.example.healthapp.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthapp.data.repository.Medicine
import com.example.healthapp.ui.components.AnimatedGradientBackground
import com.example.healthapp.ui.components.HealthAppCard
import com.example.healthapp.ui.components.HealthAppTextField

import androidx.compose.ui.graphics.Color

@Composable
fun PharmacyDashboard(
    onLogout: () -> Unit,
    viewModel: PharmacyViewModel = hiltViewModel()
) {
    val inventory by viewModel.inventory.collectAsState()
    val predictions by viewModel.predictions.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Inventory", "AI Forecast")

    AnimatedGradientBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                HealthAppCard {
                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                         Row(verticalAlignment = Alignment.CenterVertically) {
                             Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                             Spacer(modifier = Modifier.width(8.dp))
                             Text("Pharmacy Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                         }
                         IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                        }
                     }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                when(selectedTab) {
                    0 -> { // Inventory
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(inventory) { medicine ->
                                MedicineCard(medicine = medicine)
                            }
                        }
                    }
                    1 -> { // AI Forecast
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                // Seasonal Insight
                                HealthAppCard(color = MaterialTheme.colorScheme.primaryContainer) {
                                    Column {
                                        Text("Seasonal Insight: Monsoon", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text("Predicted spike in Dengue and Viral Fever cases. Ensure stock of Paracetamol and Antibiotics.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                            
                            items(predictions) { pred ->
                                PredictionCard(pred)
                            }
                        }
                    }
                }
            }
            
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Medicine", tint = Color.White)
                }
            }
            
            if (showAddDialog) {
                AddMedicineDialog(
                    onDismiss = { showAddDialog = false },
                    onAdd = { name, stock, price ->
                        viewModel.addMedicine(name, stock, price)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun PredictionCard(pred: com.example.healthapp.data.repository.PredictionResult) {
    val riskColor = when(pred.riskLevel) {
        "Shortage Risk" -> MaterialTheme.colorScheme.error
        "Overstock Risk" -> Color(0xFFFFA000) // Amber
        else -> Color(0xFF43A047) // Green
    }
    
    HealthAppCard(
        border = if(pred.riskLevel != "Normal") BorderStroke(1.dp, riskColor) else null
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(pred.medicineName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(pred.riskLevel, color = riskColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Current: ${pred.currentStock}", style = MaterialTheme.typography.bodySmall)
                Text("Predicted Demand: ${pred.predictedDemand}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
            if (pred.seasonTag != "None") {
                 Spacer(modifier = Modifier.height(4.dp))
                 Text("Tag: ${pred.seasonTag} - ${pred.explanation}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
             if (pred.riskLevel == "Shortage Risk") {
                 Spacer(modifier = Modifier.height(8.dp))
                 Text("Recommended Restock: +${pred.recommendedStock - pred.currentStock} units", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun MedicineCard(medicine: Medicine) {
    val isLowStock = medicine.stock < 10
    val cardColors = if (isLowStock) 
        MaterialTheme.colorScheme.errorContainer
    else 
        MaterialTheme.colorScheme.surface

    HealthAppCard(color = cardColors) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(medicine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isLowStock) {
                    Icon(Icons.Default.Warning, contentDescription = "Low Stock", tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                 Text("Stock: ${medicine.stock}", style = MaterialTheme.typography.bodyMedium, fontWeight = if(isLowStock) FontWeight.Bold else FontWeight.Normal)
                 Text("₹${medicine.price}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            
            if (isLowStock) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                Text("LOW STOCK WARNING", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AddMedicineDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medicine", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                HealthAppTextField(value = name, onValueChange = { name = it }, label = "Name")
                Spacer(modifier = Modifier.height(8.dp))
                HealthAppTextField(value = stock, onValueChange = { stock = it }, label = "Stock", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                Spacer(modifier = Modifier.height(8.dp))
                HealthAppTextField(value = price, onValueChange = { price = it }, label = "Price", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(name, stock, price) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    )
}
