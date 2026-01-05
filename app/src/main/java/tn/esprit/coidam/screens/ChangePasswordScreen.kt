package tn.esprit.coidam.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.AuthRepository
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(navController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    // Password visibility toggles
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    // Password requirements
    val hasMinLength = newPassword.length >= 6
    val passwordsMatch = newPassword.isNotEmpty() && newPassword == confirmPassword
    val isFormValid = hasMinLength && passwordsMatch && currentPassword.isNotEmpty()

    fun updatePassword() {
        if (!isFormValid) return

        scope.launch {
            isSaving = true
            val result = authRepository.updatePassword(currentPassword, newPassword)
            isSaving = false

            result.onSuccess {
                dialogMessage = "Mot de passe mis à jour avec succès!"
                isSuccess = true
                showDialog = true
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Échec de la mise à jour du mot de passe"
                isSuccess = false
                showDialog = true
            }
        }
    }

    // Light blue-grey gradient background
    val lightBlueGreyGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF5F9FA),
            Color(0xFFE8F4F8),
            Color(0xFFDBEFF6)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBlueGreyGradient)
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
                            text = "Changer le mot de passe",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                            text = "Sécurisez votre compte",
                            fontSize = 14.sp,
                            color = Color(0xFF9E9E9E)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Current Password
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Current Password",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF9E9E9E)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mot de passe actuel")
                                }
                            },
                            placeholder = { Text("Entrez votre mot de passe actuel") },
                            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                    Icon(
                                        imageVector = if (currentPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (currentPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF9E9E9E)
                                    )
                                }
                            },
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

                        // New Password
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "New Password",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF9E9E9E)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Nouveau mot de passe")
                                }
                            },
                            placeholder = { Text("Entrez votre nouveau mot de passe") },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF9E9E9E)
                                    )
                                }
                            },
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

                        // Confirm Password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Confirm Password",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF9E9E9E)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirmer le mot de passe")
                                }
                            },
                            placeholder = { Text("Confirmez votre nouveau mot de passe") },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFF9E9E9E)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF70CEE3),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedLabelColor = Color(0xFF70CEE3),
                                unfocusedLabelColor = Color(0xFF9E9E9E)
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Password Requirements
                        Text(
                            text = "Exigences du mot de passe:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Requirement 1: At least 6 characters
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (hasMinLength) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Requirement",
                                tint = if (hasMinLength) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Au moins 6 caractères",
                                fontSize = 14.sp,
                                color = if (hasMinLength) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            )
                        }

                        // Requirement 2: Passwords match
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordsMatch) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Requirement",
                                tint = if (passwordsMatch) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Les mots de passe correspondent",
                                fontSize = 14.sp,
                                color = if (passwordsMatch) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Update Button
                        Button(
                            onClick = { updatePassword() },
                            enabled = isFormValid && !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF70CEE3),
                                disabledContainerColor = Color(0xFFE0E0E0)
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
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Update",
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Mettre à jour le mot de passe",
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

    // Result Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isSuccess) "Succès" else "Erreur") },
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

