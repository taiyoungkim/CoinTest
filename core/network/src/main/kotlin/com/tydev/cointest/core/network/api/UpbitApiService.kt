package com.tydev.cointest.core.network.api

import com.tydev.cointest.core.network.dto.MarketDto
import com.tydev.cointest.core.network.dto.TickerDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class UpbitApiService(private val client: HttpClient) {

    suspend fun getMarkets(): List<MarketDto> =
        client.get("v1/market/all").body()

    suspend fun getTickers(markets: String): List<TickerDto> =
        client.get("v1/ticker") {
            parameter("markets", markets)
        }.body()
}
