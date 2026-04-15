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
            // Fetch total account value history or use mock data
            val now = System.currentTimeMillis() / 1000
            val mockHistory = listOf(
                StockPricePoint(now - 86400 * 6, 95000.0),
                StockPricePoint(now - 86400 * 5, 97000.0),
                StockPricePoint(now - 86400 * 4, 96000.0),
                StockPricePoint(now - 86400 * 3, 102000.0),
                StockPricePoint(now - 86400 * 2, 105000.0),
                StockPricePoint(now - 86400 * 1, 103000.0),
                StockPricePoint(now, 108000.0)
            )
            _portfolioHistory.value = mockHistory
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadData(forceRefresh = true)
            _isRefreshing.value = false
        }
    }

    private fun loadData(forceRefresh: Boolean) {
        viewModelScope.launch {
            try {
                val portfolioItems = marketRepository.getPortfolioWithQuotes(forceRefresh)
                _uiState.value = PortfolioUiState.Success(portfolioItems)
                
                // Sync total account value if we have data
                val balance = marketRepository.getUserBalance().first()
                val totalStockValue = portfolioItems.sumOf { it.first.price * it.second }
                val totalAccountValue = balance + totalStockValue
                if (totalAccountValue > 0) {
                    marketRepository.syncTotalAccountValue(totalAccountValue)
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
