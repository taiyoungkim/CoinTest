package com.tydev.cointest.data.di

import com.tydev.cointest.data.monitor.ConnectivityNetworkMonitor
import com.tydev.cointest.data.repository.CoinRepositoryImpl
import com.tydev.cointest.data.repository.OrderBookRepositoryImpl
import com.tydev.cointest.domain.monitor.NetworkMonitor
import com.tydev.cointest.domain.repository.CoinRepository
import com.tydev.cointest.domain.repository.OrderBookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindCoinRepository(impl: CoinRepositoryImpl): CoinRepository

    @Binds
    @Singleton
    abstract fun bindOrderBookRepository(impl: OrderBookRepositoryImpl): OrderBookRepository

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor
}
