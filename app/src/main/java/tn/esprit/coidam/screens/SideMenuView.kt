package tn.esprit.coidam.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import tn.esprit.coidam.data.local.TokenManager
import tn.esprit.coidam.data.models.UserResponse
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.data.repository.WebSocketManager
import tn.esprit.coidam.data.api.VoiceWebSocketClient
import tn.esprit.coidam.ui.theme.AppTheme
import tn.esprit.coidam.ui.theme.ThemedBackground

@Composable
fun SideMenuView(
    isShowing: Boolean,
    onDismiss: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository(context) }
    val tokenManager = remember { TokenManager(context) }

    var userProfile by remember { mutableStateOf<UserResponse?>(null) }
    var showSignOutAlert by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(isShowing) {
        if (isShowing) {
            val result = authRepository.getProfile()
            result.onSuccess { userProfile = it }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed Background
        AnimatedVisibility(
            visible = isShowing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(onClick = onDismiss)
            )
        }

        // Menu Content
        AnimatedVisibility(
            visible = isShowing,
            enter = slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            )
        ) {
            Row(modifier = Modifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                        .background(Color.White)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header with User Info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppTheme.buttonGradient)
                            .padding(top = 40.dp, bottom = 30.dp, start = 24.dp, end = 24.dp)
                    ) {
                        if (userProfile != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Profile Image
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!userProfile?.photoUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = userProfile?.photoUrl,
                                            contentDescription = "Profile",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = "Profile",
                                            modifier = Modifier.size(50.dp),
                                            tint = Color.White
                                        )
                                    }
                                }

                                VStack(alignment = Alignment.Start, spacing = 6.dp) {
                                    Text(
                                        text = "${userProfile?.firstName ?: ""} ${userProfile?.lastName ?: ""}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = userProfile?.email ?: "",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        maxLines = 1
                                    )
                                    userProfile?.userType?.let { type ->
                                        Surface(
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = type.replaceFirstChar { it.uppercase() },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.8f),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Chargement...",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    // Menu Items
                    Column(
                        modifier = Modifier
                            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SideMenuLink(
                            icon = Icons.Default.Person,
                            title = "Profil",
                            subtitle = "Voir et modifier",
                            onClick = {
                                onDismiss()
                                navController.navigate("profil")
                            }
                        )

                        SideMenuLink(
                            icon = Icons.Default.Settings,
                            title = "Paramètres",
                            subtitle = "Configuration",
                            onClick = {
                                showSettings = true
                            }
                        )

                        SideMenuLink(
                            icon = Icons.Default.Help,
                            title = "Aide",
                            subtitle = "Support",
                            onClick = {
                                showHelp = true
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )

                        SideMenuLink(
                            icon = Icons.Default.ExitToApp,
                            title = "Déconnexion",
                            subtitle = "Se déconnecter",
                            iconColor = Color.Red,
                            onClick = {
                                showSignOutAlert = true
                            }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Footer
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CO-I Family",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppTheme.textGray
                        )
                        Text(
                            text = "Version 1.0.0",
                            fontSize = 11.sp,
                            color = AppTheme.textGray.copy(alpha = 0.7f)
                        )
                    }
                }

                // Empty space to the right of the menu
                Box(modifier = Modifier.weight(1f).clickable(onClick = onDismiss))
            }
        }
    }

    // Settings Dialog
    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }

    // Help Dialog
    if (showHelp) {
        HelpDialog(onDismiss = { showHelp = false })
    }

    // Sign Out Alert
    if (showSignOutAlert) {
        AlertDialog(
            onDismissRequest = { showSignOutAlert = false },
            title = { Text("Déconnexion") },
            text = { Text("Êtes-vous sûr de vouloir vous déconnecter?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            tokenManager.clear()
                            WebSocketManager.getInstance(context).disconnect()
                            VoiceWebSocketClient.getInstance(context).disconnect()
                            onDismiss()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Déconnexion", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutAlert = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun SideMenuLink(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconColor: Color = AppTheme.darkGray,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.darkGray
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = AppTheme.textGray
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AppTheme.textGray.copy(alpha = 0.5f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                ThemedBackground()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(48.dp))
                        Text(
                            text = "Paramètres",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.darkGray
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = AppTheme.buttonBlue
                        )

                        Text(
                            text = "Paramètres",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.darkGray
                        )

                        Text(
                            text = "Les paramètres seront disponibles prochainement",
                            fontSize = 14.sp,
                            color = AppTheme.textGray,
                            modifier = Modifier.padding(horizontal = 40.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box {
                ThemedBackground()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(48.dp))
                        Text(
                            text = "Aide",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.darkGray
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = AppTheme.buttonBlue
                        )

                        Text(
                            text = "Aide & Support",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.darkGray
                        )

                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HelpSection(
                                title = "Comment ajouter une personne?",
                                content = "Allez dans 'Known People' et appuyez sur le bouton '+' pour ajouter une nouvelle personne."
                            )
                            HelpSection(
                                title = "Comment modifier mon profil?",
                                content = "Ouvrez le menu latéral et sélectionnez 'Profil', puis appuyez sur 'Edit Profile'."
                            )
                            HelpSection(
                                title = "Comment changer mon mot de passe?",
                                content = "Dans votre profil, sélectionnez 'Change Password' et suivez les instructions."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpSection(title: String, content: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.darkGray
            )
            Text(
                text = content,
                fontSize = 14.sp,
                color = AppTheme.textGray
            )
        }
    }
}

@Composable
fun VStack(
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    spacing: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}
