package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.data.StockPricePoint
import com.rahul.stocksim.model.Stock
import dagger.hilt.android.lifecycle.HiltViewModel
import com.rahul.stocksim.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PortfolioUiState>(PortfolioUiState.Loading)
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val userBalance: Flow<Double> = marketRepository.getUserBalance()

    private val _contracts = MutableStateFlow<List<TradeContract>>(emptyList())
    val contracts: StateFlow<List<TradeContract>> = _contracts.asStateFlow()

    private val _executedContracts = MutableStateFlow<List<TradeContract>>(emptyList())
    val executedContracts: StateFlow<List<TradeContract>> = _executedContracts.asStateFlow()

    private val _portfolioHistory = MutableStateFlow<List<StockPricePoint>>(emptyList())
    val portfolioHistory: StateFlow<List<StockPricePoint>> = _portfolioHistory.asStateFlow()

    init {
        loadData(forceRefresh = false)
        loadContracts()
        loadExecutedContracts()
        loadHistory()
    }

    private fun loadContracts() {
        viewModelScope.launch {
            marketRepository.getTradeContracts(ContractStatus.PENDING)
                .catch { emit(emptyList()) }
                .collect {
                    _contracts.value = it
                }
        }
    }

    private fun loadExecutedContracts() {
        viewModelScope.launch {
            marketRepository.getTradeContracts(listOf(ContractStatus.EXECUTED, ContractStatus.CANCELLED))
                .catch { emit(emptyList()) }
                .collect {
                    _executedContracts.value = it
                }
        }
    }

    fun cancelContract(contractId: String) {
        viewModelScope.launch {
            marketRepository.cancelTradeContract(contractId)
            loadContracts()
            loadExecutedContracts()
        }
    }

    fun closeOptionPosition(contract: TradeContract) {
        viewModelScope.launch {
            val quote = marketRepository.getStockQuote(contract.symbol)
            if (quote != null) {
                marketRepository.settleOption(contract, quote.price)
                loadContracts()
                loadExecutedContracts()
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            marketRepository.getAccountValueHistory().collect { historyPairs ->
                // Convert Firestore pairs (ms) to StockPricePoints (s)
                val points = historyPairs.map { (timestampMs, value) ->
                    StockPricePoint(timestampMs / 1000, value)
                }
                _portfolioHistory.value = points
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData(forceRefresh = true)
            // Re-fetch history to get the latest synced point
            loadHistory()
            _isRefreshing.value = false
        }
    }

    private fun loadData(forceRefresh: Boolean) {
        viewModelScope.launch {
            try {
                val portfolioItems = marketRepository.getPortfolioWithQuotes(forceRefresh)
                
                // 1. Calculate current real-time values
                val balance = marketRepository.getUserBalance().first()
                val totalStockValue = portfolioItems.sumOf { it.first.price * it.second }
                val totalAccountValue = balance + totalStockValue
                
                _uiState.value = PortfolioUiState.Success(portfolioItems)
                
                // 2. Sync to Firestore if valid
                if (totalAccountValue > 0) {
                    marketRepository.syncTotalAccountValue(totalAccountValue)
                    
                    // 3. Update the local graph state immediately so it ends at the current value
                    val currentHistory = _portfolioHistory.value.toMutableList()
                    val now = System.currentTimeMillis() / 1000
                    
                    // Remove any existing point for "today" (seconds within last hour approx) 
                    // and add the precise latest value
                    if (currentHistory.isNotEmpty() && now - currentHistory.last().timestamp < 3600) {
                        currentHistory.removeAt(currentHistory.size - 1)
                    }
                    currentHistory.add(StockPricePoint(now, totalAccountValue))
                    _portfolioHistory.value = currentHistory
                }
            } catch (e: Exception) {
                _uiState.value = PortfolioUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class PortfolioUiState {
    object Loading : PortfolioUiState()
    data class Success(val portfolioItems: List<Pair<Stock, Long>>, val contracts: List<TradeContract> = emptyList()) : PortfolioUiState()
    data class Error(val message: String) : PortfolioUiState()
}
