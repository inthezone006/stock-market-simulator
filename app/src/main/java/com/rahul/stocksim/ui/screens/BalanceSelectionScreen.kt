package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rahul.stocksim.data.AuthRepository
import kotlinx.coroutines.launch

data class BalanceLevel(val level: Int, val amount: Double, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSelectionScreen(navController: NavController) {
    val authRepository = AuthRepository()
    val coroutineScope = rememberCoroutineScope()
    
    val levels = listOf(
        BalanceLevel(1, 100000.0, "$100,000 (Beginner)"),
        BalanceLevel(2, 50000.0, "$50,000 (Casual)"),
        BalanceLevel(3, 25000.0, "$25,000 (Standard)"),
        BalanceLevel(4, 10000.0, "$10,000 (Intermediate)"),
        BalanceLevel(5, 5000.0, "$5,000 (Hard)"),
        BalanceLevel(6, 1000.0, "$1,000 (Difficult)"),
        BalanceLevel(7, 100.0, "$100 (Impossible)")
    )

    var selectedLevel by remember { mutableStateOf(levels[3]) } // Default to $10,000
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Choose Your Difficulty", color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF121212))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isLoading = true
                    coroutineScope.launch {
                        val result = authRepository.setUserBalance(selectedLevel.amount)
                        isLoading = false
                        if (result.isSuccess) {
                            navController.navigate(Screen.Main.route) {
                                popUpTo(Screen.BalanceSelection.route) { inclusive = true }
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Confirm Selection")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "Select a starting balance for your simulator account. This will determine your initial trading power.",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Column(Modifier.selectableGroup()) {
                levels.forEach { level ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (level == selectedLevel),
                                onClick = { selectedLevel = level },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (level == selectedLevel),
                            onClick = null, // null recommended for accessibility with selectable modifier
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = Color.Gray
                            )
                        )
                        Text(
                            text = "Level ${level.level}: ${level.label}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (level == selectedLevel) Color.White else Color.Gray,
                            fontWeight = if (level == selectedLevel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        }
    }
}
