plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.bergerkiller.gradle"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("simdPlugin") {
            id = "com.bergerkiller.gradle.simd"
            implementationClass = "com.bergerkiller.gradle.simd.SimdPlugin"
        }
    }
}

tasks.withType<ValidatePlugins>().configureEach {
    failOnWarning.set(true)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

publishing {
    repositories {
        maven("https://ci.mg-dev.eu/plugin/repository/everything") {
            name = "MGDev"
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
