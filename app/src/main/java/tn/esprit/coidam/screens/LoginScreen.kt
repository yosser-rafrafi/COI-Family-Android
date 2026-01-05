package tn.esprit.coidam.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import tn.esprit.coidam.ui.theme.AppTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import android.util.Log
import tn.esprit.coidam.MainActivity
import tn.esprit.coidam.R
import tn.esprit.coidam.data.api.VoiceWebSocketClient  // ✅ AJOUT
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.data.repository.WebSocketManager  // ✅ AJOUT
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
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

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
                if (authResponse.options != null && authResponse.options.isNotEmpty()) {

                    val option = authResponse.options.firstOrNull()

                    if (option == null) {
                        dialogMessage = "Login failed: no valid option received"
                        showDialog = true
                        return@launch
                    }

                    val loginResult = authRepository.loginAs(option.userId, option.userType)

                    loginResult.onSuccess {
                        // ✅ SOLUTION 1: Connecter WebSockets IMMÉDIATEMENT après login
                        Log.d("LoginScreen", "🔌 Connecting sockets after login for ${option.userType}...")
                        scope.launch {
                            try {
                                val webSocketManager = WebSocketManager.getInstance(context)
                                val voiceSocketManager = VoiceWebSocketClient.getInstance(context)
                                
                                webSocketManager.connect()
                                voiceSocketManager.connect()
                                
                                Log.d("LoginScreen", "✅ Sockets connection initiated")
                            } catch (e: Exception) {
                                Log.e("LoginScreen", "❌ Error connecting sockets: ${e.message}", e)
                            }
                        }
                        
                        // Puis naviguer vers le dashboard approprié
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
                    text = "Hello Again !",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.darkGray
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Welcome Back!",
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
                    // Email Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.mail),
                            contentDescription = "Email Icon",
                            modifier = Modifier.size(20.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(AppTheme.buttonBlue)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            TextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = { 
                                    Text(
                                        "Email Address",
                                        color = AppTheme.textGray
                                    ) 
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = AppTheme.darkGray,
                                    unfocusedTextColor = AppTheme.darkGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(
                                color = AppTheme.textGray.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.padlock),
                            contentDescription = "Password Icon",
                            modifier = Modifier.size(20.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(AppTheme.buttonBlue)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            TextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { 
                                    Text(
                                        "Password",
                                        color = AppTheme.textGray
                                    ) 
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = AppTheme.darkGray,
                                    unfocusedTextColor = AppTheme.darkGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HorizontalDivider(
                                color = AppTheme.textGray.copy(alpha = 0.3f),
                                thickness = 1.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In Button with gradient
                    Button(
                        onClick = { signIn() },
                        enabled = !isLoading && !isGoogleLoading && email.isNotEmpty() && password.isNotEmpty(),
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
                                    text = "Sign In",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password
                    TextButton(
                        onClick = { navController.navigate("forgot_password") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Forget password",
                            fontSize = 14.sp,
                            color = AppTheme.textGray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Not a member
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Don't have an account? ",
                            fontSize = 14.sp,
                            color = AppTheme.textGray
                        )
                        Text(
                            text = "Sign Up",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.buttonBlue,
                            modifier = Modifier.clickable {
                                navController.navigate("register")
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
    val activity = LocalActivity.current as? MainActivity

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