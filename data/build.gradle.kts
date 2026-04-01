plugins {
    id("cointest.android.library")
    id("cointest.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.tydev.cointest.data"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:network"))
    implementation(libs.ktor.client.core)
}
