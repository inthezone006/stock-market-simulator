package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.rahul.stocksim.data.*
import com.rahul.stocksim.data.local.entity.PriceAlertEntity
import com.rahul.stocksim.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val marketRepository: MarketRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val symbol: String? = savedStateHandle["symbol"]

    private val _uiState = MutableStateFlow<StockDetailUiState>(StockDetailUiState.Loading)
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<List<StockPricePoint>>(emptyList())
    val history: StateFlow<List<StockPricePoint>> = _history.asStateFlow()

    private val _isGraphLoading = MutableStateFlow(false)
    val isGraphLoading: StateFlow<Boolean> = _isGraphLoading.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("1D")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _isInWatchlist = MutableStateFlow(false)
    val isInWatchlist: StateFlow<Boolean> = _isInWatchlist.asStateFlow()

    private val _ownedQuantity = MutableStateFlow(0L)
    val ownedQuantity: StateFlow<Long> = _ownedQuantity.asStateFlow()

    private val _priceAlerts = MutableStateFlow<List<PriceAlertEntity>>(emptyList())
    val priceAlerts: StateFlow<List<PriceAlertEntity>> = _priceAlerts.asStateFlow()

    private val _activeContracts = MutableStateFlow<List<TradeContract>>(emptyList())
    val activeContracts: StateFlow<List<TradeContract>> = _activeContracts.asStateFlow()

    val userBalance: Flow<Double> = marketRepository.getUserBalance()

    init {
        val stockSymbol = symbol
        if (stockSymbol != null) {
            observePersistentData(stockSymbol)
            refreshGraph(_selectedPeriod.value)
        }
        refreshData()
    }

    private fun observePersistentData(stockSymbol: String) {
        viewModelScope.launch {
            marketRepository.getPriceAlerts(stockSymbol).collect {
                _priceAlerts.value = it
            }
        }

        viewModelScope.launch {
            marketRepository.getTradeContracts(ContractStatus.PENDING)
                .map { contracts -> contracts.filter { it.symbol == stockSymbol } }
                .collect { _activeContracts.value = it }
        }
    }

    fun refreshData() {
        val stockSymbol = symbol ?: return
        viewModelScope.launch {
            _uiState.value = StockDetailUiState.Loading
            try {
                val stockResult = marketRepository.getStockQuote(stockSymbol)
                if (stockResult == null) {
                    _uiState.value = StockDetailUiState.Error("Stock not found")
                    return@launch
                }

                // Fetch data in parallel
                val profileFlow = flow { emit(marketRepository.getCompanyProfile(stockSymbol)) }
                val financialsFlow = flow { emit(marketRepository.getBasicFinancials(stockSymbol)) }
                val newsFlow = flow { emit(marketRepository.getCompanyNews(stockSymbol)) }
                val recsFlow = flow { emit(marketRepository.getRecommendations(stockSymbol)) }
                val peersFlow = flow { emit(marketRepository.getPeers(stockSymbol)) }
                val earningsFlow = flow { emit(marketRepository.getEarningsCalendar(stockSymbol)) }
                val rsiFlow = flow { emit(marketRepository.getTechnicalIndicator(stockSymbol, "rsi")) }
                val sma50Flow = flow { emit(marketRepository.getTechnicalIndicator(stockSymbol, "sma", 50)) }
                val sma200Flow = flow { emit(marketRepository.getTechnicalIndicator(stockSymbol, "sma", 200)) }
                val dividendsFlow = flow { emit(marketRepository.getDividends(stockSymbol)) }
                val marketStatusFlow = flow { emit(marketRepository.getMarketStatus()) }
                val priceTargetFlow = flow { emit(marketRepository.getPriceTarget(stockSymbol)) }
                val esgFlow = flow { 
                    if (!stockResult.isCrypto && !stockResult.isForex) 
                        emit(marketRepository.getEsgScores(stockSymbol))
                    else emit(null)
                }

                combine(
                    profileFlow, financialsFlow, newsFlow, recsFlow, peersFlow,
                    earningsFlow, rsiFlow, sma50Flow, sma200Flow, dividendsFlow,
                    marketStatusFlow, priceTargetFlow, esgFlow
                ) { results ->
                    val profile = results[0] as? FinnhubProfileResponse
                    val financials = results[1] as? FinnhubFinancialsResponse
                    @Suppress("UNCHECKED_CAST")
                    val news = results[2] as? List<FinnhubNewsArticle> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val recs = results[3] as? List<FinnhubRecommendationResponse> ?: emptyList()
                    @Suppress("UNCHECKED_CAST")
                    val peers = results[4] as? List<String> ?: emptyList()
                    val earnings = results[5] as? FinnhubEarningsCalendarResponse
                    val rsi = results[6] as? FinnhubIndicatorResponse
                    val sma50 = results[7] as? FinnhubIndicatorResponse
                    val sma200 = results[8] as? FinnhubIndicatorResponse
                    @Suppress("UNCHECKED_CAST")
                    val dividends = results[9] as? List<FinnhubDividendResponse> ?: emptyList()
                    val marketStatus = results[10] as? FinnhubMarketStatusResponse
                    val priceTarget = results[11] as? FinnhubPriceTargetResponse
                    val esg = results[12] as? FinnhubEsgResponse

                    val aiRec = marketRepository.analyzeStock(
                        stock = stockResult,
                        financials = financials,
                        priceTarget = priceTarget,
                        rsi = rsi?.rsi?.lastOrNull(),
                        sma50 = sma50?.sma?.lastOrNull(),
                        sma200 = sma200?.sma?.lastOrNull(),
                        sentiment = null,
                        analystRecs = recs.firstOrNull(),
                        news = news
                    )

                    StockDetailUiState.Success(
                        stock = stockResult,
                        profile = profile,
                        financials = financials,
                        newsArticles = news,
                        recommendations = recs,
                        peers = peers,
                        earnings = earnings,
                        rsiData = rsi,
                        sma50Data = sma50,
                        sma200Data = sma200,
                        dividends = dividends,
                        newsSentiment = null,
                        marketStatus = marketStatus,
                        esgScores = esg,
                        priceTarget = priceTarget,
                        aiRecommendation = aiRec
                    )
                }.collect {
                    _uiState.value = it
                }

                _isInWatchlist.value = marketRepository.getWatchlist().any { it.symbol == stockSymbol }
                _ownedQuantity.value = marketRepository.getPortfolio().find { it.first == stockSymbol }?.second ?: 0L
                
                refreshGraph("1D")

            } catch (e: Exception) {
                _uiState.value = StockDetailUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refreshGraph(period: String) {
        val stockSymbol = symbol ?: return
        _selectedPeriod.value = period
        viewModelScope.launch {
            _isGraphLoading.value = true
            _history.value = marketRepository.getStockHistory(stockSymbol, period)
            _isGraphLoading.value = false
        }
    }

    fun toggleWatchlist() {
        val stockSymbol = symbol ?: return
        viewModelScope.launch {
            if (_isInWatchlist.value) {
                marketRepository.removeFromWatchlist(stockSymbol)
                _isInWatchlist.value = false
            } else {
                marketRepository.addToWatchlist(stockSymbol)
                _isInWatchlist.value = true
            }
        }
    }

    suspend fun buyStock(quantity: Int, price: Double): Result<Double> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        val result = marketRepository.buyStock(stockSymbol, quantity, price)
        if (result.isSuccess) {
            _ownedQuantity.value = marketRepository.getPortfolio().find { it.first == stockSymbol }?.second ?: 0L
        }
        return result
    }

    suspend fun sellStock(quantity: Int, price: Double): Result<Unit> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        val result = marketRepository.sellStock(stockSymbol, quantity, price)
        if (result.isSuccess) {
            _ownedQuantity.value = marketRepository.getPortfolio().find { it.first == stockSymbol }?.second ?: 0L
        }
        return result
    }

    suspend fun createContract(type: ContractType, targetPrice: Double, quantity: Long): Result<Unit> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        val contract = TradeContract(
            symbol = stockSymbol,
            type = type,
            targetPrice = targetPrice,
            quantity = quantity,
            status = ContractStatus.PENDING,
            createdAt = Timestamp.now()
        )
        val res = marketRepository.createTradeContract(contract)
        return res
    }

    suspend fun buyOption(isCall: Boolean, strikePrice: Double, premium: Double, contracts: Int): Result<Unit> {
        val stockSymbol = symbol ?: return Result.failure(Exception("No symbol"))
        
        // Option contracts are usually 100 shares each
        val totalCost = premium * 100 * contracts
        
        // Check balance first (Wait for first value from flow)
        val balance = marketRepository.getUserBalance().first()
        if (balance < totalCost) return Result.failure(Exception("Insufficient balance for premium"))

        // Create the contract
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 30) // Default 30-day expiration
        
        val contract = TradeContract(
            symbol = stockSymbol,
            type = if (isCall) ContractType.CALL_OPTION else ContractType.PUT_OPTION,
            targetPrice = strikePrice,
            quantity = contracts.toLong(), // Represents number of contracts (each 100 shares)
            status = ContractStatus.PENDING,
            createdAt = Timestamp.now(),
            premium = premium,
            expirationDate = Timestamp(calendar.time)
        )
        
        return marketRepository.createTradeContract(contract)
    }

    fun cancelContract(contractId: String) {
        viewModelScope.launch {
            marketRepository.cancelTradeContract(contractId)
        }
    }

    fun addPriceAlert(targetPrice: Double, isAbove: Boolean) {
        val stockSymbol = symbol ?: return
        viewModelScope.launch {
            marketRepository.addPriceAlert(
                PriceAlertEntity(
                    symbol = stockSymbol,
                    targetPrice = targetPrice,
                    isAbove = isAbove
                )
            )
        }
    }

    fun deletePriceAlert(alert: PriceAlertEntity) {
        viewModelScope.launch {
            marketRepository.deletePriceAlert(alert)
        }
    }
}

sealed class StockDetailUiState {
    object Loading : StockDetailUiState()
    data class Success(
        val stock: Stock,
        val profile: FinnhubProfileResponse?,
        val financials: FinnhubFinancialsResponse?,
        val newsArticles: List<FinnhubNewsArticle>,
        val recommendations: List<FinnhubRecommendationResponse>,
        val peers: List<String>,
        val earnings: FinnhubEarningsCalendarResponse?,
        val rsiData: FinnhubIndicatorResponse?,
        val sma50Data: FinnhubIndicatorResponse?,
        val sma200Data: FinnhubIndicatorResponse?,
        val dividends: List<FinnhubDividendResponse>,
        val newsSentiment: FinnhubNewsSentimentResponse?,
        val marketStatus: FinnhubMarketStatusResponse?,
        val esgScores: FinnhubEsgResponse?,
        val priceTarget: FinnhubPriceTargetResponse?,
        val aiRecommendation: AIRecommendation?
    ) : StockDetailUiState()
    data class Error(val message: String) : StockDetailUiState()
}
