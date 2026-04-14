package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.MarketRepository
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

    init {
        loadData(forceRefresh = false)
        loadContracts()
        loadExecutedContracts()
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
            // Settle option profit/loss manually if closed before expiration
            val quote = marketRepository.getStockQuote(contract.symbol)
            if (quote != null) {
                val currentPrice = quote.price
                val profitPerShare = if (contract.type == ContractType.CALL_OPTION) {
                    (currentPrice - contract.targetPrice).coerceAtLeast(0.0)
                } else {
                    (contract.targetPrice - currentPrice).coerceAtLeast(0.0)
                }
                
                // For simplicity, we just mark it as EXECUTED (or EXPIRED) and maybe adjust balance
                // In this simplified sim, we'll just cancel it to stop tracking, 
                // but real settlement happens in PriceAlertWorker for simplicity of logic.
                marketRepository.cancelTradeContract(contract.id)
                loadContracts()
                loadExecutedContracts()
            }
        }
    }

    private fun loadHistory() {
        // Portfolio history graph removed from UI
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
