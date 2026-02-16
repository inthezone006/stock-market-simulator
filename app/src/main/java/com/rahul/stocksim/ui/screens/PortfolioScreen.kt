package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
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
        val cached = marketRepository.getPortfolioWithQuotes(forceRefresh = false)
        if (cached.isNotEmpty()) {
            portfolioItems = cached
            isLoading = false
        }
        refreshData()
    }

    val totalStockValue = portfolioItems.sumOf { it.first.price * it.second }
    val totalAccountValue = balance + totalStockValue
    
    // Portfolio Analytics
    val dayChange = portfolioItems.sumOf { it.first.change * it.second }
    val dayChangePercent = if (totalStockValue > 0) (dayChange / (totalStockValue - dayChange)) * 100 else 0.0
    
    val bestPerformer = portfolioItems.maxByOrNull { it.first.percentChange }
    val largestHolding = portfolioItems.maxByOrNull { it.first.price * it.second }
    val stockConcentration = if (totalAccountValue > 0) (totalStockValue / totalAccountValue) * 100 else 0.0
    val singleStockConcentration = if (totalAccountValue > 0) ((largestHolding?.let { it.first.price * it.second } ?: 0.0) / totalAccountValue) * 100 else 0.0

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Enhanced Fintech Header with Deep Insights
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(text = "Total Account Value", color = Color.Gray, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$${String.format("%,.2f", totalAccountValue)}",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Surface(
                                color = (if (dayChange >= 0) Color.Green else Color.Red).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (dayChange >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = if (dayChange >= 0) Color.Green else Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${if (dayChange >= 0) "+" else ""}${String.format("%.2f%%", dayChangePercent)}",
                                        color = if (dayChange >= 0) Color.Green else Color.Red,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Invested vs Available breakdown
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            InsightSummaryItem(
                                modifier = Modifier.weight(1f),
                                label = "Stocks Value",
                                value = "$${String.format("%,.2f", totalStockValue)}",
                                subValue = "${String.format("%.1f%%", stockConcentration)} of total",
                                icon = Icons.Default.BarChart,
                                color = MaterialTheme.colorScheme.primary
                            )
                            InsightSummaryItem(
                                modifier = Modifier.weight(1f),
                                label = "Buying Power",
                                value = "$${String.format("%,.2f", balance)}",
                                subValue = "Available to trade",
                                icon = Icons.Default.AccountBalanceWallet,
                                color = Color.Green
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(20.dp))

                        // Deep Insights Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            InsightItem(
                                label = "Portfolio Risk",
                                value = if (singleStockConcentration > 50) "High" else if (singleStockConcentration > 25 || portfolioItems.size < 3) "Medium" else "Low",
                                icon = Icons.Default.Warning,
                                color = if (singleStockConcentration > 50) Color.Red else if (singleStockConcentration > 25 || portfolioItems.size < 3) Color(0xFFFFA726) else Color.Green
                            )
                            InsightItem(
                                label = "Top Performer",
                                value = bestPerformer?.first?.symbol ?: "N/A",
                                icon = Icons.Default.Star,
                                color = Color(0xFFFFD700)
                            )
                            InsightItem(
                                label = "Largest Asset",
                                value = largestHolding?.first?.symbol ?: "N/A",
                                icon = Icons.Default.PieChart,
                                color = Color.Cyan
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Your Assets",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (isLoading && portfolioItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else if (portfolioItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(text = "No stocks owned yet", color = Color.Gray)
                    }
                }
            } else {
                items(portfolioItems) { (stock, quantity) ->
                    StockRow(
                        stock = stock,
                        ownedQuantity = quantity,
                        onRowClick = { navController.navigate("details/${stock.symbol}") }
                    )
                    Text(
                        text = "$quantity ${if (stock.isCrypto) "units" else "shares"} • $${String.format("%.2f", stock.price * quantity)} total",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun InsightSummaryItem(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subValue: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.03f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = label, color = Color.Gray, fontSize = 10.sp)
            Text(text = subValue, color = color.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun InsightItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color = Color.Gray) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}
