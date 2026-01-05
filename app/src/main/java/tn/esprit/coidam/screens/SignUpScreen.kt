package tn.esprit.coidam.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import tn.esprit.coidam.ui.theme.AppTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.ui.theme.ThemedBackground

@Composable
fun SignupScreen(
    navController: NavController,
    isGoogleLoading: Boolean = false // ✅ PARAMÈTRE AJOUTÉ
) {
    BackHandler(enabled = true) {
        // Ne rien faire - empêche le retour
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}\$")
        return emailRegex.matches(email)
    }

    fun signUp() {
        scope.launch {
            when {
                email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    dialogMessage = "Please fill in all fields."
                    isSuccess = false
                    showDialog = true
                }
                !isValidEmail(email.trim()) -> {
                    dialogMessage = "Please enter a valid email address."
                    isSuccess = false
                    showDialog = true
                }
                password.length < 6 -> {
                    dialogMessage = "Password must be at least 6 characters."
                    isSuccess = false
                    showDialog = true
                }
                password.trim() != confirmPassword.trim() -> {
                    dialogMessage = "Passwords do not match."
                    isSuccess = false
                    showDialog = true
                }
                else -> {
                    isLoading = true
                    val result = authRepository.signUp(email.trim(), password)
                    isLoading = false

                    result.onSuccess {
                        scope.launch {
                            val signInResult = authRepository.signIn(email.trim(), password)
                            signInResult.onSuccess { authResponse ->
                                if (authResponse.access_token != null) {
                                    navController.navigate("blind_dashboard") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                } else {
                                    dialogMessage = "Registration successful! Please login."
                                    isSuccess = true
                                    showDialog = true
                                }
                            }.onFailure {
                                dialogMessage = "Registration successful! Please login."
                                isSuccess = true
                                showDialog = true
                            }
                        }
                    }.onFailure { exception ->
                        dialogMessage = exception.message ?: "Registration failed. Please try again."
                        isSuccess = false
                        showDialog = true
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Hello There",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.darkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Register below with your details",
                    fontSize = 16.sp,
                    color = AppTheme.textGray
                )
            }

            // Form Container with shadow
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(top = 40.dp)
                ) {
                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email Address"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    CustomTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm Password",
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign Up Button with gradient
                    Button(
                        onClick = { signUp() },
                        enabled = !isLoading && !isGoogleLoading && email.isNotEmpty() && 
                                password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                                password == confirmPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = if (isLoading) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                AppTheme.buttonBlue.copy(alpha = 0.7f),
                                                AppTheme.buttonBlue.copy(alpha = 0.6f)
                                            )
                                        )
                                    } else {
                                        AppTheme.buttonGradient
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = "Sign Up",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Login Link
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "I am member! ",
                            fontSize = 14.sp,
                            color = AppTheme.darkGray
                        )
                        Text(
                            text = "Login Now",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.buttonBlue,
                            modifier = Modifier.clickable {
                                navController.navigate("login")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // ✅ OVERLAY DE LOADING GOOGLE SIGN-IN
        if (isGoogleLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF70CEE3))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connexion avec Google...",
                            fontSize = 16.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isSuccess) "Success" else "Error") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { 
                Text(
                    placeholder,
                    color = AppTheme.textGray
                ) 
            },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = AppTheme.darkGray,
                unfocusedTextColor = AppTheme.darkGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(
            color = AppTheme.textGray.copy(alpha = 0.3f),
            thickness = 1.dp
        )
    }
}