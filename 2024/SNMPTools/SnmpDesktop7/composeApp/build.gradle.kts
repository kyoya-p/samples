import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)

//    id("io.realm.kotlin") version "1.11.0"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
//            implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0")
//            implementation("org.mongodb:bson-kotlin:4.11.0")
//            implementation("io.realm.kotlin:library-base:1.11.0")
//            implementation("io.realm.kotlin:library-sync:1.11.0") // If using Device Sync
//            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0") // If using coroutines with the SDK

            implementation("org.snmp4j:snmp4j:3.7.8") // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
            implementation("jp.wjg.shokkaa:snmp4jutils:1.8.1")  // local private library
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}


compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SnmpDesktop"
            packageVersion = "1.7.0"
            windows {
                menu = true
                shortcut = true
                upgradeUuid = "836f0fc9-1179-4d4b-9eda-4e0b7513cd72"
            }
        }
    }
}

