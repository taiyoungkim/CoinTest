package com.tydev.cointest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tydev.cointest.feature.list.navigation.CoinListRoute
import com.tydev.cointest.feature.list.navigation.coinListScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = CoinListRoute,
            ) {
                coinListScreen(
                    onNavigateToOrderBook = { market ->
                        // TODO: OrderBook 화면 구현 후 연결
                    },
                )
            }
        }
    }
}
