package tn.esprit.coidam.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.R
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.services.BatteryMonitorService
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.*

data class UserProfile(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val userType: String = "companion"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilScreen(navController: NavController) {
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = authRepository.getProfile()
            isLoading = false

            result.onSuccess { profile ->
                userProfile = UserProfile(
                    email = profile.email ?: "",
                    firstName = profile.firstName ?: "",
                    lastName = profile.lastName ?: "",
                    phoneNumber = profile.phoneNumber ?: "",
                    userType = profile.userType ?: "companion"
                )
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Failed to load profile"
                showDialog = true
            }
        }
    }

    fun logout() {
        scope.launch {
            // Stop battery monitoring service
            try {
                val intent = android.content.Intent(context, tn.esprit.coidam.services.BatteryMonitorService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                // Ignore if service wasn't running
            }

            authRepository.logout()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Light blue background with gradient
    val lightBlueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE6F7FF),
            Color(0xFFD0EFFF),
            Color(0xFFB8E6FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBlueGradient)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(40.dp))

                    // Companion Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF70CEE3)
                    ) {
                        Text(
                            text = "Companion",
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Info Card
                    ProfileInfoCard(
                        icon = Icons.Default.Email,
                        iconColor = Color(0xFF70CEE3),
                        label = "Email",
                        value = userProfile?.email ?: ""
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Info Card
                    ProfileInfoCard(
                        icon = Icons.Default.Phone,
                        iconColor = Color(0xFF9E9E9E),
                        label = "Téléphone",
                        value = userProfile?.phoneNumber?.takeIf { it.isNotEmpty() } ?: "Non renseigné"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Membership Date Card
                    val membershipDate = "23 nov. 2025" // TODO: Get actual date from profile
                    ProfileInfoCard(
                        icon = Icons.Default.CalendarToday,
                        iconColor = Color(0xFFFF9800),
                        label = "Membre depuis",
                        value = membershipDate
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Modify Profile Action Card
                    ProfileActionCard(
                        icon = Icons.Default.Person,
                        iconColor = Color(0xFF70CEE3),
                        title = "Modifier le profil",
                        subtitle = "Mettre à jour vos informations",
                        onClick = { navController.navigate("update_profile") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Change Password Action Card
                    ProfileActionCard(
                        icon = Icons.Default.Lock,
                        iconColor = Color(0xFFFF9800),
                        title = "Changer le mot de passe",
                        subtitle = "Sécuriser votre compte",
                        onClick = { navController.navigate("change_password") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Blind Profile Configuration Action Card
                    ProfileActionCard(
                        icon = Icons.Default.PersonAdd,
                        iconColor = Color(0xFF70CEE3),
                        title = "Configuration du profil aveugle",
                        subtitle = "Gérer le visage et le PIN vocal",
                        onClick = { navController.navigate("blind_profile_config") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logout Action Card
                    ProfileActionCard(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        iconColor = Color(0xFFE53935),
                        title = "Déconnexion",
                        subtitle = "Se déconnecter de l'application",
                        onClick = { showLogoutDialog = true }
                    )

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    logout()
                }) {
                    Text("Yes", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
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
                TextButton(onClick = {
                    showDialog = false
                    navController.navigateUp()
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProfileInfoCard(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in colored circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun ProfileActionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon in colored circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Arrow",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
MaterialTheme {
    ProfilScreen(navController = NavController(LocalContext.current))

}
}