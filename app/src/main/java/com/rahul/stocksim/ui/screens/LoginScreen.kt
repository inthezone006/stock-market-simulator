package com.rahul.stocksim.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

//function loginscreen takes another function as parameter, returning
//unit(nothing). () -> Unit defines the type of parameter it is
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    //mutableStateOf creates state object that compose can
    //track through refreshes. initial value is empty string
    //remember holds those values in memory when screen refreshes
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    //column stacks elements vertically
    Column(
        //makes login column fill available space
        modifier = Modifier.fillMaxSize(),
        //arranges elements all in center
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        //create header
        Text(text = "Welcome to Stock Market Simulator",
            style = MaterialTheme.typography.headlineMedium)

        //creates gaps between elements so UI is not cramped
        Spacer(modifier = Modifier.height(32.dp))

        //standard text field
        OutlinedTextField(
            //sets value of username to what is typed
            value = username,
            //callback to update username variable when text changes
            onValueChange = { username = it },
            label = { Text("Username") },
            //use modifier to fillmaxwidth
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

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            //when button is pressed, call onloginsuccess
            onClick = { onLoginSuccess() },
            modifier = Modifier.fillMaxWidth(),
            //button is greyed out until user has typed at least
            //one character in both username and password
            enabled = username.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Login")
        }
    }
}