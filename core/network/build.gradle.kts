plugins {
    id("cointest.android.library")
    id("cointest.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tydev.cointest.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
}
