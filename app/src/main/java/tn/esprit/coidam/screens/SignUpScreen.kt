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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(55.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "HELLO THERE",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Register below with your details!",
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.White.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 25.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CustomTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm Password",
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { signUp() },
                        enabled = !isLoading && !isGoogleLoading, // ✅ DÉSACTIVER SI GOOGLE LOADING
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70CEE3)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "I am member!",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " Login Now",
                            color = Color(0xFF129FA9),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                navController.navigate("login")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    // ✅ GOOGLE SIGN-IN BUTTON AVEC LOADING
                    GoogleSignInButton(
                        isLoading = isGoogleLoading,
                        enabled = !isLoading && !isGoogleLoading
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
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
            .background(Color.White.copy(alpha = 0.01f))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        HorizontalDivider(
            color = Color.Gray,
            thickness = 1.dp
        )
    }
}