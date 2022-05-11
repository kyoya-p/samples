import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") // https://plugins.gradle.org/plugin/org.jetbrains.compose
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "jp.wjg.shokkaa"
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

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val jvmTest by getting
    }
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