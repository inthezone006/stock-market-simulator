package com.rahul.stocksim

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.rahul.stocksim.ui.theme.TradeSimTheme
import com.rahul.stocksim.ui.screens.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.auth
import com.google.firebase.messaging.messaging
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.service.PriceAlertWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        
        PriceAlertWorker.schedule(this)
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.dark(
                scrim = android.graphics.Color.TRANSPARENT
            )
        )
        
        super.onCreate(savedInstanceState)
        
        setContent {
            val navController = rememberNavController()
            val auth = Firebase.auth
            val analytics = Firebase.analytics
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()

            val fetchAndSaveToken = {
                Firebase.messaging.token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("FCM_TOKEN", "Token: $token")
                        coroutineScope.launch {
                            authRepository.saveFcmToken(token)
                        }
                    }
                }
            }

            // Notification Permission Request for Android 13+
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    fetchAndSaveToken()
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        fetchAndSaveToken()
                    } else {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    fetchAndSaveToken()
                }
            }
            
            val startDest = if (auth.currentUser != null) Screen.Main.route else Screen.Login.route

            // Track screen views
            LaunchedEffect(navController) {
                navController.currentBackStackEntryFlow.collect { backStackEntry ->
                    val route = backStackEntry.destination.route
                    if (route != null) {
                        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
                            putString(FirebaseAnalytics.Param.SCREEN_NAME, route)
                            putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity")
                        })
                    }
                }
            }

            TradeSimTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {
                        composable(route = Screen.Login.route) {
                            LoginScreen(navController = navController)
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(navController = navController)
                        }
                        composable(
                            route = Screen.PasswordSetup.route,
                            arguments = listOf(
                                navArgument("isChangePassword") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                },
                                navArgument("name") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("email") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val isChange = backStackEntry.arguments?.getBoolean("isChangePassword") ?: false
                            val name = backStackEntry.arguments?.getString("name")
                            val email = backStackEntry.arguments?.getString("email")
                            PasswordSetupScreen(
                                navController = navController, 
                                isChangePassword = isChange,
                                initialName = name,
                                initialEmail = email
                            )
                        }
                        composable(
                            route = Screen.BalanceSelection.route,
                            arguments = listOf(
                                navArgument("name") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("email") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                },
                                navArgument("password") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val name = backStackEntry.arguments?.getString("name")
                            val email = backStackEntry.arguments?.getString("email")
                            val password = backStackEntry.arguments?.getString("password")
                            BalanceSelectionScreen(
                                navController = navController,
                                name = name,
                                email = email,
                                password = password
                            )
                        }
                        composable(Screen.Main.route) {
                            MainScreen(
                                mainNavController = navController,
                                onStockClick = { stock ->
                                    navController.navigate("details/${stock.symbol}")
                                }
                            )
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(navController = navController)
                        }
                        composable(Screen.EditProfile.route) {
                            EditProfileScreen(navController = navController)
                        }
                        composable(
                            route = Screen.Details.route,
                            arguments = listOf(
                                navArgument("symbol") {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val symbol = backStackEntry.arguments?.getString("symbol")
                            StockDetailScreen(
                                stockSymbol = symbol,
                                navController = navController,
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.MarketTutorial.route) {
                            MarketTutorialScreen(
                                onComplete = {
                                    coroutineScope.launch {
                                        authRepository.setTutorialCompleted()
                                        navController.popBackStack()
                                    }
                                },
                                onDismiss = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
