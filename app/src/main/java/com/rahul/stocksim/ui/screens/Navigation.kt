package com.rahul.stocksim.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.ui.graphics.vector.ImageVector

// Shared constant to avoid duplication and conflicts
const val WEB_CLIENT_ID = "921964890596-iqltc99aa0dbc73p644csaa5p8qcmeph.apps.googleusercontent.com"

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object PasswordSetup : Screen("password_setup_screen/{isChangePassword}") {
        fun createRoute(isChangePassword: Boolean) = "password_setup_screen/$isChangePassword"
    }
    object BalanceSelection : Screen("balance_selection_screen")
    object Settings : Screen("settings_screen")
    object EditProfile : Screen("edit_profile_screen")
    object Main : Screen("main_screen")
    object Details : Screen("details/{symbol}")
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Portfolio : BottomNavItem("portfolio_screen", "Portfolio", Icons.Default.AccountBalanceWallet)
    object Market : BottomNavItem("market_screen", "Market", Icons.Default.BarChart)
    object Trade : BottomNavItem("trade_screen", "Trade", Icons.Default.AttachMoney)
    object Leaderboard : BottomNavItem("leaderboard_screen", "Leaders", Icons.Default.Leaderboard)
}
