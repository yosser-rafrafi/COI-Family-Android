package tn.esprit.coidam.screens

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
import tn.esprit.coidam.data.models.Call
import tn.esprit.coidam.data.models.Enums.CallStatus
import tn.esprit.coidam.data.models.Enums.CallType
import tn.esprit.coidam.data.repository.CallRepository
import tn.esprit.coidam.ui.theme.ThemedBackground

@Composable
fun CallHistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callRepository = remember { CallRepository(context) }
    val tokenManager = remember { TokenManager(context) }

    var calls by remember { mutableStateOf<List<Call>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf<tn.esprit.coidam.data.models.CallResponses.CallStatsResponse?>(null) }

    // Load calls and stats
    LaunchedEffect(Unit) {
        val userId = tokenManager.getUserIdSync()
        val userType = tokenManager.getUserTypeSync()

        if (userId != null && userType != null) {
            // Load history
            val historyResult = callRepository.getCallHistory(userId, userType)
            historyResult.onSuccess {
                calls = it.calls
            }.onFailure {
                errorMessage = it.message ?: "Erreur de chargement"
                showError = true
            }

            // Load stats
            val statsResult = callRepository.getCallStats(userId, userType)
            statsResult.onSuccess { stats = it }
        }

        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ThemedBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = Color(0xFF424242)
                    )
                }

                Text(
                    text = "Historique d'appels",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242)
                )

                IconButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val userId = tokenManager.getUserIdSync()
                            val userType = tokenManager.getUserTypeSync()
                            if (userId != null && userType != null) {
                                callRepository.getCallHistory(userId, userType).onSuccess {
                                    calls = it.calls
                                }
                            }
                            isLoading = false
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rafraîchir",
                        tint = Color(0xFF70CEE3)
                    )
                }
            }

            // Stats Card
            if (stats != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            number = stats!!.totalCalls.toString(),
                            label = "Total",
                            color = Color(0xFF70CEE3)
                        )
                        StatItem(
                            number = stats!!.completedCalls.toString(),
                            label = "Terminés",
                            color = Color(0xFF4CAF50)
                        )
                        StatItem(
                            number = stats!!.missedCalls.toString(),
                            label = "Manqués",
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }

            // Calls List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF70CEE3))
                }
            } else if (calls.isEmpty()) {
                EmptyCallsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(calls) { call ->
                        CallHistoryCard(
                            call = call,
                            onClick = {
                                // Navigate to call details if needed
                            }
                        )
                    }
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
fun CallHistoryCard(call: Call, onClick: () -> Unit) {
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
            // Icon based on status
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(callStatusColor(call.status).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (call.status) {
                        CallStatus.COMPLETED -> Icons.Default.CheckCircle
                        CallStatus.MISSED -> Icons.Default.PhoneMissed
                        CallStatus.REJECTED -> Icons.Default.PhoneDisabled
                        else -> if (call.callType == CallType.VIDEO)
                            Icons.Default.Videocam else Icons.Default.Phone
                    },
                    contentDescription = null,
                    tint = callStatusColor(call.status),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.companion?.let { "${it.firstName} ${it.lastName}" }
                        ?: call.blindUser?.let { "${it.firstName} ${it.lastName}" }
                        ?: "Inconnu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF424242)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (call.callType == CallType.VIDEO)
                            Icons.Default.Videocam else Icons.Default.Phone,
                        contentDescription = null,
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = call.callType.displayName(),
                        fontSize = 12.sp,
                        color = Color(0xFF757575)
                    )

                    if (call.duration != null && call.duration > 0) {
                        Text(
                            text = " • ${call.formattedDuration()}",
                            fontSize = 12.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }

                Text(
                    text = call.timeAgo(),
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }

            // Status Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = callStatusColor(call.status).copy(alpha = 0.15f)
            ) {
                Text(
                    text = call.status.displayName(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = callStatusColor(call.status),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyCallsView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PhoneDisabled,
                contentDescription = null,
                tint = Color(0xFF757575),
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Aucun appel",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
        }
    }
}

fun callStatusColor(status: CallStatus): Color {
    return when (status) {
        CallStatus.COMPLETED -> Color(0xFF4CAF50)
        CallStatus.MISSED -> Color(0xFFF44336)
        CallStatus.REJECTED -> Color(0xFFFF9800)
        CallStatus.ACTIVE -> Color(0xFF2196F3)
        else -> Color(0xFF757575)
    }
}