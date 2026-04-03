package com.tydev.cointest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tydev.cointest.feature.list.navigation.CoinListRoute
import com.tydev.cointest.feature.list.navigation.coinListScreen
import com.tydev.cointest.feature.orderbook.navigation.OrderBookRoute
import com.tydev.cointest.feature.orderbook.navigation.orderBookScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = CoinListRoute,
                ) {
                    coinListScreen(
                        onNavigateToOrderBook = { market ->
                            navController.navigate(OrderBookRoute(market))
                        },
                    )
                    orderBookScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
