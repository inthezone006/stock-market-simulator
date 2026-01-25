package com.rahul.stocksim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rahul.stocksim.ui.theme.StockMarketSimulatorTheme

//MainActivity = starting point of app
//ComponentActivity = base class for all activities
class MainActivity : ComponentActivity() {
    //onCreate is first function called when app starts
    //if app was previously closed, then savedInstanceState
    //contains a Bundle of data from the last state
    override fun onCreate(savedInstanceState: Bundle?) {
        //call this function
        super.onCreate(savedInstanceState)
        //for modern devices to draw content under system bars too
        enableEdgeToEdge()
        //defines layout of activity with Composables
        setContent {
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

            //fundamental Compose component to provide background
            //it handles color and elevation clipping automatically
            Surface(
                //allows surface to expand and take up entire screen
                //and pushes down content to not be overlaid
                modifier = Modifier.fillMaxSize()
                    .safeDrawingPadding(),
                //sets background color
                color = MaterialTheme.colorScheme.background
            ) {
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
                        currentStock -> StockRow(stock = currentStock)
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
        }
    }
}

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