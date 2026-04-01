package com.tydev.cointest.domain.error

sealed interface NetworkError {
    data object Connectivity : NetworkError
    data class Server(val code: Int) : NetworkError
    data class Api(val message: String) : NetworkError
    data object Unknown : NetworkError
}
