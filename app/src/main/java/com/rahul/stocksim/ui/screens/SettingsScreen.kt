package com.rahul.stocksim.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.NotificationSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = AuthRepository()
    val user = authRepository.currentUser
    
    var isUploading by remember { mutableStateOf(false) }
    var profilePhotoUrl by remember { mutableStateOf(user?.photoUrl) }
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var notifSettings by remember { mutableStateOf(NotificationSettings()) }

    // Dialog States
    var showNameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        notifSettings = authRepository.getNotificationSettings()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploading = true
            coroutineScope.launch {
                val result = authRepository.updateProfilePicture(it)
                isUploading = false
                if (result.isSuccess) {
                    try {
                        authRepository.currentUser?.reload()?.await()
                        profilePhotoUrl = authRepository.currentUser?.photoUrl
                        Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Upload failed: ${result.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = Color.White) },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable(enabled = !isUploading) { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (isUploading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                        ProfileImageInternal(displayName, profilePhotoUrl)
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.BottomEnd).size(36.dp).offset(x = 4.dp, y = 4.dp),
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Change Picture",
                            tint = Color.White,
                            modifier = Modifier.padding(8.dp).fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // User Profile Section
            SettingsSection(title = "Profile") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    label = "Display Name",
                    value = displayName.ifEmpty { "Not set" },
                    onClick = { showNameDialog = true }
                )
                SettingsItem(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = user?.email ?: "N/A",
                    trailing = {
                        if (user?.isEmailVerified == true) {
                            Icon(Icons.Default.Verified, contentDescription = "Verified", tint = Color.Green, modifier = Modifier.size(20.dp))
                        } else {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    authRepository.sendEmailVerification()
                                    Toast.makeText(context, "Verification email sent!", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Verify", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notifications Section
            SettingsSection(title = "Notifications") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Master Switch", color = Color.White)
                    Switch(
                        checked = notifSettings.masterEnabled,
                        onCheckedChange = { 
                            val updated = notifSettings.copy(masterEnabled = it)
                            notifSettings = updated
                            coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                        }
                    )
                }
                
                if (notifSettings.masterEnabled) {
                    Divider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    NotificationCheckbox("Via Email", notifSettings.viaEmail) { 
                        val updated = notifSettings.copy(viaEmail = it)
                        notifSettings = updated
                        coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                    }
                    NotificationCheckbox("Via Push", notifSettings.viaPush) { 
                        val updated = notifSettings.copy(viaPush = it)
                        notifSettings = updated
                        coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Notify me when:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    NotificationCheckbox("Large stock drop (>5%)", notifSettings.notifyLargeDrop) { 
                        val updated = notifSettings.copy(notifyLargeDrop = it)
                        notifSettings = updated
                        coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                    }
                    NotificationCheckbox("Low balance alert", notifSettings.notifyLowBalance) { 
                        val updated = notifSettings.copy(notifyLowBalance = it)
                        notifSettings = updated
                        coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                    }
                    NotificationCheckbox("New sign-in detected", notifSettings.notifyNewSignIn) { 
                        val updated = notifSettings.copy(notifyNewSignIn = it)
                        notifSettings = updated
                        coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account Security Section
            SettingsSection(title = "Security") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    label = "Change Password",
                    value = "********",
                    onClick = { showPasswordDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button
            Button(
                onClick = {
                    authRepository.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        }
    }

    // --- IMMERSIVE DIALOGS (DARK THEME) ---

    // Name Edit Dialog
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(displayName) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            containerColor = Color(0xFF1F1F1F),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            title = { Text("Edit Display Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val result = authRepository.updateDisplayName(tempName)
                        if (result.isSuccess) {
                            displayName = tempName
                            showNameDialog = false
                            Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Save", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = Color(0xFF1F1F1F),
            titleContentColor = Color.White,
            title = { Text("Change Password") },
            text = {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val result = authRepository.updatePassword(newPassword)
                        if (result.isSuccess) {
                            showPasswordDialog = false
                            Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Update", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }
}

@Composable
fun NotificationCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun ProfileImageInternal(
    displayName: String?,
    photoUrl: Uri?,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val initials = displayName?.split(" ")
        ?.mapNotNull { it.firstOrNull()?.toString() }
        ?.joinToString("") ?: ""

    if (photoUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials.ifEmpty { "?" },
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, 
    label: String, 
    value: String, 
    onClick: () -> Unit = {},
    trailing: @Composable () -> Unit = { Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray) }
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        trailing()
    }
}
