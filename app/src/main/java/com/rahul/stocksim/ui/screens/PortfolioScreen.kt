package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun PortfolioScreen(navController: NavController) {
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

    val totalStockValue = portfolioItems.sumOf { it.first.price * it.second }
    val totalAccountValue = balance + totalStockValue

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
            // Fintech Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = "Total Account Value", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        text = "$${String.format("%,.2f", totalAccountValue)}",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(text = "Cash Balance", color = Color.Gray, fontSize = 12.sp)
                            Text(text = "$${String.format("%,.2f", balance)}", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Stock Value", color = Color.Gray, fontSize = 12.sp)
                            Text(text = "$${String.format("%,.2f", totalStockValue)}", color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your Assets",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isLoading && portfolioItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (portfolioItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No stocks owned yet", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(portfolioItems) { (stock, quantity) ->
                        StockRow(
                            stock = stock,
                            ownedQuantity = quantity,
                            onRowClick = { navController.navigate("details/${stock.symbol}") }
                        )
                        Text(
                            text = "$quantity shares • $${String.format("%.2f", stock.price * quantity)} total",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    }
                }
            }
        }
    }
}