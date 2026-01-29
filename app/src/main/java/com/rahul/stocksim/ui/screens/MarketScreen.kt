package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.StockRow

@Composable
fun MarketScreen(
    navController: NavController, 
    onStockClick: (Stock) -> Unit,
    onSettingsClick: () -> Unit
) {
    // Mock stock data - In production this would come from MarketRepository
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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