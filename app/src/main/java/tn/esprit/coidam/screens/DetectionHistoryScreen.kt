package tn.esprit.coidam.screens

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.models.DetectionHistory.DetectionHistory

import tn.esprit.coidam.data.repository.DetectionHistoryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { DetectionHistoryRepository(context) }

    var detections by remember { mutableStateOf<List<DetectionHistory>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var selectedDetection by remember { mutableStateOf<DetectionHistory?>(null) }

    // ✅ CHARGER L'HISTORIQUE AU DÉMARRAGE
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val result = repository.getAll()
            isLoading = false

            result.onSuccess { list ->
                detections = list
            }.onFailure { exception ->
                dialogMessage = "Failed to load history: ${exception.message}"
                showDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detection History") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFECF9FD))
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF70CEE3)
                    )
                }
                detections.isEmpty() -> {
                    EmptyHistoryView()
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(detections) { detection ->
                            DetectionHistoryCard(
                                detection = detection,
                                repository = repository,
                                onClick = { selectedDetection = detection }
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ DIALOG DÉTAILS
    selectedDetection?.let { detection ->
        DetectionDetailDialog(
            detection = detection,
            repository = repository,
            onDismiss = { selectedDetection = null },
            onDelete = {
                scope.launch {
                    val result = repository.delete(detection.id)
                    result.onSuccess {
                        detections = detections.filter { it.id != detection.id }
                        selectedDetection = null
                    }.onFailure { exception ->
                        dialogMessage = "Failed to delete: ${exception.message}"
                        showDialog = true
                    }
                }
            }
        )
    }

    // ✅ DIALOG ERREUR
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Error") },
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
fun EmptyHistoryView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFBDBDBD)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "No Detections Yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Face detection history will appear here",
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )
    }
}

@Composable
fun DetectionHistoryCard(
    detection: DetectionHistory,
    repository: DetectionHistoryRepository,
    onClick: () -> Unit
) {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    val scope = rememberCoroutineScope()

    // ✅ CHARGER L'IMAGE
    LaunchedEffect(detection.id) {
        scope.launch {
            val result = repository.loadImage(detection.capturedImage)
            result.onSuccess { bitmap ->
                capturedImage = bitmap
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En-tête avec date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFF70CEE3),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = detection.getFormattedDate(),
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }

                // Badge taux de reconnaissance
                Text(
                    text = "${detection.recognitionRate.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = if (detection.recognitionRate > 70)
                                Color(0xFF4CAF50) else Color(0xFFFF9800),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Image capturée
            if (capturedImage != null) {
                Image(
                    bitmap = capturedImage!!.asImageBitmap(),
                    contentDescription = "Captured",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        color = Color(0xFF70CEE3)
                    )
                }
            }

            // Statistiques
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(
                    icon = Icons.Default.People,
                    value = detection.totalFacesDetected.toString(),
                    label = "Total",
                    color = Color(0xFF2196F3)
                )
                StatBadge(
                    icon = Icons.Default.CheckCircle,
                    value = detection.knownFacesCount.toString(),
                    label = "Known",
                    color = Color(0xFF4CAF50)
                )
                StatBadge(
                    icon = Icons.Default.Help,
                    value = detection.unknownFacesCount.toString(),
                    label = "Unknown",
                    color = Color(0xFFFF9800)
                )
            }

            // Personnes détectées (preview)
            if (detection.detectedPersons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Detected:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575)
                    )
                    detection.detectedPersons.take(3).forEach { person ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (person.isRecognized)
                                    Icons.Default.CheckCircle else Icons.Default.Help,
                                contentDescription = null,
                                tint = if (person.isRecognized)
                                    Color(0xFF4CAF50) else Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = person.name,
                                fontSize = 14.sp,
                                color = Color(0xFF424242)
                            )
                        }
                    }
                    if (detection.detectedPersons.size > 3) {
                        Text(
                            text = "+ ${detection.detectedPersons.size - 3} more",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF757575)
        )
    }
}

@Composable
fun DetectionDetailDialog(
    detection: DetectionHistory,
    repository: DetectionHistoryRepository,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(detection.id) {
        scope.launch {
            val result = repository.loadImage(detection.capturedImage)
            result.onSuccess { capturedImage = it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Detection Details") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Image
                item {
                    if (capturedImage != null) {
                        Image(
                            bitmap = capturedImage!!.asImageBitmap(),
                            contentDescription = "Captured",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Statistiques
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Statistics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text("Total Faces: ${detection.totalFacesDetected}")
                        Text("Known Faces: ${detection.knownFacesCount}")
                        Text("Unknown Faces: ${detection.unknownFacesCount}")
                        Text("Recognition Rate: ${detection.recognitionRate.toInt()}%")
                    }
                }

                // Personnes détectées
                item {
                    Text(
                        text = "Detected Persons",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                items(detection.detectedPersons) { person ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (person.isRecognized)
                                Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = person.name,
                                fontWeight = FontWeight.Bold
                            )
                            person.relation?.let {
                                Text(text = "Relation: $it", fontSize = 14.sp)
                            }
                            if (person.isRecognized) {
                                Text(
                                    text = "Confidence: ${person.confidencePercentage}%",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text("Delete", color = Color.Red)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Detection?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}