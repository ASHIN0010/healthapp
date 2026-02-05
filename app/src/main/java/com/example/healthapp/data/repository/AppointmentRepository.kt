package com.example.healthapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.Date

data class Appointment(
    val id: String = "",
    val patientId: String = "",
    val doctorId: String = "",
    val doctorName: String = "",
    val patientName: String = "Patient", // In real app, fetch from profile
    val timestamp: Date = Date(),
    val timeSlot: String = "10:00 AM", // New: Scheduled Slot
    val priority: String = "Standard", // AI Priority: High Risk, Medium Risk, Low Risk, Standard
    val status: String = "Confirmed" // Confirmed, Completed, Cancelled
)

class AppointmentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun bookAppointment(doctorId: String, doctorName: String, timeSlot: String, priority: String) {
        val uid = auth.currentUser?.uid ?: throw Exception("User not logged in")
        
        val appointment = Appointment(
            patientId = uid,
            doctorId = doctorId,
            doctorName = doctorName,
            status = "Confirmed",
            timeSlot = timeSlot,
            priority = priority, // Integrated AI Risk Assessment
            timestamp = Date() // Booking for 'now' logic for MVP or specific date later
        )
        
        val ref = firestore.collection("appointments").document()
        ref.set(appointment.copy(id = ref.id)).await()
    }
}
