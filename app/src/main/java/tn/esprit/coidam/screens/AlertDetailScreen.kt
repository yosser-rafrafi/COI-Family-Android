package tn.esprit.coidam.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.models.Alert
import tn.esprit.coidam.data.models.Enums.AlertStatus
import tn.esprit.coidam.data.repository.AlertRepository
import tn.esprit.coidam.ui.theme.ThemedBackground
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Composable
fun AlertDetailScreen(navController: NavController, alertId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alertRepository = remember { AlertRepository(context) }

    var alert by remember { mutableStateOf<Alert?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    // Load alert
    LaunchedEffect(alertId) {
        val result = alertRepository.getAlert(alertId)
        result.onSuccess { alert = it }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground()

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF70CEE3))
            }
        } else if (alert == null) {
            EmptyView("Alerte non trouvée")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                HeaderBar(
                    title = "Détails",
                    onBackClick = { navController.popBackStack() },
                    onRefreshClick = {}
                )

                Column(modifier = Modifier.padding(20.dp)) {
                    // Alert Type Header
                    AlertTypeHeader(alert!!)

                    Spacer(modifier = Modifier.height(20.dp))

                    // User Info Card
                    alert!!.blindUser?.let {
                        InfoCard(
                            title = "Utilisateur",
                            icon = Icons.Default.Person
                        ) {
                            InfoRow("Nom", it.fullName())
                            it.email?.let { email ->
                                InfoRow("Email", email)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time Info Card
                    InfoCard(
                        title = "Informations",
                        icon = Icons.Default.Info
                    ) {
                        InfoRow("Créée le", "${alert!!.formattedDate()} à ${alert!!.formattedTime()}")
                        InfoRow("Il y a", alert!!.timeAgo())
                        alert!!.acknowledgedAt?.let {
                            InfoRow("Vue à", formatDate(it))
                        }
                        alert!!.resolvedAt?.let {
                            InfoRow("Résolue à", formatDate(it))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Location Card
                    alert!!.location?.let { location ->
                        InfoCard(
                            title = "Localisation",
                            icon = Icons.Default.LocationOn
                        ) {
                            // Google Map
                            val position = LatLng(location.latitude, location.longitude)
                            val cameraPositionState = rememberCameraPositionState {
                                this.position = CameraPosition.fromLatLngZoom(position, 15f)
                            }

                            OSMMap(
                                context = LocalContext.current,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )


                            Spacer(modifier = Modifier.height(12.dp))

                            location.address?.let {
                                Text(
                                    text = it,
                                    fontSize = 14.sp,
                                    color = Color(0xFF757575)
                                )
                            }

                            InfoRow("Latitude", String.format("%.6f", location.latitude))
                            InfoRow("Longitude", String.format("%.6f", location.longitude))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons
                    if (alert!!.status == AlertStatus.PENDING) {
                        ActionButton(
                            text = "J'AI VU L'ALERTE",
                            color = Color(0xFFFFC107),
                            icon = Icons.Default.Visibility,
                            enabled = !isUpdating,
                            onClick = {
                                scope.launch {
                                    isUpdating = true
                                    alertRepository.acknowledgeAlert(alertId)
                                    isUpdating = false
                                    navController.popBackStack()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (alert!!.status != AlertStatus.RESOLVED) {
                        ActionButton(
                            text = "PROBLÈME RÉSOLU",
                            color = Color(0xFF4CAF50),
                            icon = Icons.Default.CheckCircle,
                            enabled = !isUpdating,
                            onClick = {
                                scope.launch {
                                    isUpdating = true
                                    alertRepository.resolveAlert(alertId)
                                    isUpdating = false
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Loading Overlay
        if (isUpdating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun AlertTypeHeader(alert: Alert) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (alert.type.value == "emergency") {
                Icons.Default.Warning
            } else {
                Icons.Default.Message
            },
            contentDescription = null,
            tint = typeColor(alert.type.value),
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = alert.type.displayName().uppercase(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = typeColor(alert.type.value)
        )

        Spacer(modifier = Modifier.height(8.dp))

        StatusBadge(status = alert.status)
    }
}

@Composable
fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF70CEE3)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF757575)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF424242)
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    color: Color,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

fun typeColor(type: String): Color {
    return if (type == "emergency") Color(0xFFF44336) else Color(0xFFFFC107)
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString)

        val outputFormat = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        dateString
    }
}

@Composable
fun EmptyView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
        }
    }
}