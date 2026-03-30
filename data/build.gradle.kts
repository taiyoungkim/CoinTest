plugins {
    id("cointest.android.library")
    id("cointest.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:network"))
}
