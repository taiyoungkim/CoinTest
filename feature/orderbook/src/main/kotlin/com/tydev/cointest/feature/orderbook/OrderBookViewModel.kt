package com.tydev.cointest.feature.orderbook

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tydev.cointest.domain.repository.OrderBookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OrderBookRepository,
) : ViewModel() {

    private val market: String = checkNotNull(savedStateHandle["market"])

    private val _uiState = MutableStateFlow(OrderBookUiState(market = market))
    val uiState = _uiState.asStateFlow()

    private val _sideEffect = Channel<OrderBookSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    private var collectJob: Job? = null

    init {
        startCollecting()
    }

    fun onIntent(intent: OrderBookIntent) {
        when (intent) {
            is OrderBookIntent.Retry -> {
                collectJob?.cancel()
                startCollecting()
            }
            is OrderBookIntent.NavigateBack -> {
                viewModelScope.launch {
                    _sideEffect.send(OrderBookSideEffect.NavigateBack)
                }
            }
        }
    }

    private fun startCollecting() {
        collectJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.getOrderBook(market)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _sideEffect.send(
                        OrderBookSideEffect.ShowError(e.message ?: "오류 발생"),
                    )
                }
                .collect { orderBook ->
                    _uiState.update {
                        it.copy(isLoading = false, orderBook = orderBook, error = null)
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        collectJob?.cancel()
    }
}
