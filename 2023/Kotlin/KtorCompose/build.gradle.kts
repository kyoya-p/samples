import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
//    id("com.github.johnrengelman.shadow") version "7.1.2" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    val ktor_version="2.3.3" // https://mvnrepository.com/artifact/io.ktor/ktor-server-core
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

    implementation("commons-daemon:commons-daemon:1.3.4")  // https://mvnrepository.com/artifact/commons-daemon/commons-daemon

    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "KtorCompose"
            packageVersion = "1.0.0"
            windows {
                menu = true
                this.shortcut = true
            }
        }
    }
}

