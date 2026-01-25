package com.rahul.stocksim.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rahul.stocksim.model.Stock

@Composable
fun StockRow(stock: Stock) {
    Row(
        modifier = Modifier
            .fillMaxWidth() //fill screen
            .padding(16.dp), //padding from edges of screen
        horizontalArrangement = Arrangement.SpaceBetween
        //ensures elements are evenly spaced to each side of screen
    ) {
        //left side
        Column {
            //create a column in this row to show symbol and name vertically
            //use text to edit text and style
            Text(text = stock.symbol, style = MaterialTheme.typography.titleLarge)
            Text(text = stock.name, style = MaterialTheme.typography.bodyMedium)
        }
        //right side
        Text(
            text = "$${"%.2f".format(stock.price)}",
            color = if (stock.change >= 0) Color.Green else Color.Red,
            style = MaterialTheme.typography.titleMedium
        )
    }
}