import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "jp.wjg.shokkaa.snmp-desktop"
version = "1.5.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
    implementation("com.charleskorn.kaml:kaml:0.57.0")  // https://mvnrepository.com/artifact/com.charleskorn.kaml/kaml
    implementation("org.snmp4j:snmp4j:3.7.8") // https://mvnrepository.com/artifact/org.snmp4j/snmp4j
    implementation("jp.wjg.shokkaa:snmp4jutils:1.6.0")  // local private library
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SnmpDesktop"
            packageVersion = "$version"
            windows {
                menu = true
                shortcut = true
                upgradeUuid = "836f0fc9-1179-4d4b-9eda-4e0b7513cd72"
            }
        }
    }
}
