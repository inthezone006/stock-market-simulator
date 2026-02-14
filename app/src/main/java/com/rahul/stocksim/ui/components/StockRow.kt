package com.rahul.stocksim.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rahul.stocksim.model.Stock

@Composable
fun StockRow(stock: Stock, ownedQuantity: Long = 0, onRowClick: (Stock) -> Unit) {
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
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          text = stock.symbol,
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = Color.White
                      )
                      if (stock.isCrypto) {
                          Spacer(modifier = Modifier.width(8.dp))
                          Box(
                              modifier = Modifier
                                  .clip(RoundedCornerShape(4.dp))
                                  .background(Color(0xFFFFA726).copy(alpha = 0.2f)) // Orange color for crypto tag
                                  .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                              Text(
                                  text = "CRYPTO",
                                  color = Color(0xFFFFA726),
                                  fontSize = 10.sp,
                                  fontWeight = FontWeight.Bold
                              )
                          }
                      }
                      if (ownedQuantity > 0) {
                          Spacer(modifier = Modifier.width(8.dp))
                          Box(
                              modifier = Modifier
                                  .clip(RoundedCornerShape(4.dp))
                                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                  .padding(horizontal = 6.dp, vertical = 2.dp)
                          ) {
                              Text(
                                  text = "$ownedQuantity",
                                  color = MaterialTheme.colorScheme.primary,
                                  fontSize = 10.sp,
                                  fontWeight = FontWeight.Bold
                              )
                          }
                      }
                  }
                  Text(
                      text = stock.name,
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color.Gray,
                      maxLines = 1
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
                    text = "${if (stock.change >= 0) "+" else ""}${"%.2f".format(stock.change)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (stock.change >= 0) Color(0xFF4CAF50) else Color(0xFFCF6679)
                )
            }
        }
    }
}
