package tn.esprit.coidam.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.repository.FaceRecognitionRepository
import android.provider.MediaStore
import tn.esprit.coidam.data.models.FaceRecognition.RecognizedPerson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRecognitionScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FaceRecognitionRepository(context) }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var recognitionResult by remember { mutableStateOf<List<RecognizedPerson>?>(null) }
    var isRecognizing by remember { mutableStateOf(false) }
    var isProcessingAll by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var showImageSourceDialog by remember { mutableStateOf(false) }

    // ✅ LAUNCHER POUR LA CAMÉRA
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            capturedBitmap = it
            recognitionResult = null
        }
    }

    // ✅ LAUNCHER POUR LA GALERIE
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                capturedBitmap = bitmap
                recognitionResult = null
            } catch (e: Exception) {
                dialogMessage = "Failed to load image: ${e.message}"
                showDialog = true
            }
        }
    }

    // ✅ FONCTION POUR RECONNAÎTRE LES VISAGES
    fun recognizeFaces() {
        if (capturedBitmap == null) {
            dialogMessage = "Please capture or select an image first"
            showDialog = true
            return
        }

        scope.launch {
            isRecognizing = true
            val result = repository.recognizeFaces(capturedBitmap!!, saveToHistory = true)
            isRecognizing = false

            result.onSuccess { response ->
                recognitionResult = response.recognizedPersons

                if (response.facesDetected == 0) {
                    dialogMessage = "No faces detected in the image. Try a different photo."
                    showDialog = true
                } else if (response.summary?.knownFaces == 0) {
                    dialogMessage = "${response.facesDetected} face(s) detected but none recognized."
                    showDialog = true
                }
            }.onFailure { exception ->
                dialogMessage = "Recognition failed: ${exception.message}"
                showDialog = true
            }
        }
    }

    // ✅ FONCTION POUR TRAITER TOUTES LES PERSONNES
    fun processAllPersons() {
        scope.launch {
            isProcessingAll = true
            val result = repository.processAllKnownPersons()
            isProcessingAll = false

            result.onSuccess { response ->
                dialogMessage = "Processing complete!\n" +
                        "Total: ${response.total}\n" +
                        "Processed: ${response.processed}\n" +
                        "Failed: ${response.failedCount}"
                showDialog = true
            }.onFailure { exception ->
                dialogMessage = "Processing failed: ${exception.message}"
                showDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Face Recognition") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF70CEE3),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFECF9FD))
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ✅ SECTION PRÉPARATION
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 1: Prepare Face Database",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )

                        Text(
                            text = "Process all known persons to extract their facial encodings.",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )

                        Button(
                            onClick = { processAllPersons() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessingAll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF70CEE3)
                            )
                        ) {
                            if (isProcessingAll) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Processing...")
                            } else {
                                Icon(Icons.Default.Refresh, "Process")
                                Spacer(Modifier.width(8.dp))
                                Text("Process All Persons")
                            }
                        }
                    }
                }
            }

            // ✅ SECTION CAPTURE IMAGE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 2: Capture or Select Image",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CameraAlt, "Camera")
                                Spacer(Modifier.width(4.dp))
                                Text("Camera")
                            }

                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, "Gallery")
                                Spacer(Modifier.width(4.dp))
                                Text("Gallery")
                            }
                        }

                        // Afficher l'image capturée
                        capturedBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captured",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // ✅ SECTION RECONNAISSANCE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Step 3: Recognize Faces",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF424242)
                        )

                        Button(
                            onClick = { recognizeFaces() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = capturedBitmap != null && !isRecognizing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            if (isRecognizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Recognizing...")
                            } else {
                                Icon(Icons.Default.Face, "Recognize")
                                Spacer(Modifier.width(8.dp))
                                Text("Recognize Faces")
                            }
                        }
                    }
                }
            }

            // ✅ AFFICHER LES RÉSULTATS
            recognitionResult?.let { persons ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Results (${persons.size} face(s))",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }

                items(persons) { person ->
                    PersonRecognitionCard(person)
                }
            }

            // ✅ BOUTON VERS L'HISTORIQUE
            item {
                OutlinedButton(
                    onClick = { navController.navigate("detection_history") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.History, "History")
                    Spacer(Modifier.width(8.dp))
                    Text("View Detection History")
                }
            }
        }
    }

    // ✅ DIALOG
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Information") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PersonRecognitionCard(person: RecognizedPerson) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (person.isRecognized)
                Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        if (person.isRecognized)
                            Color(0xFF4CAF50) else Color(0xFFFF9800)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (person.isRecognized)
                        Icons.Default.CheckCircle else Icons.Default.Help,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )

                person.relation?.let { relation ->
                    Text(
                        text = relation,
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }

                if (person.isRecognized) {
                    Text(
                        text = "${person.confidencePercentage}% confident",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Badge
            Text(
                text = if (person.isRecognized) "Known" else "Unknown",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = if (person.isRecognized)
                            Color(0xFF4CAF50) else Color(0xFFFF9800),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}