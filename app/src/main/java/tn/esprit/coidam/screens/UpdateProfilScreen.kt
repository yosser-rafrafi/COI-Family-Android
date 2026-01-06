package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.AuthRepository
import android.content.Context
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfilScreen(navController: NavController) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("companion") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    // Load current user data
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = authRepository.getProfile()
            isLoading = false

            result.onSuccess { profile ->
                email = profile.email ?: ""
                firstName = profile.firstName ?: ""
                lastName = profile.lastName ?: ""
                phoneNumber = profile.phoneNumber ?: ""
                userType = profile.userType ?: "companion"
            }.onFailure { exception ->
                dialogMessage = "Failed to load profile: ${exception.message}"
                showDialog = true
                isSuccess = false
            }
        }
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = Regex("^[0-9]{8}\$")
        return phoneRegex.matches(phoneNumber)
    }

    fun updateProfile() {
        scope.launch {
            when {
                firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() || email.isEmpty() -> {
                    dialogMessage = "Please fill in all required fields."
                    isSuccess = false
                    showDialog = true
                }
                !isValidPhoneNumber(phoneNumber.trim()) -> {
                    dialogMessage = "Please enter a valid phone number (8 digits)."
                    isSuccess = false
                    showDialog = true
                }
                else -> {
                    isSaving = true
                    val result = authRepository.updateProfile(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim(),
                        phoneNumber = phoneNumber.trim()
                    )
                    isSaving = false

                    result.onSuccess {
                        dialogMessage = "Profile updated successfully!"
                        isSuccess = true
                        showDialog = true
                    }.onFailure { exception ->
                        dialogMessage = exception.message ?: "Failed to update profile"
                        isSuccess = false
                        showDialog = true
                    }
                }
            }
        }
    }
    // Light blue gradient background
    val lightBlueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE6F7FF),
            Color(0xFFD0EFFF),
            Color(0xFFB8E6FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBlueGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Close button in top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { navController.navigateUp() }
                ) {
                    Text(
                        text = "Fermer",
                        color = Color(0xFF9E9E9E),
                        fontSize = 16.sp
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF70CEE3))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Form Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            // Title
                            Text(
                                text = "Modifier le profil",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Subtitle
                            Text(
                                text = "Mettez à jour vos informations personnelles",
                                fontSize = 14.sp,
                                color = Color(0xFF9E9E9E)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // First Name
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "First Name",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF9E9E9E)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Prénom")
                                    }
                                },
                                placeholder = { Text("Entrez votre prénom") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    unfocusedLabelColor = Color(0xFF9E9E9E)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Last Name
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Last Name",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF9E9E9E)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Nom")
                                    }
                                },
                                placeholder = { Text("Entrez votre nom") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    unfocusedLabelColor = Color(0xFF9E9E9E)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Email
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Email",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF9E9E9E)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Email")
                                        Text(
                                            text = " *",
                                            color = Color(0xFFE53935)
                                        )
                                    }
                                },
                                placeholder = { Text("Entrez votre email") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    unfocusedLabelColor = Color(0xFF9E9E9E)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Phone Number
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Phone",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF9E9E9E)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Téléphone")
                                    }
                                },
                                placeholder = { Text("Ex: 28 190 800") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    unfocusedBorderColor = Color(0xFFE0E0E0),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    unfocusedLabelColor = Color(0xFF9E9E9E)
                                )
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            // Update Button
                            Button(
                                onClick = { updateProfile() },
                                enabled = !isSaving && !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF70CEE3)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Update",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Mettre à jour",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Result Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isSuccess) "Success" else "Error") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    if (isSuccess) {
                        navController.navigateUp()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview
@Composable
fun UpdateProfilePagePreview() {
    MaterialTheme {
        UpdateProfilScreen(navController = NavController(LocalContext.current))
    }
}