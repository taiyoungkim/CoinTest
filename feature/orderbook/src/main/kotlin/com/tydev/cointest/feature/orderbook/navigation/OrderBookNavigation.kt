package com.tydev.cointest.feature.orderbook.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tydev.cointest.feature.orderbook.OrderBookRoute
import kotlinx.serialization.Serializable

@Serializable
data class OrderBookRoute(val market: String)

fun NavGraphBuilder.orderBookScreen(onNavigateBack: () -> Unit) {
    composable<OrderBookRoute> {
        OrderBookRoute(onNavigateBack = onNavigateBack)
    }
}
