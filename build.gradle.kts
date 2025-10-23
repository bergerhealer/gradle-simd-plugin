plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.bergerkiller.gradle"
version = "1.0.0"

dependencies {
    compileOnly("com.github.johnrengelman.shadow:shadow:8.1.1")
}

gradlePlugin {
    plugins {
        create("simdPlugin") {
            id = "com.bergerkiller.simd"
            implementationClass = "com.bergerkiller.gradle.simd.SimdPlugin"
        }
    }
}

repositories {
    maven("https://jitpack.io")
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
