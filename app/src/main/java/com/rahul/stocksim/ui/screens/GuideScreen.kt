package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                GuideSection(
                    title = "Getting Started",
                    icon = Icons.Default.RocketLaunch,
                    content = "Welcome to the Stock Market Simulator! Start by choosing your difficulty level. This determines your initial virtual balance, ranging from $100 to $100,000. Your goal is to grow this capital through strategic trading."
                )
            }

            item {
                GuideSection(
                    title = "How to Trade",
                    icon = Icons.Default.SwapHoriz,
                    content = "Use the search bar at the top to find stocks by ticker symbol (e.g., AAPL) or company name. On the stock detail screen, you can see live prices and market stats. Use the 'BUY' and 'SELL' buttons to execute trades using your virtual balance."
                )
            }

            item {
                GuideSection(
                    title = "Managing Your Portfolio",
                    icon = Icons.Default.AccountBalanceWallet,
                    content = "The Portfolio tab shows your total account value, combining your cash balance and the real-time value of your stock holdings. The Trade tab keeps track of your active and past positions."
                )
            }

            item {
                GuideSection(
                    title = "How the Market Works",
                    icon = Icons.Default.TrendingUp,
                    content = "Stock prices fluctuate based on supply and demand, influenced by company news, economic reports, and global events. A ticker symbol is a unique code for a company. Prices shown are real-time quotes from the NASDAQ and other major exchanges."
                )
            }

            item {
                GuideSection(
                    title = "App Architecture",
                    icon = Icons.Default.Memory,
                    content = "This app uses the Finnhub API for live market data and news. Your profile, balance, and trading history are securely stored in Firebase Firestore, ensuring your simulation persists across sessions."
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
fun GuideSection(title: String, icon: ImageVector, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
        }
    }
}
