package com.example.healthapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.healthapp.data.repository.TriageResult

@Composable
fun SharedTriageResultCard(result: TriageResult) {
    val cardColor = when (result.priority) {
        "High Risk" -> Color(0xFFFFCDD2) // Red 100
        "Medium Risk" -> Color(0xFFFFF9C4) // Yellow 100
        else -> Color(0xFFC8E6C9) // Green 100
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val title = if(result.patientAge > 0) 
                "${result.priority} - ${result.patientAge}y ${result.patientGender}" 
            else result.priority
            
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Symptoms: ${result.symptoms}", style = MaterialTheme.typography.bodyMedium)
            
            // Extract location if present (dirty hack for demo)
            if (result.symptoms.contains("Location:", ignoreCase = true)) {
                 Spacer(modifier = Modifier.height(4.dp))
                 Text("📍 ${result.symptoms.substringAfter("Location:")}", style = MaterialTheme.typography.labelLarge, color = Color.Blue)
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Text("Action: ${result.recommendedAction}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
