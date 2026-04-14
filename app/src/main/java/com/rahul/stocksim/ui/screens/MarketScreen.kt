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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.rahul.stocksim.data.FinnhubNewsArticle
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.NewsArticleItem
import com.rahul.stocksim.ui.components.StockRow
import com.rahul.stocksim.ui.viewmodels.MarketUiState
import com.rahul.stocksim.ui.viewmodels.MarketViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    navController: NavController, 
    onStockClick: (Stock) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MarketViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = uiState) {
            is MarketUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is MarketUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (state.stockList.isNotEmpty()) {
                        item {
                            Text(
                                "My Watchlist",
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(state.stockList) { currentStock ->
                            StockRow(
                                stock = currentStock,
                                ownedQuantity = state.portfolio[currentStock.symbol] ?: 0L,
                                onRowClick = { stock -> onStockClick(stock) }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }

                    if (state.marketNews.isNotEmpty()) {
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
                        items(state.marketNews) { article ->
                            NewsArticleItem(article) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                context.startActivity(intent)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                    
                    if (state.stockList.isEmpty() && state.marketNews.isEmpty()) {
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
            is MarketUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = Color.Red)
                }
            }
        }
    }
}
