package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.data.StockPricePoint
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.StockRow
import com.rahul.stocksim.ui.components.VicoLineChart
import com.rahul.stocksim.ui.components.VicoPieChart
import com.rahul.stocksim.ui.viewmodels.PortfolioUiState
import com.rahul.stocksim.ui.viewmodels.PortfolioViewModel
import com.google.accompanist.pager.HorizontalPagerIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    navController: NavController,
    viewModel: PortfolioViewModel = hiltViewModel()
) {
    val balance by viewModel.userBalance.collectAsState(initial = 0.0)
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val portfolioHistory by viewModel.portfolioHistory.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        when (val state = uiState) {
            is PortfolioUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is PortfolioUiState.Success -> {
                val portfolioItems = state.portfolioItems
                val totalStockValue = portfolioItems.sumOf { it.first.price * it.second }
                val totalAccountValue = balance + totalStockValue
                
                // Portfolio Analytics
                val dayChange = portfolioItems.sumOf { it.first.change * it.second }
                val dayChangePercent = if (totalStockValue > 0) (dayChange / (totalStockValue - dayChange)) * 100 else 0.0
                
                val bestPerformer = portfolioItems.maxByOrNull { it.first.percentChange }
                val largestHolding = portfolioItems.maxByOrNull { it.first.price * it.second }
                val stockConcentration = if (totalAccountValue > 0) (totalStockValue / totalAccountValue) * 100 else 0.0
                val singleStockConcentration = if (totalAccountValue > 0) ((largestHolding?.let { it.first.price * it.second } ?: 0.0) / totalAccountValue) * 100 else 0.0

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Portfolio",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        // Enhanced Fintech Header with Deep Insights
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            val pagerState = rememberPagerState(pageCount = { 3 })
                            
                            Column {
                                HorizontalPager(state = pagerState) {
                                    page ->
                                    Column(
                                        modifier = Modifier
                                            .padding(24.dp)
                                            .height(250.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        when (page) {
                                            0 -> { // Overview Card
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
                                                                text = "${if (dayChange >= 0) "+" else ""}${String.format("%,.2f%%", dayChangePercent)}",
                                                                color = if (dayChange >= 0) Color.Green else Color.Red,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                if (portfolioHistory.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(24.dp))
                                                    VicoLineChart(
                                                        history = portfolioHistory,
                                                        lineColor = if (dayChange >= 0) Color.Green else Color.Red,
                                                        modifier = Modifier.fillMaxWidth().height(180.dp)
                                                    )
                                                }
                                            }
                                            1 -> { // Diversification Card
                                                if (portfolioItems.isNotEmpty()) {
                                                    val industryData = portfolioItems.groupBy { it.first.industry ?: "Unknown" }
                                                        .mapValues { entry -> entry.value.sumOf { it.first.price * it.second } }
                                                    
                                                    Text(
                                                        text = "Industry Diversification",
                                                        color = Color.White,
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(bottom = 16.dp)
                                                    )
                                                    VicoPieChart(
                                                        data = industryData,
                                                        modifier = Modifier.fillMaxWidth().height(200.dp)
                                                    )
                                                }
                                            }
                                            2 -> { // Insights/Breakdown Card
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
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalPagerIndicator(
                                    pagerState = pagerState,
                                    pageCount = pagerState.pageCount,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(bottom = 8.dp),
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    inactiveColor = Color.Gray.copy(alpha = 0.5f),
                                    indicatorWidth = 8.dp,
                                    indicatorHeight = 8.dp,
                                    spacing = 6.dp
                                )
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
                    }

                    if (portfolioItems.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxHeight(0.5f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(text = "No stocks owned yet", color = Color.Gray)
                            }
                        }
                    } else {
                        items(portfolioItems) { (stock, quantity) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { navController.navigate(Screen.Details.createRoute(stock.symbol)) },
                                color = Color(0xFF1F1F1F),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    StockRow(
                                        stock = stock,
                                        ownedQuantity = quantity
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$quantity ${if (stock.isCrypto) "units" else "shares"}",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Value: $${String.format("%,.2f", stock.price * quantity)}",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
            is PortfolioUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
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
