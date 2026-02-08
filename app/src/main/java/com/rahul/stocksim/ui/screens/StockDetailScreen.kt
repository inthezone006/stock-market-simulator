package com.rahul.stocksim.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rahul.stocksim.data.FinnhubNewsArticle
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.data.StockPricePoint
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(stockSymbol: String?, onBackClick: () -> Unit) {
    val marketRepository = MarketRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var stock by remember { mutableStateOf<Stock?>(null) }
    var newsArticles by remember { mutableStateOf<List<FinnhubNewsArticle>>(emptyList()) }
    var history by remember { mutableStateOf<List<StockPricePoint>>(emptyList()) }
    
    var isLoading by remember { mutableStateOf(true) }
    var isGraphLoading by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(1) }
    var isInWatchlist by remember { mutableStateOf(false) }
    var ownedQuantity by remember { mutableLongStateOf(0L) }
    
    val refreshStockData = {
        if (stockSymbol != null) {
            coroutineScope.launch {
                stock = marketRepository.getStockQuote(stockSymbol)
                newsArticles = marketRepository.getCompanyNews(stockSymbol)
                val watchlist = marketRepository.getWatchlist()
                isInWatchlist = watchlist.any { it.symbol == stockSymbol }
                
                val portfolio = marketRepository.getPortfolio()
                ownedQuantity = portfolio.find { it.first == stockSymbol }?.second ?: 0L
                isLoading = false
            }
        }
    }

    val refreshGraph = {
        if (stockSymbol != null) {
            coroutineScope.launch {
                isGraphLoading = true
                history = marketRepository.getStockHistory(stockSymbol, "1D")
                isGraphLoading = false
            }
        }
    }

    LaunchedEffect(stockSymbol) {
        isLoading = true
        refreshStockData()
        refreshGraph()
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text(stockSymbol ?: "Stock Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (stock != null) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (isInWatchlist) {
                                    marketRepository.removeFromWatchlist(stock!!.symbol)
                                    isInWatchlist = false
                                } else {
                                    marketRepository.addToWatchlist(stock!!.symbol)
                                    isInWatchlist = true
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212), titleContentColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else if (stock != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF121212))
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = stock!!.symbol, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = "$${String.format("%.2f", stock!!.price)}", style = MaterialTheme.typography.displaySmall, color = if (stock!!.change >= 0) Color.Green else Color.Red)
                        Text(
                            text = "${if (stock!!.change >= 0) "+" else ""}${String.format("%.2f", stock!!.change)} (${String.format("%.2f", stock!!.percentChange)}%)",
                            color = if (stock!!.change >= 0) Color.Green else Color.Red,
                            fontSize = 16.sp
                        )
                    }
                }

                // --- STOCK GRAPH SECTION ---
                item {
                    Column(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            if (isGraphLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                            } else if (history.isNotEmpty()) {
                                StockLineChart(
                                    data = history,
                                    modifier = Modifier.fillMaxSize(),
                                    color = if (history.last().price >= history.first().price) Color.Green else Color.Red
                                )
                            }
                        }
                        // Description of the graph
                        Text(
                            text = "Last 24 Hours",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                if (ownedQuantity > 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Shares Owned", color = Color.Gray, fontSize = 12.sp)
                                    Text("$ownedQuantity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Equity Value", color = Color.Gray, fontSize = 12.sp)
                                    Text("$${String.format("%.2f", ownedQuantity * stock!!.price)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }

                // Trade Controls
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Quantity", color = Color.Gray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (quantity > 1) quantity-- }) { Icon(Icons.Default.Remove, null, tint = Color.White) }
                                Text(text = quantity.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp))
                                IconButton(onClick = { quantity++ }) { Icon(Icons.Default.Add, null, tint = Color.White) }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (marketRepository.buyStock(stock!!.symbol, quantity, stock!!.price).isSuccess) {
                                                Toast.makeText(context, "Purchase Successful", Toast.LENGTH_SHORT).show()
                                                refreshStockData()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                                ) { Text("BUY") }
                                if (ownedQuantity > 0) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                if (marketRepository.sellStock(stock!!.symbol, quantity, stock!!.price).isSuccess) {
                                                    Toast.makeText(context, "Sale Successful", Toast.LENGTH_SHORT).show()
                                                    refreshStockData()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                                        enabled = quantity <= ownedQuantity
                                    ) { Text("SELL") }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Market Stats", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("Open", "$${String.format("%.2f", stock!!.open)}")
                                StatItem("Prev Close", "$${String.format("%.2f", stock!!.prevClose)}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("High", "$${String.format("%.2f", stock!!.high)}")
                                StatItem("Low", "$${String.format("%.2f", stock!!.low)}")
                            }
                        }
                    }
                }

                if (newsArticles.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(text = "Company News", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    }
                    items(newsArticles) { article ->
                        NewsArticleItem(article) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun StockLineChart(data: List<StockPricePoint>, modifier: Modifier, color: Color) {
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val maxPrice = data.maxOf { it.price }
        val minPrice = data.minOf { it.price }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.1)
        
        val path = Path()
        data.forEachIndexed { index, point ->
            val x = index * (size.width / (data.size - 1))
            val y = size.height - ((point.price - minPrice) / priceRange * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun NewsArticleItem(article: FinnhubNewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = article.source.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(text = article.headline, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = article.summary, color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (article.image.isNotEmpty()) {
                Spacer(modifier = Modifier.width(12.dp))
                AsyncImage(model = article.image, contentDescription = null, modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            }
        }
    }
}
