package com.rahul.stocksim.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rahul.stocksim.model.Stock

@Composable
fun StockRow(stock: Stock, onRowClick: (Stock) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onRowClick(stock) },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
              Column(
                  modifier = Modifier.weight(1f)
              ) {
                  Text(
                      text = stock.symbol,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color.White
                  )
                  Text(
                      text = stock.name,
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.Gray
                  )
              }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$${"%.2f".format(stock.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "${if (stock.change >= 0) "+" else ""}${stock.change}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFCF6679)
                )
            }
        }
    }
}
