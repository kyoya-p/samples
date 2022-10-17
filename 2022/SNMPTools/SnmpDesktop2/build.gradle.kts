import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.7.20"
    id("org.jetbrains.compose") version "1.2.0"

    kotlin("plugin.serialization") version "1.5.31"
}

group = "jp.wjg.shokkaa"
version = "1.0.8"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    mavenLocal() // for jp.wjg.shokkaa:snmp4jutils
}

//kotlin {
//    jvm {
//        compilations.all { kotlinOptions.jvmTarget = "11" }
//        withJava()
//    }
//}

dependencies {
    implementation(compose.desktop.currentOs)

    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
    implementation("com.charleskorn.kaml:kaml:0.40.0") // https://github.com/charleskorn/kaml/releases/latest
    implementation("org.snmp4j:snmp4j:3.7.0")

    implementation("jp.wjg.shokkaa:snmp4jutils:1.2")  // local private library
}
// https://github.com/JetBrains/compose-jb/blob/master/tutorials/Native_distributions_and_local_execution/README.md

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SNMP Desktop"
            packageVersion = version.toString()
            windows {
                perUserInstall = true
                shortcut = true
                menu = true
                upgradeUuid = "836f0fc9-1179-4d4b-9eda-4e0b7513cd72"
            }
        }
    }
}