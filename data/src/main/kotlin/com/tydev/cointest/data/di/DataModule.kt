package com.tydev.cointest.data.di

import com.tydev.cointest.data.repository.CoinRepositoryImpl
import com.tydev.cointest.domain.repository.CoinRepository
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
}
