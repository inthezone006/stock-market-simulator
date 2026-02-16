package com.rahul.stocksim.ui.screens

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rahul.stocksim.data.*
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper function to format dates from YYYY-MM-DD to MM/DD/YYYY
fun formatDate(inputDate: String?): String {
    if (inputDate == null) return "N/A"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val date = inputFormat.parse(inputDate)
        if (date != null) outputFormat.format(date) else inputDate
    } catch (e: Exception) {
        inputDate
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(stockSymbol: String?, navController: NavController, onBackClick: () -> Unit) {
    val marketRepository = MarketRepository()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var stock by remember { mutableStateOf<Stock?>(null) }
    var profile by remember { mutableStateOf<FinnhubProfileResponse?>(null) }
    var financials by remember { mutableStateOf<FinnhubFinancialsResponse?>(null) }
    var newsArticles by remember { mutableStateOf<List<FinnhubNewsArticle>>(emptyList()) }
    var history by remember { mutableStateOf<List<StockPricePoint>>(emptyList()) }
    
    var recommendations by remember { mutableStateOf<List<FinnhubRecommendationResponse>>(emptyList()) }
    var peers by remember { mutableStateOf<List<String>>(emptyList()) }
    var earnings by remember { mutableStateOf<FinnhubEarningsCalendarResponse?>(null) }
    var rsiData by remember { mutableStateOf<FinnhubIndicatorResponse?>(null) }
    var sma50Data by remember { mutableStateOf<FinnhubIndicatorResponse?>(null) }
    var sma200Data by remember { mutableStateOf<FinnhubIndicatorResponse?>(null) }
    var dividends by remember { mutableStateOf<List<FinnhubDividendResponse>>(emptyList()) }
    var newsSentiment by remember { mutableStateOf<FinnhubNewsSentimentResponse?>(null) }
    var marketStatus by remember { mutableStateOf<FinnhubMarketStatusResponse?>(null) }
    var esgScores by remember { mutableStateOf<FinnhubEsgResponse?>(null) }
    
    var isLoading by remember { mutableStateOf(true) }
    var isGraphLoading by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(1) }
    var isInWatchlist by remember { mutableStateOf(false) }
    var ownedQuantity by remember { mutableLongStateOf(0L) }
    
    var errorOccurred by remember { mutableStateOf(false) }

    val refreshStockData = {
        if (stockSymbol != null) {
            coroutineScope.launch {
                try {
                    val stockResult = marketRepository.getStockQuote(stockSymbol)
                    if (stockResult == null) {
                        errorOccurred = true
                        return@launch
                    }
                    stock = stockResult
                    
                    // Fetch data in parallel
                    launch { profile = marketRepository.getCompanyProfile(stockSymbol) }
                    launch { financials = marketRepository.getBasicFinancials(stockSymbol) }
                    launch { newsArticles = marketRepository.getCompanyNews(stockSymbol) }
                    launch { recommendations = marketRepository.getRecommendations(stockSymbol) }
                    launch { peers = marketRepository.getPeers(stockSymbol) }
                    launch { earnings = marketRepository.getEarningsCalendar(stockSymbol) }
                    launch { rsiData = marketRepository.getTechnicalIndicator(stockSymbol, "rsi") }
                    launch { sma50Data = marketRepository.getTechnicalIndicator(stockSymbol, "sma", 50) }
                    launch { sma200Data = marketRepository.getTechnicalIndicator(stockSymbol, "sma", 200) }
                    launch { dividends = marketRepository.getDividends(stockSymbol) }
                    launch { newsSentiment = marketRepository.getNewsSentiment(stockSymbol) }
                    launch { marketStatus = marketRepository.getMarketStatus() }
                    launch { if (stockResult.isCrypto == false && stockResult.isForex == false) esgScores = marketRepository.getEsgScores(stockSymbol) }
                    
                    val watchlist = marketRepository.getWatchlist()
                    isInWatchlist = watchlist.any { it.symbol == stockSymbol }
                    
                    val portfolio = marketRepository.getPortfolio()
                    ownedQuantity = portfolio.find { it.first == stockSymbol }?.second ?: 0L
                    isLoading = false
                } catch (e: Exception) {
                    errorOccurred = true
                }
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

    LaunchedEffect(errorOccurred) {
        if (errorOccurred) {
            Toast.makeText(context, "Market data unavailable for $stockSymbol. Please try again later.", Toast.LENGTH_LONG).show()
            onBackClick()
        }
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(stockSymbol ?: "Stock Details", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        marketStatus?.let { status ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (status.isOpen) Color.Green else Color.Red)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (status.isOpen) "Market Open" else "Market Closed",
                                    color = Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                },
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
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (profile?.logo?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = profile?.logo,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = stock!!.symbol, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                    if (stock?.isCrypto == true) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFA726).copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(text = "CRYPTO", color = Color(0xFFFFA726), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (stock?.isForex == true) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF2196F3).copy(alpha = 0.2f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                            Text(text = "FOREX", color = Color(0xFF2196F3), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Text(text = stock!!.name, style = MaterialTheme.typography.bodyLarge, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "$${String.format("%.2f", stock!!.price)}", style = MaterialTheme.typography.headlineSmall, color = if (stock!!.change >= 0) Color.Green else Color.Red, fontWeight = FontWeight.Bold)
                            Text(text = "${if (stock!!.change >= 0) "+" else ""}${String.format("%.2f", stock!!.change)} (${String.format("%.2f", stock!!.percentChange)}%)", color = if (stock!!.change >= 0) Color.Green else Color.Red, fontSize = 14.sp)
                        }
                    }
                }

                // Sentiment & Technical Indicators Row
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (newsSentiment?.sentiment != null) {
                                    val bullish = newsSentiment!!.sentiment?.bullishPercent ?: 0.0
                                    val sentimentColor = if (bullish > 50) Color.Green else Color.Red
                                    Icon(
                                        imageVector = if (bullish > 50) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = null,
                                        tint = sentimentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sentiment: ${String.format("%.0f%%", bullish)} Bullish",
                                        color = sentimentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            if (rsiData?.rsi?.isNotEmpty() == true) {
                                val currentRsi = rsiData!!.rsi!!.last()
                                val rsiStatus = when {
                                    currentRsi > 70 -> "Overbought"
                                    currentRsi < 30 -> "Oversold"
                                    else -> "Neutral"
                                }
                                val rsiColor = when {
                                    currentRsi > 70 -> Color.Red
                                    currentRsi < 30 -> Color.Green
                                    else -> Color.Gray
                                }
                                Row {
                                    Text("RSI: ", color = Color.Gray, fontSize = 12.sp)
                                    Text("${String.format("%.1f", currentRsi)} ($rsiStatus)", color = rsiColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        if (sma50Data?.sma?.isNotEmpty() == true && sma200Data?.sma?.isNotEmpty() == true) {
                            val currentSma50 = sma50Data!!.sma!!.last()
                            val currentSma200 = sma200Data!!.sma!!.last()
                            val trend = if (currentSma50 > currentSma200) "Bullish Cross" else "Bearish Cross"
                            val trendColor = if (currentSma50 > currentSma200) Color.Green else Color.Red
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "50D SMA: $${String.format("%.2f", currentSma50)}", color = Color.Gray, fontSize = 11.sp)
                                Text(text = trend, color = trendColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(text = "200D SMA: $${String.format("%.2f", currentSma200)}", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // --- STOCK GRAPH SECTION ---
                item {
                    Column(modifier = Modifier.fillMaxWidth().height(300.dp).padding(vertical = 16.dp)) {
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
                    }
                }

                // Analyst Recommendations
                if (recommendations.isNotEmpty()) {
                    item {
                        val rec = recommendations.first()
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Analyst Sentiment", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                RecItem("Buy", rec.buy + rec.strongBuy, Color.Green)
                                RecItem("Hold", rec.hold, Color.Gray)
                                RecItem("Sell", rec.sell + rec.strongSell, Color.Red)
                            }
                        }
                    }
                }

                // ESG Scores Section
                esgScores?.let { esg ->
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Sustainability (ESG)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("Total Score", esg.totalScore?.toString() ?: "N/A")
                                StatItem("Env", esg.environmentScore?.toString() ?: "N/A")
                                StatItem("Social", esg.socialScore?.toString() ?: "N/A")
                                StatItem("Gov", esg.governanceScore?.toString() ?: "N/A")
                            }
                        }
                    }
                }

                // Upcoming Earnings
                earnings?.earningsCalendar?.firstOrNull()?.let { nextEarnings ->
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F).copy(alpha = 0.5f))) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Next Earnings: ", color = Color.Gray, fontSize = 14.sp)
                                Text(formatDate(nextEarnings.date), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Dividends Section
                if (dividends.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Dividend History", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                dividends.take(3).forEach { div ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = formatDate(div.date), color = Color.Gray, fontSize = 14.sp)
                                        Text(text = "$${div.amount} per share", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                    }
                                    if (div != dividends.take(3).last()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                // Trade Controls
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            val unitLabel = when {
                                stock?.isCrypto == true -> "Units"
                                stock?.isForex == true -> "Lots"
                                else -> "Shares"
                            }
                            Text(unitLabel, color = Color.Gray)
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

                // Peers Section
                if (peers.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Similar Stocks", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(peers) { peerSymbol ->
                                SuggestionChip(
                                    onClick = { navController.navigate("details/$peerSymbol") },
                                    label = { Text(peerSymbol, color = Color.White) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color(0xFF1F1F1F))
                                )
                            }
                        }
                    }
                }

                // Financials & Stats Section
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F))) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Market Stats", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("Open", "$${String.format("%.2f", stock!!.open)}")
                                StatItem("Prev Close", "$${String.format("%.2f", stock!!.prevClose)}")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("High", "$${String.format("%.2f", stock!!.high)}")
                                StatItem("Low", "$${String.format("%.2f", stock!!.low)}")
                            }
                            
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("Market Cap", profile?.marketCapitalization?.let { "${String.format("%.2f", it / 1000)}B" } ?: "N/A")
                                StatItem("Shares Out.", profile?.shareOutstanding?.let { "${String.format("%.2f", it)}M" } ?: "N/A")
                            }
                            
                            if (financials?.metric != null) {
                                val metrics = financials!!.metric!!
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    StatItem("52W High", metrics["52WeekHigh"]?.let { if (it is Double) "$${String.format("%.2f", it)}" else "N/A" } ?: "N/A")
                                    StatItem("52W Low", metrics["52WeekLow"]?.let { if (it is Double) "$${String.format("%.2f", it)}" else "N/A" } ?: "N/A")
                                }
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
                        NewsArticleItem(article) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url))) }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun RecItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 12.sp)
        Text(text = value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun StockLineChart(data: List<StockPricePoint>, modifier: Modifier, color: Color) {
    val textPaint = remember {
        Paint().apply {
            this.color = Color.Gray.toArgb()
            this.textSize = 24f
            this.typeface = Typeface.DEFAULT
            this.textAlign = Paint.Align.RIGHT
        }
    }
    
    val timePaint = remember {
        Paint().apply {
            this.color = Color.Gray.toArgb()
            this.textSize = 22f
            this.typeface = Typeface.DEFAULT
            this.textAlign = Paint.Align.CENTER
        }
    }

    Canvas(modifier = modifier.padding(end = 48.dp, top = 16.dp, bottom = 32.dp)) {
        if (data.size < 2) return@Canvas
        
        val maxPrice = data.maxOf { it.price }
        val minPrice = data.minOf { it.price }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.1)
        
        // Draw Y-axis price labels
        val stepCount = 4
        for (i in 0..stepCount) {
            val price = maxPrice - (i * (priceRange / stepCount))
            val y = (i * (size.height / stepCount)).toFloat()
            drawContext.canvas.nativeCanvas.drawText("$${String.format("%.2f", price)}", size.width + 44.dp.toPx(), y + 8f, textPaint)
        }

        // Draw X-axis time labels
        val timeSdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStepCount = 3
        for (i in 0..timeStepCount) {
            val index = (i * (data.size - 1) / timeStepCount)
            val point = data[index]
            val x = i * (size.width / timeStepCount)
            val timeStr = timeSdf.format(Date(point.timestamp * 1000))
            drawContext.canvas.nativeCanvas.drawText(timeStr, x, size.height + 24.dp.toPx(), timePaint)
        }

        val chartPath = Path()
        data.forEachIndexed { index, point ->
            val x = index * (size.width / (data.size - 1))
            val y = size.height - ((point.price - minPrice) / priceRange * size.height).toFloat()
            if (index == 0) chartPath.moveTo(x, y) else chartPath.lineTo(x, y)
        }
        drawPath(path = chartPath, color = color, style = Stroke(width = 3.dp.toPx()))
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
