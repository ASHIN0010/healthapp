package com.example.healthapp.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.healthapp.ui.components.AnimatedGradientBackground
import com.example.healthapp.ui.components.HealthAppButton
import com.example.healthapp.ui.components.HealthAppCard
import com.example.healthapp.ui.components.HealthAppTextField

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Observe login success
    LaunchedEffect(uiState.userId) {
        if (uiState.userId != null && selectedRole != null) {
            onLoginSuccess(selectedRole!!)
        }
    }

    AnimatedGradientBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Branding
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Rural Health",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Integrated Care System",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(targetState = selectedRole, label = "LoginState") { role ->
                if (role == null) {
                    RoleSelectionGrid(onRoleSelected = { selectedRole = it })
                } else if (role == "Guest") {
                    // Guest mode - skip authentication
                    LaunchedEffect(Unit) {
                        onLoginSuccess("Patient") // Default to Patient dashboard for guests
                    }
                } else {
                    LoginInputForm(
                        role = role,
                        phoneNumber = phoneNumber,
                        onPhoneChange = { phoneNumber = it },
                        otp = otpCode,
                        onOtpChange = { otpCode = it },
                        uiState = uiState,
                        onBack = { selectedRole = null },
                        onSendOtp = { 
                            if(context is Activity) viewModel.sendOtp(phoneNumber, context)
                        },
                        onVerify = { viewModel.verifyOtp(otpCode) },
                        onGuestLogin = { onLoginSuccess(role) } // Pass guest callback
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectionGrid(onRoleSelected: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Select Your Login Role", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        val roles = listOf(
            RoleItem("Hospital", Icons.Default.Home, Color(0xFFE57373)),
            RoleItem("Pharmacy", Icons.Default.ShoppingCart, Color(0xFF64B5F6)),
            RoleItem("Patient", Icons.Default.Person, Color(0xFF81C784)),
            RoleItem("ASHA", Icons.Default.Favorite, Color(0xFFFFD54F)), // Heart icon for Health Saathi
            RoleItem("Guest", Icons.Default.Warning, Color(0xFFBA68C8)) // Purple for Guest
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(roles) { role ->
                HealthAppCard(
                    onClick = { onRoleSelected(role.name) },
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(role.icon, contentDescription = null, tint = role.color, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(role.name, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

data class RoleItem(val name: String, val icon: ImageVector, val color: Color)

@Composable
fun LoginInputForm(
    role: String,
    phoneNumber: String,
    onPhoneChange: (String) -> Unit,
    otp: String,
    onOtpChange: (String) -> Unit,
    uiState: LoginUiState,
    onBack: () -> Unit,
    onSendOtp: () -> Unit,
    onVerify: () -> Unit,
    onGuestLogin: () -> Unit, // Add guest callback
    viewModel: LoginViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Phone, 1: Email
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                account.idToken?.let { token ->
                    viewModel.signInWithGoogle(token)
                } ?: run {
                    // Handle missing token error
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                // Handle Sign In Error
            }
        }
    }

    HealthAppCard {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Login as $role", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Phone") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Email") })
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (selectedTab == 0) {
                // Phone Auth Logic
                if (!uiState.isOtpSent) {
                    HealthAppTextField(
                        value = phoneNumber,
                        onValueChange = onPhoneChange,
                        label = "Phone Number",
                        leadingIcon = Icons.Default.Phone
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HealthAppButton(
                        text = "Send OTP",
                        onClick = onSendOtp,
                        isLoading = uiState.isLoading
                    )
                } else {
                    HealthAppTextField(
                        value = otp,
                        onValueChange = onOtpChange,
                        label = "Verification Code"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HealthAppButton(
                        text = "Verify & Login",
                        onClick = onVerify,
                        isLoading = uiState.isLoading
                    )
                }
            } else {
                // Email Auth Logic
                HealthAppTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    leadingIcon = Icons.Default.Person
                )
                Spacer(modifier = Modifier.height(12.dp))
                HealthAppTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    // Visualize password transformation ideally
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                     ExpandedButton(text = "Sign In", onClick = { viewModel.signInWithEmail(email, password) }, isLoading = uiState.isLoading, modifier = Modifier.weight(1f))
                     ExpandedButton(text = "Register", onClick = { viewModel.signUpWithEmail(email, password) }, isLoading = uiState.isLoading, modifier = Modifier.weight(1f), isSecondary = true)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))
            
            HealthAppButton(
                text = "Sign in with Google",
                onClick = {
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(com.example.healthapp.R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                containerColor = Color.White,
                // contentColor = Color.Black // Custom button needed for specific styling
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick Guest Access for Testing
            OutlinedButton(
                onClick = onGuestLogin, // Direct navigation without auth
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue as Guest (Testing)")
            }
            
            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                 Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) {
                Text("Select Different Role")
            }
        }
    }
}

@Composable
fun ExpandedButton(text: String, onClick: () -> Unit, isLoading: Boolean, modifier: Modifier = Modifier, isSecondary: Boolean = false) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if(isSecondary) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
    ) {
         if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
        } else {
            Text(text)
        }
    }
}
