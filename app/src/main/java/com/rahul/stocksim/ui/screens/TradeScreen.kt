package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.StockRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(mainNavController: NavController) {
    val marketRepository = remember { MarketRepository() }
    val balance by marketRepository.getUserBalance().collectAsState(initial = 0.0)
    var portfolioItems by remember { mutableStateOf<List<Pair<Stock, Long>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val refreshData = {
        coroutineScope.launch {
            if (!isRefreshing) isLoading = true
            portfolioItems = marketRepository.getPortfolioWithQuotes(forceRefresh = true)
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        // Try to load from cache immediately
        val cached = marketRepository.getPortfolioWithQuotes(forceRefresh = false)
        if (cached.isNotEmpty()) {
            portfolioItems = cached
            isLoading = false
        }
        // Then refresh in the background
        refreshData()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            refreshData()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Buying Power Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Buying Power", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        text = "$${String.format("%,.2f", balance)}",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && portfolioItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                val activePositions = portfolioItems.filter { it.second > 0 }
                val oldPositions = portfolioItems.filter { it.second <= 0 }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (activePositions.isNotEmpty()) {
                        item {
                            Text(
                                text = "Active Positions",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                        items(activePositions) { (stock, quantity) ->
                            PositionRow(stock, quantity, mainNavController)
                        }
                    }

                    if (oldPositions.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Old Positions",
                                color = Color.Gray,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                        items(oldPositions) { (stock, quantity) ->
                            PositionRow(stock, quantity, mainNavController, isOld = true)
                        }
                    }

                    if (activePositions.isEmpty() && oldPositions.isEmpty() && !isLoading) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "You don't have any trading history yet.", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PositionRow(stock: Stock, quantity: Long, mainNavController: NavController, isOld: Boolean = false) {
    Column {
        StockRow(
            stock = stock,
            ownedQuantity = quantity,
            onRowClick = { mainNavController.navigate("details/${stock.symbol}") }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isOld) "Sold All" else "$quantity Shares", 
                color = if (isOld) Color.Gray else Color.White, 
                fontSize = 14.sp
            )
            if (!isOld) {
                Text(
                    text = "Value: $${String.format("%.2f", stock.price * quantity)}",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
    }
}
