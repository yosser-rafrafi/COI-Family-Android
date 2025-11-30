package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import tn.esprit.coidam.R
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.repository.AuthRepository

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        delay(2000) // Animation du splash (2 secondes)

        // Check if user is already logged in
        val token = tokenManager.getTokenSync()
        val userId = tokenManager.getUserIdSync()
        val userType = tokenManager.getUserTypeSync()
        val isLoggedIn = !token.isNullOrEmpty() && !userId.isNullOrEmpty() && !userType.isNullOrEmpty()

        if (isLoggedIn) {
            // User is already logged in, go to dashboard
            val destination = if (userType == "companion") "companion_dashboard" else "blind_dashboard"
            navController.navigate(destination) {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // Check if "Remember Me" is enabled and try auto-login
            val rememberMe = tokenManager.getRememberMeSync()
            val savedEmail = tokenManager.getSavedEmailSync()
            val savedPassword = tokenManager.getSavedPasswordSync()
            
            if (rememberMe && savedEmail != null && savedPassword != null) {
                // Try auto-login
                scope.launch {
                    val result = authRepository.signIn(savedEmail, savedPassword)
                    result.onSuccess { authResponse ->
                        if (authResponse.options != null && authResponse.options.isNotEmpty()) {
                            val option = authResponse.options.firstOrNull()
                            if (option != null) {
                                val loginResult = authRepository.loginAs(option.userId, option.userType)
                                loginResult.onSuccess {
                                    val destination = if (option.userType == "companion") "companion_dashboard" else "blind_dashboard"
                                    navController.navigate(destination) {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                    return@launch
                                }
                            }
                        }
                        // Auto-login failed, go to login screen
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }.onFailure {
                        // Auto-login failed, go to login screen
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            } else {
                // No saved credentials, go to login screen
                navController.navigate("login") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Loading indicator
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            CircularProgressIndicator(
                color = Color(0xFF70CEE3),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreen(navController = NavController(LocalContext.current))
    }
}