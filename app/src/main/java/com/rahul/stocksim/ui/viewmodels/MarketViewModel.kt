package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.AssetFilter
import com.rahul.stocksim.data.FinnhubNewsArticle
import com.rahul.stocksim.data.MarketRepository
import com.rahul.stocksim.model.Stock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketViewModel @Inject constructor(
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
    val uiState: StateFlow<MarketUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData(forceRefresh = false)
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
                val stockList = marketRepository.getWatchlistWithQuotes(forceRefresh)
                val portfolio = marketRepository.getPortfolio().toMap()
                val marketNews = marketRepository.getMarketNews()
                
                _uiState.value = MarketUiState.Success(
                    stockList = stockList,
                    portfolio = portfolio,
                    marketNews = marketNews
                )
            } catch (e: Exception) {
                _uiState.value = MarketUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun searchStocks(query: String): Flow<List<Stock>> = flow {
        try {
            val results = marketRepository.searchStocks(query, AssetFilter.STOCKS)
            emit(results)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(
        val stockList: List<Stock>,
        val portfolio: Map<String, Long>,
        val marketNews: List<FinnhubNewsArticle>
    ) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}
