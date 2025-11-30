package tn.esprit.coidam.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.api.Detection
import tn.esprit.coidam.data.repository.PhotoRepository
import tn.esprit.coidam.data.repository.ReconnaissanceRepository
import java.io.File
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BlindCameraScreen(navController: androidx.navigation.NavController? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val photoRepository = remember { PhotoRepository(context) }
    val reconnaissanceRepository = remember { ReconnaissanceRepository(context) }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    var hasPermissions by remember { mutableStateOf(permissionsState.allPermissionsGranted) }

    LaunchedEffect(permissionsState) {
        if (!hasPermissions) {
            permissionsState.launchMultiplePermissionRequest()
        }
        hasPermissions = permissionsState.allPermissionsGranted
    }

    if (hasPermissions) {
        CameraViewWithObjectDetection(
            navController = navController,
            reconnaissanceRepository = reconnaissanceRepository,
            photoRepository = photoRepository
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text(text = "Request Permissions")
            }
        }
    }
}

@Composable
internal fun CameraViewWithObjectDetection(
    navController: androidx.navigation.NavController?,
    reconnaissanceRepository: ReconnaissanceRepository,
    photoRepository: PhotoRepository
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Text-to-Speech
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isTtsReady by remember { mutableStateOf(false) }

    // Detection state
    var isDetecting by remember { mutableStateOf(false) }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var detectionError by remember { mutableStateOf<String?>(null) }
    
    // Photo saving state
    var lastCapturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var isSavingPhoto by remember { mutableStateOf(false) }

    // Initialize Text-to-Speech
    LaunchedEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.getDefault())
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                } else {
                    isTtsReady = true
                    // Welcome message
                    tts?.speak("Camera ready. Say take photo to capture, detect objects to analyze, or save photo to save the last photo.", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            } else {
                Log.e("TTS", "TTS initialization failed")
            }
        }
    }

    // Cleanup TTS
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Speech Recognition
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechRecognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
    }

    // Function to describe detections vocally
    fun describeDetections(detections: List<Detection>) {
        if (detections.isEmpty()) {
            tts?.speak("No objects detected.", TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }

        val description = buildString {
            append("I detected ${detections.size} object")
            if (detections.size > 1) append("s")
            append(": ")
            
            // Group by class name
            val grouped = detections.groupBy { it.className }
            grouped.forEach { (className, items) ->
                val count = items.size
                if (count == 1) {
                    val confidence = (items.first().confidence * 100).toInt()
                    append("$className with ${confidence}% confidence. ")
                } else {
                    val avgConfidence = (items.map { it.confidence }.average() * 100).toInt()
                    append("$count $className with ${avgConfidence}% confidence. ")
                }
            }
        }
        
        tts?.speak(description, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // Function to detect objects in captured image
    fun detectObjectsInImage(uri: Uri) {
        coroutineScope.launch {
            isDetecting = true
            detectionError = null
            detections = emptyList()
            
            reconnaissanceRepository.detectObjects(uri, minConfidence = 0.25)
                .onSuccess { response ->
                    detections = response.detections
                    describeDetections(response.detections)
                    Toast.makeText(context, "Detected ${response.count} objects", Toast.LENGTH_SHORT).show()
                }
                .onFailure { exception ->
                    detectionError = exception.message
                    val errorMsg = "Detection failed: ${exception.message}"
                    tts?.speak(errorMsg, TextToSpeech.QUEUE_FLUSH, null, null)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("BlindCameraScreen", "❌ Detection failed", exception)
                }
            
            isDetecting = false
        }
    }

    // Function to save photo in background
    fun savePhoto(uri: Uri) {
        if (isSavingPhoto) {
            tts?.speak("Photo is already being saved", TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }
        
        coroutineScope.launch {
            isSavingPhoto = true
            tts?.speak("Saving photo", TextToSpeech.QUEUE_FLUSH, null, null)
            
            photoRepository.uploadPhoto(uri, null, null, null, null)
                .onSuccess { photo ->
                    val successMsg = "Photo saved successfully"
                    tts?.speak(successMsg, TextToSpeech.QUEUE_FLUSH, null, null)
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                    Log.d("BlindCameraScreen", "✅ Photo saved successfully: ${photo.id}")
                }
                .onFailure { exception ->
                    val errorMsg = "Failed to save photo: ${exception.message}"
                    tts?.speak(errorMsg, TextToSpeech.QUEUE_FLUSH, null, null)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("BlindCameraScreen", "❌ Photo save failed: ${exception.message}", exception)
                }
            
            isSavingPhoto = false
        }
    }

    DisposableEffect(Unit) {
        val recognitionListener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                if (error != SpeechRecognizer.ERROR_CLIENT) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            speechRecognizer.startListening(speechRecognizerIntent)
                        } catch (e: Exception) {
                            // Ignore if already listening
                        }
                    }, 500)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    for (result in matches) {
                        val lowerResult = result.lowercase(Locale.getDefault())

                        // Photo capture commands
                        val capturePatterns = listOf(
                            "take photo", "take a photo", "take the photo",
                            "capture", "capture photo", "capture a photo",
                            "snap", "snap photo", "snap a photo",
                            "take picture", "take a picture", "take the picture"
                        )

                        val shouldCapture = capturePatterns.any { pattern ->
                            lowerResult.contains(pattern)
                        }

                        if (shouldCapture) {
                            Toast.makeText(context, "Taking photo...", Toast.LENGTH_SHORT).show()
                            tts?.speak("Taking photo", TextToSpeech.QUEUE_FLUSH, null, null)
                            takePhoto(context, imageCapture) { uri ->
                                // Store the captured photo URI
                                lastCapturedPhotoUri = uri
                                // Automatically detect objects after capture
                                detectObjectsInImage(uri)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    speechRecognizer.startListening(speechRecognizerIntent)
                                } catch (e: Exception) {
                                    // Ignore if already listening
                                }
                            }, 3000)
                            return
                        }

                        // Save photo commands
                        val savePatterns = listOf(
                            "save photo", "save the photo", "save picture",
                            "save", "save image", "upload photo"
                        )

                        val shouldSave = savePatterns.any { pattern ->
                            lowerResult.contains(pattern)
                        }

                        if (shouldSave) {
                            if (lastCapturedPhotoUri == null) {
                                val noPhotoMsg = "No photo to save. Please take a photo first"
                                tts?.speak(noPhotoMsg, TextToSpeech.QUEUE_FLUSH, null, null)
                                Toast.makeText(context, noPhotoMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                savePhoto(lastCapturedPhotoUri!!)
                            }
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    speechRecognizer.startListening(speechRecognizerIntent)
                                } catch (e: Exception) {
                                    // Ignore if already listening
                                }
                            }, 2000)
                            return
                        }

                        // Object detection commands
                        val detectPatterns = listOf(
                            "detect objects", "detect", "what do you see",
                            "what's in the image", "analyze", "scan"
                        )

                        val shouldDetect = detectPatterns.any { pattern ->
                            lowerResult.contains(pattern)
                        }

                        if (shouldDetect && detections.isEmpty()) {
                            Toast.makeText(context, "Please take a photo first", Toast.LENGTH_SHORT).show()
                            tts?.speak("Please take a photo first", TextToSpeech.QUEUE_FLUSH, null, null)
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    speechRecognizer.startListening(speechRecognizerIntent)
                                } catch (e: Exception) {
                                    // Ignore if already listening
                                }
                            }, 2000)
                            return
                        }

                        // Close camera commands
                        val closePatterns = listOf(
                            "close camera", "close the camera",
                            "exit camera", "exit the camera",
                            "close", "go back", "back"
                        )

                        val shouldClose = closePatterns.any { pattern ->
                            lowerResult.contains(pattern)
                        }

                        if (shouldClose) {
                            Toast.makeText(context, "Closing camera...", Toast.LENGTH_SHORT).show()
                            tts?.speak("Closing camera", TextToSpeech.QUEUE_FLUSH, null, null)
                            navController?.navigateUp()
                            return
                        }
                    }
                }
                // Continue listening if no command matched
                try {
                    speechRecognizer.startListening(speechRecognizerIntent)
                } catch (e: Exception) {
                    // Ignore if already listening
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Handle partial results for faster response
                val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                partialMatches?.let { matches ->
                    for (result in matches) {
                        val lowerResult = result.lowercase(Locale.getDefault())
                        if (lowerResult.contains("capture") ||
                            lowerResult.contains("take photo") ||
                            lowerResult.contains("photo") ||
                            lowerResult.contains("detect")
                        ) {
                            // Don't act on partial results, wait for final results
                        }
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(recognitionListener)

        // Start listening when component is created
        try {
            speechRecognizer.startListening(speechRecognizerIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Speech recognition error: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        onDispose {
            try {
                speechRecognizer.stopListening()
                speechRecognizer.destroy()
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = {
                val previewView = PreviewView(it)
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (exc: Exception) {
                    Log.e("BlindCameraScreen", "Use case binding failed", exc)
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Detection Results Overlay
        if (detections.isNotEmpty() || isDetecting || detectionError != null || isSavingPhoto) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSavingPhoto) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Saving photo...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    } else if (isDetecting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Detecting objects...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    } else if (detectionError != null) {
                        Text(
                            text = "Error: $detectionError",
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    } else if (detections.isNotEmpty()) {
                        Text(
                            text = "Detected ${detections.size} object${if (detections.size > 1) "s" else ""}",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        detections.take(5).forEach { detection ->
                            Text(
                                text = "• ${detection.className} (${(detection.confidence * 100).toInt()}%)",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        if (detections.size > 5) {
                            Text(
                                text = "... and ${detections.size - 5} more",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Uri) -> Unit
) {
    val photoFile = File(
        context.filesDir,
        "${System.currentTimeMillis()}.jpg"
    )

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(context, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                Log.e("BlindCameraScreen", "Photo capture failed", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }
        }
    )
}
