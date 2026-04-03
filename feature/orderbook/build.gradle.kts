plugins {
    id("cointest.android.feature")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tydev.cointest.feature.orderbook"
}
