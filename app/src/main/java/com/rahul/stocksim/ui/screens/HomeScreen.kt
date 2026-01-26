package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.StockRow

@Composable
fun HomeScreen(navController: NavController, onStockClick: (Stock) -> Unit) {
    //creates an immutable list of stocks

    val stockList = listOf(
        Stock("AAPL", "Apple Inc.", 150.00, 5.00),
        Stock("GOOGL", "Alphabet Inc.", 2500.00, 100.00),
        Stock("MSFT", "Microsoft Corporation", 200.00, 2.00),
        Stock("AMZN", "Amazon.com Inc.", 3000.00, 500.00),
        Stock("FB", "Facebook Inc.", 200.00, 1.00),
        Stock("TSLA", "Tesla Inc.", 500.00, 10.00),
        Stock("NFLX", "Netflix Inc.", 400.00, 2.00),
        Stock("NVDA", "Nvidia Corporation", 100.00, 0.50),
        Stock("AMD", "AMD Inc.", 80.00, 0.25),
        Stock("INTC", "Intel Corporation", 50.00, 0.10),
        Stock("PYPL", "PayPal Holdings Inc.", 30.00, 0.05),
        Stock("CSCO", "Cisco Systems Inc.", 70.00, 0.15),
        Stock("ADBE", "Adobe Inc.", 120.00, 0.20),
        Stock("EBAY", "eBay Inc.", 15.00, 0.02)
    )

    //container that renders only items on screen efficiently
    LazyColumn(
        //takes up all the available width/height
        modifier = Modifier.fillMaxSize()
    ) {
        //loops through collection, executing code for each
        //use itemsIndexed to get index of current item
        items(stockList) {
            //names current item in loop to reference
            //creating a defined StockRow for the current stock
            currentStock -> StockRow(
                //passes in the current stock to stockrow as
                //a stock
                stock = currentStock,
                //and the onrowclick function to use
                onRowClick = {
                    //that same stock is piped in to the
                    //onstockclick command
                    stock -> onStockClick(stock)
                }
            )
            //creates a divider between items
            HorizontalDivider(
                //thin line
                thickness = 0.5.dp,
                //color of line (70% transparent)
                color = Color.Gray.copy(alpha = 0.3f)
            )
        }
    }
}
