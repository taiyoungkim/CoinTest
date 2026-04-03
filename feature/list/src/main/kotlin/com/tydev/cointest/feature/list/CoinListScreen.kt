package com.tydev.cointest.feature.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tydev.cointest.core.ui.ObserveAsEvents
import com.tydev.cointest.domain.model.Coin
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CoinListRoute(
    onNavigateToOrderBook: (String) -> Unit,
    viewModel: CoinListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(flow = viewModel.sideEffect) { effect ->
        when (effect) {
            is CoinListSideEffect.NavigateToOrderBook -> onNavigateToOrderBook(effect.market)
            is CoinListSideEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    CoinListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinListScreen(
    uiState: CoinListUiState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CoinListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("거래소") })
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
                uiState.errorMessage != null && uiState.coins.isEmpty() -> {
                    ErrorContent(
                        message = uiState.errorMessage,
                        onRetry = { onIntent(CoinListIntent.LoadCoins) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                uiState.coins.isEmpty() -> {
                    EmptyContent(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { onIntent(CoinListIntent.Refresh) },
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                CoinListHeader()
                            }
                            items(
                                items = uiState.coins,
                                key = { coin -> coin.symbol },
                            ) { coin ->
                                CoinListItem(
                                    coin = coin,
                                    onClick = { onIntent(CoinListIntent.OnCoinClick(coin.market)) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoinListHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = "종목명",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "현재가",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
        Text(
            text = "전일대비",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
    HorizontalDivider()
}

@Composable
private fun CoinListItem(
    coin: Coin,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val changeColor = when (coin.change) {
        "RISE" -> Color.Red
        "FALL" -> Color.Blue
        else -> MaterialTheme.colorScheme.onSurface
    }
    val priceFormat = NumberFormat.getNumberInstance(Locale.KOREA)
    val rateText = "%+.2f%%".format(coin.signedChangeRate * 100)
    val priceChangeText = "%+,.0f".format(coin.signedChangePrice)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = coin.koreanName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = coin.symbol,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = priceFormat.format(coin.tradePrice),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = changeColor,
            textAlign = TextAlign.End,
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = rateText,
                style = MaterialTheme.typography.bodySmall,
                color = changeColor,
            )
            Text(
                text = priceChangeText,
                style = MaterialTheme.typography.labelSmall,
                color = changeColor,
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
            text = "표시할 종목이 없습니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CoinListItemPreview() {
    val coin = Coin(
        market = "KRW-BTC",
        koreanName = "비트코인",
        symbol = "BTC",
        tradePrice = 143_000_000.0,
        signedChangeRate = 0.0235,
        signedChangePrice = 3_000_000.0,
        change = "RISE",
        accTradePrice24h = 500_000_000_000.0,
        highPrice = 145_000_000.0,
        lowPrice = 140_000_000.0,
        prevClosingPrice = 140_000_000.0,
    )
    CoinListItem(coin = coin, onClick = {})
}
