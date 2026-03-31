require(JavaVersion.current() >= JavaVersion.VERSION_17) {
    "JDK 17+ is required (found ${JavaVersion.current()}). AGP 9.1.0 + Gradle 9.4.1 need JDK 17."
}

plugins {
    val agpVersion: String = "9.1.0"
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version(agpVersion).apply(false)
    id("com.android.library").version(agpVersion).apply(false)
    kotlin("android").version("2.3.20").apply(false)
    kotlin("multiplatform").version("2.3.20").apply(false)
    kotlin("plugin.serialization").version("2.3.20").apply(false)
    kotlin("plugin.compose").version("2.3.20").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
