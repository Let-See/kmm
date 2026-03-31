pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LetSee-KMM"
includeBuild("convention-plugins")
include(":LetSeeCore")
include(":androidApp")
