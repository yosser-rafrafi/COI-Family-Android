package tn.esprit.coidam.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlindProfileConfigScreen(navController: NavController) {
    var currentStep by remember { mutableStateOf(1) }
    val totalSteps = 3

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var showRelationshipPicker by remember { mutableStateOf(false) }

    val relationships = listOf(
        "Famille",
        "Ami(e)",
        "Aide-soignant(e)",
        "Autre"
    )

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

                    // First Name
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Prénom") },
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Relationship Question
                    Text(
                        text = "Quel est votre lien avec cette personne ?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Relationship Picker
                    OutlinedTextField(
                        value = relationship.ifEmpty { "" },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Sélectionnez") },
                        placeholder = { Text("Sélectionnez") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select",
                                tint = Color(0xFF9E9E9E)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRelationshipPicker = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF70CEE3),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = Color(0xFF70CEE3),
                            unfocusedLabelColor = Color(0xFF9E9E9E)
                        )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Next Button
                    Button(
                        onClick = { 
                            if (currentStep < totalSteps) {
                                currentStep++
                            } else {
                                // TODO: Save profile configuration
                                navController.navigateUp()
                            }
                        },
                        enabled = firstName.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70CEE3),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = if (currentStep < totalSteps) "Suivant" else "Terminer",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
}

