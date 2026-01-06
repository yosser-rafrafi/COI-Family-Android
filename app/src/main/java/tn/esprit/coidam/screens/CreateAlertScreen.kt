// screens/BlindUserAlertScreen.kt
package tn.esprit.coidam.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import kotlinx.coroutines.launch
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Enums.AlertType
import tn.esprit.coidam.data.repository.AlertRepository
import tn.esprit.coidam.ui.theme.ThemedBackground
import java.lang.System.console

@Composable
fun CreateAlertScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alertRepository = remember { AlertRepository(context) }
    val tokenManager = remember { TokenManager(context) }

    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var currentLocation by remember { mutableStateOf<Location?>(null) }
    var locationPermissionGranted by remember { mutableStateOf(false) }

    // Fused Location Provider
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    // Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (locationPermissionGranted) {
            getCurrentLocation(fusedLocationClient) { location ->
                currentLocation = location
            }
        }
    }

    // Check and request permissions
    LaunchedEffect(Unit) {
        locationPermissionGranted = checkLocationPermission(context)

        if (locationPermissionGranted) {
            getCurrentLocation(fusedLocationClient) { location ->
                currentLocation = location
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF424242)
                    )
                }

                Text(
                    text = "Envoyer une Alerte",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )

                // Spacer for alignment
                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Location Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (currentLocation != null) {
                            Icons.Default.LocationOn
                        } else {
                            Icons.Default.LocationOff
                        },
                        contentDescription = null,
                        tint = if (currentLocation != null) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (currentLocation != null) {
                            "Position GPS prête"
                        } else if (!locationPermissionGranted) {
                            "Permission GPS requise"
                        } else {
                            "Recherche de position..."
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (currentLocation != null) {
                            Color(0xFF4CAF50)
                        } else {
                            Color(0xFFF44336)
                        }
                    )

                    if (currentLocation != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Lat: ${String.format("%.4f", currentLocation!!.latitude)}",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                        Text(
                            text = "Lon: ${String.format("%.4f", currentLocation!!.longitude)}",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }

                    if (!locationPermissionGranted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF70CEE3)
                            )
                        ) {
                            Text("Autoriser la localisation")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Emergency Button (LARGE)
            Button(
                onClick = {
                    sendAlert(
                        alertType = AlertType.EMERGENCY,
                        currentLocation = currentLocation,
                        tokenManager = tokenManager,
                        alertRepository = alertRepository,
                        scope = scope,
                        onLoading = { isLoading = it },
                        onSuccess = { showSuccess = true },
                        onError = { msg ->
                            errorMessage = msg
                            showError = true
                        }
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336),
                    disabledContainerColor = Color(0xFFF44336).copy(alpha = 0.5f)
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency",
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "URGENCE",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Assistance Button (smaller)
            Button(
                onClick = {
                    sendAlert(
                        alertType = AlertType.ASSISTANCE,
                        currentLocation = currentLocation,
                        tokenManager = tokenManager,
                        alertRepository = alertRepository,
                        scope = scope,
                        onLoading = { isLoading = it },
                        onSuccess = { showSuccess = true },
                        onError = { msg ->
                            errorMessage = msg
                            showError = true
                        }
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "Assistance",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Demande d'Assistance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(50.dp))
        }

        // Loading Overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF70CEE3))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Envoi de l'alerte...",
                            fontSize = 16.sp,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }
    }

    // Success Dialog
    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {
                showSuccess = false
                navController.popBackStack()
            },
            title = { Text("Alerte envoyée") },
            text = { Text("Votre alerte a été envoyée avec succès à votre accompagnant.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccess = false
                    navController.popBackStack()
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Error Dialog
    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            title = { Text("Erreur") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(onClick = { showError = false }) {
                    Text("OK")
                }
            }
        )
    }
}

// Helper function to check location permission
private fun checkLocationPermission(context: android.content.Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

// Helper function to get current location
private fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location?) -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                onLocationReceived(location)
            }
            .addOnFailureListener {
                onLocationReceived(null)
            }
    } catch (e: SecurityException) {
        onLocationReceived(null)
    }
}

// Helper function to send alert
private fun sendAlert(
    alertType: AlertType,
    currentLocation: Location?,
    tokenManager: TokenManager,
    alertRepository: AlertRepository,
    scope: kotlinx.coroutines.CoroutineScope,
    onLoading: (Boolean) -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        val blindUserId = tokenManager.getUserIdSync()
        //val blindUserId = "691088c2b15ba40f375efed9"
         val companionId = tokenManager.getLinkedUserIdSync()
        //val companionId = "691088c2b15ba40f375efed7"

        if (blindUserId.isNullOrEmpty()) {
            onError("Utilisateur non connecté")
            return@launch
        }


        if (companionId.isNullOrEmpty()) {
            onError("Aucun accompagnant lié à ce compte. Veuillez vous reconnecter.")
            return@launch
        }

        onLoading(true)

        val alertLocation = currentLocation?.let {
            tn.esprit.coidam.data.models.Location(
                latitude = it.latitude,
                longitude = it.longitude,
                address = null
            )
        }

        val result = alertRepository.createAlert(
            blindUserId = blindUserId,
            companionId = companionId,
            type = alertType,
            location = alertLocation
        )

        onLoading(false)

        result.onSuccess {
            onSuccess()
        }.onFailure { exception ->
            onError(exception.message ?: "Erreur lors de l'envoi de l'alerte")
        }
    }
}
// Helper to get linked companion ID
// TODO: Implement proper way to get linkedUserId
private suspend fun getLinkedCompanionId(tokenManager: TokenManager): String? {
    // ✅ RÉCUPÉRER linkedUserId DEPUIS TokenManager
    return tokenManager.getLinkedUserIdSync()
}
