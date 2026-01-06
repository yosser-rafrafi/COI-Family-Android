package tn.esprit.coidam.screens

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import tn.esprit.coidam.data.models.Enums.DetectionState
import tn.esprit.coidam.data.repository.FaceRecognitionRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AutoBlindScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }

    // ✅ Services (using singleton)
    val voiceService = remember { VoiceCommandService(context) }
    val wsClient = remember { VoiceWebSocketClient.getInstance(context) }
    val faceRepository = remember { FaceRecognitionRepository(context) }

    // ✅ États WebSocket et Voice
    val isVoiceReady by voiceService.isReady.collectAsState()
    val recognizedText by voiceService.recognizedText.collectAsState()
    val connectionState by wsClient.connectionState.collectAsState()
    val voiceInstruction by wsClient.voiceInstruction.collectAsState()
    val voiceResponse by wsClient.voiceResponse.collectAsState()

    // ✅ États UI
    var isListening by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // ✅ MACHINE À ÉTATS - Variables d'état
    var detectionState by remember { mutableStateOf(DetectionState.IDLE) }
    var faceFirstDetectedAt by remember { mutableLongStateOf(0L) }
    var faceLastSeenAt by remember { mutableLongStateOf(0L) }
    var lastRecognitionAt by remember { mutableLongStateOf(0L) }
    var lastAnalysisTime by remember { mutableLongStateOf(0L) }
    var currentPersonId by remember { mutableStateOf<String?>(null) }
    var currentPersonName by remember { mutableStateOf<String?>(null) }
    var hasAnnouncedDetection by remember { mutableStateOf(false) }
    var hasAnnouncedPerson by remember { mutableStateOf(false) }
    var recognitionAttempts by remember { mutableIntStateOf(0) }
    var consecutiveFramesWithFace by remember { mutableIntStateOf(0) }
    var consecutiveFramesWithoutFace by remember { mutableIntStateOf(0) }
    var detectionCount by remember { mutableIntStateOf(0) }
    var lastFacePosition by remember { mutableStateOf<android.graphics.RectF?>(null) }  // ✅ Tracker position du visage
    
    // ✅ Configuration (ULTRA-OPTIMISÉ pour réactivité maximale - utilisateur aveugle)
    val STABILITY_THRESHOLD_MS = 300L              // ✅ Réduit à 300ms pour déclenchement ultra-rapide
    val DISAPPEAR_THRESHOLD_MS = 2000L             // ✅ Réduit à 2s sans visage = reset rapide (pour changement de direction)
    val RECOGNITION_COOLDOWN_MS = 1000L            // ✅ Réduit à 1s entre deux reconnaissances (réactivité maximale)
    val MIN_CONFIDENCE = 0.3f                      // Confiance minimum pour annoncer
    val MAX_RECOGNITION_ATTEMPTS = 3               // Maximum 3 captures
    val FRAME_ANALYSIS_INTERVAL_MS = 200L          // ✅ Réduit à 200ms entre analyses (plus réactif)
    val MAX_FRAMES_WITHOUT_FACE_BEFORE_RESET = 10  // ✅ Réduit à 10 frames (~2s) pour reset rapide
    
    // ✅ ML Kit Face Detector
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setMinFaceSize(0.15f)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }
    
    // ✅ Détection locale de visage (ML Kit) avec tracking de position
    suspend fun detectFaceLocally(bitmap: Bitmap): Pair<Boolean, android.graphics.RectF?> {
        return suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        // Retourner le bounding box du premier visage
                        val face = faces[0]
                        val bounds = face.boundingBox
                        val rectF = android.graphics.RectF(
                            bounds.left.toFloat(),
                            bounds.top.toFloat(),
                            bounds.right.toFloat(),
                            bounds.bottom.toFloat()
                        )
                        continuation.resume(Pair(true, rectF))
                    } else {
                        continuation.resume(Pair(false, null))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AutoBlind", "Face detection failed: ${e.message}")
                    continuation.resume(Pair(false, null))
                }
        }
    }
    
    // ✅ Vérifier si le visage a significativement changé de position (plus sensible pour changement de direction)
    fun hasFacePositionChanged(oldRect: android.graphics.RectF?, newRect: android.graphics.RectF?): Boolean {
        if (oldRect == null || newRect == null) return true
        
        // Calculer le déplacement du centre
        val oldCenterX = (oldRect.left + oldRect.right) / 2
        val oldCenterY = (oldRect.top + oldRect.bottom) / 2
        val newCenterX = (newRect.left + newRect.right) / 2
        val newCenterY = (newRect.top + newRect.bottom) / 2
        
        val deltaX = kotlin.math.abs(newCenterX - oldCenterX)
        val deltaY = kotlin.math.abs(newCenterY - oldCenterY)
        
        // ✅ Utiliser la largeur du visage comme référence (20% de déplacement - plus sensible)
        val faceWidth = oldRect.right - oldRect.left
        val threshold = faceWidth * 0.2f  // Réduit de 30% à 20% pour détecter plus rapidement
        
        val changed = deltaX > threshold || deltaY > threshold
        if (changed) {
            Log.d("AutoBlind", "📍 Face moved significantly: deltaX=$deltaX, deltaY=$deltaY, threshold=$threshold")
        }
        
        return changed
    }
    
    // ✅ Reset vers IDLE
    fun resetToIdle() {
        Log.d("AutoBlind", "🔄 Resetting to IDLE state")
        detectionState = DetectionState.IDLE
        faceFirstDetectedAt = 0L
        faceLastSeenAt = 0L
        consecutiveFramesWithFace = 0
        consecutiveFramesWithoutFace = 0
        hasAnnouncedDetection = false
        hasAnnouncedPerson = false
        recognitionAttempts = 0
        currentPersonId = null
        currentPersonName = null
        lastFacePosition = null  // ✅ Reset position
    }
    
    // ✅ Annonce personne reconnue
    fun announceRecognizedPerson(person: tn.esprit.coidam.data.models.FaceRecognition.RecognizedPerson) {
        val confidencePercent = (person.confidence * 100).toInt()
        val announcement = buildString {
            append("C'est ${person.name}")
            person.relation?.let { append(", votre $it") }
            append(". Confiance $confidencePercent pour cent.")
        }
        Log.d("AutoBlind", "🗣️ Announcing: $announcement")
        voiceService.speak(announcement)
    }
    
    // ✅ Annonce personne inconnue
    fun announceUnknownPerson() {
        val announcement = "Personne inconnue devant vous."
        Log.d("AutoBlind", "🗣️ Announcing: $announcement")
        voiceService.speak(announcement)
    }
    
    // ✅ Reconnaissance faciale
    suspend fun startRecognition(bitmap: Bitmap) {
        val currentTime = System.currentTimeMillis()
        
        // ✅ Vérifier cooldown (mais permettre si visage a changé de position)
        val timeSinceLastRecognition = currentTime - lastRecognitionAt
        if (timeSinceLastRecognition < RECOGNITION_COOLDOWN_MS) {
            // ✅ Si le visage a changé de position, ignorer le cooldown (changement de direction)
            if (lastFacePosition != null) {
                val (hasFace, facePosition) = detectFaceLocally(bitmap)
                if (hasFace && hasFacePositionChanged(lastFacePosition, facePosition)) {
                    Log.d("AutoBlind", "🔄 Face position changed, ignoring cooldown for new detection")
                    // Réinitialiser les tentatives pour nouvelle personne
                    recognitionAttempts = 0
                    hasAnnouncedPerson = false
                    currentPersonId = null
                    currentPersonName = null
                } else {
                    Log.d("AutoBlind", "⏳ Recognition cooldown active (${timeSinceLastRecognition}ms < ${RECOGNITION_COOLDOWN_MS}ms), skipping...")
                    return
                }
            } else {
                Log.d("AutoBlind", "⏳ Recognition cooldown active, skipping...")
                return
            }
        }
        
        // Vérifier nombre de tentatives
        if (recognitionAttempts >= MAX_RECOGNITION_ATTEMPTS) {
            Log.w("AutoBlind", "⚠️ Max recognition attempts reached")
            detectionState = DetectionState.PERSON_IDENTIFIED
            voiceService.speak("Impossible de reconnaître la personne.")
            hasAnnouncedPerson = true
            return
        }
        
        // Transition: → RECOGNIZING
        detectionState = DetectionState.RECOGNIZING
        recognitionAttempts++
        lastRecognitionAt = currentTime
        
        Log.d("AutoBlind", "🔍 Starting recognition (attempt $recognitionAttempts)...")
        
        // Appel API (silencieux)
        val result = faceRepository.recognizeFaces(bitmap, saveToHistory = true)
        
        result.onSuccess { response ->
            detectionCount++
            
            if (response.facesDetected == 0) {
                Log.w("AutoBlind", "⚠️ No faces in recognition result")
                
                // Retry si pas au max
                if (recognitionAttempts < MAX_RECOGNITION_ATTEMPTS) {
                    delay(300)
                    currentBitmap?.let { startRecognition(it) }
                } else {
                    resetToIdle()
                }
                return@onSuccess
            }
            
            val person = response.recognizedPersons.firstOrNull()
            
            if (person == null) {
                // Personne inconnue
                announceUnknownPerson()
                currentPersonId = "unknown"
                currentPersonName = "Inconnu"
                hasAnnouncedPerson = true
                detectionState = DetectionState.PERSON_IDENTIFIED
                return@onSuccess
            }
            
            // Vérifier si c'est la même personne qu'avant
            if (person.personId == currentPersonId && hasAnnouncedPerson) {
                Log.d("AutoBlind", "🔇 Same person (${person.name}), staying silent...")
                detectionState = DetectionState.PERSON_IDENTIFIED
                return@onSuccess
            }
            
            // Nouvelle personne ou première annonce
            currentPersonId = person.personId
            currentPersonName = person.name
            hasAnnouncedPerson = true
            detectionState = DetectionState.PERSON_IDENTIFIED
            
            if (person.isRecognized && person.confidence >= MIN_CONFIDENCE) {
                announceRecognizedPerson(person)
            } else {
                announceUnknownPerson()
            }
        }.onFailure { error ->
            Log.e("AutoBlind", "❌ Recognition failed: ${error.message}")
            
            // Retry si pas au max
            if (recognitionAttempts < MAX_RECOGNITION_ATTEMPTS) {
                delay(500)
                currentBitmap?.let { startRecognition(it) }
            } else {
                detectionState = DetectionState.PERSON_IDENTIFIED
                voiceService.speak("Erreur de reconnaissance.")
                hasAnnouncedPerson = true
            }
        }
    }
    
    // ✅ Traitement frame caméra (MACHINE À ÉTATS)
    fun onCameraFrame(bitmap: Bitmap) {
        scope.launch {
            val currentTime = System.currentTimeMillis()
            
            // Limiter framerate d'analyse
            if (currentTime - lastAnalysisTime < FRAME_ANALYSIS_INTERVAL_MS) {
                return@launch
            }
            lastAnalysisTime = currentTime
            
            // Détection locale (rapide) avec position
            val (hasFace, facePosition) = detectFaceLocally(bitmap)
            
            when (detectionState) {
                DetectionState.IDLE -> {
                    if (hasFace) {
                        // Transition: IDLE → FACE_DETECTED
                        faceFirstDetectedAt = currentTime
                        faceLastSeenAt = currentTime
                        consecutiveFramesWithFace = 1
                        consecutiveFramesWithoutFace = 0
                        lastFacePosition = facePosition  // ✅ Sauvegarder position
                        detectionState = DetectionState.FACE_DETECTED
                        Log.d("AutoBlind", "🟢 Face detected, waiting for stability...")
                    }
                }
                
                DetectionState.FACE_DETECTED -> {
                    if (hasFace) {
                        consecutiveFramesWithFace++
                        consecutiveFramesWithoutFace = 0  // ✅ Reset counter
                        faceLastSeenAt = currentTime
                        lastFacePosition = facePosition  // ✅ Mettre à jour position
                        
                        // Vérifier stabilité
                        val stableDuration = currentTime - faceFirstDetectedAt
                        if (stableDuration >= STABILITY_THRESHOLD_MS && !hasAnnouncedDetection) {
                            // Transition: FACE_DETECTED → FACE_STABLE
                            detectionState = DetectionState.FACE_STABLE
                            hasAnnouncedDetection = true
                            voiceService.speak("Visage détecté.")
                            Log.d("AutoBlind", "🟢 Face stable (${stableDuration}ms), announcing...")
                            
                            // Lancer reconnaissance immédiatement
                            startRecognition(bitmap)
                        }
                    } else {
                        consecutiveFramesWithoutFace++
                        
                        // ✅ Tolérance améliorée: accepter quelques frames sans visage
                        if (consecutiveFramesWithoutFace > MAX_FRAMES_WITHOUT_FACE_BEFORE_RESET) {
                            Log.d("AutoBlind", "⚠️ Face disappeared before stability ($consecutiveFramesWithoutFace frames without face)")
                            resetToIdle()
                        }
                    }
                }
                
                DetectionState.FACE_STABLE -> {
                    // État transitoire, géré par startRecognition
                    if (!hasFace) {
                        consecutiveFramesWithoutFace++
                        if (consecutiveFramesWithoutFace > 10) {
                            resetToIdle()
                        }
                    } else {
                        faceLastSeenAt = currentTime
                        lastFacePosition = facePosition
                    }
                }
                
                DetectionState.RECOGNIZING -> {
                    // Reconnaissance en cours, ne rien faire
                    if (!hasFace) {
                        consecutiveFramesWithoutFace++
                        
                        // Si visage disparaît pendant reconnaissance
                        if (consecutiveFramesWithoutFace > 10) {
                            Log.w("AutoBlind", "⚠️ Face disappeared during recognition")
                            resetToIdle()
                        }
                    } else {
                        faceLastSeenAt = currentTime
                        consecutiveFramesWithoutFace = 0
                        lastFacePosition = facePosition
                    }
                }
                
                DetectionState.PERSON_IDENTIFIED -> {
                    // Annonce déjà faite, en cooldown
                    if (hasFace) {
                        faceLastSeenAt = currentTime
                        consecutiveFramesWithoutFace = 0
                        
                        // ✅ NOUVEAU: Détecter si le visage a changé de position (nouvelle personne ou changement de direction)
                        if (hasFacePositionChanged(lastFacePosition, facePosition)) {
                            Log.d("AutoBlind", "🔄 Face position changed significantly - resetting for new detection!")
                            // Reset et redémarrer la détection immédiatement
                            resetToIdle()
                            // Immédiatement passer à FACE_DETECTED (pas besoin d'attendre)
                            faceFirstDetectedAt = currentTime
                            faceLastSeenAt = currentTime
                            consecutiveFramesWithFace = 1
                            lastFacePosition = facePosition
                            detectionState = DetectionState.FACE_DETECTED
                        } else {
                            // Même personne, mettre à jour position
                            lastFacePosition = facePosition
                            
                            // ✅ Si cooldown terminé et visage toujours là, permettre nouvelle reconnaissance
                            val timeSinceLastRecognition = currentTime - lastRecognitionAt
                            if (timeSinceLastRecognition >= RECOGNITION_COOLDOWN_MS && !hasAnnouncedPerson) {
                                // Réinitialiser pour permettre nouvelle reconnaissance
                                recognitionAttempts = 0
                                hasAnnouncedPerson = false
                                currentPersonId = null
                                currentPersonName = null
                                Log.d("AutoBlind", "🔄 Cooldown finished, ready for new recognition")
                            }
                        }
                    } else {
                        consecutiveFramesWithoutFace++
                        
                        // ✅ Si visage disparaît brièvement (changement de direction), reset rapide
                        val timeSinceLastSeen = currentTime - faceLastSeenAt
                        if (timeSinceLastSeen >= DISAPPEAR_THRESHOLD_MS) {
                            Log.d("AutoBlind", "🔄 Person left (${timeSinceLastSeen}ms without face), resetting...")
                            resetToIdle()
                        } else if (consecutiveFramesWithoutFace > MAX_FRAMES_WITHOUT_FACE_BEFORE_RESET) {
                            // ✅ Reset même si pas encore 2s, si beaucoup de frames sans visage (changement rapide)
                            Log.d("AutoBlind", "🔄 Many frames without face ($consecutiveFramesWithoutFace), resetting...")
                            resetToIdle()
                        }
                    }
                }
            }
        }
    }
    
    // ✅ Permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    )

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
                    Log.d("AutoBlind", "📤 Sending voice command: $recognizedText")
                    wsClient.sendVoiceCommand(recognizedText)
                    // ✅ Réinitialiser le texte reconnu pour éviter les traitements multiples
                    // Le service redémarrera automatiquement l'écoute
                }
            }
        }
    }

    // ✅ Contrôleur capture
    val captureController = remember { CaptureController() }

    // ✅ Gérer les réponses du serveur
    LaunchedEffect(voiceResponse) {
        voiceResponse?.let { response ->
            Log.d("AutoBlind", "📨 Voice response received: action=${response.action}, message=${response.message}")
            
            response.speakText?.let { 
                voiceService.speak(it) 
            }

            when (response.action) {
                "navigate-back" -> {
                    delay(500)
                    navController.popBackStack()
                }
                "capture-photo" -> {
                    // ✅ Trigger High-Res Capture via Controller
                    Log.d("AutoBlind", "📸 Capture photo requested (Action)")
                    delay(500) // Petit délai pour que le TTS commence
                    captureController.capturePhoto?.invoke()
                }
            }
        }
    }

    // ✅ Initialisation (only connect if needed)
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            if (wsClient.shouldReconnect()) {
                delay(500)
                wsClient.connect()
            } else {
                Log.d("AutoBlind", "✅ Socket already connected, reusing")
            }
            delay(1000)
            voiceService.startListening()
            isListening = true
            delay(1000)
            isCameraActive = true
            voiceService.speak("Mode automatique activé. Caméra et micro en marche.")
        }
    }

    // ✅ Cleanup (only local resources, not WebSocket)
    DisposableEffect(Unit) {
        onDispose {
            isCameraActive = false
            voiceService.cleanup()
            // Don't disconnect WebSocket - let MainActivity handle it
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ✅ Caméra avec détection
        if (isCameraActive) {
            SmartCameraPreview(
                modifier = Modifier.fillMaxSize(),
                captureController = captureController,
                onFrameAnalyzed = { bitmap ->
                    currentBitmap = bitmap
                    onCameraFrame(bitmap) // Flux visage
                },
                onPhotoCaptured = { hdBitmap ->
                    // Flux haute qualité pour YOLO
                    Log.d("AutoBlind", "✅ HD Photo Captured. Size: ${hdBitmap.width}x${hdBitmap.height}. Sending to backend for object recognition...")
                    
                    // Vérifier la connexion WebSocket avant d'envoyer
                    if (wsClient.connectionState.value == tn.esprit.coidam.data.models.Enums.ConnectionState.CONNECTED) {
                        wsClient.sendPhotoForProcessing(hdBitmap)
                    } else {
                        Log.e("AutoBlind", "❌ Cannot send photo: WebSocket not connected (state: ${wsClient.connectionState.value})")
                        voiceService.speak("Erreur: connexion au serveur perdue. Reconnexion...")
                        // Tenter une reconnexion
                        scope.launch {
                            wsClient.connect()
                        }
                    }
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

                    // Indicateur de détection (basé sur machine à états)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val (icon, text, color) = when (detectionState) {
                            DetectionState.IDLE -> Triple(Icons.Default.Search, "Recherche en cours...", Color.White)
                            DetectionState.FACE_DETECTED -> Triple(Icons.Default.Face, "Visage en cours de stabilisation...", Color(0xFFFFC107))
                            DetectionState.FACE_STABLE -> Triple(Icons.Default.Face, "Visage stable", Color(0xFF4CAF50))
                            DetectionState.RECOGNIZING -> Triple(Icons.Default.Face, "Reconnaissance en cours...", Color(0xFF2196F3))
                            DetectionState.PERSON_IDENTIFIED -> {
                                val personName = currentPersonName ?: "Personne"
                                Triple(Icons.Default.Face, personName, Color(0xFF4CAF50))
                            }
                        }
                        
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = text,
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
                            // Don't disconnect WebSocket - let MainActivity handle it
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


// ✅ Contrôleur pour délencher la capture depuis le parent
class CaptureController {
    var capturePhoto: (() -> Unit)? = null
}

@Composable
fun SmartCameraPreview(
    modifier: Modifier = Modifier,
    captureController: CaptureController,
    onFrameAnalyzed: (Bitmap) -> Unit, // Pour le Visage (basse res)
    onPhotoCaptured: (Bitmap) -> Unit // Pour YOLO (haute res)
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

        // 1. Preview
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // 2. Image Analysis (Visage - Stream Realtime)
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    val bitmap = imageProxy.toBitmap()
                    onFrameAnalyzed(bitmap)
                    imageProxy.close()
                }
            }

        // 3. Image Capture (YOLO - Haute Qualité)
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        // Lier le contrôleur
        captureController.capturePhoto = {
            imageCapture.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val bitmap = image.toBitmap()
                        onPhotoCaptured(bitmap)
                        image.close() // Important!
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("SmartCamera", "Capture failed: ${exception.message}", exception)
                    }
                }
            )
        }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            // Bind les 3 cas d'utilisation ensemble
            cameraProvider.bindToLifecycle(
                lifecycleOwner, 
                cameraSelector, 
                preview, 
                imageAnalyzer, 
                imageCapture
            )
        } catch (e: Exception) {
            Log.e("SmartCamera", "Use case binding failed", e)
        }
    }
}

// ✅ Extension helper - Handles both YUV (ImageAnalysis) and JPEG (ImageCapture)
fun ImageProxy.toBitmap(): Bitmap {
    return when (format) {
        ImageFormat.JPEG -> {
            // ImageCapture returns JPEG - decode directly
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
        ImageFormat.YUV_420_888 -> {
            // ImageAnalysis returns YUV - convert to NV21 then JPEG
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
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
        else -> {
            // Fallback for unknown formats
            Log.e("ImageProxy", "Unsupported image format: $format")
            throw IllegalArgumentException("Unsupported image format: $format")
        }
    }
}
