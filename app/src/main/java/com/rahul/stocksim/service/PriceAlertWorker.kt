package com.rahul.stocksim.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.firebase.Timestamp
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.data.local.StockDao
import com.rahul.stocksim.model.ContractStatus
import com.rahul.stocksim.model.ContractType
import com.rahul.stocksim.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class PriceAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: MarketRepository,
    private val stockDao: StockDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val alerts = stockDao.getActivePriceAlerts()
            val contracts = repository.getPendingTradeContractsForCurrentUser()
            
            if (alerts.isEmpty() && contracts.isEmpty()) {
                syncAccountHistory()
                return@withContext Result.success()
            }

            val notificationHelper = NotificationHelper(applicationContext)

            // Handle Price Alerts
            for (alert in alerts) {
                val stock = repository.getStockQuote(alert.symbol)
                if (stock != null) {
                    val triggered = if (alert.isAbove) {
                        stock.price >= alert.targetPrice
                    } else {
                        stock.price <= alert.targetPrice
                    }

                    if (triggered) {
                        val direction = if (alert.isAbove) "above" else "below"
                        notificationHelper.showNotification(
                            title = "Price Alert: ${alert.symbol}",
                            message = "${stock.name} has reached $${stock.price}, which is $direction your target of $${alert.targetPrice}.",
                            notificationId = alert.id
                        )
                        // Disable alert after it triggers to avoid spamming
                        stockDao.updatePriceAlert(alert.copy(isEnabled = false))
                    }
                }
            }

            // Handle Trade Contracts (Limit Orders \u0026 Options)
            for (contract in contracts) {
                val stock = repository.getStockQuote(contract.symbol)
                if (stock != null) {
                    var isTriggered = false
                    var message = ""
                    var title = ""

                    when (contract.type) {
                        ContractType.BUY_AT -> {
                            if (stock.price <= contract.targetPrice) {
                                isTriggered = true
                                val result = repository.buyStock(contract.symbol, contract.quantity.toInt(), stock.price, contract.userId)
                                if (result.isSuccess) {
                                    title = "Limit Order Executed: ${contract.symbol}"
                                    message = "Bought ${contract.quantity} shares at $${stock.price} (Target: $${contract.targetPrice})"
                                }
                            }
                        }
                        ContractType.SELL_AT -> {
                            if (stock.price >= contract.targetPrice) {
                                isTriggered = true
                                val result = repository.sellStock(contract.symbol, contract.quantity.toInt(), stock.price, contract.userId)
                                if (result.isSuccess) {
                                    title = "Limit Order Executed: ${contract.symbol}"
                                    message = "Sold ${contract.quantity} shares at $${stock.price} (Target: $${contract.targetPrice})"
                                }
                            }
                        }
                        ContractType.CALL_OPTION -> {
                            val now = Timestamp.now()
                            if (contract.expirationDate != null && now.seconds >= contract.expirationDate.seconds) {
                                isTriggered = true
                                val diff = stock.price - contract.targetPrice
                                val profit = if (diff > 0) diff * 100 * contract.quantity else 0.0 // 1 contract = 100 shares
                                if (profit > 0) {
                                    repository.buyStock("OPTION_PROFIT", 1, -profit, contract.userId) // Negative cost = gain
                                    title = "Call Option Expired ITM: ${contract.symbol}"
                                    message = "Your ${contract.quantity} call option contract(s) expired in the money! Profit: $${String.format(Locale.US, "%.2f", profit)}"
                                } else {
                                    title = "Option Expired: ${contract.symbol}"
                                    message = "Your ${contract.quantity} call option contract(s) expired worthless."
                                }
                            }
                        }
                        ContractType.PUT_OPTION -> {
                            val now = Timestamp.now()
                            if (contract.expirationDate != null && now.seconds >= contract.expirationDate.seconds) {
                                isTriggered = true
                                val diff = contract.targetPrice - stock.price
                                val profit = if (diff > 0) diff * 100 * contract.quantity else 0.0
                                if (profit > 0) {
                                    repository.buyStock("OPTION_PROFIT", 1, -profit, contract.userId)
                                    title = "Put Option Expired ITM: ${contract.symbol}"
                                    message = "Your ${contract.quantity} put option contract(s) expired in the money! Profit: $${String.format(Locale.US, "%.2f", profit)}"
                                } else {
                                    title = "Option Expired: ${contract.symbol}"
                                    message = "Your ${contract.quantity} put option contract(s) expired worthless."
                                }
                            }
                        }
                    }

                    if (isTriggered) {
                        repository.updateTradeContract(contract.copy(status = if (message.contains("worthless")) ContractStatus.EXPIRED else ContractStatus.EXECUTED))
                        if (title.isNotEmpty()) {
                            notificationHelper.showNotification(
                                title = title,
                                message = message,
                                notificationId = contract.id.hashCode()
                            )
                        }
                    }
                }
            }
            
            // Weekly Account Value Sync
            syncAccountHistory()

            Result.success()
        } catch (e: Exception) {
            Log.e("PriceAlertWorker", "Error in background work", e)
            Result.retry()
        }
    }

    private suspend fun syncAccountHistory() {
        try {
            val userId = repository.getCurrentUserId() ?: return
            val balance = repository.getUserBalance().first()
            val portfolio = repository.getPortfolioWithQuotes(forceRefresh = true)
            val totalStockValue = portfolio.sumOf { it.first.price * it.second }
            val totalAccountValue = balance + totalStockValue

            if (totalAccountValue > 0) {
                repository.saveAccountValueHistory(userId, totalAccountValue)
            }
        } catch (e: Exception) {
            Log.e("PriceAlertWorker", "Error syncing account history", e)
        }
    }

    companion object {
        private const val WORK_NAME = "PriceAlertWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(
                15, TimeUnit.MINUTES // Minimum interval for periodic work
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
