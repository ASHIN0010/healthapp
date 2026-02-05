package com.example.healthapp.ui.screens.voice

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.repository.AiAnalysisResult
import com.example.healthapp.data.repository.VoiceAssistantRepository
import com.example.healthapp.ui.components.HealthAppCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VoiceAssistantViewModel @Inject constructor(
    private val repository: VoiceAssistantRepository
) : ViewModel() {

    private val _analysisResult = MutableStateFlow<AiAnalysisResult?>(null)
    val analysisResult: StateFlow<AiAnalysisResult?> = _analysisResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun analyze(text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.analyzeSymptoms(text, "current_user_id", "Current User").collect { result ->
                result.onSuccess { 
                    _analysisResult.value = it 
                    _isLoading.value = false
                }
                result.onFailure {
                    _isLoading.value = false
                }
            }
        }
    }
}

@Composable
fun VoiceAssistantScreen(
    onDismiss: () -> Unit,
    viewModel: VoiceAssistantViewModel = hiltViewModel()
) {
    var spokenText by remember { mutableStateOf("") }
    val analysis by viewModel.analysisResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText = results?.get(0) ?: ""
            if (spokenText.isNotBlank()) {
                viewModel.analyze(spokenText)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Voice Health Assistant", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            
            // Mic Button
            FloatingActionButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your symptoms...")
                    }
                    speechLauncher.launch(intent)
                },
                modifier = Modifier.size(80.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Speak", modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tap to Speak Symptoms", style = MaterialTheme.typography.bodyLarge)
            
            if (spokenText.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                HealthAppCard {
                    Text("You said:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Text(spokenText, style = MaterialTheme.typography.bodyLarge)
                }
            }
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator()
                Text("Analyzing with AI...", style = MaterialTheme.typography.bodySmall)
            }
            
            analysis?.let { result ->
                Spacer(modifier = Modifier.height(24.dp))
                
                // Prevention Card
                HealthAppCard(color = Color(0xFFE8F5E9)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Prevention Advice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(result.preventionAdvice, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Rx Draft Card
                HealthAppCard(color = Color(0xFFFFF8E1)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pending Doctor Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFFFA000))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Draft Prescription:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text(result.rxDraft, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("⚠️ This is NOT a final prescription. A doctor must review and approve this.", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                    }
                }
            }
        }
    }
}
