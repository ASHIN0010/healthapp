package com.example.healthapp.ui.screens.voice

import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val repository: VoiceAssistantRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _analysisResult = MutableStateFlow<AiAnalysisResult?>(null)
    val analysisResult: StateFlow<AiAnalysisResult?> = _analysisResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // TTS Manager
    private val ttsManager = com.example.healthapp.util.TextToSpeechManager(context)
    private val _isSpeaking = ttsManager.isSpeaking
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    fun analyze(text: String) {
        viewModelScope.launch {
            _isLoading.value = true
            ttsManager.stop() // Stop previous if any
            
            // "Dr. User" or similar role can be passed dynamically later
            repository.analyzeSymptoms(text, "current_user_id", "Patient").collect { result ->
                result.onSuccess { 
                    _analysisResult.value = it 
                    _isLoading.value = false
                    
                    // Auto-speak the advice for accessibility
                    speakResponse(it.preventionAdvice)
                }
                result.onFailure {
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun speakResponse(text: String) {
        ttsManager.speak(text)
    }
    
    fun stopSpeaking() {
        ttsManager.stop()
    }
    
    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
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
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    
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
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Dr. Consultation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Dynamic State UI
            if (spokenText.isBlank()) {
                // Idle State
                Text("Tap the mic to start consultation", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))
                
                FloatingActionButton(
                    onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your symptoms...")
                        }
                        speechLauncher.launch(intent)
                    },
                    modifier = Modifier.size(100.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Speak", modifier = Modifier.size(48.dp))
                }
            } else {
                // Processing or Result State
                HealthAppCard {
                    Column {
                        Text("You said:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(spokenText, style = MaterialTheme.typography.bodyLarge, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Doctor is analyzing...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                } 
                else if (analysis != null) {
                    val result = analysis!!
                    
                    // Risk Level Banner
                    val riskColor = when(result.riskLevel) {
                        "High Risk" -> Color.Red
                        "Medium Risk" -> Color(0xFFFFA000)
                        else -> Color(0xFF43A047)
                    }
                    
                    Surface(
                        color = riskColor.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, riskColor),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if(result.riskLevel == "High Risk") Icon(Icons.Default.Warning, null, tint = riskColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(result.riskLevel.uppercase(), color = riskColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Audio Control
                    Button(
                        onClick = { if(isSpeaking) viewModel.stopSpeaking() else viewModel.speakResponse(result.preventionAdvice) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(if(isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if(isSpeaking) "Stop Speaking" else "Hear Doctor's Advice")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Advice Card
                    HealthAppCard(color = Color(0xFFE8F5E9)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Prevention Advice", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(result.preventionAdvice, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Clinical Draft (Hidden by default or smaller)
                    HealthAppCard(color = Color.White) {
                         Column {
                            Text("Clinical Note (Draft)", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(result.rxDraft, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                         }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Start Over
                    OutlinedButton(onClick = { spokenText = "" }) {
                        Text("New Consultation")
                    }
                }
            }
        }
    }
}

