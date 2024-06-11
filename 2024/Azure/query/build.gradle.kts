import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.6.11"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
}

group = "shokkaa.wjg.jp"
version = "1.0.3"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-serialization
    implementation("io.github.xxfast:kstore:0.8.0") // https://mvnrepository.com/artifact/io.github.xxfast/kstore
    implementation("io.github.xxfast:kstore-file:0.8.0") // https://mvnrepository.com/artifact/io.github.xxfast/kstore

    implementation("org.mongodb:mongo-java-driver:3.12.14") // https://mvnrepository.com/artifact/org.mongodb/mongo-java-driver

    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.23.1")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "MongoQuery"
            packageVersion = "$version"
            windows {
                menu = true
                shortcut = true
                upgradeUuid = "7f8982c0-95b2-136b-5dc8-335e005dd4fd"
            }
        }
    }
}
