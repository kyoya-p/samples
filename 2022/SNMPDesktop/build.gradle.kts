import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.0.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
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
    api(compose.preview)
    api(compose.runtime)
    api(compose.foundation)
    api(compose.material)
    implementation(compose.desktop.currentOs)
    implementation("jp.wjg.shokkaa:snmp4jutils:1.1")
    implementation("org.snmp4j:snmp4j:3.6.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SNMPDesktop"
            packageVersion = "1.0.0"
        }
    }
}