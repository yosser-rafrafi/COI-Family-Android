package tn.esprit.coidam.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.ui.theme.AppTheme

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)

    // Animation states
    var logoScale by remember { mutableStateOf(0.5f) }
    var logoOpacity by remember { mutableStateOf(0f) }
    var textOpacity by remember { mutableStateOf(0f) }
    var rotationAngle by remember { mutableStateOf(0f) }

    // Infinite rotation animation
    val infiniteRotation = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteRotation.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // ✅ VÉRIFIER LA SESSION AU DÉMARRAGE
    LaunchedEffect(Unit) {
        // Logo animation - scale and fade in
        logoScale = 1.0f
        logoOpacity = 1.0f

        // Text fade in with delay
        delay(300)
        textOpacity = 1.0f

        // Dismiss after 3 seconds
        delay(3000)
        
        // Fade out animation
        logoOpacity = 0f
        textOpacity = 0f
        logoScale = 0.8f
        delay(500)

        // Vérifier si l'utilisateur est déjà connecté
        val token = tokenManager.getTokenSync()
        val userId = tokenManager.getUserIdSync()
        val userType = tokenManager.getUserTypeSync()

        val isLoggedIn = !token.isNullOrEmpty() && !userId.isNullOrEmpty() &&
                !userType.isNullOrEmpty()

        if (isLoggedIn) {
            // ✅ UTILISATEUR DÉJÀ CONNECTÉ → Navigate to appropriate dashboard
            val destination = when (userType) {
                "companion" -> "companion_dashboard"
                "blind" -> "blind_dashboard"
                else -> {
                    // Invalid user type, clear session and go to login
                    tokenManager.clear()
                    "login"
                }
            }
            navController.navigate(destination) {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // ❌ PAS CONNECTÉ → Login
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF2F7FF),  // rgb(0.95, 0.97, 1.0)
                        Color(0xFFD9EBFA),  // rgb(0.85, 0.92, 0.98)
                        Color(0xFFBFE0F2)   // rgb(0.75, 0.88, 0.95)
                    )
                )
            )
    ) {
        // Subtle animated circles for depth
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-150).dp, y = (-200).dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .offset(x = 150.dp, y = 300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AppTheme.primaryBlue.copy(alpha = 0.2f),
                            AppTheme.primaryBlue.copy(alpha = 0.05f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo/Icon with animation
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Glowing circle behind logo
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(logoScale)
                        .alpha(logoOpacity)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AppTheme.primaryBlue.copy(alpha = 0.3f),
                                    AppTheme.primaryBlue.copy(alpha = 0.0f)
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // Main logo circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale)
                        .alpha(logoOpacity)
                        .rotate(rotation)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AppTheme.buttonBlue,
                                    AppTheme.primaryBlue
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "👥",
                        fontSize = 50.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // App Name with fade-in
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(textOpacity)
            ) {
                Text(
                    text = "CO-I",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.darkGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Family",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = AppTheme.textGray
                )
            }
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