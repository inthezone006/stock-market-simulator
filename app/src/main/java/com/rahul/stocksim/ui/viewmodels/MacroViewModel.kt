package com.rahul.stocksim.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.stocksim.data.FinnhubEconomicEntry
import com.rahul.stocksim.data.MarketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MacroViewModel @Inject constructor(
    private val marketRepository: MarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MacroUiState>(MacroUiState.Loading)
    val uiState: StateFlow<MacroUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadEconomicCalendar()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadEconomicCalendar()
            _isRefreshing.value = false
        }
    }

    private fun loadEconomicCalendar() {
        viewModelScope.launch {
            try {
                val calendar = marketRepository.getEconomicCalendar().filter { it.country == "US" }
                _uiState.value = MacroUiState.Success(calendar)
            } catch (e: Exception) {
                _uiState.value = MacroUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class MacroUiState {
    object Loading : MacroUiState()
    data class Success(val economicCalendar: List<FinnhubEconomicEntry>) : MacroUiState()
    data class Error(val message: String) : MacroUiState()
}
