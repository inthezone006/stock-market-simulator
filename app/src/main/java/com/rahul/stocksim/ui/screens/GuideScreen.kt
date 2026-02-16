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
                    title = "The Life of a Stock",
                    icon = Icons.Default.Factory,
                    content = "How a private idea becomes a public asset traded by millions.",
                    detailedContent = "It starts with a Private Company. To grow, they seek 'Seeding' from Angel Investors. As they scale, 'Venture Capitalists' provide series A/B/C funding. Finally, the company goes through an IPO (Initial Public Offering) to list on exchanges like NASDAQ. At this point, the public (you!) can buy shares, which represent partial ownership of that company's future."
                )
            }

            item {
                GuideSection(
                    title = "How the Market Works",
                    icon = Icons.Default.Public,
                    content = "An invisible auction that never stops during business hours.",
                    detailedContent = "The Stock Market is essentially a giant auction house. For every buyer, there must be a seller. The 'Price' you see is the last agreed-upon value. If more people want to buy (Demand) than sell (Supply), the price goes up. News, earnings, and global events shift this balance every second."
                )
            }

            item {
                GuideSection(
                    title = "Portfolio Analytics",
                    icon = Icons.Default.QueryStats,
                    content = "Understanding your top performers and investment concentration.",
                    detailedContent = "Your Portfolio card now shows 'Concentration' (how much of your wealth is in one stock) and 'Diversification'. A 'High Diversification' means you've spread your risk across many companies. 'Day Return' shows your total profit or loss specifically for the current trading day."
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
                    icon = Icons.Default.LegendToggle,
                    content = "Use the RSI indicator to identify if a stock is overvalued or undervalued.",
                    detailedContent = "RSI (Relative Strength Index) is a momentum oscillator measured on a scale of 0 to 100. Generally, an RSI above 70 indicates a stock is 'Overbought' (may be due for a price drop), and an RSI below 30 indicates it is 'Oversold' (may be due for a price bounce)."
                )
            }

            item {
                GuideSection(
                    title = "Moving Averages (SMA)",
                    icon = Icons.Default.TrendingUp,
                    content = "Identifying long-term trends using 50-day and 200-day averages.",
                    detailedContent = "The SMA (Simple Moving Average) smooths out price spikes. A 'Bullish Cross' occurs when the short-term 50-day average moves above the long-term 200-day average, suggesting a strong upward trend. The opposite is a 'Bearish Cross'."
                )
            }

            item {
                GuideSection(
                    title = "ESG & Sustainability",
                    icon = Icons.Default.Eco,
                    content = "Invest ethically by checking a company's ESG scores.",
                    detailedContent = "ESG stands for Environmental, Social, and Governance. It measures a company's impact on the planet and its ethical standards. Higher scores suggest the company is well-managed and prepared for a sustainable future."
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Knowledge is the best asset! 📈",
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
