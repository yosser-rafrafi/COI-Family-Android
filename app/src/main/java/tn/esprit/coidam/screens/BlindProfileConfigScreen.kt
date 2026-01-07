package tn.esprit.coidam.screens

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.data.repository.KnownPersonRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlindProfileConfigScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    val knownPersonRepository = remember { KnownPersonRepository(context) }
    val tokenManager = remember { TokenManager(context) }

    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var showRelationshipPicker by remember { mutableStateOf(false) }
    
    // ✅ État pour l'image de profil
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
    
    // ✅ États de chargement et feedback
    var isSaving by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    val relationships = listOf(
        "Famille",
        "Ami(e)",
        "Aide-soignant(e)",
        "Autre"
    )

    // ✅ Launcher pour sélectionner une image de la galerie
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                selectedBitmap = bitmap
                uploadedImageUrl = null // Réinitialiser l'URL uploadée pour permettre un nouvel upload
            } catch (e: Exception) {
                dialogMessage = "Erreur lors du chargement de l'image: ${e.message}"
                showDialog = true
                isSuccess = false
            }
        }
    }

    // ✅ Fonction pour uploader l'image
    fun uploadImage() {
        selectedImageUri?.let { uri ->
            scope.launch {
                isUploadingImage = true
                val result = knownPersonRepository.uploadImage(uri)
                isUploadingImage = false

                result.onSuccess { imageUrl ->
                    uploadedImageUrl = imageUrl
                    dialogMessage = "Image téléchargée avec succès!"
                    isSuccess = true
                    showDialog = true
                }.onFailure { exception ->
                    dialogMessage = "Erreur lors du téléchargement: ${exception.message}"
                    isSuccess = false
                    showDialog = true
                }
            }
        } ?: run {
            dialogMessage = "Veuillez sélectionner une image d'abord"
            showDialog = true
            isSuccess = false
        }
    }

    // ✅ Fonction pour sauvegarder le profil
    fun saveProfile() {
        if (firstName.isEmpty()) {
            dialogMessage = "Le prénom est obligatoire"
            showDialog = true
            isSuccess = false
            return
        }

        scope.launch {
            isSaving = true
            
            // Obtenir le blindUserId depuis le tokenManager
            val blindUserId = tokenManager.getLinkedUserIdSync()
            
            if (blindUserId == null) {
                dialogMessage = "Aucun utilisateur aveugle lié trouvé"
                isSaving = false
                showDialog = true
                isSuccess = false
                return@launch
            }

            // Utiliser l'image uploadée si disponible, sinon utiliser celle déjà sélectionnée
            val finalImageUrl = uploadedImageUrl
            
            val result = authRepository.updateBlindProfile(
                blindUserId = blindUserId,
                firstName = firstName.trim(),
                lastName = lastName.takeIf { it.isNotEmpty() }?.trim(),
                phoneNumber = phoneNumber.takeIf { it.isNotEmpty() }?.trim(),
                relation = relationship.takeIf { it.isNotEmpty() },
                profileImage = finalImageUrl
            )

            isSaving = false

            result.onSuccess {
                dialogMessage = "Profil configuré avec succès!"
                isSuccess = true
                showDialog = true
            }.onFailure { exception ->
                dialogMessage = "Erreur: ${exception.message ?: "Échec de la sauvegarde"}"
                isSuccess = false
                showDialog = true
            }
        }
    }

    // Light blue gradient background
    val lightBlueGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFE6F7FF),
            Color(0xFFD0EFFF),
            Color(0xFFB8E6FF)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(lightBlueGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // ✅ Bouton de retour en haut à gauche
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = { navController.navigateUp() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color(0xFF333333),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Progress Indicator
            LinearProgressIndicator(
                progress = { currentStep.toFloat() / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF70CEE3),
                trackColor = Color(0xFFE0E0E0)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Configuration du profil",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Heading
                    Text(
                        text = "Créez le profil de la personne accompagnée",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // ✅ Section Image de profil (Step 1)
                    if (currentStep == 1) {
                        Text(
                            text = "Photo de profil (optionnel)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // ✅ Afficher l'image sélectionnée ou placeholder
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .align(Alignment.CenterHorizontally)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF70CEE3), CircleShape)
                                .clickable {
                                    galleryLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                selectedBitmap != null -> {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "Photo de profil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // ✅ Badge "remplacer" en haut à droite
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(Color(0xFF70CEE3), CircleShape)
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Remplacer",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.AddAPhoto,
                                        contentDescription = "Ajouter une photo",
                                        tint = Color(0xFF70CEE3),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ✅ Bouton pour uploader l'image
                        if (selectedBitmap != null && uploadedImageUrl == null) {
                            OutlinedButton(
                                onClick = { uploadImage() },
                                enabled = !isUploadingImage,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF70CEE3)
                                )
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF70CEE3),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Téléchargement...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Télécharger l'image")
                                }
                            }
                        }

                        // ✅ Indicateur si l'image est déjà uploadée
                        if (uploadedImageUrl != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Uploadé",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Image téléchargée",
                                    color = Color(0xFF4CAF50),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        TextButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choisir une image de la galerie")
                        }
                    }

                    // ✅ Step 2: Informations personnelles
                    if (currentStep == 2) {
                        // First Name
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("Prénom *") },
                            placeholder = { Text("Prénom") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF70CEE3),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedLabelColor = Color(0xFF70CEE3),
                                unfocusedLabelColor = Color(0xFF9E9E9E)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Last Name (optional)
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Nom (optionnel)") },
                            placeholder = { Text("Nom (optionnel)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF70CEE3),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedLabelColor = Color(0xFF70CEE3),
                                unfocusedLabelColor = Color(0xFF9E9E9E)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Phone Number (optional)
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Numéro de téléphone (optionnel)") },
                            placeholder = { Text("Numéro de téléphone (optionnel)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF70CEE3),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                focusedLabelColor = Color(0xFF70CEE3),
                                unfocusedLabelColor = Color(0xFF9E9E9E)
                            )
                        )
                    }

                    // ✅ Step 3: Relation
                    if (currentStep == 3) {
                        // Relationship Question
                        Text(
                            text = "Quel est votre lien avec cette personne ?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Relationship Picker
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRelationshipPicker = true }
                        ) {
                            OutlinedTextField(
                                value = relationship.ifEmpty { "" },
                                onValueChange = { },
                                readOnly = true,
                                enabled = false, // Disable to let Box handle clicks
                                label = { Text("Sélectionnez") },
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
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Bouton Retour (sauf au step 1)
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF70CEE3)
                                )
                            ) {
                                Text("Retour")
                            }
                        }

                        // Next/Finish Button
                        Button(
                            onClick = { 
                                if (currentStep < totalSteps) {
                                    currentStep++
                                } else {
                                    saveProfile()
                                }
                            },
                            enabled = when (currentStep) {
                                1 -> true // Image est optionnelle
                                2 -> firstName.isNotEmpty()
                                3 -> true // Relation est optionnelle
                                else -> false
                            } && !isSaving && !isUploadingImage,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF70CEE3),
                                disabledContainerColor = Color(0xFFE0E0E0)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(if (currentStep > 1) 1f else 1f)
                                .height(56.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = if (currentStep < totalSteps) "Suivant" else "Terminer",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Relationship Picker Dialog
    if (showRelationshipPicker) {
        AlertDialog(
            onDismissRequest = { showRelationshipPicker = false },
            title = { Text("Sélectionnez votre relation") },
            text = {
                Column {
                    relationships.forEach { rel ->
                        TextButton(
                            onClick = {
                                relationship = rel
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

    // Result Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isSuccess) "Succès" else "Erreur") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    if (isSuccess && isSaving) {
                        navController.navigateUp()
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }
}
