package com.rahul.stocksim.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.model.Achievement
import com.rahul.stocksim.model.Stock
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.*
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

class StockWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun marketRepository(): MarketRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
        val repository = entryPoint.marketRepository()

        // Fetch data
        val watchlist = try { repository.getWatchlistWithQuotes(forceRefresh = false) } catch (e: Exception) { emptyList() }
        val balance = try { repository.getUserBalance().first() } catch (e: Exception) { 0.0 }
        val portfolioHistory = try { repository.getAccountValueHistory().first() } catch (e: Exception) { emptyList() }

        provideContent {
            val prefs = currentState<Preferences>()
            val modeStr = prefs[WidgetKeys.MODE] ?: WidgetMode.WATCHLIST.name
            val mode = try { WidgetMode.valueOf(modeStr) } catch (e: Exception) { WidgetMode.WATCHLIST }

            WidgetContainer(mode, watchlist, balance, portfolioHistory)
        }
    }

    @Composable
    private fun WidgetContainer(
        mode: WidgetMode,
        watchlist: List<Stock>,
        balance: Double,
        history: List<Pair<Long, Double>>
    ) {
        // Use a Box to wrap everything and make it clickable
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .clickable(actionRunCallback<ToggleModeAction>())
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp), // Increased padding for corners/edges
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(
                        text = "TradeSim",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFBB86FC)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = mode.label,
                        style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 14.sp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(4.dp))

                Box(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    when (mode) {
                        WidgetMode.WATCHLIST -> WatchlistContent(watchlist)
                        WidgetMode.PORTFOLIO -> PortfolioContent(balance, history)
                        WidgetMode.SPOTLIGHT -> SpotlightContent(watchlist)
                    }
                }

                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = "Tap to cycle",
                        style = TextStyle(color = ColorProvider(Color.DarkGray), fontSize = 10.sp)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Row {
                        WidgetMode.entries.forEach { m ->
                            Box(
                                modifier = GlanceModifier
                                    .size(6.dp)
                                    .padding(horizontal = 2.dp)
                                    .background(if (m == mode) Color(0xFFBB86FC) else Color.DarkGray)
                            ) {}
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WatchlistContent(stocks: List<Stock>) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 20.dp), // Pull items left and right closer together
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            if (stocks.isEmpty()) {
                Text("Watchlist empty", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 18.sp))
            } else {
                stocks.take(3).forEach { stock ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = stock.symbol,
                            style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 20.sp),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        val color = if (stock.change >= 0) Color.Green else Color.Red
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", stock.price)}",
                            style = TextStyle(color = ColorProvider(color), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PortfolioContent(balance: Double, history: List<Pair<Long, Double>>) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text("Available Cash", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 14.sp))
            Text(
                text = "$${String.format(Locale.US, "%,.2f", balance)}",
                style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 28.sp)
            )
            
            if (history.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(16.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(50.dp),
                    verticalAlignment = Alignment.Vertical.Bottom,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    val recentHistory = history.takeLast(12)
                    val maxOfHistory = recentHistory.maxOfOrNull { it.second } ?: 1.0
                    val minOfHistory = recentHistory.minOfOrNull { it.second } ?: 0.0
                    val range = (maxOfHistory - minOfHistory).coerceAtLeast(1.0)

                    recentHistory.forEach { point ->
                        val heightFactor = ((point.second - minOfHistory) / range).toFloat()
                        val barHeight = (heightFactor * 50).coerceAtLeast(4f).toInt()
                        Box(
                            modifier = GlanceModifier
                                .width(7.dp)
                                .height(barHeight.dp)
                                .padding(horizontal = 1.dp)
                                .background(Color(0xFFBB86FC).copy(alpha = 0.6f))
                        ) {}
                    }
                }
            }
        }
    }

    @Composable
    private fun SpotlightContent(stocks: List<Stock>) {
        val topMover = stocks.maxByOrNull { kotlin.math.abs(it.percentChange) }

        if (topMover != null) {
            Column(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "Top Mover",
                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 12.sp)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = topMover.symbol,
                    style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                )
                val color = if (topMover.change >= 0) Color.Green else Color.Red
                Text(
                    text = "${if (topMover.change >= 0) "+" else ""}${String.format(Locale.US, "%.2f%%", topMover.percentChange)}",
                    style = TextStyle(color = ColorProvider(color), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                )
                Text(
                    text = "$${String.format(Locale.US, "%.2f", topMover.price)}",
                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 14.sp)
                )
            }
        } else {
            Text("No data available", style = TextStyle(color = ColorProvider(Color.Gray)))
        }
    }
}

enum class WidgetMode(val label: String) {
    WATCHLIST("Watchlist"),
    PORTFOLIO("Account"),
    SPOTLIGHT("Spotlight")
}

object WidgetKeys {
    val MODE = stringPreferencesKey("widget_mode")
}

class ToggleModeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        Log.d("StockWidget", "ToggleModeAction triggered")
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val current = prefs[WidgetKeys.MODE] ?: WidgetMode.WATCHLIST.name
            Log.d("StockWidget", "Current mode: $current")
            val next = when (try { WidgetMode.valueOf(current) } catch (e: Exception) { WidgetMode.WATCHLIST }) {
                WidgetMode.WATCHLIST -> WidgetMode.PORTFOLIO
                WidgetMode.PORTFOLIO -> WidgetMode.SPOTLIGHT
                WidgetMode.SPOTLIGHT -> WidgetMode.WATCHLIST
            }
            Log.d("StockWidget", "Next mode: ${next.name}")
            prefs.toMutablePreferences().apply {
                this[WidgetKeys.MODE] = next.name
            }
        }
        StockWidget().update(context, glanceId)
    }
}
