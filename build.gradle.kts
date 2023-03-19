
plugins {
    val agpVersion: String = "8.0.0-beta05"
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("$agpVersion").apply(false)
    id("com.android.library").version("$agpVersion").apply(false)
    kotlin("android").version("1.8.0").apply(false)
    kotlin("multiplatform").version("1.8.0").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
