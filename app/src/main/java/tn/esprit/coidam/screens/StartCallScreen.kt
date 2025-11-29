package tn.esprit.coidam.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.repository.CallRepository
import tn.esprit.coidam.ui.theme.ThemedBackground
import android.util.Log

@Composable
fun StartCallScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callRepository = remember { CallRepository(context) }
    val tokenManager = remember { TokenManager(context) }

    var isLoading by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var callType by remember { mutableStateOf("video") } // "video" or "audio"

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color(0xFF424242)
                    )
                }

                Text(
                    text = "Appeler l'accompagnant",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF70CEE3),
                                Color(0xFF129FA9)
                            )
                        )
                    )
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Votre Accompagnant",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (callType == "video") "Appel vidéo" else "Appel audio",
                fontSize = 16.sp,
                color = Color(0xFF757575),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Call Type Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallTypeButton(
                        icon = Icons.Default.Videocam,
                        label = "Vidéo",
                        isSelected = callType == "video",
                        onClick = { callType = "video" }
                    )

                    CallTypeButton(
                        icon = Icons.Default.Phone,
                        label = "Audio",
                        isSelected = callType == "audio",
                        onClick = { callType = "audio" }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Call Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        val currentUserId = tokenManager.getUserIdSync()
                        val currentUserType = tokenManager.getUserTypeSync()
                        val linkedUserId = tokenManager.getLinkedUserIdSync()

                        Log.d("StartCallScreen", "🔍 Current User ID: $currentUserId")
                        Log.d("StartCallScreen", "🔍 Current User Type: $currentUserType")
                        Log.d("StartCallScreen", "🔍 Linked User ID: $linkedUserId")

                        // ✅ LOGIQUE CORRECTE SELON LE TYPE D'UTILISATEUR
                        val blindUserId: String?
                        val companionId: String?

                        if (currentUserType == "blind") {
                            // Si l'utilisateur est BLIND
                            blindUserId = currentUserId  // Son propre ID
                            companionId = linkedUserId   // Son companion lié
                        } else {
                            // Si l'utilisateur est COMPANION (cas rare mais possible)
                            blindUserId = linkedUserId   // Son blind lié
                            companionId = currentUserId  // Son propre ID
                        }

                        Log.d("StartCallScreen", "📞 Blind User ID: $blindUserId")
                        Log.d("StartCallScreen", "📞 Companion ID: $companionId")

                        if (blindUserId != null && companionId != null) {
                            val result = callRepository.startCall(
                                blindUserId = blindUserId,
                                companionId = companionId,
                                callType = callType,
                                initiatedBy = currentUserType ?: "blind"
                            )

                            result.onSuccess { response ->
                                Log.d("StartCallScreen", "✅ Appel créé avec succès: ${response.call.id}")
                                // Navigate to active call screen with call data
                                navController.navigate("active_call/${response.call.id}") {
                                    popUpTo("start_call") { inclusive = true }
                                }
                            }.onFailure { e ->
                                Log.e("StartCallScreen", "❌ Erreur: ${e.message}")
                                errorMessage = e.message ?: "Erreur lors du démarrage de l'appel"
                                showError = true
                            }
                        } else {
                            errorMessage = "Informations utilisateur manquantes.\n" +
                                    "Blind ID: ${if (blindUserId == null) "❌ MANQUANT" else "✅"}\n" +
                                    "Companion ID: ${if (companionId == null) "❌ MANQUANT" else "✅"}"
                            showError = true
                            Log.e("StartCallScreen", "❌ $errorMessage")
                        }

                        isLoading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                shape = RoundedCornerShape(35.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (callType == "video") Icons.Default.Videocam else Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Démarrer l'appel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Error Dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Erreur") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CallTypeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF70CEE3) else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF757575),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else Color(0xFF757575)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStartCallScreen() {
    // NavController factice pour le preview
    val navController = rememberNavController()

    // Appel de ton composable
    StartCallScreen(navController = navController)
}