package com.example.healthapp.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthapp.ui.screens.auth.LoginScreen
import com.google.firebase.auth.FirebaseAuth
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(
    userProfileRepository: com.example.healthapp.data.repository.UserProfileRepository = hiltViewModel<AppNavigationViewModel>().userProfileRepository
) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val coroutineScope = rememberCoroutineScope()
    
    // Check if user is already logged in
    var startDestination by remember { mutableStateOf("login") }
    var isCheckingAuth by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Get user profile from Firebase
            val profile = userProfileRepository.getUserProfile(currentUser.uid)
            
            if (profile != null) {
                startDestination = when(profile.role) {
                    "Hospital" -> "dashboard_hospital"
                    "Pharmacy" -> "dashboard_pharmacy"
                    "Patient" -> "dashboard_patient"
                    "ASHA" -> "dashboard_asha"
                    else -> "login"
                }
            }
        }
        isCheckingAuth = false
    }

    if (!isCheckingAuth) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") {
                LoginScreen(
                    onLoginSuccess = { role -> 
                        coroutineScope.launch {
                            // Save role to Firebase (skip for Guest)
                            val userId = auth.currentUser?.uid
                            if (userId != null && role != "Guest") {
                                userProfileRepository.saveUserProfile(userId, role)
                            }
                            
                            val route = when(role) {
                                "Hospital" -> "dashboard_hospital"
                                "Pharmacy" -> "dashboard_pharmacy"
                                "Patient" -> "dashboard_patient"
                                "Ambulance" -> "dashboard_asha"
                                "ASHA" -> "dashboard_asha"
                                "Guest" -> "dashboard_patient" // Guests use Patient dashboard
                                else -> "login"
                            }
                            navController.navigate(route) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable("dashboard_hospital") { 
                com.example.healthapp.ui.screens.dashboard.HospitalDashboard(
                    onLogout = { 
                        auth.signOut()
                        navController.navigate("login") { popUpTo(0) } 
                    }
                ) 
            }
            composable("dashboard_pharmacy") { 
                com.example.healthapp.ui.screens.dashboard.PharmacyDashboard(
                    onLogout = { 
                        auth.signOut()
                        navController.navigate("login") { popUpTo(0) } 
                    }
                ) 
            }
            composable("dashboard_patient") { 
                com.example.healthapp.ui.screens.dashboard.PatientDashboard(
                    onLogout = { 
                        auth.signOut()
                        navController.navigate("login") { popUpTo(0) } 
                    }
                ) 
            }
            composable("dashboard_asha") { 
                com.example.healthapp.ui.screens.dashboard.AshaDashboard(
                    onLogout = { 
                        auth.signOut()
                        navController.navigate("login") { popUpTo(0) } 
                    }
                ) 
            }
        }
    }
}
