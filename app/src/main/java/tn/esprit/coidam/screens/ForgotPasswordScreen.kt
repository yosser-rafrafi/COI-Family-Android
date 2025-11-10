package tn.esprit.coidam.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tn.esprit.coidam.R
import tn.esprit.coidam.data.repository.AuthRepository
import tn.esprit.coidam.ui.theme.ThemedBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    // Step 2 state
    var codeSent by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf(List(6) { "" }) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var inlineError by remember { mutableStateOf<String?>(null) }
    var inlineSuccess by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val scope = rememberCoroutineScope()

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
        return emailRegex.matches(email)
    }

    fun forgotPassword() {
        if (email.isEmpty()) {
            inlineError = "Please enter your email address."
            inlineSuccess = null
            return
        }
        if (!isValidEmail(email.trim())) {
            inlineError = "Please enter a valid email address."
            inlineSuccess = null
            return
        }
        scope.launch {
            isLoading = true
            inlineError = null
            inlineSuccess = null
            val result = authRepository.forgotPassword(email.trim())
            isLoading = false
            result.onSuccess {
                inlineSuccess = "Code sent to your email. Enter the code and your new password."
                codeSent = true
            }.onFailure { exception ->
                inlineError = exception.message ?: "Failed to send reset email. Please try again."
            }
        }
    }

    fun isCodeComplete(): Boolean = code.all { it.isNotEmpty() } && code.joinToString("").length == 6

    fun isResetButtonDisabled(): Boolean {
        if (!codeSent) return isLoading || email.isEmpty()
        return isLoading || !isCodeComplete() || newPassword.isEmpty() || confirmPassword.isEmpty() || newPassword != confirmPassword || newPassword.length < 6
    }

    fun resetPassword() {
        val combined = code.joinToString("")
        if (combined.length != 6) {
            inlineError = "Please enter the complete 6-digit code."
            return
        }
        if (newPassword != confirmPassword) {
            inlineError = "Passwords do not match."
            return
        }
        if (newPassword.length < 6) {
            inlineError = "Password must be at least 6 characters."
            return
        }
        scope.launch {
            isLoading = true
            inlineError = null
            inlineSuccess = null
            val result = authRepository.resetPassword(combined, newPassword)
            isLoading = false
            result.onSuccess {
                dialogMessage = "Password reset successfully. You can now log in."
                isSuccess = true
                showDialog = true
            }.onFailure { e ->
                inlineError = e.message ?: "Failed to reset password. Please try again."
            }
        }
    }

    val focusRequesters = remember { List(6) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        ThemedBackground()

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 25.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (codeSent) "ENTER CODE" else "FORGOT PASSWORD?",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (codeSent)
                            "Enter the 6-digit code and your new password."
                        else
                            "Don't worry! Enter your email address and we'll send you a reset code.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

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

                    if (!codeSent) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.01f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.mail),
                                    contentDescription = "Email Icon",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                TextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    placeholder = { Text("Email") },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            HorizontalDivider(
                                color = Color.Gray,
                                thickness = 1.dp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Verification Code",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(6) { index ->
                                    val value = code[index]
                                    TextField(
                                        value = value,
                                        onValueChange = { newValue ->
                                            val single = newValue.filter { it.isDigit() }.take(1)
                                            val mutable = code.toMutableList()
                                            mutable[index] = single
                                            code = mutable.toList()
                                            if (single.isNotEmpty()) {
                                                if (index < 5) focusRequesters[index + 1].requestFocus() else focusManager.clearFocus()
                                            } else {
                                                if (index > 0) focusRequesters[index - 1].requestFocus()
                                            }
                                        },
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(55.dp)
                                            .focusRequester(focusRequesters[index]),
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.01f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.padlock),
                                    contentDescription = "Password Icon",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                TextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    placeholder = { Text("New Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            HorizontalDivider(
                                color = Color.Gray,
                                thickness = 1.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.01f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.padlock),
                                    contentDescription = "Password Icon",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                TextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    placeholder = { Text("Confirm Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            HorizontalDivider(
                                color = Color.Gray,
                                thickness = 1.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    inlineError?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    inlineSuccess?.let {
                        Text(
                            text = it,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF2E7D32).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { if (codeSent) resetPassword() else forgotPassword() },
                        enabled = !isResetButtonDisabled(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF70CEE3)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = if (codeSent) "Reset Password" else "Send Code",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    if (!codeSent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Remember your password?",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " Login",
                                color = Color(0xFF129FA9),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    navController.navigate("login")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isSuccess) "Success" else "Error") },
            text = { Text(dialogMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    if (isSuccess) {
                        navController.navigate("login")
                    }
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview
@Composable
fun ForgotPasswordPagePreview() {
    MaterialTheme {
        ForgotPasswordScreen(navController = NavController(LocalContext.current))
    }
}