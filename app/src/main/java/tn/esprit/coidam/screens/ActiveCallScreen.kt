package tn.esprit.coidam.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Enums.CallStatus
import tn.esprit.coidam.data.repository.CallRepository
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import tn.esprit.coidam.data.models.Call




/**
 * Écran d'appel actif (version simplifiée sans Agora pour test)
 *
 * NOTES POUR L'INTÉGRATION AGORA:
 * 1. Ajouter dépendance: implementation 'io.agora.rtc:full-sdk:4.x.x'
 * 2. Initialiser RtcEngine avec agoraCredentials
 * 3. Rejoindre le canal avec token
 * 4. Gérer les callbacks vidéo/audio
 */
@Composable
fun ActiveCallScreen(
    navController: NavController,
    callId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callRepository = remember { CallRepository(context) }
    val tokenManager = remember { TokenManager(context) }

    var call by remember { mutableStateOf<tn.esprit.coidam.data.models.Call?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0) }
    var showEndDialog by remember { mutableStateOf(false) }

    // Timer pour la durée d'appel
    LaunchedEffect(call?.status) {
        if (call?.status == CallStatus.ACTIVE) {
            while (true) {
                delay(1000)
                callDuration++
            }
        }
    }

    // Charger l'appel
    LaunchedEffect(callId) {
        val result = callRepository.getCall(callId)
        result.onSuccess {
            call = it

            // Si l'appel est en "ringing", accepter automatiquement (pour test)
            if (it.status == CallStatus.RINGING) {
                callRepository.acceptCall(callId).onSuccess { acceptedCall ->
                    call = acceptedCall
                }
            }
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (call != null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF70CEE3),
                                        Color(0xFF129FA9)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = call!!.companion?.let { "${it.firstName} ${it.lastName}" }
                            ?: "Accompagnant",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (call!!.status) {
                            CallStatus.RINGING -> "Appel en cours..."
                            CallStatus.ACTIVE -> formatDuration(callDuration)
                            else -> call!!.status.displayName()
                        },
                        fontSize = 16.sp,
                        color = Color(0xFFCCCCCC)
                    )
                }

                // Video Placeholder (pour test sans Agora)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF757575),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Vidéo de l'accompagnant",
                            color = Color(0xFF757575),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "(Intégration Agora requise)",
                            color = Color(0xFF555555),
                            fontSize = 12.sp
                        )
                    }
                }

                // Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp, vertical = 40.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        backgroundColor = if (isMuted) Color(0xFFE53935) else Color(0xFF424242),
                        onClick = { isMuted = !isMuted }
                    )

                    // Camera
                    CallControlButton(
                        icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                        backgroundColor = if (isCameraOff) Color(0xFFE53935) else Color(0xFF424242),
                        onClick = { isCameraOff = !isCameraOff }
                    )

                    // End Call (large red button)
                    FloatingActionButton(
                        onClick = { showEndDialog = true },
                        containerColor = Color(0xFFE53935),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Terminer",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Flash
                    CallControlButton(
                        icon = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        backgroundColor = if (isFlashOn) Color(0xFFFFC107) else Color(0xFF424242),
                        onClick = { isFlashOn = !isFlashOn }
                    )

                    // Speaker
                    CallControlButton(
                        icon = Icons.Default.VolumeUp,
                        backgroundColor = Color(0xFF424242),
                        onClick = { /* Toggle speaker */ }
                    )
                }
            }
        }
    }

    // End Call Dialog
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("Terminer l'appel ?") },
            text = { Text("Voulez-vous vraiment terminer cet appel ?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val userType = tokenManager.getUserTypeSync() ?: "blind"
                            callRepository.endCall(
                                callId = callId,
                                endedBy = userType,
                                endReason = "completed"
                            )
                            navController.navigate("blind_dashboard") {
                                popUpTo("active_call/$callId") { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Terminer", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = backgroundColor,
        modifier = Modifier.size(56.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}


