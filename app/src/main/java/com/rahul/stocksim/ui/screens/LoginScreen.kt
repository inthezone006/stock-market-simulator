package com.rahul.stocksim.ui.screens

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.rahul.stocksim.R
import com.rahul.stocksim.data.AuthRepository
import kotlinx.coroutines.launch
import androidx.core.graphics.toColorInt
import com.google.accompanist.systemuicontroller.rememberSystemUiController

val WEB_CLIENT_ID = "921964890596-iqltc99aa0dbc73p644csaa5p8qcmeph.apps.googleusercontent.com"

//function login screen takes onLoginSuccess as parameter, returning
//unit(nothing). () -> Unit defines the type of parameter it is. it also
//takes a onRegisterClick as parameter for what to do when register
//text button is clicked
@Composable
fun LoginScreen(navController: NavController) {
    //mutableStateOf creates state object that compose can
    //track through refreshes. initial value is empty string
    //remember holds those values in memory when screen refreshes
    val authRepository = AuthRepository() //TODO: Implement dependency injection
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    //google signin
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = CredentialManager.create(context)

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212))
    ) {
        //column stacks elements vertically
        Column(
            //makes login column fill available space
            modifier = Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
            //arranges elements all in center
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.stock_market_sim),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(
                modifier = Modifier.height(20.dp)
            )

//            Text(
//                text = "Stock Market Simulator",
//                style = MaterialTheme.typography.headlineLarge,
//                color = Color.White,
//                fontWeight = FontWeight.ExtraBold
//            )

            Spacer(
                modifier = Modifier.height(48.dp)
            )


            //standard text field
            OutlinedTextField(
                //sets value of email to what is typed
                value = email,
                //callback to update email variable when text changes
                onValueChange = { email = it },
                label = { Text(text = "Email", color = Color.LightGray) },
                shape = RoundedCornerShape(8.dp),
                //use modifier to fill max width
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "Password", color = Color.Gray) },
                //masks the input with dots for security
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
            )

            Spacer(modifier = Modifier.height(24.dp))

            //if error message contains a value, display text
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                //check if error message is null, if not, display error message
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                //when button is pressed, call on login success
                onClick = {
                    isLoading = true
                    authRepository.login(email, password) { success, error ->
                        //lambda function to check success and handle navigation inside function if auth true
                        isLoading = false
                        if (success) {
                            navController.navigate("home_screen") {
                                popUpTo("login_screen") {
                                    inclusive = true
                                }
                            }
                        } else {
                            errorMessage = error ?: "Registration failed"
                        }
                    }
                },
                //button is greyed out until user has typed at least one character in both email and password
                enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text("Sign in", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            //google sign in button
            OutlinedButton(
                onClick = {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(WEB_CLIENT_ID)
                        .setAutoSelectEnabled(true)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    coroutineScope.launch {
                        try {
                            val result = credentialManager.getCredential(
                                request = request,
                                context = context as Activity
                            )
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                            authRepository.signInWithGoogle(googleIdTokenCredential.idToken) { success ->
                                if (success) {
                                    navController.navigate("home_screen") {
                                        popUpTo("login_screen") { inclusive = true }
                                    }
                                } else {
                                    errorMessage = "Firebase Google Auth Failed"
                                }
                            }
                        } catch (e: GetCredentialException) {
                            Log.e("Auth", "Google Sign-in failed: ${e.message}")
                            errorMessage = "Google Sign-In Cancelled"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color.DarkGray)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.android_light_rd_na),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Google", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            //create a button with just text that has an onclick function (passed in through function)
            TextButton(
                onClick = {
                    navController.navigate("register_screen") {
                        popUpTo("register_screen") {
                            inclusive = true
                        }
                    }
                }
            ) {
                Text(text = "Don't have an account? Sign up", color = Color.Gray)
            }
        }
    }
}