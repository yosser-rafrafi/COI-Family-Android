package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import tn.esprit.coidam.R


@Composable
fun SignupScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var university by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("companion") }

    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }


    val scope = rememberCoroutineScope()

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}\$")
        return emailRegex.matches(email)
    }

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = Regex("^[0-9]{10}\$")
        return phoneRegex.matches(phoneNumber)
    }



    fun signUp() {
        scope.launch {
            // Validate all fields
            when {
                firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty() ||
                        email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                    dialogMessage = "Please fill in all fields."
                    showDialog = true
                }
                !isValidEmail(email.trim()) -> {
                    dialogMessage = "Please enter a valid email address."
                    showDialog = true
                }

                !isValidPhoneNumber(phoneNumber.trim()) -> {
                    dialogMessage = "Please enter a valid phone number."
                    showDialog = true
                }
                password.trim() != confirmPassword.trim() -> {
                    dialogMessage = "Passwords do not match."
                    showDialog = true
                }
                else -> {
                    dialogMessage = "Registration successful!"
                    showDialog = true
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.clipfly),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(55.dp))

                // Title
                Text(
                    text = "HELLO THERE",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Register below with your details!",
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Form Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.White.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, Color.White, RoundedCornerShape(12.dp))
                        .padding(horizontal = 25.dp)
                ) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // First Name Field
                    CustomTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        placeholder = "First Name"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Last Name Field
                    CustomTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        placeholder = "Last Name"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone Number Field
                    CustomTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        placeholder = "Phone Number"
                    )


                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Field
                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Password Field
                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirm Password Field
                    CustomTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = "Confirm Password",
                        isPassword = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sign Up Button
                    Button(
                        onClick = { signUp() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70CEE3)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Sign Up",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    // Already a member
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "I am member!",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " Login Now",
                            color = Color(0xFF129FA9),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                navController.navigate("login")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(15.dp))
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Alert Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Message") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.01f))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        Divider(
            color = Color.Gray,
            thickness = 1.dp
        )
    }

}

@Preview
@Composable
fun SignupScreenPreview(){
    MaterialTheme {
        SignupScreen(navController = NavController(LocalContext.current))
    }
}
