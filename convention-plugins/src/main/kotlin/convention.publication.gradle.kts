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

// Grabbing secrets from local.properties file or from environment variables, which could be used on CI
val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["signing.keyId"] = System.getenv("SIGNING_KEY_ID")
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.secretKeyRingFile"] = System.getenv("SIGNING_SECRET_KEY_RING_FILE")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
    ext["centralPortalUsername"] = System.getenv("CENTRAL_PORTAL_USERNAME")
    ext["centralPortalPassword"] = System.getenv("CENTRAL_PORTAL_PASSWORD")
}
//val javadocJar by tasks.registering(Jar::class) {
//    archiveClassifier.set("javadoc")
//}

fun getExtraString(name: String) = ext[name]?.toString()

publishing {
    // Configure maven central repository
    repositories {
        maven {
            name = "centralPortalOssrhCompat"
            // Sonatype Central's OSSRH Staging API compatibility endpoint.
            setUrl("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                // Prefer Central Portal user-token credentials; fall back to legacy OSSRH keys.
                username = getExtraString("centralPortalUsername")
                    ?: getExtraString("ossrhUsername")
                password = getExtraString("centralPortalPassword")
                    ?: getExtraString("ossrhPassword")
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
    if (listOf(keyId, password, ringFile).all { !it.isNullOrBlank() }) {
        sign(publishing.publications)
    }
}
