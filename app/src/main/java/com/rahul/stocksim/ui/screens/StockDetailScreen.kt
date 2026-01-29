package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

//allows me to use experimental features of m3 in app ui
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(stockSymbol: String?, onBackClick: () -> Unit) {
    //scaffold automatically places the top bar at top and provides padding
    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            //defines the top bar
            TopAppBar(
                //sets header title to stock details
                title = { Text("Stock Details", color = Color.White) },
                //includes nav icon (back arrow)
                navigationIcon = {
                    IconButton(
                        //when clicked, execute onbackclick
                        onClick = {
                            onBackClick()
                        }
                    ) {
                        //back arrow icon
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212),
                    titleContentColor = Color.White
                )
            )
        }
    ) { //innerPadding is provided by material design to ensure content
        //is not obscured by top bar
        innerPadding ->
        //arranges children vertically
        Column(
            //container fills the screen w/ padding
            //column takes up the whole screen while respecting top space
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF121212)),
            //center elements
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //for the detail (the symbol you are passing in), just show the symbol for now
            Text(
                text = "Details for",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            //display stocksymbol is passed in, Unknown if not
            Text(
                text = stockSymbol ?: "Unknown",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Market data and charts coming soon!",
                color = Color.LightGray
            )
        }
    }
}
