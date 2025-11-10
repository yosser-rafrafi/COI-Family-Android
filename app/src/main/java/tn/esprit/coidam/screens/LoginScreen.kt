package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.R
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.data.models.UserOption

@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var showUserSelection by remember { mutableStateOf(false) }
    var userOptions by remember { mutableStateOf<List<UserOption>>(emptyList()) }
    
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    fun signIn() {
        if (email.isBlank() || password.isBlank()) {
            dialogMessage = "Please fill in all fields"
            showDialog = true
            return
        }

        scope.launch {
            isLoading = true
            val result = authRepository.signIn(email.trim(), password)
            isLoading = false

            result.onSuccess { authResponse ->
                // Check if user needs to select profile
                if (authResponse.options != null && authResponse.options.isNotEmpty()) {
                    userOptions = authResponse.options
                    showUserSelection = true
                } else if (authResponse.access_token != null) {
                    // Login successful, navigate to profile
                    navController.navigate("profil") {
                        popUpTo("login") { inclusive = true }
                    }
                } else {
                    dialogMessage = authResponse.error ?: "Login failed"
                    showDialog = true
                }
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Login failed. Please check your credentials."
                showDialog = true
            }
        }
    }

    fun loginAsUser(userId: String, userType: String) {
        scope.launch {
            isLoading = true
            val result = authRepository.loginAs(userId, userType)
            isLoading = false

            result.onSuccess {
                navController.navigate("profil") {
                    popUpTo("login") { inclusive = true }
                }
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Failed to login"
                showDialog = true
                showUserSelection = false
            }
        }
    }



    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.clipfly),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HELLO AGAIN",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Welcome Back!",
                    fontSize = 20.sp
                )
            }

            // Form Container
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

                // Email Field
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.01f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mail),
                            contentDescription = "Email Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("Email") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(
                        color = Color.Gray,
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Password Field
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.01f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.padlock),
                            contentDescription = "Password Icon",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        TextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(
                        color = Color.Gray,
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(25.dp))

                // Sign In Button
                Button(
                    onClick = { signIn() },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF70CEE3)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Sign In",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // Forgot Password
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp, bottom = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Forgot Password",
                        color = Color(0xFF129FA9),
                        modifier = Modifier.clickable {
                            navController.navigate("forgot_password")
                        }
                    )
                }

                // Not a member
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Not a member?",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " Register Now",
                        color = Color(0xFF129FA9),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            navController.navigate("register")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // User Selection Dialog
    if (showUserSelection && userOptions.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showUserSelection = false },
            title = { Text("Select Profile") },
            text = {
                Column {
                    Text("Choose which profile you want to use:")
                    Spacer(modifier = Modifier.height(16.dp))
                    userOptions.forEach { option ->
                        Button(
                            onClick = {
                                loginAsUser(option.userId, option.userType)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (option.userType == "companion") Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = option.userType.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                if (!option.email.isNullOrEmpty()) {
                                    Text(
                                        text = option.email,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserSelection = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Error") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(navController = NavController(LocalContext.current))
    }
}
