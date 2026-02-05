package com.example.healthapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.random.Random

class SeedingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val illnessCatalogRepository: IllnessCatalogRepository
) {
    suspend fun seedAll() {
        seedDoctors()
        seedPatients()
        seedMedicines()
        seedIllnessCatalog()
    }

    suspend fun seedIllnessCatalog() {
        illnessCatalogRepository.seedInitialCatalog()
    }

    suspend fun seedDoctors() {
        val markerDoc = firestore.collection("doctors").document("mock_doc_10").get().await()
        if (markerDoc.exists()) return

        val specializations = listOf("General Physician", "Pediatrician", "Cardiologist", "Gynecologist", "Orthopedic", "Dermatologist")
        val names = listOf("Dr. Arjun Reddy", "Dr. Priya Menon", "Dr. Vikram Singh", "Dr. Anjali Rao", 
                           "Dr. Rohan Gupta", "Dr. Meera Iyer", "Dr. Kabir Khan", "Dr. Sneha Patil", "Dr. Rajesh Kumar", "Dr. Neha Verma")

        val batch = firestore.batch()
        
        names.forEachIndexed { index, name ->
            val id = "mock_doc_${index + 10}" // Offset ID
            val spec = specializations[index % specializations.size]
            val exp = Random.nextInt(2, 25)
            val schedule = if (Random.nextBoolean()) "Mon-Fri: 9AM-1PM" else "Mon-Sat: 10AM-4PM"
            
            val doc = DoctorProfile(
                id = id,
                name = name,
                specialization = spec,
                isAvailable = Random.nextBoolean(),
                experienceYears = exp,
                opdSchedule = schedule,
                isEmergencyOnCall = Random.nextBoolean()
            )
            val ref = firestore.collection("doctors").document(id)
            batch.set(ref, doc)
        }
        batch.commit().await()
    }

    suspend fun seedPatients() {
        val snapshot = firestore.collection("triage_logs").get().await()
        // If we have fewer than 20 records, wipe and re-seed to ensure a full test set
        if (snapshot.size() < 20) {
            
            // Wipe existing if any
            if (!snapshot.isEmpty) {
                val deleteBatch = firestore.batch()
                snapshot.documents.forEach { deleteBatch.delete(it.reference) }
                deleteBatch.commit().await()
            }

            val names = listOf("Ramesh", "Suresh", "Mahesh", "Geeta", "Seeta", "Rahul", "Pooja", "Amit", "Sumit", "Kavita",
                               "Vijay", "Sanjay", "Manoj", "Anita", "Sunita", "Raj", "Simran", "Karan", "Arjun", "Deepa")
            
            val batch = firestore.batch()

            names.forEachIndexed { index, name ->
                val priority = when {
                    index % 10 == 0 -> "High Risk" // 2 patients (0, 10)
                    index % 3 == 0 -> "Medium Risk" // ~6-7 patients
                    else -> "Low Risk" // ~11-12 patients
                }
                
                // Detailed Symptoms & Explanations
                val (symptoms, explanation, solution, prevention) = when(priority) {
                    "High Risk" -> Quadruple(
                        "Severe chest pain, radiating to left arm, sweating, shortness of breath",
                        "Symptoms indicate potential Myocardial Infarction (Heart Attack). Immediate attention required.",
                        "Administer Aspirin if not allergic. Keep patient calm. Prepare for ECG and transport to ICU.",
                        "Regular cardiac checkups, control blood pressure, healthy diet."
                    )
                    "Medium Risk" -> Quadruple(
                        "High fever (103F), persistent dry cough, fatigue, body ache",
                        "Likely viral infection or flu. Monitoring required for dehydration.",
                        "Prescribe Antipyretics (Paracetamol). Ensure hydration. Isolation recommended.",
                        "Flu vaccination, hand hygiene, avoid close contact with infected individuals."
                    )
                    else -> Quadruple(
                        "Mild tension headache, slight nausea, general weakness",
                        "Likely due to stress or dehydration. Vitals are stable.",
                        "Rest, hydration, and mild analgesics if needed.",
                        "Stress management, adequate sleep, regular water intake."
                    )
                }

                val triage = TriageResult(
                    priority = priority,
                    explanation = explanation,
                    recommendedAction = if(priority == "High Risk") "Immediate Hospital Admission" else "Visit OPD",
                    patientAge = Random.nextInt(18, 80),
                    patientGender = if (Random.nextBoolean()) "Male" else "Female",
                    symptoms = "$name - $symptoms",
                    timestamp = java.util.Date(System.currentTimeMillis() - Random.nextLong(0, 86400000 * 5)), // Past 5 days
                    status = "New",
                    immediateSolutions = solution,
                    preventiveMeasures = prevention
                )
                
                val ref = firestore.collection("triage_logs").document()
                batch.set(ref, triage)
            }
             batch.commit().await()
        }
    }

    suspend fun seedMedicines() {
        val snapshot = firestore.collection("medicine_inventory").get().await()
        if (snapshot.size() < 10) {
            val medicines = listOf(
                com.example.healthapp.data.repository.Medicine("med_1", "Paracetamol 500mg", 500, 20.0, "2026-12-31"),
                com.example.healthapp.data.repository.Medicine("med_2", "Amoxicillin 250mg", 150, 45.0, "2025-10-15"),
                com.example.healthapp.data.repository.Medicine("med_3", "Metformin 500mg", 300, 30.0, "2026-05-20"),
                com.example.healthapp.data.repository.Medicine("med_4", "Cetirizine 10mg", 200, 15.0, "2027-01-01"),
                com.example.healthapp.data.repository.Medicine("med_5", "Ibuprofen 400mg", 100, 25.0, "2025-08-30"),
                com.example.healthapp.data.repository.Medicine("med_6", "Omeprazole 20mg", 250, 40.0, "2026-03-10"),
                com.example.healthapp.data.repository.Medicine("med_7", "Aspirin 75mg", 400, 10.0, "2026-11-25"),
                com.example.healthapp.data.repository.Medicine("med_8", "Atorvastatin 10mg", 120, 80.0, "2025-12-12"),
                com.example.healthapp.data.repository.Medicine("med_9", "Azithromycin 500mg", 50, 120.0, "2025-09-05"),
                com.example.healthapp.data.repository.Medicine("med_10", "Vitamin C 500mg", 600, 50.0, "2027-04-10")
            )
            
            val batch = firestore.batch()
            medicines.forEach { med ->
                val ref = firestore.collection("medicine_inventory").document(med.id)
                batch.set(ref, med)
            }
            batch.commit().await()
        }
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
