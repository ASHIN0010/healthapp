package com.example.healthapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.healthapp.util.TranslationManager

@Composable
fun LanguageSelectorButton(
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentLangCode by TranslationManager.currentLanguage.collectAsState()
    val currentLang = TranslationManager.supportedLanguages.find { it.code == currentLangCode }

    IconButton(onClick = { showDialog = true }, modifier = modifier) {
        Text(currentLang?.flag ?: "🌐", style = MaterialTheme.typography.titleLarge)
    }

    if (showDialog) {
        LanguageSelectionDialog(onDismiss = { showDialog = false })
    }
}

@Composable
fun LanguageSelectionDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    TranslationManager.getString("select_language"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TranslationManager.supportedLanguages) { lang ->
                        LanguageItem(lang) {
                            TranslationManager.setLanguage(lang.code)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageItem(language: TranslationManager.Language, onClick: () -> Unit) {
    val currentCode by TranslationManager.currentLanguage.collectAsState()
    val isSelected = currentCode == language.code

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(language.flag, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(16.dp))
            Text(language.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
