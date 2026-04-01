package com.tydev.cointest.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MarketDto(
    @SerialName("market") val market: String,
    @SerialName("korean_name") val koreanName: String,
    @SerialName("english_name") val englishName: String,
)
