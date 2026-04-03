import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import java.util.*

plugins {
    `maven-publish`
    signing
}
// Stub secrets to let the project sync and build without the publication values set up
ext["signing.keyId"] = null
ext["signing.password"] = null
ext["signing.secretKeyRingFile"] = null
ext["signing.key"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null
ext["centralPortalUsername"] = null
ext["centralPortalPassword"] = null

val publishVersion: String by rootProject
val publishGroupId: String by rootProject

// Grabbing secrets from local.properties and environment variables (CI/local).
// local.properties values win; missing values fall back to env.
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
}

fun fallbackFromEnv(extKey: String, envKey: String) {
    val current = ext[extKey]?.toString()
    if (current.isNullOrBlank()) {
        ext[extKey] = System.getenv(envKey)
    }
}

fallbackFromEnv("signing.keyId", "SIGNING_KEY_ID")
fallbackFromEnv("signing.password", "SIGNING_PASSWORD")
fallbackFromEnv("signing.secretKeyRingFile", "SIGNING_SECRET_KEY_RING_FILE")
fallbackFromEnv("signing.key", "SIGNING_KEY")
fallbackFromEnv("ossrhUsername", "OSSRH_USERNAME")
fallbackFromEnv("ossrhPassword", "OSSRH_PASSWORD")
fallbackFromEnv("centralPortalUsername", "CENTRAL_PORTAL_USERNAME")
fallbackFromEnv("centralPortalPassword", "CENTRAL_PORTAL_PASSWORD")
//val javadocJar by tasks.registering(Jar::class) {
//    archiveClassifier.set("javadoc")
//}

fun getExtraString(name: String) = ext[name]?.toString()
fun firstNonBlank(vararg values: String?): String? = values.firstOrNull { !it.isNullOrBlank() }

publishing {
    // Configure maven central repository
    repositories {
        maven {
            name = "centralPortalOssrhCompat"
            // Sonatype Central's OSSRH Staging API compatibility endpoint.
            setUrl("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                // Prefer Central Portal user-token credentials; fall back to legacy OSSRH keys.
                username = firstNonBlank(
                    getExtraString("centralPortalUsername"),
                    getExtraString("ossrhUsername")
                )
                password = firstNonBlank(
                    getExtraString("centralPortalPassword"),
                    getExtraString("ossrhPassword")
                )
            }
        }
    }

    // Configure all publications
    publications.withType<MavenPublication> {
        // Stub javadoc.jar artifact
        // artifact(javadocJar.get())
        groupId = publishGroupId
        version = publishVersion
        artifactId = project.name
        // Provide artifacts information requited by Maven Central
        pom {
            name.set(project.name)
            description.set("LetSee provides an easy way to provide mock data to your iOS application. The main intention of having a library like this is to have a way to mock the response of requests on runtime in an easy way to be able to test all available scenarios without the need to rerun or change the code or put in the extra effort.")
            url.set("https://github.com/Let-See/LetSee-KMM")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("Jahanmanesh")
                    name.set("Farshad Jahanmanesh")
                    email.set("Farshad.Jahanmanesh@gmail.com")
                }
            }
            scm {
                url.set("https://github.com/Let-See/LetSee-KMM")
            }

        }
    }
}

// Signing artifacts. Only sign when all credentials are present (guards CI/local builds without keys).
signing {
    val keyId = findProperty("signing.keyId") as? String
    val password = findProperty("signing.password") as? String
    val ringFile = findProperty("signing.secretKeyRingFile") as? String
    val inMemoryKey = findProperty("signing.key") as? String

    when {
        // Preferred for CI: use ASCII-armored private key directly from env/secret.
        !inMemoryKey.isNullOrBlank() && !password.isNullOrBlank() -> {
            // Avoid key-id filtering issues in CI; use whatever key is provided.
            useInMemoryPgpKeys(inMemoryKey, password)
            sign(publishing.publications)
        }
        // Backward-compatible legacy ring-file flow.
        listOf(keyId, password, ringFile).all { !it.isNullOrBlank() } -> {
            // Gradle picks up signing.secretKeyRingFile/signing.keyId/signing.password properties.
            sign(publishing.publications)
        }
    }
}
