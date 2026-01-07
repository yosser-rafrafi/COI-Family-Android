package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.api.ApiClient
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Photo
import tn.esprit.coidam.data.repository.PhotoRepository
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosListScreen(navController: NavController) {
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }

    val context = LocalContext.current
    val repository = remember { PhotoRepository(context) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            android.util.Log.d("PhotosListScreen", "🔄 Loading photos...")
            val result = repository.getPhotos()
            isLoading = false

            result.onSuccess { response ->
                android.util.Log.d("PhotosListScreen", "Response received: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val photoList = response.body()!!
                    android.util.Log.d("PhotosListScreen", "✅ Loaded ${photoList.size} photos")
                    photos = photoList
                    if (photoList.isEmpty()) {
                        android.util.Log.d("PhotosListScreen", "⚠️ Photo list is empty")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = errorBody ?: response.message() ?: "Failed to load photos (${response.code()})"
                    android.util.Log.e("PhotosListScreen", "❌ Failed to load photos: $errorMsg")
                    dialogMessage = errorMsg
                    showDialog = true
                }
            }.onFailure { exception ->
                android.util.Log.e("PhotosListScreen", "❌ Exception loading photos: ${exception.message}", exception)
                dialogMessage = exception.message ?: "Failed to load photos"
                showDialog = true
            }
        }
    }

    fun refreshList() {
        scope.launch {
            isLoading = true
            val result = repository.getPhotos()
            isLoading = false

            result.onSuccess { response ->
                android.util.Log.d("PhotosListScreen", "Refresh response: code=${response.code()}, isSuccessful=${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val photoList = response.body()!!
                    android.util.Log.d("PhotosListScreen", "✅ Refreshed ${photoList.size} photos")
                    photos = photoList
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = errorBody ?: response.message() ?: "Failed to load photos (${response.code()})"
                    android.util.Log.e("PhotosListScreen", "❌ Refresh failed: $errorMsg")
                    dialogMessage = errorMsg
                    showDialog = true
                }
            }.onFailure { exception ->
                android.util.Log.e("PhotosListScreen", "❌ Refresh exception: ${exception.message}", exception)
                dialogMessage = exception.message ?: "Failed to load photos"
                showDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Photos",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF70CEE3)
                ),
                actions = {
                    IconButton(onClick = { refreshList() }) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFECF9FD))
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF70CEE3)
                )
            } else if (photos.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Empty",
                        modifier = Modifier.size(80.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No photos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Photos will appear here",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { refreshList() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70CEE3)
                        )
                    ) {
                        Text("Refresh", color = Color.White)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos) { photo ->
                        PhotoCard(
                            photo = photo,
                            onClick = { selectedPhoto = photo }
                        )
                    }
                }
            }
        }
    }

    // Error Dialog
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

    // Photo Detail Dialog
    selectedPhoto?.let { photo ->
        PhotoDetailDialog(
            photo = photo,
            onDismiss = { selectedPhoto = null }
        )
    }
}

@Composable
fun PhotoCard(photo: Photo, onClick: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val token = remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        token.value = tokenManager.getTokenSync()
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() }
            .shadow(6.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data("${ApiClient.BASE_URL}/photos/${photo.id}/download")
                        .apply {
                            token.value?.let { authToken ->
                                addHeader("Authorization", "Bearer $authToken")
                            }
                        }
                        .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                        .error(android.R.drawable.stat_notify_error)
                        .build()
                ),
                contentDescription = photo.caption ?: "Photo",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Overlay with caption if available
            if (!photo.caption.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = photo.caption,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoDetailDialog(photo: Photo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val token = remember { mutableStateOf<String?>(null) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    LaunchedEffect(Unit) {
        token.value = tokenManager.getTokenSync()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = photo.caption ?: "Photo",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data("${ApiClient.BASE_URL}/photos/${photo.id}/download")
                            .apply {
                                token.value?.let { authToken ->
                                    addHeader("Authorization", "Bearer $authToken")
                                }
                            }
                            .build()
                    ),
                    contentDescription = photo.caption ?: "Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                
                if (!photo.caption.isNullOrEmpty()) {
                    Text(
                        text = "Caption:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = photo.caption,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                photo.createdAt?.let { createdAt ->
                    Text(
                        text = "Date: ${dateFormat.format(createdAt)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                photo.location?.let { location ->
                    Text(
                        text = "Location: ${String.format("%.4f", location.lat)}, ${String.format("%.4f", location.lng)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // ✅ Afficher les détections d'objets
                if (!photo.detections.isNullOrEmpty()) {
                    Text(
                        text = "Detected Objects (${photo.detections.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        photo.detections.forEach { detection ->
                            val name = detection.className ?: detection.label ?: "Unknown object"
                            val confidence = detection.confidence?.let { (it * 100).toInt() }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$name ${confidence?.let { "($it%)" } ?: ""}",
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
