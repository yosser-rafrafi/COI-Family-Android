package tn.esprit.coidam.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.MainActivity
import tn.esprit.coidam.R
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.ui.theme.ThemedBackground

@Composable
fun LoginScreen(
    navController: NavController,
    isGoogleLoading: Boolean = false // ✅ PARAMÈTRE AJOUTÉ
) {
    BackHandler(enabled = true) {
        // Ne rien faire - empêche le retour
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val tokenManager = remember { tn.esprit.coidam.data.local.TokenManager(context) }
    val scope = rememberCoroutineScope()
    
    // Load saved credentials on screen load
    LaunchedEffect(Unit) {
        val savedEmail = tokenManager.getSavedEmailSync()
        val savedPassword = tokenManager.getSavedPasswordSync()
        val isRemembered = tokenManager.getRememberMeSync()
        
        if (isRemembered && savedEmail != null) {
            email = savedEmail
            rememberMe = true
            if (savedPassword != null) {
                password = savedPassword
            }
        }
    }

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
                if (authResponse.options != null && authResponse.options.isNotEmpty()) {

                    val option = authResponse.options.firstOrNull()

                    if (option == null) {
                        dialogMessage = "Login failed: no valid option received"
                        showDialog = true
                        return@launch
                    }

                    val loginResult = authRepository.loginAs(option.userId, option.userType)

                    loginResult.onSuccess {
                        // Save credentials if "Remember Me" is checked
                        tokenManager.saveRememberMe(rememberMe, email.trim(), if (rememberMe) password else null)
                        
                        if (option.userType == "companion") {
                            navController.navigate("companion_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else if (option.userType == "blind") {
                            navController.navigate("blind_dashboard") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            dialogMessage = "Unknown user type: ${option.userType}"
                            showDialog = true
                        }
                    }.onFailure { exception ->
                        dialogMessage = exception.message ?: "Failed to login"
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
                .padding(horizontal = 25.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                    HorizontalDivider(color = Color.Gray, thickness = 1.dp)
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
                    HorizontalDivider(color = Color.Gray, thickness = 1.dp)
                }

                Spacer(modifier = Modifier.height(15.dp))

                // Remember Me Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF70CEE3),
                            uncheckedColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Remember Me",
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sign In Button
                Button(
                    onClick = { signIn() },
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

                Spacer(modifier = Modifier.height(15.dp))

                // ✅ GOOGLE SIGN-IN BUTTON AVEC LOADING
                GoogleSignInButton(
                    isLoading = isGoogleLoading,
                    enabled = !isLoading && !isGoogleLoading
                )

                Spacer(modifier = Modifier.height(20.dp))
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

@Composable
fun GoogleSignInButton(
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val activity = LocalContext.current as? MainActivity

    Button(
        onClick = {
            if (!isLoading) {
                activity?.signInWithGoogle()
            }
        },
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color(0xFF70CEE3),
                modifier = Modifier.size(24.dp)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.mail),
                contentDescription = "Google Logo",
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isLoading) "Connexion..." else "Sign in with Google",
            color = if (enabled) Color.Black else Color.Black.copy(alpha = 0.4f)
        )
    }
}
