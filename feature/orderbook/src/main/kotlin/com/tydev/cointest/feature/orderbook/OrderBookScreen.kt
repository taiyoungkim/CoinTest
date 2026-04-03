package com.tydev.cointest.feature.orderbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tydev.cointest.core.ui.ObserveAsEvents
import com.tydev.cointest.domain.model.OrderBook
import com.tydev.cointest.domain.model.OrderBookTicker
import com.tydev.cointest.domain.model.OrderBookUnit
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OrderBookRoute(
    onNavigateBack: () -> Unit,
    viewModel: OrderBookViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.sideEffect) { effect ->
        when (effect) {
            is OrderBookSideEffect.NavigateBack -> onNavigateBack()
            is OrderBookSideEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    OrderBookScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderBookScreen(
    uiState: OrderBookUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (OrderBookIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val priceFormat = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val sizeFormat = remember { DecimalFormat("#,##0.####") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val title = uiState.orderBook?.ticker?.let { ticker ->
                        "${ticker.koreanName} ${uiState.market}".trim()
                    } ?: uiState.market
                    Text(text = title)
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(OrderBookIntent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.orderBook == null -> {
                    ErrorContent(
                        message = uiState.error,
                        onRetry = { onIntent(OrderBookIntent.Retry) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                uiState.orderBook != null -> {
                    OrderBookContent(
                        orderBook = uiState.orderBook,
                        priceFormat = priceFormat,
                        sizeFormat = sizeFormat,
                    )
                }
                else -> {
                    EmptyContent(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun OrderBookContent(
    orderBook: OrderBook,
    priceFormat: NumberFormat,
    sizeFormat: DecimalFormat,
    modifier: Modifier = Modifier,
) {
    val ticker = orderBook.ticker
    val currentPriceColor = when {
        ticker.tradePrice > ticker.prevClosingPrice -> Color.Red
        ticker.tradePrice < ticker.prevClosingPrice -> Color.Blue
        else -> MaterialTheme.colorScheme.onSurface
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item(key = "header") {
            OrderBookHeader(
                ticker = ticker,
                priceFormat = priceFormat,
                currentPriceColor = currentPriceColor,
            )
        }

        items(
            items = orderBook.askUnits,
            key = { "ask_${it.price}" },
        ) { unit ->
            AskPriceItem(
                unit = unit,
                priceFormat = priceFormat,
                sizeFormat = sizeFormat,
            )
        }

        item(key = "current_price") {
            CurrentPriceItem(
                tradePrice = ticker.tradePrice,
                priceFormat = priceFormat,
                color = currentPriceColor,
            )
        }

        items(
            items = orderBook.bidUnits,
            key = { "bid_${it.price}" },
        ) { unit ->
            BidPriceItem(
                unit = unit,
                priceFormat = priceFormat,
                sizeFormat = sizeFormat,
            )
        }

        item(key = "total") {
            OrderBookTotalItem(
                totalAskSize = orderBook.totalAskSize,
                totalBidSize = orderBook.totalBidSize,
                sizeFormat = sizeFormat,
            )
        }
    }
}

@Composable
private fun OrderBookHeader(
    ticker: com.tydev.cointest.domain.model.OrderBookTicker,
    priceFormat: NumberFormat,
    currentPriceColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            text = priceFormat.format(ticker.tradePrice),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = currentPriceColor,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "고가",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = priceFormat.format(ticker.highPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                )
            }
            Column {
                Text(
                    text = "저가",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = priceFormat.format(ticker.lowPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Blue,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "거래대금",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${priceFormat.format((ticker.accTradePrice24h / 1_000_000).toLong())}백만",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    HorizontalDivider()
}

@Composable
private fun AskPriceItem(
    unit: OrderBookUnit,
    priceFormat: NumberFormat,
    sizeFormat: DecimalFormat,
    modifier: Modifier = Modifier,
) {
    val barColor = Color.Blue
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = barColor.copy(alpha = 0.15f),
                    size = Size(
                        width = size.width * unit.sizeRatio.toFloat(),
                        height = size.height,
                    ),
                    topLeft = Offset(
                        x = size.width * (1f - unit.sizeRatio.toFloat()),
                        y = 0f,
                    ),
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = sizeFormat.format(unit.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = priceFormat.format(unit.price),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Blue,
        )
    }
}

@Composable
private fun BidPriceItem(
    unit: OrderBookUnit,
    priceFormat: NumberFormat,
    sizeFormat: DecimalFormat,
    modifier: Modifier = Modifier,
) {
    val barColor = Color.Red
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    color = barColor.copy(alpha = 0.15f),
                    size = Size(
                        width = size.width * unit.sizeRatio.toFloat(),
                        height = size.height,
                    ),
                    topLeft = Offset(
                        x = size.width * (1f - unit.sizeRatio.toFloat()),
                        y = 0f,
                    ),
                )
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = priceFormat.format(unit.price),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Red,
        )
        Text(
            text = sizeFormat.format(unit.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CurrentPriceItem(
    tradePrice: Double,
    priceFormat: NumberFormat,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = priceFormat.format(tradePrice),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
    HorizontalDivider()
}

@Composable
private fun OrderBookTotalItem(
    totalAskSize: Double,
    totalBidSize: Double,
    sizeFormat: DecimalFormat,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "총 매도잔량",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = sizeFormat.format(totalAskSize),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Blue,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "총 매수잔량",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = sizeFormat.format(totalBidSize),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRetry) {
            Text("재시도")
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "호가 데이터가 없습니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OrderBookScreenPreview() {
    val orderBook = OrderBook(
        ticker = OrderBookTicker(
            koreanName = "비트코인",
            market = "KRW-BTC",
            tradePrice = 143_000_000.0,
            prevClosingPrice = 140_000_000.0,
            highPrice = 145_000_000.0,
            lowPrice = 139_000_000.0,
            accTradePrice24h = 500_000_000_000.0,
        ),
        askUnits = listOf(
            OrderBookUnit(price = 143_500_000.0, size = 0.5, sizeRatio = 0.8),
            OrderBookUnit(price = 143_200_000.0, size = 0.3, sizeRatio = 0.5),
        ),
        bidUnits = listOf(
            OrderBookUnit(price = 142_800_000.0, size = 0.6, sizeRatio = 1.0),
            OrderBookUnit(price = 142_500_000.0, size = 0.4, sizeRatio = 0.6),
        ),
        totalAskSize = 10.5,
        totalBidSize = 12.3,
    )
    MaterialTheme {
        OrderBookScreen(
            uiState = OrderBookUiState(
                market = "KRW-BTC",
                orderBook = orderBook,
                isLoading = false,
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {},
        )
    }
}
