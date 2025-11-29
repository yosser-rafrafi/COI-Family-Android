package tn.esprit.coidam.screens

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.CallRepository
import tn.esprit.coidam.data.models.Call
import tn.esprit.coidam.data.models.Enums.CallType


// IncomingCallScreen.kt
@Composable
fun IncomingCallScreen(
    navController: NavController,
    callId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callRepository = remember { CallRepository(context) }

    var call by remember { mutableStateOf<Call?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    // Charger les détails de l'appel
    LaunchedEffect(callId) {
        val result = callRepository.getCall(callId)
        result.onSuccess {
            call = it
        }.onFailure {
            // Retour si erreur
            navController.popBackStack()
        }
        isLoading = false
    }

    // Jouer une sonnerie (optionnel)
    DisposableEffect(Unit) {
        val ringtone = RingtoneManager.getRingtone(
            context,
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        ringtone.play()

        // Vibration
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 1000, 500, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            vibrator.vibrate(pattern, 0)
        }

        onDispose {
            ringtone.stop()
            vibrator.cancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E),
                        Color(0xFF0D47A1)
                    )
                )
            )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (call != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // Titre
                Text(
                    fontSize = 24.sp,
                    text = "Appel ${if (call!!.callType == CallType.VIDEO) "vidéo" else "audio"} entrant",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Avatar de l'appelant
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Nom de l'appelant
                Text(
                    text = if (call!!.initiatedBy == "blind") {
                        call!!.blindUser?.let { "${it.firstName} ${it.lastName}" } ?: "Utilisateur"
                    } else {
                        call!!.companion?.let { "${it.firstName} ${it.lastName}" } ?: "Accompagnant"
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Souhaite vous appeler",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Animation pulsation
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (call!!.callType == CallType.VIDEO)
                        Icons.Default.Videocam
                        else
                            Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(60.dp))

                // Boutons Accepter / Rejeter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Bouton Rejeter (Rouge)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (!isProcessing) {
                                    isProcessing = true
                                    scope.launch {
                                        callRepository.rejectCall(callId, "declined")
                                        navController.popBackStack()
                                    }
                                }
                            },
                            containerColor = Color(0xFFE53935),
                            modifier = Modifier.size(70.dp),
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Rejeter",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rejeter",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    // Bouton Accepter (Vert)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FloatingActionButton(
                            onClick = {
                                if (!isProcessing) {
                                    isProcessing = true
                                    scope.launch {
                                        val result = callRepository.acceptCall(callId)
                                        result.onSuccess { acceptResponse ->
                                            // ✅ Navigate with call and Agora credentials
                                            navController.navigate("active_call/$callId") {
                                                popUpTo("incoming_call/$callId") { inclusive = true }
                                            }
                                        }.onFailure {
                                            isProcessing = false
                                        }
                                    }
                                }
                            },
                            containerColor = Color(0xFF4CAF50),
                            modifier = Modifier.size(70.dp),
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Accepter",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Accepter",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}