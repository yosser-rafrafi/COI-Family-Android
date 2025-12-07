package tn.esprit.coidam.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.Battery
import tn.esprit.coidam.data.repository.BatteryRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryLevelsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    val batteryRepository = remember { BatteryRepository(context) }

    var batteryReports by remember { mutableStateOf<List<Battery>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var linkedBlindUserId by remember { mutableStateOf<String?>(null) }

    suspend fun loadBatteryReports() {
        try {
            isLoading = true
            errorMessage = null

            // Get linked blind user ID
            val linkedId = tokenManager.getLinkedUserIdSync()
            if (linkedId.isNullOrEmpty()) {
                errorMessage = "No linked blind user found"
                isLoading = false
                return
            }

            linkedBlindUserId = linkedId

            val result = batteryRepository.getRecentBatteryReports(linkedId)
            result.onSuccess { reports ->
                batteryReports = reports
            }.onFailure { error ->
                errorMessage = error.message ?: "Failed to load battery reports"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error occurred"
        } finally {
            isLoading = false
        }
    }

    // Load battery reports when screen opens
    LaunchedEffect(Unit) {
        loadBatteryReports()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFECF9FD))
    ) {
        // Header with gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF70CEE3),
                            Color(0xFF129FA9)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Battery Levels",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(onClick = {
                    scope.launch {
                        loadBatteryReports()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF70CEE3)
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Error",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage ?: "Unknown error",
                            color = Color(0xFFF44336),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    loadBatteryReports()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF70CEE3)
                            )
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
                batteryReports.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryUnknown,
                            contentDescription = "No battery data",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No battery reports available",
                            color = Color(0xFF9E9E9E),
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(batteryReports) { report ->
                            BatteryReportCard(report)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatteryReportCard(report: Battery) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Battery icon based on level and charging status
                val batteryIcon = when {
                    report.isCharging -> Icons.Default.BatteryChargingFull
                    report.level <= 20 -> Icons.Default.BatteryAlert
                    report.level <= 50 -> Icons.Default.BatterySaver
                    report.level <= 80 -> Icons.Default.BatteryStd
                    else -> Icons.Default.BatteryFull
                }

                Icon(
                    imageVector = batteryIcon,
                    contentDescription = "Battery",
                    tint = Color(android.graphics.Color.parseColor(report.getBatteryColor())),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = report.getBatteryStatusText(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF424242)
                    )
                    Text(
                        text = report.timeAgo(),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = report.formattedDate(),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )
                }
            }

            // Current battery level display
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${report.level}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(android.graphics.Color.parseColor(report.getBatteryColor()))
                )
                if (report.note?.isNotEmpty() == true) {
                    Text(
                        text = report.note,
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
