package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rahul.stocksim.model.Stock
import com.rahul.stocksim.ui.components.StockRow

//allow to use experimental material3
@OptIn(ExperimentalMaterial3Api::class)
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
        Stock("EBAY", "eBay Inc.", 15.00, 0.02),
        Stock("QCOM", "Qualcomm Inc.", 90.00, 0.12),
        Stock("CRM", "Salesforce.com Inc.", 180.00, 0.30),
    )

    Scaffold(
        //create top bar
        topBar = {
            //create center aligned top bar
            CenterAlignedTopAppBar(
                title = {
                    Text("Stock Market Simulator",
                    style = MaterialTheme.typography.titleLarge)
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
            )
            //create bottom action button
        }, floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO: Add stock logic */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add Stock")
            }
        }
    ) {
        //use provided inner padding values
        innerPadding ->
        //container that renders only items on screen efficiently
        LazyColumn(
            //takes up all the available width/height
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            //loops through collection, executing code for each
            //use itemsIndexed to get index of current item
            items(stockList) {
                //names current item in loop to reference
                //creating a defined StockRow for the current stock
                currentStock -> StockRow(
                    //passes in the current stock to stock row as
                    //a stock
                    stock = currentStock,
                    //and the on row click function to use
                    onRowClick = {
                        //that same stock is piped in to the
                        //on stock click command
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
}
