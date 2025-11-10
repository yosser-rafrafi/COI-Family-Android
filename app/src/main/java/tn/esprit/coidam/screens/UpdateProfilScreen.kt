package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    var university by remember { mutableStateOf("") }
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
                university = profile.university ?: ""
                userType = profile.userType ?: "companion"
            }.onFailure { exception ->
                dialogMessage = "Failed to load profile: ${exception.message}"
                showDialog = true
                isSuccess = false
            }
        }
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = Regex("^[0-9]{10}\$")
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
                    dialogMessage = "Please enter a valid phone number (10 digits)."
                    isSuccess = false
                    showDialog = true
                }
                else -> {
                    isSaving = true
                    val result = authRepository.updateProfile(
                        firstName = firstName.trim(),
                        lastName = lastName.trim(),
                        email = email.trim(),
                        university = university.trim(),
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFECF9FD))
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF70CEE3)
                )
            )

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
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile Picture with Edit Icon
                    Box(
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF70CEE3))
                                .border(4.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Picture",
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }

                        IconButton(
                            onClick = { /* TODO: Change profile picture */ },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF70CEE3), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Change Picture",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // Form Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.9f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Personal Information",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF70CEE3)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // First Name
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text("First Name *") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "First Name"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    focusedLeadingIconColor = Color(0xFF70CEE3)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Last Name
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text("Last Name *") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Last Name"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    focusedLeadingIconColor = Color(0xFF70CEE3)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Phone Number
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number *") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Phone"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    focusedLeadingIconColor = Color(0xFF70CEE3)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // University
                            OutlinedTextField(
                                value = university,
                                onValueChange = { university = it },
                                label = { Text("University (Optional)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = "University"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    focusedLeadingIconColor = Color(0xFF70CEE3)
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Email
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email *") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email"
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF70CEE3),
                                    focusedLabelColor = Color(0xFF70CEE3),
                                    focusedLeadingIconColor = Color(0xFF70CEE3)
                                )
                            )

                            Spacer(modifier = Modifier.height(30.dp))

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
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "Save"
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Save Changes",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
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