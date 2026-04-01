package com.tydev.cointest.feature.list.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tydev.cointest.feature.list.CoinListRoute
import kotlinx.serialization.Serializable

@Serializable
object CoinListRoute

fun NavGraphBuilder.coinListScreen(onNavigateToOrderBook: (String) -> Unit) {
    composable<CoinListRoute> {
        CoinListRoute(onNavigateToOrderBook = onNavigateToOrderBook)
    }
}
