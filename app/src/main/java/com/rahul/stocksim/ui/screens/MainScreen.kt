package com.rahul.stocksim.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rahul.stocksim.data.AuthRepository
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.model.Stock
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(mainNavController: NavController, onStockClick: (Stock) -> Unit) {
    val bottomNavController = rememberNavController()
    val navItems = listOf(
        BottomNavItem.Portfolio,
        BottomNavItem.Market,
        BottomNavItem.Trade,
        BottomNavItem.Leaderboard,
        BottomNavItem.Guide
    )
    
    val authRepository = remember { AuthRepository() }
    val marketRepository = remember { MarketRepository() }
    val user = authRepository.currentUser
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // Debounce Job to prevent rate limiting
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = { 
                    searchQuery = it
                    searchJob?.cancel() // Cancel previous search request
                    
                    if (it.isNotEmpty()) {
                        searchJob = coroutineScope.launch {
                            delay(500) // Wait 500ms after user stops typing
                            isSearching = true
                            searchResults = marketRepository.searchStocks(it, nasdaqOnly = true)
                            isSearching = false
                        }
                    } else {
                        searchResults = emptyList()
                    }
                },
                onSearch = { 
                    focusManager.clearFocus()
                },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Search stocks...") },
                leadingIcon = { 
                    if (searchActive) {
                        IconButton(onClick = { searchActive = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchActive && searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                searchQuery = ""
                                searchResults = emptyList()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.DarkGray)
                                .clickable { mainNavController.navigate(Screen.Settings.route) },
                            contentAlignment = Alignment.Center
                        ) {
                            val photoUrl = user?.photoUrl
                            if (photoUrl != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(photoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = user?.email?.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!searchActive) Modifier.padding(horizontal = 16.dp, vertical = 8.dp) else Modifier),
                colors = SearchBarDefaults.colors(
                    containerColor = Color(0xFF1F1F1F),
                    inputFieldColors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            ) {
                if (isSearching) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color.White)
                }
                searchResults.forEach { stock ->
                    ListItem(
                        headlineContent = { Text(stock.symbol, color = Color.White) },
                        supportingContent = { Text(stock.name, color = Color.Gray) },
                        trailingContent = { Text("$${String.format("%.2f", stock.price)}", color = Color.White) },
                        modifier = Modifier.clickable {
                            searchActive = false
                            onStockClick(stock)
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        },
        bottomBar = {
            if (!searchActive) {
                NavigationBar(
                    containerColor = Color(0xFF1F1F1F),
                    contentColor = Color.White
                ) {
                    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    navItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    tint = if (selected) Color.White else Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    item.label,
                                    color = if (selected) Color.White else Color.Gray,
                                    fontSize = 10.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color(0xFF00796B).copy(alpha = 0.5f),
                                selectedIconColor = Color.White,
                                unselectedIconColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Portfolio.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavItem.Portfolio.route) {
                    PortfolioScreen(mainNavController)
                }
                composable(BottomNavItem.Market.route) {
                    MarketScreen(
                        navController = mainNavController,
                        onStockClick = onStockClick,
                        onSettingsClick = { mainNavController.navigate(Screen.Settings.route) }
                    )
                }
                composable(BottomNavItem.Trade.route) {
                    TradeScreen(mainNavController)
                }
                composable(BottomNavItem.Leaderboard.route) {
                    LeaderboardScreen(mainNavController)
                }
                composable(BottomNavItem.Guide.route) {
                    GuideScreen()
                }
            }
        }
    }
}