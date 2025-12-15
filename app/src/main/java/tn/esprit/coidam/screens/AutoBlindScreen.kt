package tn.esprit.coidam.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.api.VoiceCommandService
import tn.esprit.coidam.data.api.VoiceWebSocketClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Enums.ConnectionState
import tn.esprit.coidam.data.repository.FaceRecognitionRepository
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoBlindScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    // ✅ Services
    val voiceService = remember { VoiceCommandService(context) }
    val wsClient = remember { VoiceWebSocketClient(context) }
    val faceRepository = remember { FaceRecognitionRepository(context) }

    // ✅ États
    val isVoiceReady by voiceService.isReady.collectAsState()
    val recognizedText by voiceService.recognizedText.collectAsState()
    val connectionState by wsClient.connectionState.collectAsState()
    val voiceInstruction by wsClient.voiceInstruction.collectAsState()
    val voiceResponse by wsClient.voiceResponse.collectAsState()

    var isListening by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }
    var faceDetected by remember { mutableStateOf(false) }
    var lastRecognitionTime by remember { mutableLongStateOf(0L) }
    var lastAnnouncedPerson by remember { mutableStateOf("") }
    var detectionCount by remember { mutableIntStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // ✅ Permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )

    // ✅ Fonction pour capturer et reconnaître
    fun recognizeFace(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()

        // Limiter à une reconnaissance toutes les 3 secondes
        if (currentTime - lastRecognitionTime < 3000) {
            return
        }

        scope.launch {
            try {
                // Reconnaissance avec le repository
                val result = faceRepository.recognizeFaces(bitmap, saveToHistory = true)

                result.onSuccess { response ->
                    lastRecognitionTime = currentTime
                    detectionCount++

                    if (response.recognizedPersons.isNotEmpty()) {
                        val person = response.recognizedPersons.first()

                        // Annoncer seulement si c'est une nouvelle personne
                        if (person.name != lastAnnouncedPerson) {
                            lastAnnouncedPerson = person.name

                            val announcement = if (person.isRecognized) {
                                "Je vois ${person.name}${person.relation?.let { ", votre $it" } ?: ""}. " +
                                        "Confiance ${person.confidencePercentage} pourcent."
                            } else {
                                "Personne inconnue détectée."
                            }

                            voiceService.speak(announcement)

                            // Envoyer via WebSocket
                            wsClient.sendVoiceCommand("face-recognition-result: ${person.name}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoBlind", "Recognition error: ${e.message}")
            }
        }
    }

    // ✅ Simulateur de capture périodique (remplace CameraX)
    LaunchedEffect(isCameraActive) {
        if (isCameraActive) {
            while (true) {
                delay(3000) // Capture toutes les 3 secondes
                currentBitmap?.let { bitmap ->
                    faceDetected = true
                    recognizeFace(bitmap)
                }
            }
        }
    }

    // ✅ Gérer les commandes vocales
    LaunchedEffect(recognizedText) {
        if (recognizedText.isNotEmpty()) {
            val lower = recognizedText.lowercase()

            when {
                lower.contains("quitter") || lower.contains("sortir") ||
                        lower.contains("exit") || lower.contains("quit") -> {
                    showExitDialog = true
                }
                else -> {
                    // Envoyer au serveur
                    wsClient.sendVoiceCommand(recognizedText)
                }
            }
        }
    }

    // ✅ Gérer les réponses du serveur
    LaunchedEffect(voiceResponse) {
        voiceResponse?.let { response ->
            response.speakText?.let { voiceService.speak(it) }

            when (response.action) {
                "navigate-back" -> {
                    delay(500)
                    navController.popBackStack()
                }
            }
        }
    }

    // ✅ Initialisation
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            delay(500)
            wsClient.connect()
            delay(1000)
            voiceService.startListening()
            isListening = true
            delay(1000)
            isCameraActive = true
            voiceService.speak("Mode automatique activé. Caméra et micro en marche.")
        }
    }

    // ✅ Cleanup
    DisposableEffect(Unit) {
        onDispose {
            isCameraActive = false
            voiceService.cleanup()
            wsClient.disconnect()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Simulateur de caméra (fond noir avec texte)
        if (isCameraActive) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onFrameCaptured = { bitmap ->
                    currentBitmap = bitmap
                    faceDetected = true
                    recognizeFace(bitmap)
                }
            )

        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF212121)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Caméra désactivée",
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        }

        // ✅ Overlay d'informations
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // En-tête
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mode Auto",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // État WebSocket
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = when (connectionState) {
                                            ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                            ConnectionState.CONNECTING -> Color(0xFFFFC107)
                                            else -> Color(0xFFF44336)
                                        },
                                        shape = CircleShape
                                    )
                            )

                            // État micro
                            if (isListening) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Listening",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Indicateur de détection
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (faceDetected) Icons.Default.Face else Icons.Default.Search,
                            contentDescription = null,
                            tint = if (faceDetected) Color(0xFF4CAF50) else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (faceDetected) "Visage détecté" else "Recherche en cours...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Text(
                        text = "Détections: $detectionCount",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ✅ Commandes vocales
            if (recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Commande:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = recognizedText,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Boutons de contrôle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Toggle micro
                FloatingActionButton(
                    onClick = {
                        if (isListening) {
                            voiceService.stopListening()
                            isListening = false
                            voiceService.speak("Micro désactivé")
                        } else {
                            voiceService.startListening()
                            isListening = true
                            voiceService.speak("Micro activé")
                        }
                    },
                    containerColor = if (isListening) Color(0xFF4CAF50) else Color.White
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Toggle Mic",
                        tint = if (isListening) Color.White else Color.Black
                    )
                }

                // Toggle caméra
                FloatingActionButton(
                    onClick = {
                        if (isCameraActive) {
                            isCameraActive = false
                            faceDetected = false
                            voiceService.speak("Caméra désactivée")
                        } else {
                            isCameraActive = true
                            voiceService.speak("Caméra activée")
                        }
                    },
                    containerColor = if (isCameraActive) Color(0xFF4CAF50) else Color.White
                ) {
                    Icon(
                        imageVector = if (isCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle Camera",
                        tint = if (isCameraActive) Color.White else Color.Black
                    )
                }

                // Aide
                FloatingActionButton(
                    onClick = {
                        wsClient.requestHelp()
                    },
                    containerColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Help",
                        tint = Color.Black
                    )
                }

                // Quitter
                FloatingActionButton(
                    onClick = {
                        showExitDialog = true
                    },
                    containerColor = Color(0xFFF44336)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Exit",
                        tint = Color.White
                    )
                }
            }
        }
    }

    // ✅ Dialog de permissions
    if (!permissionsState.allPermissionsGranted) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Permissions requises") },
            text = {
                Column {
                    Text("Cette fonctionnalité nécessite :")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• Microphone : commandes vocales")
                    Text("• Caméra : reconnaissance faciale")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                ) {
                    Text("Autoriser")
                }
            }
        )
    }

    // ✅ Dialog de sortie
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Quitter le mode automatique ?") },
            text = { Text("La caméra et le micro seront désactivés.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isCameraActive = false
                            voiceService.cleanup()
                            wsClient.disconnect()
                            voiceService.speak("Mode automatique désactivé")
                            delay(1000)
                            navController.popBackStack()
                        }
                    }
                ) {
                    Text("Oui, quitter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}


@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrameCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    // Convertir ImageProxy en Bitmap
                    val bitmap = imageProxy.toBitmap()
                    onFrameCaptured(bitmap)
                    imageProxy.close()
                }
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
    }
}

// Extension helper
fun ImageProxy.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
