package tn.esprit.coidam.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AppTheme - Définition des couleurs et gradients de l'application
 */
object AppTheme {
    // Colors
    val lightBlue = Color(0xFFABD9F2)        // rgb(0.67, 0.85, 0.95)
    val lightBlueDark = Color(0xFF8CC7E0)    // rgb(0.55, 0.78, 0.88)
    val primaryBlue = Color(0xFF66B3E6)      // rgb(0.4, 0.7, 0.9)
    val buttonBlue = Color(0xFF73BFF2)       // rgb(0.45, 0.75, 0.95)
    val darkGray = Color(0xFF333333)         // rgb(0.2, 0.2, 0.2)
    val lightGray = Color(0xFFD9D9D9)        // rgb(0.85, 0.85, 0.85)
    val textGray = Color(0xFF808080)         // rgb(0.5, 0.5, 0.5)

    // Background Gradient
    val backgroundGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE6F2FF),  // rgb(0.9, 0.95, 1.0)
            Color(0xFFBFE0F2),  // rgb(0.75, 0.88, 0.95)
            Color(0xFFA6D9EB)   // rgb(0.65, 0.85, 0.92)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // Button Gradient
    val buttonGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF73BFF2),  // rgb(0.45, 0.75, 0.95)
            Color(0xFF66B3E6)   // rgb(0.4, 0.7, 0.9)
        )
    )
}

/**
 * ThemedBackground - Background avec formes géométriques
 * Équivalent de ThemedBackground() en SwiftUI
 */
@Composable
fun ThemedBackground() {
    Spacer(modifier = Modifier.height(20.dp))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.backgroundGradient)
    ) {
        // Circle 1 - Top Left
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-100).dp, y = (-150).dp)
                .background(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )

        // Circle 2 - Top Right
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 150.dp, y = (-100).dp)
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )

        // Circle 3 - Bottom Left
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = (-120).dp, y = 200.dp)
                .background(
                    color = AppTheme.lightBlue.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )

        // Line 1 - Diagonal
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(300.dp)
                .offset(x = 100.dp, y = 100.dp)
                .rotate(45f)
                .background(Color.White.copy(alpha = 0.2f))
        )

        // Line 2 - Diagonal
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(200.dp)
                .offset(x = (-80).dp, y = 150.dp)
                .rotate(-30f)
                .background(AppTheme.lightBlue.copy(alpha = 0.3f))
        )
    }
}

/**
 * Alternative : Background avec Canvas pour plus de contrôle
 */
@Composable
fun ThemedBackgroundCanvas() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.backgroundGradient)
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Circle 1
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 100.dp.toPx(),
                center = Offset(
                    x = -100.dp.toPx(),
                    y = -150.dp.toPx()
                )
            )

            // Circle 2
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = 75.dp.toPx(),
                center = Offset(
                    x = canvasWidth - 150.dp.toPx(),
                    y = -100.dp.toPx()
                )
            )

            // Circle 3
            drawCircle(
                color = Color(0xFFABD9F2).copy(alpha = 0.4f),
                radius = 50.dp.toPx(),
                center = Offset(
                    x = -120.dp.toPx(),
                    y = canvasHeight - 200.dp.toPx()
                )
            )

            // Line 1 (approximation avec rotation)
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = Offset(100.dp.toPx(), 100.dp.toPx()),
                end = Offset(
                    100.dp.toPx() + 212.dp.toPx(), // 300*cos(45°)
                    100.dp.toPx() + 212.dp.toPx()  // 300*sin(45°)
                ),
                strokeWidth = 2.dp.toPx()
            )

            // Line 2
            drawLine(
                color = Color(0xFFABD9F2).copy(alpha = 0.3f),
                start = Offset(-80.dp.toPx(), 150.dp.toPx()),
                end = Offset(
                    -80.dp.toPx() + 173.dp.toPx(), // 200*cos(-30°)
                    150.dp.toPx() - 100.dp.toPx()  // 200*sin(-30°)
                ),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

/**
 * Exemple d'utilisation dans vos pages
 */
@Composable
fun ExampleUsageInLoginScreen(navController: androidx.navigation.NavController) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background avec formes géométriques
        ThemedBackground()

        // Votre contenu par-dessus
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 25.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            // Titre
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Text(
                    text = "HELLO AGAIN",
                    fontSize = 36.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color.White
                )
                androidx.compose.material3.Text(
                    text = "Welcome Back!",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }

            // Formulaire avec fond blanc
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 25.dp, vertical = 20.dp)
            ) {
                // Vos champs de formulaire ici
                androidx.compose.material3.Text(
                    text = "Login Form",
                    color = AppTheme.darkGray
                )
            }
        }
    }
}

/**
 * Extension pour créer des TextField avec ligne en dessous
 */
@Composable
fun UnderlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                androidx.compose.material3.Text(
                    text = placeholder,
                    color = AppTheme.textGray
                )
            },
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Ligne en dessous
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppTheme.textGray.copy(alpha = 0.3f))
        )
    }
}