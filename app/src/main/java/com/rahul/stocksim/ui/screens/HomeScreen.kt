package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.StockRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, onStockClick: (Stock) -> Unit) {
    val authRepository = AuthRepository()
    val user = authRepository.currentUser
    
    val stockList = listOf(
        Stock("AAPL", "Apple Inc.", 150.00, 5.00),
        Stock("GOOGL", "Alphabet Inc.", 2500.00, 100.00),
        Stock("MSFT", "Microsoft Corporation", 200.00, 2.00),
        Stock("AMZN", "Amazon.com Inc.", 3000.00, 500.00),
        Stock("FB", "Facebook Inc.", 200.00, 1.00),
        Stock("TSLA", "Tesla Inc.", 500.00, 10.00),
        Stock("NFLX", "Netflix Inc.", 400.00, 2.00),
        Stock("NVDA", "Nvidia Corporation", 100.00, 0.50),
        Stock("AMD", "AMD Inc.", 80.00, 0.25),
        Stock("INTC", "Intel Corporation", 50.00, 0.10),
        Stock("PYPL", "PayPal Holdings Inc.", 30.00, 0.05),
        Stock("CSCO", "Cisco Systems Inc.", 70.00, 0.15),
        Stock("ADBE", "Adobe Inc.", 120.00, 0.20),
        Stock("EBAY", "eBay Inc.", 15.00, 0.02),
        Stock("QCOM", "Qualcomm Inc.", 90.00, 0.12),
        Stock("CRM", "Salesforce.com Inc.", 180.00, 0.30),
    )

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Stock Market Simulator",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Profile Icon / Initials
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.DarkGray)
                            .clickable { navController.navigate("settings_screen") },
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = user?.photoUrl
                        if (photoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            val initials = user?.displayName?.split(" ")
                                ?.mapNotNull { it.firstOrNull()?.toString() }
                                ?.joinToString("") ?: user?.email?.firstOrNull()?.toString()?.uppercase() ?: "?"
                            
                            Text(
                                text = initials,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Add stock logic */ },
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Stock")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212))
        ) {
            items(stockList) { currentStock ->
                StockRow(
                    stock = currentStock,
                    onRowClick = { stock -> onStockClick(stock) }
                )
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}