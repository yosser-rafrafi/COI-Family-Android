package tn.esprit.coidam.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import tn.esprit.coidam.data.local.TokenManager



@Composable
fun BlindDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenManager = remember { TokenManager(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CO-I Family",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Notification icon + badge
                        Box {
                            IconButton(onClick = { /* Navigate to notifications */ }) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White
                                )
                            }
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text("3", color = Color.White, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Profile icon
                        IconButton(onClick = { navController.navigate("profil") }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White
                            )
                        }

                        /* // ✅ LOGOUT ICON
                         IconButton(onClick = { showLogoutDialog = true }) {
                             Icon(
                                 imageVector = Icons.Default.Logout,
                                 contentDescription = "Logout",
                                 tint = Color.White
                             )
                         }*/
                    }
                }
            }
        }

        // Summary Statistics Card
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
                StatItem(number = "5", label = "People", color = Color(0xFF70CEE3))
                StatItem(number = "3", label = "Alerts", color = Color(0xFFFFC107))
                StatItem(number = "12", label = "Photos", color = Color(0xFF4CAF50))
            }
        }

        // Main Menu
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Main Menu",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val menuItems = listOf(

                MenuItem("Send Alert", Icons.Default.Notifications, Color(0xFFFFC107), "send_alert"),

            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(menuItems) { item ->
                    MenuCard(
                        item = item,
                        onClick = { navController.navigate(item.route) }
                    )
                }
            }
        }
    }


}
