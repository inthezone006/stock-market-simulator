package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GuideScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        Text(
            text = "Simulator Guide",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                GuideSection(
                    title = "Getting Started",
                    icon = Icons.Default.RocketLaunch,
                    content = "Welcome to the Stock Market Simulator! Start by choosing your difficulty level.",
                    detailedContent = "Difficulty levels affect your starting cash. Beginner ($100,000) gives you plenty of room for error and experimentation, while Impossible ($100) requires extreme precision and market timing. Choose wisely, as this sets the baseline for your rank on the leaderboard."
                )
            }

            item {
                GuideSection(
                    title = "How to Trade",
                    icon = Icons.Default.SwapHoriz,
                    content = "Use the search bar at the top to find stocks and execute BUY or SELL orders.",
                    detailedContent = "Search by symbol (e.g., AAPL) or company name. On the detail screen, you'll see live prices and statistics. When you BUY, cash is deducted from your balance. When you SELL, the current market value is added back. Note: Market data is real-time but may vary slightly between exchanges."
                )
            }

            item {
                GuideSection(
                    title = "Analyst Sentiment",
                    icon = Icons.Default.Groups,
                    content = "Learn what professional analysts think about a stock's future performance.",
                    detailedContent = "The sentiment numbers come from major financial institutions (like JP Morgan, Goldman Sachs). They represent an aggregate of ratings: 'Strong Buy' and 'Buy' suggest a positive outlook, 'Hold' suggests neutral, and 'Sell' suggests analysts expect the price to drop. These are updated monthly."
                )
            }

            item {
                GuideSection(
                    title = "Technical Indicators (RSI)",
                    icon = Icons.Default.QueryStats,
                    content = "Use the RSI indicator to identify if a stock is overvalued or undervalued.",
                    detailedContent = "RSI (Relative Strength Index) is a momentum oscillator measured on a scale of 0 to 100. Generally, an RSI above 70 indicates a stock is 'Overbought' (may be due for a price drop), and an RSI below 30 indicates it is 'Oversold' (may be due for a price bounce)."
                )
            }

            item {
                GuideSection(
                    title = "Earnings & Similar Stocks",
                    icon = Icons.Default.Lightbulb,
                    content = "Discover new opportunities by looking at peer companies and earnings dates.",
                    detailedContent = "The 'Next Earnings' date tells you when a company will report its profits—this often leads to high volatility! 'Similar Stocks' shows you competitors in the same industry. Tapping on a similar stock lets you quickly jump to its data for comparison."
                )
            }

            item {
                GuideSection(
                    title = "Managing Your Portfolio",
                    icon = Icons.Default.AccountBalanceWallet,
                    content = "The Portfolio tab shows your total account value and active assets.",
                    detailedContent = "Total Account Value = Cash Balance + Total Equity Value. Your equity is calculated by multiplying your shares by the current market price. Use the Trade tab to see your full history, including positions you've sold out of entirely."
                )
            }

            item {
                GuideSection(
                    title = "Market Data Sources",
                    icon = Icons.Default.Memory,
                    content = "This app uses the Finnhub API and Firebase for a modern, real-time experience.",
                    detailedContent = "Finnhub provides the institutional-grade market data, analyst ratings, and news. Firebase Firestore stores your personal trading data securely. This combination ensures that the simulator behaves like a real trading platform while keeping your data safe."
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Good luck, Trader! 📈",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun GuideSection(title: String, icon: ImageVector, content: String, detailedContent: String) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1F1F)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.LightGray,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to learn more →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = Color(0xFF1F1F1F),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = detailedContent,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Got it!", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
