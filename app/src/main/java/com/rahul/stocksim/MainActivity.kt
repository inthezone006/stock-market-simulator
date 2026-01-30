package com.rahul.stocksim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import com.rahul.stocksim.ui.theme.StockMarketSimulatorTheme
import com.rahul.stocksim.ui.screens.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            
            val startDest = if (auth.currentUser != null) Screen.Main.route else Screen.Login.route

            StockMarketSimulatorTheme() {
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
                                }
                            )
                        ) { backStackEntry ->
                            val isChange = backStackEntry.arguments?.getBoolean("isChangePassword") ?: false
                            PasswordSetupScreen(navController = navController, isChangePassword = isChange)
                        }
                        composable(Screen.BalanceSelection.route) {
                            BalanceSelectionScreen(navController = navController)
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
                                onBackClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}