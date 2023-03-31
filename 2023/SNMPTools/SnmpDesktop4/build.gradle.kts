import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.example"
version = "1.3.0"

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
                implementation("com.charleskorn.kaml:kaml:0.52.0")  // https://mvnrepository.com/artifact/com.charleskorn.kaml/kaml
                implementation("org.snmp4j:snmp4j:3.7.4") // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
                implementation("jp.wjg.shokkaa:snmp4jutils:1.2")  // local private library
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
            packageName = "SnmpDesktop"
            packageVersion = "1.3.1"
            windows {
                menu = true
                upgradeUuid = "836f0fc9-1179-4d4b-9eda-4e0b7513cd72"
            }
        }
    }
}
