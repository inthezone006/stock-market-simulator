package com.rahul.stocksim.ui.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

data class LeaderboardUser(
    val id: String,
    val name: String,
    val balance: Double,
    val photoUrl: String? = null,
    val level: Int = 4
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(mainNavController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid
    
    var leaders by remember { mutableStateOf<List<LeaderboardUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedLevelFilter by remember { mutableIntStateOf(0) } // 0 = All Levels
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    val fetchLeaders = {
        coroutineScope.launch {
            errorMessage = null
            if (!isRefreshing) isLoading = true
            try {
                // Determine the query
                val baseQuery = if (selectedLevelFilter == 0) {
                    firestore.collection("users")
                        .orderBy("balance", Query.Direction.DESCENDING)
                } else {
                    firestore.collection("users")
                        .whereEqualTo("level", selectedLevelFilter)
                        .orderBy("balance", Query.Direction.DESCENDING)
                }

                val snapshot = baseQuery.limit(30).get().await()
                leaders = snapshot.documents.map { doc ->
                    LeaderboardUser(
                        id = doc.id,
                        name = doc.getString("displayName") ?: doc.getString("email")?.split("@")?.get(0) ?: "Trader",
                        balance = doc.getDouble("balance") ?: 0.0,
                        photoUrl = doc.getString("photoUrl"),
                        level = (doc.getLong("level") ?: 4L).toInt()
                    )
                }
            } catch (e: Exception) {
                Log.e("Leaderboard", "Query failed, falling back to local filter", e)
                
                // Fallback: Fetch all users and filter locally if the index isn't ready
                try {
                    val fallbackSnapshot = firestore.collection("users")
                        .orderBy("balance", Query.Direction.DESCENDING)
                        .limit(50)
                        .get().await()
                    
                    val allUsers = fallbackSnapshot.documents.map { doc ->
                        LeaderboardUser(
                            id = doc.id,
                            name = doc.getString("displayName") ?: doc.getString("email")?.split("@")?.get(0) ?: "Trader",
                            balance = doc.getDouble("balance") ?: 0.0,
                            photoUrl = doc.getString("photoUrl"),
                            level = (doc.getLong("level") ?: 4L).toInt()
                        )
                    }
                    
                    leaders = if (selectedLevelFilter == 0) {
                        allUsers
                    } else {
                        allUsers.filter { it.level == selectedLevelFilter }
                    }
                } catch (fallbackEx: Exception) {
                    leaders = emptyList()
                    errorMessage = "Leaderboard currently unavailable."
                }
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(selectedLevelFilter) {
        fetchLeaders()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            fetchLeaders()
        },
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = "Global Leaderboard",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Level Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                val levels = listOf(0, 1, 2, 3, 4, 5, 6, 7)
                levels.forEach { level ->
                    FilterChip(
                        selected = selectedLevelFilter == level,
                        onClick = { 
                            if (selectedLevelFilter != level) {
                                selectedLevelFilter = level
                            }
                        },
                        label = { Text(if (level == 0) "All" else "Level $level") },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1F1F1F),
                            labelColor = Color.Gray
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading && !isRefreshing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (leaders.isEmpty() && errorMessage == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selectedLevelFilter == 0) "No traders found." else "No traders found for Level $selectedLevelFilter.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(leaders) { index, user ->
                        LeaderCard(
                            rank = if (selectedLevelFilter == 0) index + 1 else 0, // Only show rank on Global
                            user = user, 
                            isCurrentUser = user.id == currentUserId
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderCard(rank: Int, user: LeaderboardUser, isCurrentUser: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) Color(0xFF2C2C2C) else Color(0xFF1F1F1F)
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isCurrentUser) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (rank > 0) {
                Text(
                    text = "#$rank",
                    color = when(rank) {
                        1 -> Color(0xFFFFD700) // Gold
                        2 -> Color(0xFFC0C0C0) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.Gray
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.width(40.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                if (user.photoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(user.name.firstOrNull()?.toString()?.uppercase() ?: "?", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name, 
                        color = Color.White, 
                        fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "👑", fontSize = 14.sp)
                    }
                }
                Text(text = "Level ${user.level}", color = Color.Gray, fontSize = 12.sp)
            }

            Text(
                text = "$${String.format("%,.0f", user.balance)}",
                color = if (isCurrentUser) MaterialTheme.colorScheme.primary else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
