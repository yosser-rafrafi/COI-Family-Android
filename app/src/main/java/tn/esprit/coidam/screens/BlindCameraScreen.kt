package tn.esprit.coidam.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.PhotoRepository
import java.io.File
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BlindCameraScreen(navController: androidx.navigation.NavController? = null) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { PhotoRepository(context) }

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
        CameraViewWithSpeech(
            navController = navController,
            onImageCaptured = { uri ->
            coroutineScope.launch {
                repository.uploadPhoto(uri, null, null, null).onSuccess { photo ->
                    android.util.Log.d("BlindCameraScreen", "✅ Photo uploaded successfully: ${photo.id}")
                    Toast.makeText(context, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
                    // Don't close camera automatically, keep it open for more photos
                }.onFailure { exception ->
                    android.util.Log.e("BlindCameraScreen", "❌ Photo upload failed: ${exception.message}", exception)
                    Toast.makeText(context, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
            }
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
internal fun CameraViewWithSpeech(
    navController: androidx.navigation.NavController?,
    onImageCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechRecognizerIntent = remember { 
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
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
                // Continue listening even on errors (except client error)
                if (error != SpeechRecognizer.ERROR_CLIENT) {
                    // Restart listening after a short delay
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
                        
                        // Check for photo capture commands - more specific patterns
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
                            takePhoto(context, imageCapture, onImageCaptured)
                            // Continue listening after taking photo
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                try {
                                    speechRecognizer.startListening(speechRecognizerIntent)
                                } catch (e: Exception) {
                                    // Ignore if already listening
                                }
                            }, 2000) // Wait 2 seconds before listening again
                            return
                        }
                        
                        // Check for close camera commands - more specific patterns
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
                            lowerResult.contains("photo")) {
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
                //Log.e(TAG, "Use case binding failed", exc)
            }
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, onImageCaptured: (Uri) -> Unit) {
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
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                onImageCaptured(savedUri)
            }
        }
    )
}
