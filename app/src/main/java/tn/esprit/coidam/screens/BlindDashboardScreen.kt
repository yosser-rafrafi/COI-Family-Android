package tn.esprit.coidam.screens

import android.Manifest
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.api.VoiceCommandService
import tn.esprit.coidam.data.api.VoiceWebSocketClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Enums.ConnectionState
import tn.esprit.coidam.data.repository.WebSocketManager


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BlindDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    // ✅ Services (using singleton)
    val voiceService = remember { VoiceCommandService(context) }
    val wsClient = remember { VoiceWebSocketClient.getInstance(context) }

    // ✅ États
    val isVoiceReady by voiceService.isReady.collectAsState()
    val recognizedText by voiceService.recognizedText.collectAsState()
    val connectionState by wsClient.connectionState.collectAsState()
    val voiceInstruction by wsClient.voiceInstruction.collectAsState()
    val voiceResponse by wsClient.voiceResponse.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var showWelcomeDialog by remember { mutableStateOf(true) }
    var showSideMenu by remember { mutableStateOf(false) }

    // ✅ Permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )

    // ✅ Connexion WebSocket au démarrage (only if needed)
    LaunchedEffect(Unit) {
        if (permissionsState.allPermissionsGranted) {
            if (wsClient.shouldReconnect()) {
                delay(500)
                wsClient.connect()
            } else {
                Log.d("BlindDashboard", "✅ Socket already connected, reusing")
            }
        }
    }

    // ✅ Gérer les instructions vocales du serveur
    LaunchedEffect(voiceInstruction) {
        voiceInstruction?.let { instruction ->
            when (instruction.action) {
                "speak" -> {
                    voiceService.speak(instruction.text)
                }
                "open-camera" -> {
                    voiceService.speak(instruction.text)
                    delay(1500)
                    navController.navigate("auto_blind")
                }
            }

            instruction.navigation?.let { route ->
                delay(1000)
                navController.navigate(route.removePrefix("/"))
            }
        }
    }

    // ✅ Gérer les réponses aux commandes vocales
    LaunchedEffect(voiceResponse) {
        voiceResponse?.let { response ->
            // Annoncer la transcription si disponible
            response.transcription?.let { transcription ->
                voiceService.speak("J'ai compris: $transcription")
                delay(1000)
            }

            // Annoncer la réponse
            response.speakText?.let { text ->
                voiceService.speak(text)
            }

            // Exécuter l'action
            when (response.action) {
                "open-camera" -> {
                    delay(1500)
                    navController.navigate("auto_blind")
                }
                "send-alert" -> {
                    delay(1000)
                    navController.navigate("send_alert")
                }
                "start-call" -> {
                    delay(1000)
                    navController.navigate("start_call")
                }
                "navigate" -> {
                    response.navigation?.let { route ->
                        delay(1000)
                        navController.navigate(route.removePrefix("/"))
                    }
                }
                "navigate-back" -> {
                    delay(500)
                    navController.popBackStack()
                }
            }
        }
    }

    // ✅ Gérer les commandes vocales locales avec envoi au serveur
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            // ✅ MODE 1: Envoyer le TEXTE transcrit localement
            wsClient.sendVoiceCommand(recognizedText)

            // Alternative - MODE 2: Envoyer l'AUDIO pour transcription serveur
            // Décommenter si vous voulez utiliser le serveur Whisper:
            // wsClient.sendVoiceCommandAudio(audioByteArray, "fr")
        }
    }

    // ✅ Démarrer l'écoute quand tout est prêt
    LaunchedEffect(isVoiceReady, connectionState) {
        if (isVoiceReady && connectionState == ConnectionState.CONNECTED) {
            delay(2000)
            showWelcomeDialog = false
            voiceService.startListening()
            isListening = true
        }
    }

    // ✅ Nettoyer à la sortie (only voice service, not WebSocket)
    DisposableEffect(Unit) {
        onDispose {
            voiceService.cleanup()
            // Don't disconnect WebSocket here - let MainActivity handle it
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFECF9FD))
        ) {
            // Header avec gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF70CEE3),
                                Color(0xFF129FA9)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showSideMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "CO-I Family",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )

                                // ✅ Indicateur de connexion
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    // État WebSocket
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = when (connectionState) {
                                                    ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                                    ConnectionState.CONNECTING -> Color(0xFFFFC107)
                                                    else -> Color(0xFFF44336)
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))

                                    // État d'écoute
                                    if (isListening) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Listening",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "En écoute...",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // ✅ Bouton micro (toggle)
                            FloatingActionButton(
                                onClick = {
                                    if (isListening) {
                                        voiceService.stopListening()
                                        isListening = false
                                        voiceService.speak("Écoute désactivée")
                                    } else {
                                        voiceService.startListening()
                                        isListening = true
                                        voiceService.speak("Écoute activée")
                                    }
                                },
                                containerColor = if (isListening) Color(0xFF4CAF50) else Color.White,
                                modifier = Modifier.size(50.dp)
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                                    contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                                    tint = if (isListening) Color.White else Color(0xFF70CEE3)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Bouton aide
                            IconButton(
                                onClick = {
                                    wsClient.requestHelp()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Help,
                                    contentDescription = "Help",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // ✅ Texte reconnu (debug)
            if (recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Dernière commande:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = recognizedText,
                            fontSize = 14.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }

            // Summary Statistics Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(number = "5", label = "People", color = Color(0xFF70CEE3))
                    StatItem(number = "3", label = "Alerts", color = Color(0xFFFFC107))
                    StatItem(number = "12", label = "Photos", color = Color(0xFF4CAF50))
                }
            }

            // Main Menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Main Menu",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val menuItems = listOf(
                    MenuItem("Send Alert", Icons.Default.Notifications, Color(0xFFFFC107), "send_alert"),
                    MenuItem("Appel Vidéo", Icons.Default.Videocam, Color(0xFF4CAF50), "start_call"),
                    MenuItem("Face Recognition", Icons.Default.Face, Color(0xFF9C27B0), "auto_blind"), // ✅ Changed to auto_blind
                    MenuItem("Auto blind", Icons.Default.RemoveRedEye, Color(0xFF9C27B0), "auto_blind"),
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(menuItems) { item ->
                        MenuCard(
                            item = item,
                            onClick = {
                                voiceService.speak(item.title)
                                navController.navigate(item.route)
                            }
                        )
                    }
                }
            }
        }

        // Side Menu Overlay
        SideMenuView(
            isShowing = showSideMenu,
            onDismiss = { showSideMenu = false },
            navController = navController
        )
    }

    // ✅ Dialog de bienvenue
    if (showWelcomeDialog && !permissionsState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = { showWelcomeDialog = false },
            title = { Text("Bienvenue") },
            text = {
                Column {
                    Text("Cette application nécessite les permissions suivantes :")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Microphone : pour les commandes vocales")
                    Text("• Caméra : pour la reconnaissance faciale")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionsState.launchMultiplePermissionRequest()
                        showWelcomeDialog = false
                    }
                ) {
                    Text("Autoriser")
                }
            }
        )
    }

    // Dialog de déconnexion
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Déconnexion") },
            text = { Text("Voulez-vous vraiment vous déconnecter ?") },
            confirmButton = {
                TextButton(
                onClick = {
                    scope.launch {
                        // ✅ Get singleton instances
                        val webSocketManager = WebSocketManager.getInstance(context)
                        val voiceSocketManager = VoiceWebSocketClient.getInstance(context)
                        
                        // Clear token
                        tokenManager.clear()
                        
                        // Cleanup services
                        voiceService.cleanup()
                        
                        // ✅ Disconnect both sockets
                        webSocketManager.disconnect()
                        voiceSocketManager.disconnect()
                        
                        // Navigate to login
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            ) {
                Text("Oui")
            }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Non")
                }
            }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BlindDashboardScreenPreview() {
    MaterialTheme {
        BlindDashboardScreen(navController = rememberNavController())
    }
}


