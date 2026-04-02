package com.rahul.stocksim.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rahul.stocksim.BuildConfig
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.NotificationSettings
import com.rahul.stocksim.util.NotificationHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authRepository = AuthRepository()
    
    // Using mutableStateOf for user to allow manual force-refresh
    var user by remember { mutableStateOf(authRepository.currentUser) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    var profilePhotoUrl by remember { mutableStateOf(user?.photoUrl) }
    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var notifSettings by remember { mutableStateOf(NotificationSettings()) }

    // Permission launcher to handle system-level permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val updated = notifSettings.copy(masterEnabled = true)
            notifSettings = updated
            coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
        } else {
            Toast.makeText(context, "Notifications permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    val loadData = {
        coroutineScope.launch {
            try {
                user?.reload()?.await()
                user = authRepository.currentUser
                displayName = user?.displayName ?: ""
                profilePhotoUrl = user?.photoUrl
                val settings = authRepository.getNotificationSettings()
                
                // Sync with system permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission && settings.masterEnabled) {
                        // System permission was denied, force disable the app-level setting
                        val updated = settings.copy(masterEnabled = false)
                        notifSettings = updated
                        authRepository.saveNotificationSettings(updated)
                    } else {
                        notifSettings = settings
                    }
                } else {
                    notifSettings = settings
                }
            } catch (e: Exception) {
                // Handle potential errors
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
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
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                loadData()
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture Section
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape)) {
                        ProfileImageInternal(displayName, profilePhotoUrl)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // User Info Display
                Text(
                    text = displayName.ifEmpty { "Trading User" },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = user?.email ?: "N/A",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(32.dp))

                // User Profile Section
                SettingsSection(title = "Profile") {
                    SettingsItem(
                        icon = Icons.Default.Edit,
                        label = "Identity",
                        value = "Edit account details",
                        onClick = { navController.navigate(Screen.EditProfile.route) }
                    )
                    SettingsItem(
                        icon = Icons.Default.Email,
                        label = "Verification",
                        value = if (user?.isEmailVerified == true) "Email verified" else "Email not verified",
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
                                    Text("Verify Now", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
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
                        Text("Use notifications", color = Color.White)
                        Switch(
                            checked = notifSettings.masterEnabled,
                            onCheckedChange = { 
                                if (it) {
                                    // User trying to enable notifications
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                        if (!hasPermission) {
                                            // Trigger system permission request
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            // Permission already exists, just update repo
                                            val updated = notifSettings.copy(masterEnabled = true)
                                            notifSettings = updated
                                            coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                                        }
                                    } else {
                                        // Legacy Android versions don't need runtime permission
                                        val updated = notifSettings.copy(masterEnabled = true)
                                        notifSettings = updated
                                        coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                                    }
                                } else {
                                    // User turning notifications OFF
                                    val updated = notifSettings.copy(masterEnabled = false)
                                    notifSettings = updated
                                    coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                                }
                            }
                        )
                    }
                    
                    if (notifSettings.masterEnabled) {
                        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Notify me when:", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                        
                        NotificationCheckbox("Large stock drop", notifSettings.notifyLargeDrop) {
                            val updated = notifSettings.copy(notifyLargeDrop = it)
                            notifSettings = updated
                            coroutineScope.launch { authRepository.saveNotificationSettings(updated) }
                        }
                        NotificationCheckbox("Low balance", notifSettings.notifyLowBalance) {
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
                SettingsSection(title = "Support") {
                    SettingsItem(
                        icon = Icons.Default.Share,
                        label = "Share App",
                        value = "Invite friends to trade",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Check out TradeSim!")
                                putExtra(Intent.EXTRA_TEXT, "I'm practicing my trading skills on TradeSim: Stock Market Sim. Download it now on Google Play! https://play.google.com/store/apps/details?id=com.rahul.stocksim")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.BugReport,
                        label = "Feedback",
                        value = "Report a bug or suggest a feature",
                        onClick = {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:rahul.menon85280@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "App Feedback - v${BuildConfig.VERSION_NAME}")
                            }
                            try {
                                context.startActivity(emailIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Security Section
                SettingsSection(title = "Security") {
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        label = "Security Credentials",
                        value = "Update password",
                        onClick = { navController.navigate(Screen.PasswordSetup.createRoute(true)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // About Section (Version Info)
                SettingsSection(title = "About") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        label = "Current Version",
                        value = "${BuildConfig.VERSION_NAME}",
                        trailing = {}
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
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
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
    val context = LocalContext.current
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
        verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Color.Gray, fontSize = 12.sp)
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        trailing()
    }
}

@Composable
fun NotificationCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}
