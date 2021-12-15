import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0"
}

group = "me.kyoya"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

    mavenLocal()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
    implementation("com.charleskorn.kaml:kaml:0.38.0") // https://mvnrepository.com/artifact/com.charleskorn.kaml/kaml
    implementation("jp.pgw.shokkaa:snmp4jutils:1.1")
    implementation("org.snmp4j:snmp4j:3.6.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SnmpAgentDesktop"
            packageVersion = "1.0.0"
        }
    }
}