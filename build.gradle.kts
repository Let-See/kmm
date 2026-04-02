require(JavaVersion.current() >= JavaVersion.VERSION_17) {
    "JDK 17+ is required (found ${JavaVersion.current()}). AGP 9.1.0 + Gradle 9.4.1 need JDK 17."
}

plugins {
    alias(libs.plugins.android.application).apply(false)
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.kotlin.android).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.kotlin.serialization).apply(false)
    alias(libs.plugins.kotlin.compose).apply(false)
    alias(libs.plugins.jetbrainsCompose).apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
