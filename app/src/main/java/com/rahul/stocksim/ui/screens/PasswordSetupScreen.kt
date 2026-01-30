package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rahul.stocksim.data.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordSetupScreen(navController: NavController) {
    val authRepository = AuthRepository()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Password validation logic
    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    val isPasswordValid = hasMinLength && hasUppercase && hasDigit && hasSpecial && passwordsMatch

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121212),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Set Your Password", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isPasswordValid) {
                        isLoading = true
                        coroutineScope.launch {
                            val result = authRepository.updatePassword(password)
                            isLoading = false
                            if (result.isSuccess) {
                                navController.navigate(Screen.BalanceSelection.route) {
                                    popUpTo(Screen.PasswordSetup.route) { inclusive = true }
                                }
                            } else {
                                snackbarHostState.showSnackbar("Error: ${result.exceptionOrNull()?.localizedMessage}")
                            }
                        }
                    }
                },
                containerColor = if (isPasswordValid) MaterialTheme.colorScheme.primary else Color.DarkGray,
                contentColor = Color.White
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Confirm Password")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "To keep your account secure, please create a password. You can use this to sign in with your Google email later.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.DarkGray
                ),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.DarkGray
                ),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Requirements List
            Text("Requirements:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            RequirementItem("At least 8 characters", hasMinLength)
            RequirementItem("At least one uppercase letter", hasUppercase)
            RequirementItem("At least one digit", hasDigit)
            RequirementItem("At least one special character", hasSpecial)
            RequirementItem("Passwords must match", passwordsMatch)
        }
    }
}

@Composable
fun RequirementItem(text: String, isMet: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = if (isMet) Color.Green else Color.DarkGray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = if (isMet) Color.White else Color.Gray,
            fontSize = 12.sp
        )
    }
}
