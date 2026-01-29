package com.rahul.stocksim.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rahul.stocksim.data.AuthRepository

//function register screen takes another function as parameter, returning
//unit(nothing). () -> Unit defines the type of parameter it is
@Composable
fun RegisterScreen(navController: NavController) {
    //mutableStateOf creates state object that compose can
    //track through refreshes. initial value is empty string
    //remember holds those values in memory when screen refreshes
    val authRepository = AuthRepository() //TODO: Implement dependency injection
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    //column stacks elements vertically
    Column(
        //makes login column fill available space with padding
        modifier = Modifier.fillMaxSize().padding(24.dp),
        //arranges elements all in center
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //create header
        Text(text = "Sign Up",
            style = MaterialTheme.typography.headlineMedium)

        //creates gaps between elements so UI is not cramped
        Spacer(modifier = Modifier.height(32.dp))

        //standard text field
        OutlinedTextField(
            //sets value of email to what is typed
            value = email,
            //callback to update email variable when text changes
            onValueChange = { email = it },
            label = { Text("Email") },
            //use modifier to fill max width
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            //masks the input with dots for security
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }

        Button(
            //when button is pressed, call on register success
            onClick = {
                isLoading = true
                authRepository.register(email, password) { success, error ->
                    isLoading = false
                    if (success) {
                        navController.navigate("home_screen") {
                            popUpTo("register_screen") {
                                inclusive = true
                            }
                        }
                    } else {
                        errorMessage = error ?: "Registration failed"
                    }
                }
            },
            //button is greyed out until user has typed at least
            //one character in both username and password
            //and the passwords match
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Create Account")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                navController.navigate("login_screen") {
                    popUpTo("login_screen") {
                        inclusive = true
                    }
                }
            }
        ) {
            Text("Already have an account? Sign in")
        }
    }
}