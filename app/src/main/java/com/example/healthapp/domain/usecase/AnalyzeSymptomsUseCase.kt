package com.example.healthapp.domain.usecase

import com.example.healthapp.data.repository.TriageRepository
import com.example.healthapp.data.repository.TriageResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AnalyzeSymptomsUseCase @Inject constructor(
    private val repository: TriageRepository
) {
    suspend operator fun invoke(
        age: Int,
        gender: String,
        symptoms: String
    ): Flow<Result<TriageResult>> {
        return repository.analyzeSymptoms(age, gender, symptoms)
    }
}
