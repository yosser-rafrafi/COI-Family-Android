package tn.esprit.coidam.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Alert

import tn.esprit.coidam.data.models.Enums.AlertStatus
import tn.esprit.coidam.data.models.Enums.AlertType
import tn.esprit.coidam.data.repository.AlertRepository
import tn.esprit.coidam.ui.theme.ThemedBackground

@Composable
fun CompanionAlertsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val alertRepository = remember { AlertRepository(context) }
    val tokenManager = remember { TokenManager(context) }

    var alerts by remember { mutableStateOf<List<Alert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Load alerts
    LaunchedEffect(Unit) {
        val userId = tokenManager.getUserIdSync()
        if (userId != null) {
            val result = alertRepository.getAlertsByCompanion(userId)
            result.onSuccess { alerts = it.sortedByDescending { alert -> alert.createdAt } }
                .onFailure {
                    errorMessage = it.message ?: "Erreur de chargement"
                    showError = true
                }
        }
        isLoading = false
    }

    // Filter alerts by status
    val pendingAlerts = alerts.filter { it.status == AlertStatus.PENDING }
    val acknowledgedAlerts = alerts.filter { it.status == AlertStatus.ACKNOWLEDGED }
    val resolvedAlerts = alerts.filter { it.status == AlertStatus.RESOLVED }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            HeaderBar(
                title = "Alertes",
                onBackClick = { navController.popBackStack() },
                onRefreshClick = {
                    scope.launch {
                        isLoading = true
                        val userId = tokenManager.getUserIdSync()
                        if (userId != null) {
                            val result = alertRepository.getAlertsByCompanion(userId)
                            result.onSuccess { alerts = it.sortedByDescending { alert -> alert.createdAt } }
                        }
                        isLoading = false
                    }
                }
            )

            // Stats Card
            StatsCard(
                pending = pendingAlerts.size,
                acknowledged = acknowledgedAlerts.size,
                resolved = resolvedAlerts.size
            )

            // Alerts List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF70CEE3))
                }
            } else if (alerts.isEmpty()) {
                EmptyAlertsView()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pending Alerts
                    if (pendingAlerts.isNotEmpty()) {
                        item {
                            SectionHeader("EN ATTENTE", Color(0xFFF44336))
                        }
                        items(pendingAlerts) { alert ->
                            AlertCard(
                                alert = alert,
                                onClick = {
                                    Log.d("ALERT_DEBUG", "Navigate with ID = ${alert.id}")
                                    navController.navigate("alert_detail/${alert.id}")
                                }
                            )
                        }
                    }

                    // Acknowledged Alerts
                    if (acknowledgedAlerts.isNotEmpty()) {
                        item {
                            SectionHeader("EN COURS", Color(0xFFFFC107))
                        }
                        items(acknowledgedAlerts) { alert ->
                            AlertCard(
                                alert = alert,
                                onClick = {
                                    navController.navigate("alert_detail/${alert.id}")
                                }
                            )
                        }
                    }

                    // Resolved Alerts
                    if (resolvedAlerts.isNotEmpty()) {
                        item {
                            SectionHeader("RÉSOLUES", Color(0xFF4CAF50))
                        }
                        items(resolvedAlerts) { alert ->
                            AlertCard(
                                alert = alert,
                                onClick = {
                                    navController.navigate("alert_detail/${alert.id}")
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
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

@Composable
fun HeaderBar(title: String, onBackClick: () -> Unit, onRefreshClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF424242)
            )
        }

        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242)
        )

        IconButton(onClick = onRefreshClick) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = Color(0xFF70CEE3)
            )
        }
    }
}

@Composable
fun StatsCard(pending: Int, acknowledged: Int, resolved: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusItem(number = pending.toString(), label = "En attente", color = Color(0xFFF44336))
            StatusItem(number = acknowledged.toString(), label = "Vues", color = Color(0xFFFFC107))
            StatusItem(number = resolved.toString(), label = "Résolues", color = Color(0xFF4CAF50))
        }
    }
}

@Composable
fun StatusItem(number: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = number,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF757575)
        )
    }
}

@Composable
fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 10.dp, bottom = 8.dp)
    )
}

@Composable
fun AlertCard(alert: Alert, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(statusColor(alert.status).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (alert.type == AlertType.EMERGENCY) {
                        Icons.Default.Warning
                    } else {
                        Icons.Default.Message
                    },
                    contentDescription = null,
                    tint = statusColor(alert.status),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = alert.type.displayName(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF424242)
                    )

                    StatusBadge(status = alert.status)
                }

                Spacer(modifier = Modifier.height(4.dp))

                alert.blindUser?.let {
                    Text(
                        text = it.fullName(),
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = alert.location?.address ?: "Position partagée",
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        maxLines = 1
                    )
                }

                Text(
                    text = alert.timeAgo(),
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun StatusBadge(status: AlertStatus) {
    val (color, text) = when (status) {
        AlertStatus.PENDING -> Color(0xFFF44336) to "En attente"
        AlertStatus.ACKNOWLEDGED -> Color(0xFFFFC107) to "Vue"
        AlertStatus.RESOLVED -> Color(0xFF4CAF50) to "Résolue"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyAlertsView() {
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
                text = "Aucune alerte",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
        }
    }
}

fun statusColor(status: AlertStatus): Color {
    return when (status) {
        AlertStatus.PENDING -> Color(0xFFF44336)
        AlertStatus.ACKNOWLEDGED -> Color(0xFFFFC107)
        AlertStatus.RESOLVED -> Color(0xFF4CAF50)
    }
}