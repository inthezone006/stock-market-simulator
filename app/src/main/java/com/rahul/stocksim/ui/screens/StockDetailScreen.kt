package com.rahul.stocksim.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(stockSymbol: String?, onBackClick: () -> Unit) {
    val marketRepository = MarketRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var stock by remember { mutableStateOf<Stock?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var quantity by remember { mutableIntStateOf(1) }
    var isInWatchlist by remember { mutableStateOf(false) }
    
    LaunchedEffect(stockSymbol) {
        if (stockSymbol != null) {
            isLoading = true
            stock = marketRepository.getStockQuote(stockSymbol)
            // Check if in watchlist
            val watchlist = marketRepository.getWatchlist()
            isInWatchlist = watchlist.any { it.symbol == stockSymbol }
            isLoading = false
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text(stockSymbol ?: "Stock Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (stock != null) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (isInWatchlist) {
                                    marketRepository.removeFromWatchlist(stock!!.symbol)
                                    isInWatchlist = false
                                    Toast.makeText(context, "Removed from Watchlist", Toast.LENGTH_SHORT).show()
                                } else {
                                    marketRepository.addToWatchlist(stock!!.symbol)
                                    isInWatchlist = true
                                    Toast.makeText(context, "Added to Watchlist", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (isInWatchlist) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Watchlist",
                                tint = if (isInWatchlist) Color.Red else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212))
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else if (stock != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stock!!.symbol,
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$${String.format("%.2f", stock!!.price)}",
                        style = MaterialTheme.typography.displaySmall,
                        color = if (stock!!.change >= 0) Color.Green else Color.Red
                    )
                    Text(
                        text = "${if (stock!!.change >= 0) "+" else ""}${String.format("%.2f", stock!!.change)}",
                        color = if (stock!!.change >= 0) Color.Green else Color.Red,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Trade Controls
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Quantity", color = Color.Gray)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(onClick = { if (quantity > 1) quantity-- }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.White)
                                }
                                Text(
                                    text = quantity.toString(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                IconButton(onClick = { quantity++ }) {
                                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val result = marketRepository.buyStock(stock!!.symbol, quantity, stock!!.price)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Purchase Successful", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Purchase Failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                ) {
                                    Text("BUY")
                                }
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val result = marketRepository.sellStock(stock!!.symbol, quantity, stock!!.price)
                                            if (result.isSuccess) {
                                                Toast.makeText(context, "Sale Successful", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Sale Failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000))
                                ) {
                                    Text("SELL")
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Stock not found",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}