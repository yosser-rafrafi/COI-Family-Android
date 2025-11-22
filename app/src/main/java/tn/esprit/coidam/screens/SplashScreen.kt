package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import tn.esprit.coidam.R
import tn.esprit.coidam.data.local.TokenManager

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)

    // ✅ VÉRIFIER LA SESSION AU DÉMARRAGE
    LaunchedEffect(Unit) {
        delay(2000) // Animation du splash (2 secondes)

        // Vérifier si l'utilisateur est déjà connecté
        val token = tokenManager.getTokenSync()
        val userId = tokenManager.getUserIdSync()
        val userType = tokenManager.getUserTypeSync()

        val isLoggedIn = !token.isNullOrEmpty() && !userId.isNullOrEmpty() &&
                !userType.isNullOrEmpty()

        if (isLoggedIn) {
            // ✅ UTILISATEUR DÉJÀ CONNECTÉ → Dashboard
            navController.navigate("blind_dashboard") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            // ❌ PAS CONNECTÉ → Login
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
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