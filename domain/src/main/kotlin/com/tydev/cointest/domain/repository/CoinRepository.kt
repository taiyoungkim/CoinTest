package com.tydev.cointest.domain.repository

import com.tydev.cointest.domain.model.Coin

interface CoinRepository {
    suspend fun getCoins(): Result<List<Coin>>
}
