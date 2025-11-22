package tn.esprit.coidam.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import tn.esprit.coidam.data.models.KnownPerson
import tn.esprit.coidam.data.repository.KnownPersonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnownPersonDetailScreen(navController: NavController, personId: String) {
    var person by remember { mutableStateOf<KnownPerson?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }

    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val repository = remember { KnownPersonRepository(context) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    LaunchedEffect(personId) {
        scope.launch {
            isLoading = true
            val result = repository.findOne(personId)
            isLoading = false

            result.onSuccess { p ->
                person = p
                name = p.name
                relation = p.relation ?: ""
                phone = p.phone ?: ""
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Failed to load person"
                showDialog = true
            }
        }
    }

    fun updatePerson() {
        if (name.isBlank()) {
            dialogMessage = "Name is required"
            showDialog = true
            return
        }

        scope.launch {
            isLoading = true
            val result = repository.update(
                id = personId,
                name = name.trim(),
                relation = if (relation.isNotBlank()) relation.trim() else null,
                phone = if (phone.isNotBlank()) phone.trim() else null,
                imageUri = imageUri
            )
            isLoading = false

            result.onSuccess { updatedPerson ->
                person = updatedPerson
                isEditing = false
                imageUri = null
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Failed to update"
                showDialog = true
            }
        }
    }

    fun deletePerson() {
        scope.launch {
            isLoading = true
            val result = repository.remove(personId)
            isLoading = false

            result.onSuccess {
                navController.popBackStack()
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Failed to delete"
                showDialog = true
                showDeleteDialog = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit" else "Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            isEditing = false
                            person?.let {
                                name = it.name
                                relation = it.relation ?: ""
                                phone = it.phone ?: ""
                                imageUri = null
                            }
                        } else {
                            navController.navigateUp()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (!isEditing && person != null) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF70CEE3)
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF70CEE3))
            }
        } else if (person != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFECF9FD))
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF70CEE3).copy(alpha = 0.2f))
                            .then(
                                if (isEditing) Modifier.clickable { imagePickerLauncher.launch("image/*") }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageToShow = if (isEditing && imageUri != null) {
                            imageUri
                        } else {
                            person?.image?.let { Uri.parse(ApiClient.BASE_URL+it) }
                        }

                        if (imageToShow != null) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    ImageRequest.Builder(context)
                                        .data(imageToShow)
                                        .build()
                                ),
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFF70CEE3),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        if (isEditing) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Image",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF70CEE3))
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                // Name Field
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF70CEE3),
                            focusedLabelColor = Color(0xFF70CEE3)
                        )
                    )
                } else {
                    InfoCard("Name", person?.name ?: "")
                }

                // Relation Field
                if (isEditing) {
                    OutlinedTextField(
                        value = relation,
                        onValueChange = { relation = it },
                        label = { Text("Relation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF70CEE3),
                            focusedLabelColor = Color(0xFF70CEE3)
                        )
                    )
                } else if (!person?.relation.isNullOrEmpty()) {
                    InfoCard("Relation", person?.relation ?: "")
                }

                // Phone Field
                if (isEditing) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF70CEE3),
                            focusedLabelColor = Color(0xFF70CEE3)
                        )
                    )
                } else if (!person?.phone.isNullOrEmpty()) {
                    InfoCard("Phone", person?.phone ?: "")
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { updatePerson() },
                        enabled = !isLoading && name.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70CEE3)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Save",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete") },
            text = { Text("Are you sure you want to delete this person?") },
            confirmButton = {
                TextButton(
                    onClick = { deletePerson() }
                ) {
                    Text("Yes", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("No")
                }
            }
        )
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
}

@Composable
fun InfoCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }
    }
}

