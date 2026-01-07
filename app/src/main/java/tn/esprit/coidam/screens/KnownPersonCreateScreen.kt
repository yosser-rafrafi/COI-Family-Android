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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
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
import tn.esprit.coidam.data.repository.KnownPersonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnownPersonCreateScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var showRelationshipPicker by remember { mutableStateOf(false) }

    val relationships = listOf(
        "Famille",
        "Ami(e)",
        "Aide-soignant(e)",
        "Autre"
    )

    val context = LocalContext.current
    val repository = remember { KnownPersonRepository(context) }
    val scope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    fun createPerson() {
        if (name.isBlank()) {
            dialogMessage = "Name is required"
            showDialog = true
            return
        }

        if (imageUri == null) {
            dialogMessage = "Image is required"
            showDialog = true
            return
        }

        scope.launch {
            isLoading = true
            val result = repository.create(
                name = name.trim(),
                relation = if (relation.isNotBlank()) relation.trim() else null,
                phone = if (phone.isNotBlank()) phone.trim() else null,
                imageUri = imageUri
            )
            isLoading = false

            result.onSuccess {
                navController.popBackStack()
            }.onFailure { exception ->
                dialogMessage = exception.message ?: "Failed to create"
                showDialog = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Person",
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
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFECF9FD))
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Picker
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
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(imageUri)
                                    .build()
                            ),
                            contentDescription = "Profile Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Add Image",
                                tint = Color(0xFF70CEE3),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add Photo",
                                fontSize = 12.sp,
                                color = Color(0xFF70CEE3)
                            )
                        }
                    }
                }
            }

            // Name Field
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

            // Relation Field (Selection Picker)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRelationshipPicker = true }
            ) {
                OutlinedTextField(
                    value = relation,
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    label = { Text("Relation") },
                    placeholder = { Text("Sélectionnez") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select",
                            tint = Color(0xFF9E9E9E)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color(0xFFE0E0E0),
                        disabledLabelColor = Color(0xFF9E9E9E),
                        disabledTextColor = Color(0xFF333333),
                        disabledTrailingIconColor = Color(0xFF9E9E9E)
                    )
                )
            }

            // Phone Field
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

            Spacer(modifier = Modifier.weight(1f))

            // Create Button
            Button(
                onClick = { createPerson() },
                enabled = !isLoading && name.isNotBlank() && imageUri != null,
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
                        text = "Create",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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

    // Relationship Picker Dialog
    if (showRelationshipPicker) {
        AlertDialog(
            onDismissRequest = { showRelationshipPicker = false },
            title = { Text("Sélectionnez la relation") },
            text = {
                Column {
                    relationships.forEach { rel ->
                        TextButton(
                            onClick = {
                                relation = rel
                                showRelationshipPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rel,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Start
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRelationshipPicker = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

