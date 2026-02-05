package com.example.healthapp.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.repository.Medicine
import com.example.healthapp.data.repository.PharmacyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PharmacyViewModel @Inject constructor(
    private val repository: PharmacyRepository,
    private val predictionRepository: com.example.healthapp.data.repository.PredictionRepository
) : ViewModel() {

    private val _inventory = MutableStateFlow<List<Medicine>>(emptyList())
    val inventory: StateFlow<List<Medicine>> = _inventory

    private val _predictions = MutableStateFlow<List<com.example.healthapp.data.repository.PredictionResult>>(emptyList())
    val predictions: StateFlow<List<com.example.healthapp.data.repository.PredictionResult>> = _predictions

    init {
        loadInventory()
    }

    fun loadInventory() {
        viewModelScope.launch {
            repository.getInventory().collect { list ->
                _inventory.value = list
                // Generate Forecast on inventory update
                _predictions.value = predictionRepository.generateForecast(list)
            }
        }
    }

    fun addMedicine(name: String, stock: String, price: String) {
        viewModelScope.launch {
            val medicine = Medicine(
                name = name,
                stock = stock.toIntOrNull() ?: 0,
                price = price.toDoubleOrNull() ?: 0.0,
                expiryDate = "2026-12-31" // Placeholder
            )
            repository.addMedicine(medicine)
            // loadInventory() // Listener auto-updates
        }
    }
}
