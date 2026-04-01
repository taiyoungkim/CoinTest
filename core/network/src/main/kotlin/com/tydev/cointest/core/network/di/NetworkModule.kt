package com.tydev.cointest.core.network.di

import com.tydev.cointest.core.network.api.UpbitApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            }
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            url("https://api.upbit.com/")
            headers {
                append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }
    }

    @Provides
    @Singleton
    fun provideUpbitApiService(client: HttpClient): UpbitApiService =
        UpbitApiService(client)
}
