package com.rahul.stocksim.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rahul.stocksim.data.AuthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = AuthRepository()
    val user = authRepository.currentUser
    
    var name by remember { mutableStateOf(user?.displayName ?: "") }
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var isUpdatingName by remember { mutableStateOf(false) }
    var isUpdatingPassword by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Edit Name Section
            Text(
                "Identity",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (name.isNotEmpty() && name != user?.displayName) {
                                if (isUpdatingName) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(onClick = {
                                        isUpdatingName = true
                                        coroutineScope.launch {
                                            val result = authRepository.updateDisplayName(name)
                                            isUpdatingName = false
                                            if (result.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    "Name updated successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Update failed: ${result.exceptionOrNull()?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Confirm",
                                            tint = Color.Green
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Edit Password Section
            Text(
                "Security",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
            ) {
                val canUpdatePassword =
                    newPassword.length >= 8 && newPassword == confirmPassword && oldPassword.isNotEmpty()

                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (canUpdatePassword) {
                                if (isUpdatingPassword) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(onClick = {
                                        isUpdatingPassword = true
                                        coroutineScope.launch {
                                            val result = authRepository.updatePassword(newPassword)
                                            isUpdatingPassword = false
                                            if (result.isSuccess) {
                                                Toast.makeText(
                                                    context,
                                                    "Password updated!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                oldPassword = ""; newPassword =
                                                    ""; confirmPassword = ""
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Error: ${result.exceptionOrNull()?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Confirm",
                                            tint = Color.Green
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.DarkGray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Danger Zone
            Text("Danger Zone", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Once you delete your account, there is no going back. Please be certain.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        var deleteConfirmPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteDialog = false },
            containerColor = Color(0xFF1F1F1F),
            titleContentColor = Color.White,
            title = { Text("Are you absolutely sure?") },
            text = {
                Column {
                    Text("This action will permanently delete your portfolio, history, and account settings.", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deleteConfirmPassword,
                        onValueChange = { deleteConfirmPassword = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.DarkGray)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeletingAccount = true
                        coroutineScope.launch {
                            val result = authRepository.deleteAccount(deleteConfirmPassword)
                            isDeletingAccount = false
                            if (result.isSuccess) {
                                showDeleteDialog = false
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Main.route) { inclusive = true }
                                }
                                Toast.makeText(context, "Account deleted", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = deleteConfirmPassword.isNotEmpty() && !isDeletingAccount
                ) {
                    if (isDeletingAccount) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("DELETE FOREVER", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeletingAccount) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
