plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
    id("convention.publication")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "LetSeeCore"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
            }
        }
    }
}

android {
    namespace = "io.github.letsee"
    compileSdk = 36
    defaultConfig {
        minSdk = 23
    }
}
