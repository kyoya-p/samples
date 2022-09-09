import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.5.31"
    id("org.jetbrains.compose")
}

group = "jp.wjg.shokkaa"
version = "1.0.2"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal() // for jp.wjg.shokkaa:snmp4jutils
}

kotlin {
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "11" }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
                implementation(compose.desktop.windows_x64)
                implementation("com.charleskorn.kaml:kaml:0.40.0") // https://github.com/charleskorn/kaml/releases/latest
                implementation("org.snmp4j:snmp4j:3.6.3")

                implementation("jp.wjg.shokkaa:snmp4jutils:1.2")  // local private library
            }
        }
    }
}

// https://github.com/JetBrains/compose-jb/blob/master/tutorials/Native_distributions_and_local_execution/README.md

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi) //(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SNMPAgentDesktop"
            packageVersion = version.toString()
            windows {
                menuGroup = "SNMP Agent Desktop"
                upgradeUuid = "836f0fc9-1179-4d4b-9eda-4e0b7513cd72"
//                msiPackageVersion = "1.0.0"
//                exePackageVersion = "1.0.0"
            }
        }
    }
}
