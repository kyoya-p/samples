import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.5.31"
    id("org.jetbrains.compose")
    //kotlin("jvm") version "1.7.0"
}

group = "jp.wjg.shokkaa"
version = "1.0"


repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal()
}


kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        @Suppress("UNUSED_VARIABLE") val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
                implementation(compose.desktop.currentOs)
                implementation("com.charleskorn.kaml:kaml:0.40.0") // https://github.com/charleskorn/kaml/releases/latest
                implementation("org.snmp4j:snmp4j:3.6.3")
                implementation("jp.wjg.shokkaa:snmp4jutils:1.1")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "SNMPAgentDesktop"
            packageVersion = "1.0.0"
            windows {
                menuGroup = "SNMP Agent Desktop"
            }
        }
    }
}

//dependencies {
//    implementation(kotlin("stdlib-jdk8"))
//}
//val compileKotlin: KotlinCompile by tasks
//compileKotlin.kotlinOptions {
//    jvmTarget = "1.8"
//}
//val compileTestKotlin: KotlinCompile by tasks
//compileTestKotlin.kotlinOptions {
//    jvmTarget = "1.8"
//}