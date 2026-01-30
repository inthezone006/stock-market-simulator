package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rahul.stocksim.R
import com.rahul.stocksim.data.AuthRepository
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun RegisterScreen(navController: NavController) {
    val authRepository = AuthRepository()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Validation Logic
    val hasMinLength = password.length >= 8
    val hasUppercase = password.any { it.isUpperCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }
    val passwordsMatch = password.isNotEmpty() && password == confirmPassword
    val isFormValid = name.isNotEmpty() && email.contains("@") && hasMinLength && hasUppercase && hasDigit && hasSpecial && passwordsMatch

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121212)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.stock_market_sim),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = "Full Name", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.DarkGray
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(text = "Email", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.DarkGray
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "Password", color = Color.LightGray) },
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
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text(text = "Confirm Password", color = Color.LightGray) },
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
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Requirements List
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("Requirements:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                RequirementItem("At least 8 characters", hasMinLength)
                RequirementItem("At least one uppercase letter", hasUppercase)
                RequirementItem("At least one digit", hasDigit)
                RequirementItem("At least one special character", hasSpecial)
                RequirementItem("Passwords must match", passwordsMatch)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isLoading = true
                        authRepository.register(email, password) { success, error ->
                            if (success) {
                                coroutineScope.launch {
                                    authRepository.updateDisplayName(name)
                                    isLoading = false
                                    navController.navigate(Screen.BalanceSelection.route) {
                                        popUpTo(Screen.Register.route) { inclusive = true }
                                    }
                                }
                            } else {
                                isLoading = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(error ?: "Registration failed")
                                }
                            }
                        }
                    },
                    enabled = !isLoading && isFormValid,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isFormValid) Color.White else Color.DarkGray),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign up", fontWeight = FontWeight.Medium)
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = WEB_CLIENT_ID).build()
                        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

                        coroutineScope.launch {
                            try {
                                val activity = context.findActivity() ?: return@launch
                                val result = credentialManager.getCredential(request = request, context = activity)
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                authRepository.signInWithGoogle(googleIdTokenCredential.idToken) { success ->
                                    if (success) {
                                        navController.navigate(Screen.PasswordSetup.route) {
                                            popUpTo(Screen.Register.route) { inclusive = true }
                                        }
                                    } else {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Google Sign-in failed") }
                                    }
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Error: ${e.localizedMessage}")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.android_light_rd_na),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                Text(text = "Already have an account? Sign in", color = Color.Gray)
            }
        }
    }
}
