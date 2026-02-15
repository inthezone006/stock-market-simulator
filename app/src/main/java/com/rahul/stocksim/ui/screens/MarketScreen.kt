package com.rahul.stocksim.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rahul.stocksim.data.FinnhubNewsArticle
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.NewsArticleItem
import com.rahul.stocksim.ui.components.StockRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    navController: NavController, 
    onStockClick: (Stock) -> Unit,
    onSettingsClick: () -> Unit
) {
    val marketRepository = remember { MarketRepository() }
    val context = LocalContext.current
    var stockList by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var portfolio by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var marketNews by remember { mutableStateOf<List<FinnhubNewsArticle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Load cached data instantly if available
    LaunchedEffect(Unit) {
        val cached = marketRepository.getWatchlistWithQuotes(forceRefresh = false)
        if (cached.isNotEmpty()) {
            stockList = cached
            isLoading = false
        }
        // Then trigger a background update
        coroutineScope.launch {
            val rawPortfolio = marketRepository.getPortfolio()
            portfolio = rawPortfolio.toMap()
            stockList = marketRepository.getWatchlistWithQuotes(forceRefresh = true)
            marketNews = marketRepository.getMarketNews()
            isLoading = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                val rawPortfolio = marketRepository.getPortfolio()
                portfolio = rawPortfolio.toMap()
                stockList = marketRepository.getWatchlistWithQuotes(forceRefresh = true)
                marketNews = marketRepository.getMarketNews()
                isRefreshing = false
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.TopCenter
    ) {
        if (isLoading && stockList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (stockList.isNotEmpty()) {
                    item {
                        Text(
                            "My Watchlist",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(stockList) { currentStock ->
                        StockRow(
                            stock = currentStock,
                            ownedQuantity = portfolio[currentStock.symbol] ?: 0L,
                            onRowClick = { stock -> onStockClick(stock) }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }

                if (marketNews.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Market News",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(marketNews) { article ->
                        NewsArticleItem(article) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                            context.startActivity(intent)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                if (stockList.isEmpty() && marketNews.isEmpty() && !isLoading) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Watchlist is empty. Search for stocks to add them!",
                                color = Color.Gray,
                                modifier = Modifier.padding(32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
